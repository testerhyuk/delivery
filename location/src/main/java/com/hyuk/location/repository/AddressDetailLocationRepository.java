package com.hyuk.location.repository;

import com.hyuk.location.entity.AddressDetailLocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AddressDetailLocationRepository extends JpaRepository<AddressDetailLocation, Long> {

    Optional<AddressDetailLocation> findByMgmtNumAndDetailAddress(
            String mgmtNum, String detailAddress
    );
}
