package com.stackr.api.repository;

import com.stackr.api.entity.Contest;
import com.stackr.api.entity.enums.ContestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ContestRepository extends JpaRepository<Contest, Long> {

    List<Contest> findByStatus(ContestStatus status);

    @Query("SELECT c FROM Contest c WHERE c.status = 'UPCOMING' AND c.startTime <= :now")
    List<Contest> findUpcomingToStart(@Param("now") LocalDateTime now);

    @Query("SELECT c FROM Contest c WHERE c.status = 'RUNNING' AND c.endTime <= :now")
    List<Contest> findRunningToFinish(@Param("now") LocalDateTime now);

    @Query("SELECT COUNT(cp) FROM ContestParticipation cp WHERE cp.contest.id = :contestId")
    long countParticipants(@Param("contestId") Long contestId);
}
