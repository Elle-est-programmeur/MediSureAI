package com.example.Backend.repository;

import com.example.Backend.model.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface UserCredRepo extends JpaRepository<Users, Long> {
    Users findByUsername(String username);
}
