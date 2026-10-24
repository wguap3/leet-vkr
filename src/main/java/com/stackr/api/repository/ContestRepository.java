package com.stackr.api.repository;

import com.stackr.api.entity.Contest;
import com.stackr.api.entity.enums.ContestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ContestRepository extends JpaRepository<Contest, Long> {

    List<Contest> findByStatus(ContestStatus status);

    List<Contest> findByIsPublicTrue();

    List<Contest> findByIsPublicTrueAndStatus(ContestStatus status);

    Optional<Contest> findByInviteToken(String inviteToken);

    @Query("SELECT c FROM Contest c WHERE c.status = 'UPCOMING' AND c.startTime <= :now")
    List<Contest> findUpcomingToStart(@Param("now") LocalDateTime now);

    @Query("SELECT c FROM Contest c WHERE c.status = 'RUNNING' AND c.endTime <= :now")
    List<Contest> findRunningToFinish(@Param("now") LocalDateTime now);

    @Query("SELECT COUNT(cp) FROM ContestParticipation cp WHERE cp.contest.id = :contestId")
    long countParticipants(@Param("contestId") Long contestId);

    @Query("SELECT cp.contest.id, COUNT(cp) FROM ContestParticipation cp " +
           "WHERE cp.contest.id IN :contestIds " +
           "GROUP BY cp.contest.id")
    List<Object[]> countParticipantsInContests(@Param("contestIds") Collection<Long> contestIds);
}
