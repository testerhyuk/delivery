package com.hyuk.location.repository;

import com.hyuk.location.entity.AddressLocation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AddressLocationRepository extends JpaRepository<AddressLocation, String> {
}
