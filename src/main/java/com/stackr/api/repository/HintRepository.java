package com.stackr.api.repository;

import com.stackr.api.entity.Hint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HintRepository extends JpaRepository<Hint, Long> {
    List<Hint> findByProblemIdOrderByOrderIndexAsc(Long problemId);
    void deleteByProblemId(Long problemId);
}
