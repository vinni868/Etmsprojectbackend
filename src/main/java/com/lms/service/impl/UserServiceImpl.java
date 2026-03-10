package com.lms.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.lms.dto.RegisterRequest;
import com.lms.entity.RoleMaster;
import com.lms.entity.User;
import com.lms.enums.Status;
import com.lms.repository.RoleRepository;
import com.lms.repository.UserRepository;
import com.lms.service.UserService;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Override
    public User register(RegisterRequest request) {

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email already registered");
        }

        User user = new User();

        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setPassword(request.getPassword());

        String roleName = request.getRole() != null
                ? request.getRole().toUpperCase()
                : "STUDENT";

        RoleMaster role = roleRepository.findByRoleName(roleName);

        if (role == null) {
            throw new RuntimeException("Invalid role selected");
        }

        user.setRole(role);

        // 🔥 MAIN LOGIC
        if (roleName.equals("STUDENT") || roleName.equals("TRAINER")) {
            user.setStatus(Status.PENDING);
        } else {
            user.setStatus(Status.ACTIVE);
        }

        return userRepository.save(user);
    }

    @Override
    public User login(String email, String password) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!password.equals(user.getPassword())) {
            throw new RuntimeException("Invalid password");
        }

        if (user.getStatus() == Status.PENDING) {
            throw new RuntimeException("Your access is not approved yet");
        }

        if (user.getStatus() == Status.REJECTED) {
            throw new RuntimeException("Your account has been rejected");
        }

        if (user.getStatus() != Status.ACTIVE) {
            throw new RuntimeException("Account inactive");
        }

        return user;
    }


    @Override
    public User createAdmin(RegisterRequest request) {

        RoleMaster adminRole = roleRepository.findByRoleName("ADMIN");

        if (adminRole == null) {
            throw new RuntimeException("ADMIN role not found in DB");
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());

        // Storing plain text password
        user.setPassword(request.getPassword());

        user.setRole(adminRole);
        user.setStatus(Status.ACTIVE);

        return userRepository.save(user);
    }
}
