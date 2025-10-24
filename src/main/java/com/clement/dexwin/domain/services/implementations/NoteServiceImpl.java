package com.clement.dexwin.domain.services.implementations;

import com.clement.dexwin.domain.dtos.GenericMessageResponse;
import com.clement.dexwin.domain.dtos.notes.NoteResponse;
import com.clement.dexwin.domain.dtos.notes.NotesRequest;
import com.clement.dexwin.domain.models.notes.Note;
import com.clement.dexwin.domain.models.users.Roles;
import com.clement.dexwin.domain.models.users.User;
import com.clement.dexwin.domain.repository.NotesRepository;
import com.clement.dexwin.domain.services.contracts.NoteService;
import com.clement.dexwin.exceptions.BadRequestException;
import com.clement.dexwin.exceptions.ConflictException;
import com.clement.dexwin.exceptions.GenericException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static com.clement.dexwin.utils.ConstantMessages.INVALID_PAGE_NUMBER;

@Service
@RequiredArgsConstructor
@Slf4j
public class NoteServiceImpl implements NoteService {
    private final NotesRepository notesRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public GenericMessageResponse createNote(User user, NotesRequest request) {
        log.info("Creating note for user with id {}", user.getId());
        Note note = Note.builder()
                .title(request.title().trim())
                .content(serializeToJson(request.content()))
                .user(user)
                .tags(request.tags())
                .user(user)
                .build();

        notesRepository.save(note);

        return new GenericMessageResponse("Note created successfully");
    }

    @Transactional(readOnly = true)
    @Override
    public Page<NoteResponse> getAllNotes(User user, int page, int size, String search, List<String> filterTags) {
        if (page < 0) {
            throw new BadRequestException(INVALID_PAGE_NUMBER);
        }
        log.info("Fetching notes for user with id {} | page: {} | size: {} | search: {} | filterTags: {}",
                user.getId(), page, size, search, filterTags);

        Pageable pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));

        boolean isAdmin = user.getRoles().equals(Roles.ADMIN);

        Specification<Note> spec = isAdmin
                ? NoteSpecification.filterNotes(null, search, filterTags)
                : NoteSpecification.filterNotes(user, search, filterTags);

        Page<Note> notes = notesRepository.findAll(spec, pageRequest);
        return notes.map(note -> new NoteResponse(
                note.getId(),
                note.getTitle(),
                deserializeFromJson(note.getContent()),
                note.getTags(),
                note.getCreatedAt(),
                note.getLastModifiedBy()
        ));
    }

    @Override
    public NoteResponse getNoteById(User user, UUID noteId) {
        return notesRepository.findNoteById(noteId).map(note -> new NoteResponse(
                note.getId(),
                note.getTitle(),
                deserializeFromJson(note.getContent()),
                note.getTags(),
                note.getCreatedAt(),
                note.getLastModifiedBy()
        )).orElseThrow(() -> new BadRequestException("Note not found with id: " + noteId));
    }

    private String serializeToJson(Object field) {
        if (field == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(field);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize JSON field: {}", e.getMessage());
            throw new GenericException("Failed to process JSON content: " + e.getMessage());
        }
    }

    @Override
    public GenericMessageResponse deleteNote(User user, UUID noteId) {
        Note note = notesRepository.findActiveNoteByIdAndUser(noteId, user)
                .orElseThrow(() -> new BadRequestException("Note not found or already deleted"));

        note.setDeletedAt(Instant.now());
        note.setDeleted(true);
        notesRepository.save(note);

        log.info("Note with id {} soft deleted for user {}", noteId, user.getId());
        return new GenericMessageResponse("Note deleted successfully");
    }

    @Override
    @Transactional
    public GenericMessageResponse restoreNote(User user, UUID noteId) {
        int restored = notesRepository.restoreNote(noteId, user);

        if (restored == 0) {
            throw new BadRequestException("Note not found or not deleted");
        }

        log.info("Note with id {} restored for user {}", noteId, user.getId());
        return new GenericMessageResponse("Note restored successfully");
    }

    @Override
    @Transactional
    public GenericMessageResponse updateNote(User user, UUID noteId, NotesRequest request) {
        log.info("Updating note with id {} for user {}", noteId, user.getId());

        try {
            Note note = notesRepository.findActiveNoteByIdAndUser(noteId, user)
                    .orElseThrow(() -> new BadRequestException("Note not found or has been deleted"));

            note.setTitle(request.title().trim());
            note.setContent(serializeToJson(request.content()));
            note.setTags(request.tags());

            notesRepository.save(note);

            log.info("Note with id {} updated successfully for user {}", noteId, user.getId());
            return new GenericMessageResponse("Note updated successfully");
        }
        catch (ObjectOptimisticLockingFailureException | OptimisticLockException e) {
            log.warn("Optimistic lock conflict for note {} by user {}", noteId, user.getId());
            throw new ConflictException("This note was modified by someone else. Please reload and try again.");
        }
    }


    private JsonNode deserializeFromJson(String json) {
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readTree(json);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize JSON field: {}", e.getMessage());
            throw new GenericException("Failed to process JSON content: " + e.getMessage());
        }
    }
}
