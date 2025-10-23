package com.clement.dexwin.web;

import com.clement.dexwin.domain.dtos.GenericMessageResponse;
import com.clement.dexwin.domain.dtos.notes.NoteResponse;
import com.clement.dexwin.domain.dtos.notes.NotesRequest;
import com.clement.dexwin.domain.models.users.User;
import com.clement.dexwin.domain.services.contracts.NoteService;
import com.clement.dexwin.exceptions.BadRequestException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static com.clement.dexwin.utils.ConstantMessages.DEFAULT_PAGE_NUMBER;
import static com.clement.dexwin.utils.ConstantMessages.DEFAULT_PAGE_SIZE;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/notes")
@Slf4j
public class NotesController {

    private final NoteService noteService;


    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GenericMessageResponse createNotes(
            @AuthenticationPrincipal User user,
            @RequestBody @Valid NotesRequest request
    ) {
        log.info("Creating note for user with id {}", user.getId());
        return noteService.createNote(user, request);
    }

    @GetMapping
    public Page<NoteResponse> getAllNotes(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(defaultValue = DEFAULT_PAGE_SIZE) int size,
            @RequestParam(defaultValue = "") String search,
            @RequestParam(required = false, defaultValue = "") String filter
    ) {

        List<String> filterTags = null;
        if (filter != null && !filter.trim().isEmpty()) {
            try {
                filterTags = Arrays.stream(filter.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .toList();
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid filter format");
            }
        }
        log.info("Fetching notes for user with id {}", user.getId());
        return noteService.getAllNotes(user, page, size, search, filterTags);
    }

    @GetMapping("/{noteId}")
    public NoteResponse getNote(
            @AuthenticationPrincipal User user,
            @PathVariable UUID noteId) {
        log.info("Fetching note with id {} for user with id {}", noteId, user.getId());
        return noteService.getNoteById(user, noteId);
    }

    @DeleteMapping("/{noteId}")
    public GenericMessageResponse deleteNote(
            @AuthenticationPrincipal User user,
            @PathVariable UUID noteId) {
        log.info("Deleting note with id {} for user with id {}", noteId, user.getId());
        return noteService.deleteNote(user, noteId);
    }

    @PostMapping("/{noteId}/restore")
    public GenericMessageResponse restoreNote(
            @AuthenticationPrincipal User user,
            @PathVariable UUID noteId) {
        log.info("Restoring note with id {} for user with id {}", noteId, user.getId());
        return noteService.restoreNote(user, noteId);
    }

    @PutMapping("/{noteId}")
    public GenericMessageResponse updateNote(
            @AuthenticationPrincipal User user,
            @PathVariable UUID noteId,
            @RequestBody @Valid NotesRequest request) {
        log.info("Updating note with id {} for user with id {}", noteId, user.getId());
        return noteService.updateNote(user, noteId, request);
    }
}
