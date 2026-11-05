package com.stackr.api.security;

import com.stackr.api.entity.enums.Role;
import com.stackr.api.repository.ContestRepository;
import com.stackr.api.repository.ContestParticipationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component("contestAccess")
@RequiredArgsConstructor
public class ContestAccess {
    private final ContestRepository contests;
    private final ContestParticipationRepository participations;

    @Transactional(readOnly = true)
    public boolean canRead(Long id, Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal user)) return false;
        return contests.findById(id).map(c -> user.getRole() == Role.ADMIN || c.isPublic()
                || c.getOrganizer().getId().equals(user.getId())
                || participations.existsByUserIdAndContestId(user.getId(), id)).orElse(true);
    }

    @Transactional(readOnly = true)
    public boolean canManage(Long id, Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal user)) return false;
        return contests.findById(id).map(c -> user.getRole() == Role.ADMIN
                || c.getOrganizer().getId().equals(user.getId())).orElse(user.getRole() == Role.ADMIN);
    }
}
