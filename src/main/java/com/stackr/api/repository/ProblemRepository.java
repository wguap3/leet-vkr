package com.stackr.api.repository;

import com.stackr.api.entity.Problem;
import com.stackr.api.entity.enums.Difficulty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProblemRepository extends JpaRepository<Problem, Long> {

    Page<Problem> findByVisible(boolean visible, Pageable pageable);

    @Query("SELECT p FROM Problem p WHERE p.visible = true " +
           "AND (:difficulty IS NULL OR p.difficulty = :difficulty) " +
           "AND (:tagId IS NULL OR EXISTS (SELECT t FROM p.tags t WHERE t.id = :tagId)) " +
           "AND (:search IS NULL OR LOWER(p.title) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Problem> findByFilters(
            @Param("difficulty") Difficulty difficulty,
            @Param("tagId") Long tagId,
            @Param("search") String search,
            Pageable pageable);

    @Query("SELECT COUNT(s) FROM Submission s WHERE s.problem.id = :problemId AND s.verdict = 'AC'")
    long countAcceptedByProblemId(@Param("problemId") Long problemId);
}
