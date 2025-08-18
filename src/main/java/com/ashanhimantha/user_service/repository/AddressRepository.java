package com.ashanhimantha.user_service.repository;

import com.ashanhimantha.user_service.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {


    List<Address> findByUserId(String userId);

    Optional<Address> findByIdAndUserId(Long id, String userId);

    void deleteByIdAndUserId(Long addressId, String userId); // Ensures user owns the address before "deleting"
}