package com.example.AlumniPortal.repository;

import com.example.AlumniPortal.entity.MentorProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MentorProfileRepository extends JpaRepository<MentorProfile, Long> {

    List<MentorProfile> findByActiveTrue();

    List<MentorProfile> findTop5ByActiveTrueOrderByRatingAverageDescTotalReviewsDescSessionsCompletedDesc();

    List<MentorProfile> findTop5ByActiveTrueOrderByCreatedAtDesc();

    Optional<MentorProfile> findByUserId(Long userId);
}
