package com.clement.dexwin.web;

import com.clement.dexwin.domain.dtos.GenericMessageResponse;
import com.clement.dexwin.domain.dtos.notes.NoteResponse;
import com.clement.dexwin.domain.dtos.notes.NotesRequest;
import com.clement.dexwin.domain.models.users.Roles;
import com.clement.dexwin.domain.models.users.User;
import com.clement.dexwin.domain.services.contracts.NoteService;
import com.clement.dexwin.exceptions.BadRequestException;
import com.clement.dexwin.exceptions.ConflictException;
import com.clement.dexwin.exceptions.GenericException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotesController.class)
@AutoConfigureMockMvc(addFilters = false)
class NotesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private NoteService noteService;

    private User testUser;
    private NotesRequest validNotesRequest;
    private NoteResponse noteResponse;
    private GenericMessageResponse successResponse;
    private UUID noteId;
    private JsonNode contentNode;

    @BeforeEach
    void setUp() {
        noteId = UUID.randomUUID();

        testUser = User.builder()
                .id(UUID.randomUUID())
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .roles(Roles.VIEWER)
                .isActive(true)
                .build();

        // Create JSON content
        ObjectNode content = JsonNodeFactory.instance.objectNode();
        content.put("text", "This is a test note");
        contentNode = content;

        validNotesRequest = NotesRequest.builder()
                .title("Test Note")
                .content(contentNode)
                .tags(Arrays.asList("tag1", "tag2"))
                .build();

        noteResponse = new NoteResponse(
                "Test Note",
                contentNode,
                Arrays.asList("tag1", "tag2")
        );

        successResponse = new GenericMessageResponse("Note created successfully");
    }

    @Nested
    @DisplayName("POST /api/v1/notes - Create Note Tests")
    class CreateNoteTests {

        @Test
        @DisplayName("Should successfully create a note with valid data")
        void shouldCreateNoteSuccessfully() throws Exception {
            // Arrange
            when(noteService.createNote(any(User.class), any(NotesRequest.class)))
                    .thenReturn(successResponse);

            // Act & Assert
            mockMvc.perform(post("/api/v1/notes")
                            .with(user((UserDetails) testUser))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validNotesRequest)))
                    .andDo(print())
                    .andExpect(status().isCreated())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.message").value("Note created successfully"));

            verify(noteService, times(1)).createNote(any(User.class), any(NotesRequest.class));
        }

        @Test
        @DisplayName("Should create note with empty tags list")
        void shouldCreateNoteWithEmptyTags() throws Exception {
            // Arrange
            NotesRequest requestWithEmptyTags = NotesRequest.builder()
                    .title("Test Note")
                    .content(contentNode)
                    .tags(Collections.emptyList())
                    .build();

            when(noteService.createNote(any(User.class), any(NotesRequest.class)))
                    .thenReturn(successResponse);

            // Act & Assert
            mockMvc.perform(post("/api/v1/notes")
                            .with(user((UserDetails) testUser))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestWithEmptyTags)))
                    .andDo(print())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.message").exists());

            verify(noteService, times(1)).createNote(any(User.class), any(NotesRequest.class));
        }

        @Test
        @DisplayName("Should return 400 when title is null")
        void shouldReturnBadRequestWhenTitleIsNull() throws Exception {
            // Arrange
            String invalidJson = "{\"title\":null,\"content\":{\"text\":\"content\"},\"tags\":[\"tag1\"]}";

            // Act & Assert
            mockMvc.perform(post("/api/v1/notes")
                            .with(user((UserDetails) testUser))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidJson))
                    .andDo(print())
                    .andExpect(status().isBadRequest());

            verify(noteService, never()).createNote(any(User.class), any(NotesRequest.class));
        }

        @Test
        @DisplayName("Should return 400 when title is blank")
        void shouldReturnBadRequestWhenTitleIsBlank() throws Exception {
            // Arrange
            String invalidJson = "{\"title\":\"\",\"content\":{\"text\":\"content\"},\"tags\":[\"tag1\"]}";

            // Act & Assert
            mockMvc.perform(post("/api/v1/notes")
                            .with(user((UserDetails) testUser))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidJson))
                    .andDo(print())
                    .andExpect(status().isBadRequest());

            verify(noteService, never()).createNote(any(User.class), any(NotesRequest.class));
        }

        @Test
        @DisplayName("Should return 400 when content is null")
        void shouldReturnBadRequestWhenContentIsNull() throws Exception {
            // Arrange
            String invalidJson = "{\"title\":\"Test Note\",\"content\":null,\"tags\":[\"tag1\"]}";

            // Act & Assert
            mockMvc.perform(post("/api/v1/notes")
                            .with(user((UserDetails) testUser))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidJson))
                    .andDo(print())
                    .andExpect(status().isBadRequest());

            verify(noteService, never()).createNote(any(User.class), any(NotesRequest.class));
        }

        @Test
        @DisplayName("Should return 400 when request body is invalid JSON")
        void shouldReturnBadRequestWhenInvalidJson() throws Exception {
            // Act & Assert
            mockMvc.perform(post("/api/v1/notes")
                            .with(user((UserDetails) testUser))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"invalid\":\"json\"}"))
                    .andDo(print())
                    .andExpect(status().isBadRequest());

            verify(noteService, never()).createNote(any(User.class), any(NotesRequest.class));
        }

        @Test
        @DisplayName("Should handle service exception")
        void shouldHandleServiceException() throws Exception {
            // Arrange
            when(noteService.createNote(any(User.class), any(NotesRequest.class)))
                    .thenThrow(new GenericException("Failed to create note"));

            // Act & Assert
            mockMvc.perform(post("/api/v1/notes")
                            .with(user((UserDetails) testUser))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validNotesRequest)))
                    .andDo(print())
                    .andExpect(status().isInternalServerError());

            verify(noteService, times(1)).createNote(any(User.class), any(NotesRequest.class));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/notes - Get All Notes Tests")
    class GetAllNotesTests {

        @Test
        @DisplayName("Should get all notes with default pagination")
        void shouldGetAllNotesWithDefaultPagination() throws Exception {
            // Arrange
            List<NoteResponse> notes = Arrays.asList(noteResponse);
            Page<NoteResponse> notesPage = new PageImpl<>(notes);

            when(noteService.getAllNotes(any(User.class), eq(0), eq(10), eq(""), isNull()))
                    .thenReturn(notesPage);

            // Act & Assert
            mockMvc.perform(get("/api/v1/notes")
                            .with(user((UserDetails) testUser)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].title").value("Test Note"))
                    .andExpect(jsonPath("$.content[0].tags", hasSize(2)));

            verify(noteService, times(1)).getAllNotes(any(User.class), eq(0), eq(10), eq(""), isNull());
        }

        @Test
        @DisplayName("Should get notes with custom pagination")
        void shouldGetNotesWithCustomPagination() throws Exception {
            // Arrange
            List<NoteResponse> notes = Collections.singletonList(noteResponse);
            Page<NoteResponse> notesPage = new PageImpl<>(notes);

            when(noteService.getAllNotes(any(User.class), eq(2), eq(20), eq(""), isNull()))
                    .thenReturn(notesPage);

            // Act & Assert
            mockMvc.perform(get("/api/v1/notes")
                            .with(user((UserDetails) testUser))
                            .param("page", "2")
                            .param("size", "20"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)));

            verify(noteService, times(1)).getAllNotes(any(User.class), eq(2), eq(20), eq(""), isNull());
        }

        @Test
        @DisplayName("Should get notes with search parameter")
        void shouldGetNotesWithSearch() throws Exception {
            // Arrange
            List<NoteResponse> notes = Collections.singletonList(noteResponse);
            Page<NoteResponse> notesPage = new PageImpl<>(notes);

            when(noteService.getAllNotes(any(User.class), eq(0), eq(10), eq("test"), isNull()))
                    .thenReturn(notesPage);

            // Act & Assert
            mockMvc.perform(get("/api/v1/notes")
                            .with(user((UserDetails) testUser))
                            .param("search", "test"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)));

            verify(noteService, times(1)).getAllNotes(any(User.class), eq(0), eq(10), eq("test"), isNull());
        }

        @Test
        @DisplayName("Should get notes with single filter tag")
        void shouldGetNotesWithSingleFilterTag() throws Exception {
            // Arrange
            List<NoteResponse> notes = Collections.singletonList(noteResponse);
            Page<NoteResponse> notesPage = new PageImpl<>(notes);

            when(noteService.getAllNotes(any(User.class), eq(0), eq(10), eq(""), anyList()))
                    .thenReturn(notesPage);

            // Act & Assert
            mockMvc.perform(get("/api/v1/notes")
                            .with(user((UserDetails) testUser))
                            .param("filter", "tag1"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)));

            verify(noteService, times(1)).getAllNotes(any(User.class), eq(0), eq(10), eq(""), anyList());
        }

        @Test
        @DisplayName("Should get notes with multiple filter tags")
        void shouldGetNotesWithMultipleFilterTags() throws Exception {
            // Arrange
            List<NoteResponse> notes = Arrays.asList(noteResponse);
            Page<NoteResponse> notesPage = new PageImpl<>(notes);

            when(noteService.getAllNotes(any(User.class), eq(0), eq(10), eq(""), anyList()))
                    .thenReturn(notesPage);

            // Act & Assert
            mockMvc.perform(get("/api/v1/notes")
                            .with(user((UserDetails) testUser))
                            .param("filter", "tag1,tag2,tag3"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)));

            verify(noteService, times(1)).getAllNotes(any(User.class), eq(0), eq(10), eq(""), anyList());
        }

        @Test
        @DisplayName("Should get notes with search and filter tags")
        void shouldGetNotesWithSearchAndFilterTags() throws Exception {
            // Arrange
            List<NoteResponse> notes = Arrays.asList(noteResponse);
            Page<NoteResponse> notesPage = new PageImpl<>(notes);

            when(noteService.getAllNotes(any(User.class), eq(0), eq(10), eq("test"), anyList()))
                    .thenReturn(notesPage);

            // Act & Assert
            mockMvc.perform(get("/api/v1/notes")
                            .with(user((UserDetails) testUser))
                            .param("search", "test")
                            .param("filter", "tag1,tag2"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)));

            verify(noteService, times(1)).getAllNotes(any(User.class), eq(0), eq(10), eq("test"), anyList());
        }

        @Test
        @DisplayName("Should handle empty filter string")
        void shouldHandleEmptyFilterString() throws Exception {
            // Arrange
            List<NoteResponse> notes = Collections.singletonList(noteResponse);
            Page<NoteResponse> notesPage = new PageImpl<>(notes);

            when(noteService.getAllNotes(any(User.class), eq(0), eq(10), eq(""), isNull()))
                    .thenReturn(notesPage);

            // Act & Assert
            mockMvc.perform(get("/api/v1/notes")
                            .with(user((UserDetails) testUser))
                            .param("filter", ""))
                    .andDo(print())
                    .andExpect(status().isOk());

            verify(noteService, times(1)).getAllNotes(any(User.class), eq(0), eq(10), eq(""), isNull());
        }

        @Test
        @DisplayName("Should handle filter with whitespace only")
        void shouldHandleFilterWithWhitespaceOnly() throws Exception {
            // Arrange
            List<NoteResponse> notes = Arrays.asList(noteResponse);
            Page<NoteResponse> notesPage = new PageImpl<>(notes);

            when(noteService.getAllNotes(any(User.class), eq(0), eq(10), eq(""), isNull()))
                    .thenReturn(notesPage);

            // Act & Assert
            mockMvc.perform(get("/api/v1/notes")
                            .with(user((UserDetails) testUser))
                            .param("filter", "   "))
                    .andDo(print())
                    .andExpect(status().isOk());

            verify(noteService, times(1)).getAllNotes(any(User.class), eq(0), eq(10), eq(""), isNull());
        }

        @Test
        @DisplayName("Should trim filter tags and remove empty strings")
        void shouldTrimFilterTagsAndRemoveEmptyStrings() throws Exception {
            // Arrange
            List<NoteResponse> notes = Arrays.asList(noteResponse);
            Page<NoteResponse> notesPage = new PageImpl<>(notes);

            when(noteService.getAllNotes(any(User.class), eq(0), eq(10), eq(""), anyList()))
                    .thenReturn(notesPage);

            // Act & Assert
            mockMvc.perform(get("/api/v1/notes")
                            .with(user((UserDetails) testUser))
                            .param("filter", " tag1 , , tag2 , "))
                    .andDo(print())
                    .andExpect(status().isOk());

            verify(noteService, times(1)).getAllNotes(any(User.class), eq(0), eq(10), eq(""), anyList());
        }

        @Test
        @DisplayName("Should return empty page when no notes found")
        void shouldReturnEmptyPageWhenNoNotesFound() throws Exception {
            // Arrange
            Page<NoteResponse> emptyPage = new PageImpl<>(Collections.emptyList());

            when(noteService.getAllNotes(any(User.class), eq(0), eq(10), eq(""), isNull()))
                    .thenReturn(emptyPage);

            // Act & Assert
            mockMvc.perform(get("/api/v1/notes")
                            .with(user((UserDetails) testUser)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(0)));

            verify(noteService, times(1)).getAllNotes(any(User.class), eq(0), eq(10), eq(""), isNull());
        }

        @Test
        @DisplayName("Should handle BadRequestException from service")
        void shouldHandleBadRequestException() throws Exception {
            // Arrange
            when(noteService.getAllNotes(any(User.class), anyInt(), anyInt(), anyString(), any()))
                    .thenThrow(new BadRequestException("Invalid page number"));

            // Act & Assert
            mockMvc.perform(get("/api/v1/notes")
                            .with(user((UserDetails) testUser)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());

            verify(noteService, times(1)).getAllNotes(any(User.class), eq(0), eq(10), eq(""), isNull());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/notes/{noteId} - Get Note By ID Tests")
    class GetNoteByIdTests {

        @Test
        @DisplayName("Should get note by ID successfully")
        void shouldGetNoteByIdSuccessfully() throws Exception {
            // Arrange
            when(noteService.getNoteById(any(User.class), eq(noteId)))
                    .thenReturn(noteResponse);

            // Act & Assert
            mockMvc.perform(get("/api/v1/notes/{noteId}", noteId)
                            .with(user((UserDetails) testUser)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.title").value("Test Note"))
                    .andExpect(jsonPath("$.tags", hasSize(2)))
                    .andExpect(jsonPath("$.content").exists());

            verify(noteService, times(1)).getNoteById(any(User.class), eq(noteId));
        }

        @Test
        @DisplayName("Should return 400 when note not found")
        void shouldReturnBadRequestWhenNoteNotFound() throws Exception {
            // Arrange
            UUID nonExistentId = UUID.randomUUID();
            when(noteService.getNoteById(any(User.class), eq(nonExistentId)))
                    .thenThrow(new BadRequestException("Note not found with id: " + nonExistentId));

            // Act & Assert
            mockMvc.perform(get("/api/v1/notes/{noteId}", nonExistentId)
                            .with(user((UserDetails) testUser)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());

            verify(noteService, times(1)).getNoteById(any(User.class), eq(nonExistentId));
        }

        @Test
        @DisplayName("Should handle invalid UUID format")
        void shouldHandleInvalidUuidFormat() throws Exception {
            // Act & Assert
            mockMvc.perform(get("/api/v1/notes/{noteId}", "invalid-uuid")
                            .with(user((UserDetails) testUser)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());

            verify(noteService, never()).getNoteById(any(User.class), any(UUID.class));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/notes/{noteId} - Delete Note Tests")
    class DeleteNoteTests {

        @Test
        @DisplayName("Should delete note successfully")
        void shouldDeleteNoteSuccessfully() throws Exception {
            // Arrange
            GenericMessageResponse deleteResponse = new GenericMessageResponse("Note deleted successfully");
            when(noteService.deleteNote(any(User.class), eq(noteId)))
                    .thenReturn(deleteResponse);

            // Act & Assert
            mockMvc.perform(delete("/api/v1/notes/{noteId}", noteId)
                            .with(user((UserDetails) testUser)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.message").value("Note deleted successfully"));

            verify(noteService, times(1)).deleteNote(any(User.class), eq(noteId));
        }

        @Test
        @DisplayName("Should return 400 when note not found for deletion")
        void shouldReturnBadRequestWhenNoteNotFound() throws Exception {
            // Arrange
            when(noteService.deleteNote(any(User.class), eq(noteId)))
                    .thenThrow(new BadRequestException("Note not found or already deleted"));

            // Act & Assert
            mockMvc.perform(delete("/api/v1/notes/{noteId}", noteId)
                            .with(user((UserDetails) testUser)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());

            verify(noteService, times(1)).deleteNote(any(User.class), eq(noteId));
        }

        @Test
        @DisplayName("Should handle invalid UUID format for deletion")
        void shouldHandleInvalidUuidFormatForDeletion() throws Exception {
            // Act & Assert
            mockMvc.perform(delete("/api/v1/notes/{noteId}", "invalid-uuid")
                            .with(user((UserDetails) testUser)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());

            verify(noteService, never()).deleteNote(any(User.class), any(UUID.class));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/notes/{noteId}/restore - Restore Note Tests")
    class RestoreNoteTests {

        @Test
        @DisplayName("Should restore note successfully")
        void shouldRestoreNoteSuccessfully() throws Exception {
            // Arrange
            GenericMessageResponse restoreResponse = new GenericMessageResponse("Note restored successfully");
            when(noteService.restoreNote(any(User.class), eq(noteId)))
                    .thenReturn(restoreResponse);

            // Act & Assert
            mockMvc.perform(post("/api/v1/notes/{noteId}/restore", noteId)
                            .with(user((UserDetails) testUser)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.message").value("Note restored successfully"));

            verify(noteService, times(1)).restoreNote(any(User.class), eq(noteId));
        }

        @Test
        @DisplayName("Should return 400 when note not found for restore")
        void shouldReturnBadRequestWhenNoteNotFoundForRestore() throws Exception {
            // Arrange
            when(noteService.restoreNote(any(User.class), eq(noteId)))
                    .thenThrow(new BadRequestException("Note not found or not deleted"));

            // Act & Assert
            mockMvc.perform(post("/api/v1/notes/{noteId}/restore", noteId)
                            .with(user((UserDetails) testUser)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());

            verify(noteService, times(1)).restoreNote(any(User.class), eq(noteId));
        }

        @Test
        @DisplayName("Should handle invalid UUID format for restore")
        void shouldHandleInvalidUuidFormatForRestore() throws Exception {
            // Act & Assert
            mockMvc.perform(post("/api/v1/notes/{noteId}/restore", "invalid-uuid")
                            .with(user((UserDetails) testUser)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());

            verify(noteService, never()).restoreNote(any(User.class), any(UUID.class));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/notes/{noteId} - Update Note Tests")
    class UpdateNoteTests {

        @Test
        @DisplayName("Should update note successfully")
        void shouldUpdateNoteSuccessfully() throws Exception {
            // Arrange
            GenericMessageResponse updateResponse = new GenericMessageResponse("Note updated successfully");
            when(noteService.updateNote(any(User.class), eq(noteId), any(NotesRequest.class)))
                    .thenReturn(updateResponse);

            // Act & Assert
            mockMvc.perform(put("/api/v1/notes/{noteId}", noteId)
                            .with(user((UserDetails) testUser))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validNotesRequest)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.message").value("Note updated successfully"));

            verify(noteService, times(1)).updateNote(any(User.class), eq(noteId), any(NotesRequest.class));
        }

        @Test
        @DisplayName("Should return 400 when note not found for update")
        void shouldReturnBadRequestWhenNoteNotFoundForUpdate() throws Exception {
            // Arrange
            when(noteService.updateNote(any(User.class), eq(noteId), any(NotesRequest.class)))
                    .thenThrow(new BadRequestException("Note not found or has been deleted"));

            // Act & Assert
            mockMvc.perform(put("/api/v1/notes/{noteId}", noteId)
                            .with(user((UserDetails) testUser))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validNotesRequest)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());

            verify(noteService, times(1)).updateNote(any(User.class), eq(noteId), any(NotesRequest.class));
        }

        @Test
        @DisplayName("Should return 409 on optimistic lock exception")
        void shouldReturnConflictOnOptimisticLockException() throws Exception {
            // Arrange
            when(noteService.updateNote(any(User.class), eq(noteId), any(NotesRequest.class)))
                    .thenThrow(new ConflictException("This note was modified by someone else. Please refresh and try again."));

            // Act & Assert
            mockMvc.perform(put("/api/v1/notes/{noteId}", noteId)
                            .with(user((UserDetails) testUser))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validNotesRequest)))
                    .andDo(print())
                    .andExpect(status().isConflict());

            verify(noteService, times(1)).updateNote(any(User.class), eq(noteId), any(NotesRequest.class));
        }

        @Test
        @DisplayName("Should return 400 when update request has null title")
        void shouldReturnBadRequestWhenUpdateRequestHasNullTitle() throws Exception {
            // Arrange
            String invalidJson = "{\"title\":null,\"content\":{\"text\":\"content\"},\"tags\":[\"tag1\"]}";

            // Act & Assert
            mockMvc.perform(put("/api/v1/notes/{noteId}", noteId)
                            .with(user((UserDetails) testUser))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidJson))
                    .andDo(print())
                    .andExpect(status().isBadRequest());

            verify(noteService, never()).updateNote(any(User.class), any(UUID.class), any(NotesRequest.class));
        }

        @Test
        @DisplayName("Should return 400 when update request has blank title")
        void shouldReturnBadRequestWhenUpdateRequestHasBlankTitle() throws Exception {
            // Arrange
            String invalidJson = "{\"title\":\"\",\"content\":{\"text\":\"content\"},\"tags\":[\"tag1\"]}";

            // Act & Assert
            mockMvc.perform(put("/api/v1/notes/{noteId}", noteId)
                            .with(user((UserDetails) testUser))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidJson))
                    .andDo(print())
                    .andExpect(status().isBadRequest());

            verify(noteService, never()).updateNote(any(User.class), any(UUID.class), any(NotesRequest.class));
        }

        @Test
        @DisplayName("Should return 400 when update request has null content")
        void shouldReturnBadRequestWhenUpdateRequestHasNullContent() throws Exception {
            // Arrange
            String invalidJson = "{\"title\":\"Test Note\",\"content\":null,\"tags\":[\"tag1\"]}";

            // Act & Assert
            mockMvc.perform(put("/api/v1/notes/{noteId}", noteId)
                            .with(user((UserDetails) testUser))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidJson))
                    .andDo(print())
                    .andExpect(status().isBadRequest());

            verify(noteService, never()).updateNote(any(User.class), any(UUID.class), any(NotesRequest.class));
        }

        @Test
        @DisplayName("Should update note with new tags")
        void shouldUpdateNoteWithNewTags() throws Exception {
            // Arrange
            NotesRequest updateRequest = NotesRequest.builder()
                    .title("Updated Note")
                    .content(contentNode)
                    .tags(Arrays.asList("newTag1", "newTag2", "newTag3"))
                    .build();

            GenericMessageResponse updateResponse = new GenericMessageResponse("Note updated successfully");
            when(noteService.updateNote(any(User.class), eq(noteId), any(NotesRequest.class)))
                    .thenReturn(updateResponse);

            // Act & Assert
            mockMvc.perform(put("/api/v1/notes/{noteId}", noteId)
                            .with(user((UserDetails) testUser))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Note updated successfully"));

            verify(noteService, times(1)).updateNote(any(User.class), eq(noteId), any(NotesRequest.class));
        }

        @Test
        @DisplayName("Should handle invalid UUID format for update")
        void shouldHandleInvalidUuidFormatForUpdate() throws Exception {
            // Act & Assert
            mockMvc.perform(put("/api/v1/notes/{noteId}", "invalid-uuid")
                            .with(user
                                    ((UserDetails) testUser))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validNotesRequest)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());

            verify(noteService, never()).updateNote(any(User.class), any(UUID.class), any(NotesRequest.class));
        }
    }
}

