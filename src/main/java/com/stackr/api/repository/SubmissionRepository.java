package com.stackr.api.repository;

import com.stackr.api.entity.Submission;
import com.stackr.api.entity.enums.Verdict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    Page<Submission> findByUserId(Long userId, Pageable pageable);

    @Query("SELECT s FROM Submission s WHERE s.user.id = :userId " +
           "AND (:problemId IS NULL OR s.problem.id = :problemId) " +
           "AND (:contestId IS NULL OR s.contest.id = :contestId)")
    Page<Submission> findByUserIdWithFilters(
            @Param("userId") Long userId,
            @Param("problemId") Long problemId,
            @Param("contestId") Long contestId,
            Pageable pageable);

    Page<Submission> findByProblemId(Long problemId, Pageable pageable);

    Page<Submission> findByContestId(Long contestId, Pageable pageable);

    boolean existsByUserIdAndProblemIdAndVerdict(Long userId, Long problemId, Verdict verdict);

    @Query("SELECT COUNT(DISTINCT s.user.id) FROM Submission s WHERE s.problem.id = :problemId AND s.verdict = :verdict")
    long countDistinctUsersByProblemIdAndVerdict(@Param("problemId") Long problemId, @Param("verdict") Verdict verdict);

    Optional<Submission> findFirstByUserIdAndProblemIdAndVerdict(Long userId, Long problemId, Verdict verdict);

    long countByUserId(Long userId);

    @Query("SELECT COUNT(DISTINCT s.problem.id) FROM Submission s WHERE s.user.id = :userId AND s.verdict = :verdict")
    long countDistinctSolvedByUserId(@Param("userId") Long userId, @Param("verdict") Verdict verdict);
}
