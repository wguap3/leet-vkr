package com.stackr.api.repository;

import com.stackr.api.entity.ContestParticipation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContestParticipationRepository extends JpaRepository<ContestParticipation, Long> {

    Optional<ContestParticipation> findByUserIdAndContestId(Long userId, Long contestId);

    boolean existsByUserIdAndContestId(Long userId, Long contestId);

    @Query("SELECT cp FROM ContestParticipation cp WHERE cp.contest.id = :contestId " +
           "ORDER BY cp.score DESC, cp.penalty ASC")
    List<ContestParticipation> findLeaderboard(@Param("contestId") Long contestId);
}
