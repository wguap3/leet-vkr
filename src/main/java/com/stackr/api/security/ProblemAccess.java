package com.stackr.api.security;

import com.stackr.api.entity.enums.Role;
import com.stackr.api.repository.ProblemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component("problemAccess")
@RequiredArgsConstructor
public class ProblemAccess {
    private final ProblemRepository problems;
    private final com.stackr.api.repository.SubmissionRepository submissions;
    private final ContestAccess contestAccess;

    @Transactional(readOnly = true)
    public boolean canReadSubmission(Long id, Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal user)) return false;
        return submissions.findById(id).map(s -> s.getUser().getId().equals(user.getId()) || user.getRole() == Role.ADMIN
                || (canRead(s.getProblem().getId(), auth) && (s.getContest() == null || contestAccess.canRead(s.getContest().getId(), auth)))).orElse(true);
    }

    private final com.stackr.api.repository.ContestParticipationRepository participations;

    @Transactional(readOnly = true)
    public boolean canRead(Long problemId, Authentication authentication) {
        if (problemId == null) return true; // Let request validation report missing problemId.
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal user)) return false;
        return problems.findById(problemId).map(p -> p.isVisible() || user.getRole() == Role.ADMIN
                || p.getAuthor().getId().equals(user.getId())
                || participations.hasStartedContestProblem(user.getId(), problemId, java.time.LocalDateTime.now())).orElse(true);
    }


    @Transactional(readOnly = true)
    public boolean canManage(Long problemId, Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal user)) return false;
        if (user.getRole() == Role.ADMIN) return true;
        if (user.getRole() != Role.ORGANIZER) return false;
        return problems.findById(problemId)
                .map(p -> p.getAuthor().getId().equals(user.getId())).orElse(false);
    }
}
