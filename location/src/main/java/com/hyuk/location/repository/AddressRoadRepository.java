package com.hyuk.location.repository;

import com.hyuk.location.entity.AddressRoad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AddressRoadRepository extends JpaRepository<AddressRoad, String> {

    @Query("""
        SELECT r FROM AddressRoad r
        WHERE r.roadName = :roadName
        AND r.buildMain = :buildMain
    """)
    Optional<AddressRoad> findByRoadNameAndBuildMain(
            @Param("roadName") String roadName,
            @Param("buildMain") Integer buildMain
    );
}
