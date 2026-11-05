package com.stackr.api.controller;
import com.stackr.api.entity.Problem;
import com.stackr.api.entity.Tag;
import com.stackr.api.entity.Submission;
import com.stackr.api.entity.enums.*;
import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
class CatalogIntegrationTest extends BaseIntegrationTest {
    @Test void catalogFiltersCombineAndOwnDraftsAreVisibleOnlyToAuthor() throws Exception {
        String authorToken = registerAndLogin("catalogauthor", "author@catalog.test", "password123");
        String studentToken = registerAndLogin("catalogstudent", "student@catalog.test", "password123");
        var author = userRepository.findByUsername("catalogauthor").orElseThrow(); author.setRole(Role.ORGANIZER); userRepository.save(author);
        var student = userRepository.findByUsername("catalogstudent").orElseThrow();
        var tag = tagRepository.save(Tag.builder().name("Arrays").build());
        var p = problemRepository.save(Problem.builder().title("Catalog Sum").description("sum").difficulty(Difficulty.EASY).author(author).visible(true).tags(Set.of(tag)).build());
        problemRepository.save(Problem.builder().title("Catalog Draft").description("draft").difficulty(Difficulty.HARD).author(author).visible(false).build());
        submissionRepository.save(Submission.builder().user(student).problem(p).sourceCode("class Solution {}").verdict(Verdict.AC).build());
        mockMvc.perform(get("/api/problems").header("Authorization", bearer(studentToken))).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        mockMvc.perform(get("/api/problems?mine=true").header("Authorization", bearer(authorToken))).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2));
        mockMvc.perform(get("/api/problems?search=Sum&difficulty=EASY&solved=true&tagId="+tag.getId()).header("Authorization", bearer(studentToken))).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        mockMvc.perform(get("/api/problems?solved=false").header("Authorization", bearer(studentToken))).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        mockMvc.perform(get("/api/problems?mine=true").header("Authorization", bearer(studentToken))).andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/problems/"+p.getId()).header("Authorization", bearer(authorToken))).andExpect(status().isConflict());
    }
}
