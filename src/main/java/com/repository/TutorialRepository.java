package com.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.model.Tutorial;

public interface TutorialRepository extends JpaRepository<Tutorial, Long> {
}
