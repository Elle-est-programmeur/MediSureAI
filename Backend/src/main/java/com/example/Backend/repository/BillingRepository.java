package com.example.Backend.repository;

import com.example.Backend.model.Billing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BillingRepository extends JpaRepository<Billing, Long> {

    List<Billing> findByPatientId(Long patientId);

    List<Billing> findByPatientIdOrderByCreatedAtDesc(Long patientId);
}