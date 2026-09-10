package com.stackr.api.service;

import com.stackr.api.dto.request.CreateTagRequest;
import com.stackr.api.dto.response.TagDto;
import com.stackr.api.entity.Tag;
import com.stackr.api.exception.ConflictException;
import com.stackr.api.exception.EntityNotFoundException;
import com.stackr.api.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TagService {

    private final TagRepository tagRepository;

    public List<TagDto> getAll() {
        return tagRepository.findAll().stream()
                .map(t -> new TagDto(t.getId(), t.getName()))
                .toList();
    }

    @Transactional
    public TagDto create(CreateTagRequest req) {
        if (tagRepository.existsByName(req.name())) {
            throw new ConflictException("Tag already exists: " + req.name());
        }
        var tag = tagRepository.save(Tag.builder().name(req.name()).build());
        return new TagDto(tag.getId(), tag.getName());
    }

    @Transactional
    public void delete(Long id) {
        if (!tagRepository.existsById(id)) {
            throw new EntityNotFoundException("Tag not found: " + id);
        }
        tagRepository.deleteById(id);
    }
}
