package com.example.Backend.service;

import com.example.Backend.model.UserPrinciple;
import com.example.Backend.repository.UserCredRepo;
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
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return repo.findByUsername(username)
                .map(UserPrinciple::new)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }
}