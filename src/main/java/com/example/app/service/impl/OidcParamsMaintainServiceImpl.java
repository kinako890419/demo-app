package com.example.app.service.impl;

import com.example.app.model.OidcParams;
import com.example.app.service.OidcParamsMaintainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Keeps state, nonce and PKCE verifier in Redis as {@code oidc:state:{state}} for 5 minutes. */
@Service
@Slf4j
@RequiredArgsConstructor
public class OidcParamsMaintainServiceImpl implements OidcParamsMaintainService {

    private final StringRedisTemplate redisTemplate;

    private static final String KEY_PREFIX = "oidc:state:";
    private static final String NONCE_FIELD = "nonce";
    private static final String CODE_VERIFIER_FIELD = "codeVerifier";
    private static final Duration TTL = Duration.ofMinutes(5);

    @Override
    public void store(String state, String nonce, String codeVerifier) {
        String key = KEY_PREFIX + state;
        Map<String, String> fields = new HashMap<>();
        fields.put(NONCE_FIELD, nonce);
        if (StringUtils.isNotBlank(codeVerifier)) {
            fields.put(CODE_VERIFIER_FIELD, codeVerifier);
        }
        redisTemplate.opsForHash().putAll(key, fields);
        redisTemplate.expire(key, TTL);
        log.debug("Stored OIDC state: {}", state);
    }

    @Override
    public Optional<OidcParams> consumeParams(String state) {
        String key = KEY_PREFIX + state;
        // read and delete in one transaction, so two callbacks with the same state cannot both succeed
        List<Object> results = redisTemplate.execute(new SessionCallback<>() {
            @Override
            @SuppressWarnings({"unchecked", "rawtypes"})
            public List<Object> execute(RedisOperations ops) {
                ops.multi();
                ops.opsForHash().entries(key);
                ops.delete(key);
                return ops.exec();
            }
        });

        @SuppressWarnings("unchecked")
        Map<Object, Object> entries = results == null || results.isEmpty()
                ? Map.of() : (Map<Object, Object>) results.getFirst();
        if (entries.isEmpty()) {
            return Optional.empty();
        }
        log.debug("Consumed OIDC state: {}", state);

        return Optional.of(OidcParams.builder()
                .nonce((String) entries.get(NONCE_FIELD))
                .codeVerifier((String) entries.get(CODE_VERIFIER_FIELD))
                .build());
    }

    @Override
    public void remove(String state) {
        redisTemplate.delete(KEY_PREFIX + state);
        log.debug("Removed OIDC state: {}", state);
    }
}
