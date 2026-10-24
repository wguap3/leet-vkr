package com.stackr.api.service;

import com.stackr.api.dto.request.CreateHintRequest;
import com.stackr.api.dto.response.HintDto;
import com.stackr.api.entity.Hint;
import com.stackr.api.exception.EntityNotFoundException;
import com.stackr.api.repository.HintRepository;
import com.stackr.api.repository.ProblemRepository;
import com.stackr.api.repository.SubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HintService {

    private final HintRepository hintRepository;
    private final ProblemRepository problemRepository;
    private final SubmissionRepository submissionRepository;

    @Transactional(readOnly = true)
    public List<HintDto> getHintsForUser(Long problemId, Long userId) {
        long wrongAttempts = submissionRepository.countWrongByUserIdAndProblemId(userId, problemId);
        return hintRepository.findByProblemIdOrderByOrderIndexAsc(problemId).stream()
                .map(h -> toDto(h, wrongAttempts))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<HintDto> getHintsAdmin(Long problemId) {
        // Admin sees all hints as unlocked
        return hintRepository.findByProblemIdOrderByOrderIndexAsc(problemId).stream()
                .map(h -> toDto(h, Integer.MAX_VALUE))
                .toList();
    }

    @Transactional
    public HintDto createHint(Long problemId, CreateHintRequest req) {
        var problem = problemRepository.findById(problemId)
                .orElseThrow(() -> new EntityNotFoundException("Problem not found: " + problemId));
        var hint = Hint.builder()
                .problem(problem)
                .text(req.text())
                .orderIndex(req.orderIndex())
                .unlocksAfterAttempts(req.unlocksAfterAttempts())
                .build();
        return toDto(hintRepository.save(hint), Integer.MAX_VALUE);
    }

    @Transactional
    public void deleteHint(Long hintId) {
        if (!hintRepository.existsById(hintId)) {
            throw new EntityNotFoundException("Hint not found: " + hintId);
        }
        hintRepository.deleteById(hintId);
    }

    private HintDto toDto(Hint h, long wrongAttempts) {
        boolean unlocked = wrongAttempts >= h.getUnlocksAfterAttempts();
        return new HintDto(
                h.getId(),
                unlocked ? h.getText() : null,   // text is null if locked
                h.getOrderIndex(),
                h.getUnlocksAfterAttempts(),
                unlocked
        );
    }
}
