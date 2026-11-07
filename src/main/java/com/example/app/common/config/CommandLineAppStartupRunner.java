package com.example.app.common.config;

import com.example.app.entity.StatusEntity;
import com.example.app.entity.UsersAccountEntity;
import com.example.app.common.enums.ProjectTaskEnums;
import com.example.app.common.enums.UserRolesEnums;
import com.example.app.repository.StatusRepository;
import com.example.app.repository.UsersAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class CommandLineAppStartupRunner implements CommandLineRunner {
    @Autowired
    private UsersAccountRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private StatusRepository statusRepository;

    @Override
    public void run(String[] args){
        if (!userRepository.existsByEmail("admin0@test.com")) {
            UsersAccountEntity admin = new UsersAccountEntity();
            admin.setUsername("admin");
            admin.setEmail("admin0@test.com");
            admin.setPassword(passwordEncoder.encode("123"));
            admin.setRole(UserRolesEnums.ADMIN.getRole());
            admin.setIsDeleted(false);
            userRepository.save(admin);
        }
        for (ProjectTaskEnums e : ProjectTaskEnums.values()) {
            if (statusRepository.findById(e.getStatusId()).isEmpty()
                    && statusRepository.findByName(e.getStatusName()).isEmpty()) {
                StatusEntity status = new StatusEntity();
                status.setId(e.getStatusId());
                status.setName(e.getStatusName());
                statusRepository.save(status);
            }
        }

    }
}
