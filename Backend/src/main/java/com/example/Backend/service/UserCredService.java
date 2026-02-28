package com.example.Backend.service;

import com.example.Backend.model.UserPrinciple;
import com.example.Backend.model.Users;
import com.example.Backend.repository.UserCredRepo;
import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserCredService implements UserDetailsService {

    @Autowired
    private UserCredRepo repo;
    @Override
    public UserDetails loadUserByUsername(@Nullable String username) throws UsernameNotFoundException {
        if (username == null) {
            throw new UsernameNotFoundException("Username cannot be null");
        }

        Users user = repo.findByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("User not found");
        }

        return new UserPrinciple(user);
    }
}
