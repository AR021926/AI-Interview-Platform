package com.interview.platform.repository;

import com.interview.platform.model.Interview;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InterviewRepository extends JpaRepository<Interview, Long> {
}