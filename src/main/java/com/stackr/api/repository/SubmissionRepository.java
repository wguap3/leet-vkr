package com.stackr.api.repository;

import com.stackr.api.entity.Submission;
import com.stackr.api.entity.enums.Verdict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
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

    long countByUserIdAndProblemIdAndVerdict(Long userId, Long problemId, Verdict verdict);

    @Query("SELECT COUNT(DISTINCT s.user.id) FROM Submission s WHERE s.problem.id = :problemId AND s.verdict = :verdict")
    long countDistinctUsersByProblemIdAndVerdict(@Param("problemId") Long problemId, @Param("verdict") Verdict verdict);

    /** Batch version — returns [problemId, distinctUserCount] rows for a set of problem IDs */
    @Query("SELECT s.problem.id, COUNT(DISTINCT s.user.id) FROM Submission s " +
           "WHERE s.problem.id IN :problemIds AND s.verdict = :verdict " +
           "GROUP BY s.problem.id")
    List<Object[]> countDistinctUsersByProblemIdsAndVerdict(
            @Param("problemIds") Collection<Long> problemIds,
            @Param("verdict") Verdict verdict);

    /** Returns the set of problem IDs a user has solved (AC) among the given IDs */
    @Query("SELECT DISTINCT s.problem.id FROM Submission s " +
           "WHERE s.user.id = :userId AND s.problem.id IN :problemIds AND s.verdict = 'AC'")
    List<Long> findSolvedProblemIdsByUserIdAndProblemIds(
            @Param("userId") Long userId,
            @Param("problemIds") Collection<Long> problemIds);

    Optional<Submission> findFirstByUserIdAndProblemIdAndVerdict(Long userId, Long problemId, Verdict verdict);

    long countByUserId(Long userId);

    @Query("SELECT COUNT(DISTINCT s.problem.id) FROM Submission s WHERE s.user.id = :userId AND s.verdict = :verdict")
    long countDistinctSolvedByUserId(@Param("userId") Long userId, @Param("verdict") Verdict verdict);

    /** Count wrong (non-AC) submissions by user on a specific problem */
    @Query("SELECT COUNT(s) FROM Submission s WHERE s.user.id = :userId AND s.problem.id = :problemId AND s.verdict <> 'AC'")
    long countWrongByUserIdAndProblemId(@Param("userId") Long userId, @Param("problemId") Long problemId);

    /** Verdict distribution for a problem: returns list of [verdict, count] */
    @Query("SELECT s.verdict, COUNT(s) FROM Submission s WHERE s.problem.id = :problemId GROUP BY s.verdict")
    List<Object[]> countByProblemIdGroupByVerdict(@Param("problemId") Long problemId);

    /** Average execution time for AC submissions on a problem */
    @Query("SELECT AVG(s.executionTimeMs) FROM Submission s WHERE s.problem.id = :problemId AND s.verdict = 'AC' AND s.executionTimeMs IS NOT NULL")
    Double avgExecutionTimeByProblemId(@Param("problemId") Long problemId);

    /** All submissions for a contest */
    @Query("SELECT s FROM Submission s WHERE s.contest.id = :contestId ORDER BY s.submittedAt ASC")
    List<Submission> findAllByContestId(@Param("contestId") Long contestId);

    /** Per-tag solved count for a user:
     *  returns list of [tagId, tagName, solvedCount] */
    @Query("""
        SELECT t.id, t.name, COUNT(DISTINCT s.problem.id)
        FROM Submission s
        JOIN s.problem p
        JOIN p.tags t
        WHERE s.user.id = :userId AND s.verdict = 'AC'
        GROUP BY t.id, t.name
        """)
    List<Object[]> countSolvedPerTagByUserId(@Param("userId") Long userId);

    /** Total problems per tag */
    @Query("""
        SELECT t.id, t.name, COUNT(DISTINCT p.id)
        FROM Problem p
        JOIN p.tags t
        WHERE p.visible = true
        GROUP BY t.id, t.name
        """)
    List<Object[]> countProblemsPerTag();
    /** Batch AC count per user — returns [userId, distinctProblemCount] */
    @Query("SELECT s.user.id, COUNT(DISTINCT s.problem.id) FROM Submission s " +
           "WHERE s.user.id IN :userIds AND s.verdict = :verdict " +
           "GROUP BY s.user.id")
    List<Object[]> countDistinctSolvedByUserIdsAndVerdict(
            @Param("userIds") Collection<Long> userIds,
            @Param("verdict") Verdict verdict);

    /** Batch total submission count per user — returns [userId, count] */
    @Query("SELECT s.user.id, COUNT(s) FROM Submission s " +
           "WHERE s.user.id IN :userIds " +
           "GROUP BY s.user.id")
    List<Object[]> countSubmissionsByUserIds(@Param("userIds") Collection<Long> userIds);

}
