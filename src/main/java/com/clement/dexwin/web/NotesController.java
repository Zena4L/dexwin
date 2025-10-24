package com.clement.dexwin.web;

import com.clement.dexwin.domain.dtos.GenericMessageResponse;
import com.clement.dexwin.domain.dtos.notes.NoteResponse;
import com.clement.dexwin.domain.dtos.notes.NotesRequest;
import com.clement.dexwin.domain.models.users.User;
import com.clement.dexwin.domain.services.contracts.NoteService;
import com.clement.dexwin.exceptions.BadRequestException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Notes", description = "Endpoints for creating, retrieving, updating, deleting and restoring notes")
@SecurityRequirement(name = "bearerAuth")
public class NotesController {

    private final NoteService noteService;

    @Operation(summary = "Create a note", description = "Create a new note for the authenticated user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Note created",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = GenericMessageResponse.class),
                examples = @ExampleObject(value = "{\n  \"message\": \"Note created successfully\"\n}"))
        ),
        @ApiResponse(responseCode = "400", description = "Validation error"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GenericMessageResponse createNotes(
            @AuthenticationPrincipal User user,
            @Valid
            @RequestBody(description = "Note payload",
                required = true,
                content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = NotesRequest.class),
                    examples = @ExampleObject(name = "createNote",
                        value = "{\n  \"title\": \"Project Plan\",\n  \"content\": { \"ops\": [ { \"insert\": \"Draft project milestones\\n\" } ] },\n  \"tags\": [\"work\", \"planning\"]\n}")
                )
            ) NotesRequest request
    ) {
        log.info("Creating note for user with id {}", user.getId());
        return noteService.createNote(user, request);
    }

    @Operation(summary = "List notes", description = "Retrieve a paginated list of notes for the authenticated user, optionally filtered by tags and search term")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Notes retrieved",
            content = @Content(mediaType = "application/json",
                examples = @ExampleObject(value = "{\n  \"content\": [\n    {\n      \"id\": \"d290f1ee-6c54-4b01-90e6-d701748f0851\",\n      \"title\": \"Project Plan\",\n      \"content\": { \"ops\": [ { \"insert\": \"Draft project milestones\\n\" } ] },\n      \"tags\": [\"work\", \"planning\"],\n      \"createdAt\": \"2025-01-10T12:34:56Z\",\n      \"updatedAt\": \"2025-01-10T12:34:56Z\"\n    }\n  ],\n  \"pageable\": {\n    \"pageNumber\": 0,\n    \"pageSize\": 10\n  },\n  \"totalElements\": 1,\n  \"totalPages\": 1,\n  \"last\": true,\n  \"size\": 10,\n  \"number\": 0\n}"))
        ),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping
    public Page<NoteResponse> getAllNotes(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = DEFAULT_PAGE_NUMBER)
            @Parameter(description = "Page number (0-based)", example = "0") int page,
            @RequestParam(defaultValue = DEFAULT_PAGE_SIZE)
            @Parameter(description = "Page size", example = "10") int size,
            @RequestParam(defaultValue = "")
            @Parameter(description = "Free-text search in title/content", example = "project") String search,
            @RequestParam(required = false, defaultValue = "")
            @Parameter(description = "Comma-separated list of tags to filter by", example = "work,planning") String filter
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

    @Operation(summary = "Get note by ID", description = "Retrieve a single note by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Note retrieved",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = NoteResponse.class),
                examples = @ExampleObject(value = "{\n  \"id\": \"d290f1ee-6c54-4b01-90e6-d701748f0851\",\n  \"title\": \"Project Plan\",\n  \"content\": { \"ops\": [ { \"insert\": \"Draft project milestones\\n\" } ] },\n  \"tags\": [\"work\", \"planning\"],\n  \"createdAt\": \"2025-01-10T12:34:56Z\",\n  \"updatedAt\": \"2025-01-10T12:34:56Z\"\n}"))
        ),
        @ApiResponse(responseCode = "404", description = "Note not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/{noteId}")
    public NoteResponse getNote(
            @AuthenticationPrincipal User user,
            @PathVariable
            @Parameter(description = "Note ID", example = "d290f1ee-6c54-4b01-90e6-d701748f0851") UUID noteId) {
        log.info("Fetching note with id {} for user with id {}", noteId, user.getId());
        return noteService.getNoteById(user, noteId);
    }

    @Operation(summary = "Delete note", description = "Soft-delete a note by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Note deleted",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = GenericMessageResponse.class),
                examples = @ExampleObject(value = "{\n  \"message\": \"Note deleted successfully\"\n}"))
        ),
        @ApiResponse(responseCode = "404", description = "Note not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @DeleteMapping("/{noteId}")
    public GenericMessageResponse deleteNote(
            @AuthenticationPrincipal User user,
            @PathVariable
            @Parameter(description = "Note ID", example = "d290f1ee-6c54-4b01-90e6-d701748f0851") UUID noteId) {
        log.info("Deleting note with id {} for user with id {}", noteId, user.getId());
        return noteService.deleteNote(user, noteId);
    }

    @Operation(summary = "Restore note", description = "Restore a previously deleted note")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Note restored",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = GenericMessageResponse.class),
                examples = @ExampleObject(value = "{\n  \"message\": \"Note restored successfully\"\n}"))
        ),
        @ApiResponse(responseCode = "404", description = "Note not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping("/{noteId}/restore")
    public GenericMessageResponse restoreNote(
            @AuthenticationPrincipal User user,
            @PathVariable
            @Parameter(description = "Note ID", example = "d290f1ee-6c54-4b01-90e6-d701748f0851") UUID noteId) {
        log.info("Restoring note with id {} for user with id {}", noteId, user.getId());
        return noteService.restoreNote(user, noteId);
    }

    @Operation(summary = "Update note", description = "Update the title/content/tags of an existing note")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Note updated",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = GenericMessageResponse.class),
                examples = @ExampleObject(value = "{\n  \"message\": \"Note updated successfully\"\n}"))
        ),
        @ApiResponse(responseCode = "400", description = "Validation error"),
        @ApiResponse(responseCode = "404", description = "Note not found"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PutMapping("/{noteId}")
    public GenericMessageResponse updateNote(
            @AuthenticationPrincipal User user,
            @PathVariable
            @Parameter(description = "Note ID", example = "d290f1ee-6c54-4b01-90e6-d701748f0851") UUID noteId,
            @Valid
            @RequestBody(description = "Note payload",
                required = true,
                content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = NotesRequest.class),
                    examples = @ExampleObject(name = "updateNote",
                        value = "{\n  \"title\": \"Updated Project Plan\",\n  \"content\": { \"ops\": [ { \"insert\": \"Refined milestones\\n\" } ] },\n  \"tags\": [\"work\", \"q1\"]\n}")
                )
            ) NotesRequest request) {
        log.info("Updating note with id {} for user with id {}", noteId, user.getId());
        return noteService.updateNote(user, noteId, request);
    }
}
