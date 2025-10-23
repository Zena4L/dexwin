package com.clement.dexwin.web;

import com.clement.dexwin.domain.dtos.GenericMessageResponse;
import com.clement.dexwin.domain.dtos.notes.NoteResponse;
import com.clement.dexwin.domain.dtos.notes.NotesRequest;
import com.clement.dexwin.domain.models.users.Roles;
import com.clement.dexwin.domain.models.users.User;
import com.clement.dexwin.domain.repository.UserRepository;
import com.clement.dexwin.domain.services.contracts.NoteService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
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
@Import(TestSecurityConfig.class)
class NotesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private NoteService noteService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private UserRepository userRepository;

    private User testUser;
    private UUID testNoteId;
    private NotesRequest validNotesRequest;
    private NoteResponse noteResponse;
    private GenericMessageResponse successResponse;
    private JsonNode testContent;

    @BeforeEach
    void setUp() {
        testNoteId = UUID.randomUUID();

        testUser = User.builder()
                .id(UUID.randomUUID())
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .roles(Roles.VIEWER)
                .isActive(true)
                .isDeleted(false)
                .build();

        // Create test JSON content
        ObjectNode contentNode = JsonNodeFactory.instance.objectNode();
        contentNode.put("text", "This is a test note content");
        contentNode.put("format", "markdown");
        testContent = contentNode;

        validNotesRequest = NotesRequest.builder()
                .title("Test Note")
                .content(testContent)
                .tags(Arrays.asList("work", "important"))
                .build();

        noteResponse = new NoteResponse(
                "Test Note",
                testContent,
                Arrays.asList("work", "important")
        );

        successResponse = GenericMessageResponse.builder()
                .message("Operation completed successfully")
                .build();
    }


    @Test
    @DisplayName("POST /api/v1/notes - Should successfully create a note with authenticated user")
    @WithMockAuthenticatedUser
    void shouldSuccessfullyCreateNote() throws Exception {
        // Arrange
        when(noteService.createNote(any(User.class), any(NotesRequest.class)))
                .thenReturn(successResponse);

        // Act & Assert
        mockMvc.perform(post("/api/v1/notes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validNotesRequest)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Operation completed successfully"));

        verify(noteService, times(1)).createNote(any(User.class), any(NotesRequest.class));
    }

    @Test
    @DisplayName("POST /api/v1/notes - Should fail when title is blank")
    @WithMockAuthenticatedUser
    void shouldFailWhenTitleIsBlank() throws Exception {
        // Arrange
        NotesRequest invalidRequest = NotesRequest.builder()
                .title("")
                .content(testContent)
                .tags(Collections.singletonList("work"))
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/v1/notes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(noteService, never()).createNote(any(User.class), any(NotesRequest.class));
    }



    @Test
    @DisplayName("POST /api/v1/notes - Should successfully create note without tags")
    @WithMockAuthenticatedUser
    void shouldSuccessfullyCreateNoteWithoutTags() throws Exception {
        // Arrange
        NotesRequest requestWithoutTags = NotesRequest.builder()
                .title("Test Note")
                .content(testContent)
                .tags(null)
                .build();

        when(noteService.createNote(any(User.class), any(NotesRequest.class)))
                .thenReturn(successResponse);

        // Act & Assert
        mockMvc.perform(post("/api/v1/notes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestWithoutTags)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").exists());

        verify(noteService, times(1)).createNote(any(User.class), any(NotesRequest.class));
    }

    @Test
    @DisplayName("GET /api/v1/notes - Should successfully retrieve all notes with default pagination")
    @WithMockAuthenticatedUser
    void shouldSuccessfullyGetAllNotesWithDefaultPagination() throws Exception {
        // Arrange
        List<NoteResponse> notes = Arrays.asList(
                new NoteResponse("Note 1", testContent, Arrays.asList("tag1")),
                new NoteResponse("Note 2", testContent, Arrays.asList("tag2"))
        );
        Page<NoteResponse> notesPage = new PageImpl<>(notes, PageRequest.of(0, 10), notes.size());

        when(noteService.getAllNotes(any(User.class), eq(0), eq(10), eq(""), isNull()))
                .thenReturn(notesPage);

        // Act & Assert
        mockMvc.perform(get("/api/v1/notes"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content", hasSize(2)));


        verify(noteService, times(1)).getAllNotes(any(User.class), eq(0), eq(10), eq(""), isNull());
    }

    @Test
    @DisplayName("GET /api/v1/notes - Should successfully retrieve notes with custom pagination")
    @WithMockAuthenticatedUser
    void shouldSuccessfullyGetNotesWithCustomPagination() throws Exception {
        // Arrange
        List<NoteResponse> notes = Collections.singletonList(
                new NoteResponse("Note 1", testContent, Arrays.asList("tag1"))
        );
        Page<NoteResponse> notesPage = new PageImpl<>(notes, PageRequest.of(1, 5), 10);

        when(noteService.getAllNotes(any(User.class), eq(1), eq(5), eq(""), isNull()))
                .thenReturn(notesPage);

        // Act & Assert
        mockMvc.perform(get("/api/v1/notes")
                        .param("page", "1")
                        .param("size", "5"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));


        verify(noteService, times(1)).getAllNotes(any(User.class), eq(1), eq(5), eq(""), isNull());
    }

    @Test
    @DisplayName("GET /api/v1/notes - Should successfully search notes by search query")
    @WithMockAuthenticatedUser
    void shouldSuccessfullySearchNotes() throws Exception {
        // Arrange
        String searchQuery = "important";
        List<NoteResponse> notes = Collections.singletonList(noteResponse);
        Page<NoteResponse> notesPage = new PageImpl<>(notes, PageRequest.of(0, 10), notes.size());

        when(noteService.getAllNotes(any(User.class), eq(0), eq(10), eq(searchQuery), isNull()))
                .thenReturn(notesPage);

        // Act & Assert
        mockMvc.perform(get("/api/v1/notes")
                        .param("search", searchQuery))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title").value("Test Note"));

        verify(noteService, times(1)).getAllNotes(any(User.class), eq(0), eq(10), eq(searchQuery), isNull());
    }

    @Test
    @DisplayName("GET /api/v1/notes - Should successfully filter notes by tags")
    @WithMockAuthenticatedUser
    void shouldSuccessfullyFilterNotesByTags() throws Exception {
        // Arrange
        String filterParam = "work,important";
        List<String> expectedTags = Arrays.asList("work", "important");
        List<NoteResponse> notes = Collections.singletonList(noteResponse);
        Page<NoteResponse> notesPage = new PageImpl<>(notes, PageRequest.of(0, 10), notes.size());

        when(noteService.getAllNotes(any(User.class), eq(0), eq(10), eq(""), eq(expectedTags)))
                .thenReturn(notesPage);

        // Act & Assert
        mockMvc.perform(get("/api/v1/notes")
                        .param("filter", filterParam))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));

        verify(noteService, times(1)).getAllNotes(any(User.class), eq(0), eq(10), eq(""), eq(expectedTags));
    }

    @Test
    @DisplayName("GET /api/v1/notes - Should handle empty filter parameter")
    @WithMockAuthenticatedUser
    void shouldHandleEmptyFilterParameter() throws Exception {
        // Arrange
        List<NoteResponse> notes = Collections.singletonList(noteResponse);
        Page<NoteResponse> notesPage = new PageImpl<>(notes, PageRequest.of(0, 10), notes.size());

        when(noteService.getAllNotes(any(User.class), eq(0), eq(10), eq(""), isNull()))
                .thenReturn(notesPage);

        // Act & Assert
        mockMvc.perform(get("/api/v1/notes")
                        .param("filter", ""))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));

        verify(noteService, times(1)).getAllNotes(any(User.class), eq(0), eq(10), eq(""), isNull());
    }

    @Test
    @DisplayName("GET /api/v1/notes - Should filter notes with search and filter combined")
    @WithMockAuthenticatedUser
    void shouldFilterNotesWithSearchAndFilter() throws Exception {
        // Arrange
        String searchQuery = "test";
        String filterParam = "work";
        List<String> expectedTags = Collections.singletonList("work");
        List<NoteResponse> notes = Collections.singletonList(noteResponse);
        Page<NoteResponse> notesPage = new PageImpl<>(notes, PageRequest.of(0, 10), notes.size());

        when(noteService.getAllNotes(any(User.class), eq(0), eq(10), eq(searchQuery), eq(expectedTags)))
                .thenReturn(notesPage);

        // Act & Assert
        mockMvc.perform(get("/api/v1/notes")
                        .param("search", searchQuery)
                        .param("filter", filterParam))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));

        verify(noteService, times(1)).getAllNotes(any(User.class), eq(0), eq(10), eq(searchQuery), eq(expectedTags));
    }

    @Test
    @DisplayName("GET /api/v1/notes/{noteId} - Should successfully retrieve a note by ID")
    @WithMockAuthenticatedUser
    void shouldSuccessfullyGetNoteById() throws Exception {
        // Arrange
        when(noteService.getNoteById(any(User.class), eq(testNoteId)))
                .thenReturn(noteResponse);

        // Act & Assert
        mockMvc.perform(get("/api/v1/notes/{noteId}", testNoteId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.title").value("Test Note"))
                .andExpect(jsonPath("$.tags", hasSize(2)))
                .andExpect(jsonPath("$.tags[0]").value("work"))
                .andExpect(jsonPath("$.tags[1]").value("important"));

        verify(noteService, times(1)).getNoteById(any(User.class), eq(testNoteId));
    }

    @Test
    @DisplayName("DELETE /api/v1/notes/{noteId} - Should successfully delete a note")
    @WithMockAuthenticatedUser
    void shouldSuccessfullyDeleteNote() throws Exception {
        // Arrange
        GenericMessageResponse deleteResponse = GenericMessageResponse.builder()
                .message("Note deleted successfully")
                .build();

        when(noteService.deleteNote(any(User.class), eq(testNoteId)))
                .thenReturn(deleteResponse);

        // Act & Assert
        mockMvc.perform(delete("/api/v1/notes/{noteId}", testNoteId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Note deleted successfully"));

        verify(noteService, times(1)).deleteNote(any(User.class), eq(testNoteId));
    }

    @Test
    @DisplayName("POST /api/v1/notes/{noteId}/restore - Should successfully restore a deleted note")
    @WithMockAuthenticatedUser
    void shouldSuccessfullyRestoreNote() throws Exception {
        // Arrange
        GenericMessageResponse restoreResponse = GenericMessageResponse.builder()
                .message("Note restored successfully")
                .build();

        when(noteService.restoreNote(any(User.class), eq(testNoteId)))
                .thenReturn(restoreResponse);

        // Act & Assert
        mockMvc.perform(post("/api/v1/notes/{noteId}/restore", testNoteId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Note restored successfully"));

        verify(noteService, times(1)).restoreNote(any(User.class), eq(testNoteId));
    }

    @Test
    @DisplayName("PUT /api/v1/notes/{noteId} - Should successfully update a note")
    @WithMockAuthenticatedUser
    void shouldSuccessfullyUpdateNote() throws Exception {
        // Arrange
        NotesRequest updateRequest = NotesRequest.builder()
                .title("Updated Note")
                .content(testContent)
                .tags(Arrays.asList("updated", "modified"))
                .build();

        GenericMessageResponse updateResponse = GenericMessageResponse.builder()
                .message("Note updated successfully")
                .build();

        when(noteService.updateNote(any(User.class), eq(testNoteId), any(NotesRequest.class)))
                .thenReturn(updateResponse);

        // Act & Assert
        mockMvc.perform(put("/api/v1/notes/{noteId}", testNoteId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Note updated successfully"));

        verify(noteService, times(1)).updateNote(any(User.class), eq(testNoteId), any(NotesRequest.class));
    }

    @Test
    @DisplayName("PUT /api/v1/notes/{noteId} - Should fail when updating with blank title")
    @WithMockAuthenticatedUser
    void shouldFailWhenUpdatingWithBlankTitle() throws Exception {
        // Arrange
        NotesRequest invalidRequest = NotesRequest.builder()
                .title("")
                .content(testContent)
                .tags(Collections.singletonList("work"))
                .build();

        // Act & Assert
        mockMvc.perform(put("/api/v1/notes/{noteId}", testNoteId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());

        verify(noteService, never()).updateNote(any(User.class), any(UUID.class), any(NotesRequest.class));
    }


    @Test
    @DisplayName("Should verify that authenticated user is passed to service layer")
    @WithMockAuthenticatedUser
    void shouldVerifyAuthenticatedUserIsPassedToService() throws Exception {
        // Arrange
        when(noteService.createNote(any(User.class), any(NotesRequest.class)))
                .thenReturn(successResponse);

        // Act & Assert
        mockMvc.perform(post("/api/v1/notes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validNotesRequest)))
                .andDo(print())
                .andExpect(status().isCreated());

        // Verify that the service was called with a User object (the @AuthenticationPrincipal)
        verify(noteService, times(1)).createNote(any(User.class), any(NotesRequest.class));
    }
}

