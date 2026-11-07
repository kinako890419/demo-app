package com.example.app.security;

import com.example.app.entity.UsersAccountEntity;
import com.example.app.repository.UsersAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UsersAccountRepository userRepo;

    /** Looks up the user by email. Soft-deleted users are filtered out by @SQLRestriction. */
    @Override
    public UserDetails loadUserByUsername(String email) {
        UsersAccountEntity user = userRepo.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
        return new CustomUserDetails(user);
    }
}
