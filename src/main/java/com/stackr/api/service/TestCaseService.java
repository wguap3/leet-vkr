package com.stackr.api.service;

import com.stackr.api.dto.request.CreateTestCaseRequest;
import com.stackr.api.dto.request.UpdateTestCaseRequest;
import com.stackr.api.dto.response.TestCaseDto;
import com.stackr.api.entity.TestCase;
import com.stackr.api.exception.EntityNotFoundException;
import com.stackr.api.repository.ProblemRepository;
import com.stackr.api.repository.TestCaseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TestCaseService {

    private final TestCaseRepository testCaseRepository;
    private final ProblemRepository problemRepository;

    @Transactional(readOnly = true)
    public List<TestCaseDto> getTestCases(Long problemId, boolean showAll) {
        List<TestCase> cases = showAll
                ? testCaseRepository.findByProblemIdOrderByOrderIndexAsc(problemId)
                : testCaseRepository.findByProblemIdAndIsPublicTrueOrderByOrderIndexAsc(problemId);
        return cases.stream().map(this::toDto).toList();
    }

    @Transactional
    public TestCaseDto create(Long problemId, CreateTestCaseRequest req) {
        var problem = problemRepository.findById(problemId)
                .orElseThrow(() -> new EntityNotFoundException("Problem not found: " + problemId));
        var tc = TestCase.builder()
                .problem(problem)
                .inputData(req.inputData())
                .expectedOutput(req.expectedOutput())
                .isPublic(req.isPublic())
                .orderIndex(req.orderIndex() != null ? req.orderIndex() : 0)
                .build();
        return toDto(testCaseRepository.save(tc));
    }

    @Transactional
    public TestCaseDto update(Long problemId, Long id, UpdateTestCaseRequest req) {
        var tc = testCaseRepository.findById(id)
                .filter(t -> t.getProblem().getId().equals(problemId))
                .orElseThrow(() -> new EntityNotFoundException("TestCase not found: " + id));
        if (req.inputData() != null) tc.setInputData(req.inputData());
        if (req.expectedOutput() != null) tc.setExpectedOutput(req.expectedOutput());
        if (req.isPublic() != null) tc.setPublic(req.isPublic());
        if (req.orderIndex() != null) tc.setOrderIndex(req.orderIndex());
        return toDto(testCaseRepository.save(tc));
    }

    @Transactional
    public void delete(Long problemId, Long id) {
        var tc = testCaseRepository.findById(id)
                .filter(t -> t.getProblem().getId().equals(problemId))
                .orElseThrow(() -> new EntityNotFoundException("TestCase not found: " + id));
        testCaseRepository.delete(tc);
    }

    private TestCaseDto toDto(TestCase tc) {
        return new TestCaseDto(tc.getId(), tc.getInputData(), tc.getExpectedOutput(), tc.isPublic(), tc.getOrderIndex());
    }
}
