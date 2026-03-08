package com.example.app.service.impl;

import com.example.app.common.enums.ResponseStatusEnums;
import com.example.app.common.exception.DataNotFoundException;
import com.example.app.common.exception.DuplicatedDataException;
import com.example.app.common.models.RespMsg;
import com.example.app.entity.UsersAccountEntity;
import com.example.app.model.LoginResp;
import com.example.app.model.UserLoginReqDto;
import com.example.app.model.UserProfileResp;
import com.example.app.model.UserRegisterReqDto;
import com.example.app.repository.UsersAccountRepository;
import com.example.app.security.jwt.JwtService;
import com.example.app.service.AuthService;
//import io.micrometer.observation.annotation.Observed;
//import io.opentelemetry.api.trace.Span;
//import io.opentelemetry.api.trace.StatusCode;
//import io.micrometer.core.instrument.MeterRegistry;
//import io.opentelemetry.api.trace.Tracer;
//import io.opentelemetry.context.Scope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
//import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UsersAccountRepository userRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authManager;
//    private final MeterRegistry meterRegistry;

//    private final Tracer tracer;

    @Override
    @Transactional
    public RespMsg userRegister(UserRegisterReqDto req) {
//        Span span = tracer.spanBuilder("auth.userRegister")
//                .setAttribute("user.email", req.getEmail())
//                .startSpan();

//        try (Scope scope = span.makeCurrent()) {
        log.info("Register attempt for email={}", req.getEmail());

        if (userRepo.existsByEmail((req.getEmail()))) {
            log.warn("Registration rejected — duplicate email={}", req.getEmail());
            throw new DuplicatedDataException("User account already exists");
        }

        UsersAccountEntity user = new UsersAccountEntity();
        user.setUsername(req.getUsername().trim());
        user.setEmail(req.getEmail());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setIsDeleted(false);

        userRepo.save(user);
        log.info("User registered successfully: email={}", req.getEmail());

        RespMsg response = new RespMsg();
        response.setStatus(ResponseStatusEnums.SUCCESS.getStatus());
        response.setMessage("User registered successfully");

        return response;
//        } catch (Exception e) {
//            span.setStatus(StatusCode.ERROR, e.getMessage());
//            span.recordException(e);
//            throw e;
//        } finally {
//            span.end();
//        }
    }

//    @Observed(name = "auth.userLogin")
    @Override
    public LoginResp userLogin(UserLoginReqDto req) {
        String email = req.getUserMail();
        log.info("Login attempt for email={}", email);

        UsersAccountEntity user = userRepo.findByEmail(email).orElseThrow(() -> {
            log.warn("Login failed — user not found: email={}", email);
//            meterRegistry.counter("auth.login.failure", "reason", "user_not_found").increment();
            return new DataNotFoundException("User not found");
        });

        // Password

        Authentication authentication = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, req.getUserPassword())
        );

//        Authentication authentication;
//        try {
//            authentication = authManager.authenticate(
//                    new UsernamePasswordAuthenticationToken(email, req.getUserPassword())
//            );
//        } catch (AuthenticationException e) {
//            log.warn("Login failed — bad credentials: email={}", email);
//            meterRegistry.counter("auth.login.failure", "reason", "bad_credentials").increment();
//            throw e;
//        }

        String token = jwtService.generateToken(email);
        log.debug("JWT token generated for email={}", email);

        UserProfileResp userProf = new UserProfileResp();
        userProf.setId(user.getId());
        userProf.setName(user.getUsername());
        userProf.setEmail(user.getEmail());
        userProf.setCreatedAt(user.getCreatedTime());
        userProf.setUpdatedAt(user.getLastModified());

        LoginResp resp = new LoginResp();
        resp.setToken(token);
        resp.setUserProfile(userProf);

        log.info("Login successful for email={}", email);
        return resp;
    }

}
