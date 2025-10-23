package com.clement.dexwin.domain.services.implementations;

import com.clement.dexwin.domain.dtos.GenericMessageResponse;
import com.clement.dexwin.domain.dtos.notes.NoteResponse;
import com.clement.dexwin.domain.dtos.notes.NotesRequest;
import com.clement.dexwin.domain.models.notes.Note;
import com.clement.dexwin.domain.models.users.Roles;
import com.clement.dexwin.domain.models.users.User;
import com.clement.dexwin.domain.repository.NotesRepository;
import com.clement.dexwin.exceptions.BadRequestException;
import com.clement.dexwin.exceptions.ConflictException;
import com.clement.dexwin.exceptions.GenericException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.OptimisticLockException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.clement.dexwin.utils.ConstantMessages.INVALID_PAGE_NUMBER;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NoteServiceImplTest {

    @Mock
    private NotesRepository notesRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private NoteServiceImpl noteService;

    private User testUser;
    private User adminUser;
    private Note testNote;
    private NotesRequest notesRequest;
    private JsonNode contentNode;
    private String contentJson;
    private UUID noteId;

    @BeforeEach
    void setUp() throws JsonProcessingException {
        noteId = UUID.randomUUID();

        testUser = User.builder()
                .id(UUID.randomUUID())
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .roles(Roles.VIEWER)
                .isActive(true)
                .build();

        adminUser = User.builder()
                .id(UUID.randomUUID())
                .firstName("Admin")
                .lastName("User")
                .email("admin@example.com")
                .roles(Roles.ADMIN)
                .isActive(true)
                .build();

        // Create mock JSON content
        contentNode = mock(JsonNode.class);
        contentJson = "{\"text\":\"This is a test note\"}";

        notesRequest = NotesRequest.builder()
                .title("Test Note")
                .content(contentNode)
                .tags(Arrays.asList("tag1", "tag2"))
                .build();

        testNote = Note.builder()
                .id(noteId)
                .title("Test Note")
                .content(contentJson)
                .tags(Arrays.asList("tag1", "tag2"))
                .user(testUser)
                .deleted(false)
                .build();
    }

    @Nested
    @DisplayName("Create Note Tests")
    class CreateNoteTests {

        @Test
        @DisplayName("Should successfully create a note with valid data")
        void shouldCreateNoteSuccessfully() throws JsonProcessingException {
            // Arrange
            when(objectMapper.writeValueAsString(contentNode)).thenReturn(contentJson);
            when(notesRepository.save(any(Note.class))).thenReturn(testNote);

            // Act
            GenericMessageResponse response = noteService.createNote(testUser, notesRequest);

            // Assert
            assertNotNull(response);
            assertEquals("Note created successfully", response.message());

            ArgumentCaptor<Note> noteCaptor = ArgumentCaptor.forClass(Note.class);
            verify(notesRepository, times(1)).save(noteCaptor.capture());

            Note savedNote = noteCaptor.getValue();
            assertEquals("Test Note", savedNote.getTitle());
            assertEquals(contentJson, savedNote.getContent());
            assertEquals(testUser, savedNote.getUser());
            assertEquals(2, savedNote.getTags().size());
            verify(objectMapper, times(1)).writeValueAsString(contentNode);
        }

        @Test
        @DisplayName("Should trim title when creating note")
        void shouldTrimTitleWhenCreatingNote() throws JsonProcessingException {
            // Arrange
            NotesRequest requestWithSpaces = NotesRequest.builder()
                    .title("  Test Note  ")
                    .content(contentNode)
                    .tags(Arrays.asList("tag1"))
                    .build();

            when(objectMapper.writeValueAsString(contentNode)).thenReturn(contentJson);
            when(notesRepository.save(any(Note.class))).thenReturn(testNote);

            // Act
            noteService.createNote(testUser, requestWithSpaces);

            // Assert
            ArgumentCaptor<Note> noteCaptor = ArgumentCaptor.forClass(Note.class);
            verify(notesRepository).save(noteCaptor.capture());
            assertEquals("Test Note", noteCaptor.getValue().getTitle());
        }

        @Test
        @DisplayName("Should create note with null tags")
        void shouldCreateNoteWithNullTags() throws JsonProcessingException {
            // Arrange
            NotesRequest requestWithNullTags = NotesRequest.builder()
                    .title("Test Note")
                    .content(contentNode)
                    .tags(null)
                    .build();

            when(objectMapper.writeValueAsString(contentNode)).thenReturn(contentJson);
            when(notesRepository.save(any(Note.class))).thenReturn(testNote);

            // Act
            noteService.createNote(testUser, requestWithNullTags);

            // Assert
            ArgumentCaptor<Note> noteCaptor = ArgumentCaptor.forClass(Note.class);
            verify(notesRepository).save(noteCaptor.capture());
            assertNull(noteCaptor.getValue().getTags());
        }

        @Test
        @DisplayName("Should throw GenericException when JSON serialization fails")
        void shouldThrowExceptionWhenSerializationFails() throws JsonProcessingException {
            // Arrange
            when(objectMapper.writeValueAsString(contentNode))
                    .thenThrow(new JsonProcessingException("Serialization failed") {});

            // Act & Assert
            GenericException exception = assertThrows(GenericException.class,
                    () -> noteService.createNote(testUser, notesRequest));

            assertTrue(exception.getMessage().contains("Failed to process JSON content"));
            verify(notesRepository, never()).save(any(Note.class));
        }
    }

    @Nested
    @DisplayName("Get All Notes Tests")
    class GetAllNotesTests {

        @Test
        @DisplayName("Should get all notes for regular user")
        void shouldGetAllNotesForRegularUser() throws JsonProcessingException {
            // Arrange
            List<Note> notes = Arrays.asList(testNote);
            Page<Note> notePage = new PageImpl<>(notes);
            Pageable pageRequest = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "updatedAt"));

            when(notesRepository.findAll(any(Specification.class), eq(pageRequest))).thenReturn(notePage);
            when(objectMapper.readTree(contentJson)).thenReturn(contentNode);

            // Act
            Page<NoteResponse> response = noteService.getAllNotes(testUser, 0, 10, null, null);

            // Assert
            assertNotNull(response);
            assertEquals(1, response.getTotalElements());
            assertEquals("Test Note", response.getContent().get(0).title());
            verify(notesRepository, times(1)).findAll(any(Specification.class), eq(pageRequest));
            verify(objectMapper, times(1)).readTree(contentJson);
        }

        @Test
        @DisplayName("Should get all notes for admin user")
        void shouldGetAllNotesForAdminUser() throws JsonProcessingException {
            // Arrange
            List<Note> notes = Arrays.asList(testNote);
            Page<Note> notePage = new PageImpl<>(notes);
            Pageable pageRequest = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "updatedAt"));

            when(notesRepository.findAll(any(Specification.class), eq(pageRequest))).thenReturn(notePage);
            when(objectMapper.readTree(contentJson)).thenReturn(contentNode);

            // Act
            Page<NoteResponse> response = noteService.getAllNotes(adminUser, 0, 10, null, null);

            // Assert
            assertNotNull(response);
            assertEquals(1, response.getTotalElements());
            verify(notesRepository, times(1)).findAll(any(Specification.class), eq(pageRequest));
        }

        @Test
        @DisplayName("Should get notes with search parameter")
        void shouldGetNotesWithSearch() throws JsonProcessingException {
            // Arrange
            List<Note> notes = Arrays.asList(testNote);
            Page<Note> notePage = new PageImpl<>(notes);
            Pageable pageRequest = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "updatedAt"));

            when(notesRepository.findAll(any(Specification.class), eq(pageRequest))).thenReturn(notePage);
            when(objectMapper.readTree(contentJson)).thenReturn(contentNode);

            // Act
            Page<NoteResponse> response = noteService.getAllNotes(testUser, 0, 10, "test", null);

            // Assert
            assertNotNull(response);
            assertEquals(1, response.getTotalElements());
            verify(notesRepository, times(1)).findAll(any(Specification.class), eq(pageRequest));
        }

        @Test
        @DisplayName("Should get notes with filter tags")
        void shouldGetNotesWithFilterTags() throws JsonProcessingException {
            // Arrange
            List<Note> notes = Arrays.asList(testNote);
            Page<Note> notePage = new PageImpl<>(notes);
            Pageable pageRequest = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "updatedAt"));
            List<String> filterTags = Arrays.asList("tag1", "tag2");

            when(notesRepository.findAll(any(Specification.class), eq(pageRequest))).thenReturn(notePage);
            when(objectMapper.readTree(contentJson)).thenReturn(contentNode);

            // Act
            Page<NoteResponse> response = noteService.getAllNotes(testUser, 0, 10, null, filterTags);

            // Assert
            assertNotNull(response);
            assertEquals(1, response.getTotalElements());
            verify(notesRepository, times(1)).findAll(any(Specification.class), eq(pageRequest));
        }

        @Test
        @DisplayName("Should get notes with search and filter tags")
        void shouldGetNotesWithSearchAndFilterTags() throws JsonProcessingException {
            // Arrange
            List<Note> notes = Arrays.asList(testNote);
            Page<Note> notePage = new PageImpl<>(notes);
            Pageable pageRequest = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "updatedAt"));
            List<String> filterTags = Arrays.asList("tag1");

            when(notesRepository.findAll(any(Specification.class), eq(pageRequest))).thenReturn(notePage);
            when(objectMapper.readTree(contentJson)).thenReturn(contentNode);

            // Act
            Page<NoteResponse> response = noteService.getAllNotes(testUser, 0, 10, "test", filterTags);

            // Assert
            assertNotNull(response);
            assertEquals(1, response.getTotalElements());
            verify(notesRepository, times(1)).findAll(any(Specification.class), eq(pageRequest));
        }

        @Test
        @DisplayName("Should throw BadRequestException for negative page number")
        void shouldThrowExceptionForNegativePage() {
            // Act & Assert
            BadRequestException exception = assertThrows(BadRequestException.class,
                    () -> noteService.getAllNotes(testUser, -1, 10, null, null));

            assertEquals(INVALID_PAGE_NUMBER, exception.getMessage());
            verify(notesRepository, never()).findAll(any(Specification.class), any(Pageable.class));
        }

        @Test
        @DisplayName("Should handle empty note list")
        void shouldHandleEmptyNoteList() {
            // Arrange
            Page<Note> emptyPage = new PageImpl<>(new ArrayList<>());
            Pageable pageRequest = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "updatedAt"));

            when(notesRepository.findAll(any(Specification.class), eq(pageRequest))).thenReturn(emptyPage);

            // Act
            Page<NoteResponse> response = noteService.getAllNotes(testUser, 0, 10, null, null);

            // Assert
            assertNotNull(response);
            assertEquals(0, response.getTotalElements());
            assertTrue(response.getContent().isEmpty());
        }

        @Test
        @DisplayName("Should handle note with null content in response")
        void shouldHandleNoteWithNullContent() throws JsonProcessingException {
            // Arrange
            Note noteWithNullContent = Note.builder()
                    .id(UUID.randomUUID())
                    .title("Test Note")
                    .content(null)
                    .tags(Arrays.asList("tag1"))
                    .user(testUser)
                    .deleted(false)
                    .build();

            Page<Note> notePage = new PageImpl<>(Arrays.asList(noteWithNullContent));
            Pageable pageRequest = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "updatedAt"));

            when(notesRepository.findAll(any(Specification.class), eq(pageRequest))).thenReturn(notePage);

            // Act
            Page<NoteResponse> response = noteService.getAllNotes(testUser, 0, 10, null, null);

            // Assert
            assertNotNull(response);
            assertEquals(1, response.getTotalElements());
            assertNull(response.getContent().get(0).content());
            verify(objectMapper, never()).readTree(anyString());
        }
    }

    @Nested
    @DisplayName("Get Note By ID Tests")
    class GetNoteByIdTests {

        @Test
        @DisplayName("Should get note by ID successfully")
        void shouldGetNoteByIdSuccessfully() throws JsonProcessingException {
            // Arrange
            when(notesRepository.findNoteById(noteId)).thenReturn(Optional.of(testNote));
            when(objectMapper.readTree(contentJson)).thenReturn(contentNode);

            // Act
            NoteResponse response = noteService.getNoteById(testUser, noteId);

            // Assert
            assertNotNull(response);
            assertEquals("Test Note", response.title());
            assertEquals(contentNode, response.content());
            assertEquals(2, response.tags().size());
            verify(notesRepository, times(1)).findNoteById(noteId);
            verify(objectMapper, times(1)).readTree(contentJson);
        }

        @Test
        @DisplayName("Should throw BadRequestException when note not found")
        void shouldThrowExceptionWhenNoteNotFound() throws JsonProcessingException {
            // Arrange
            UUID nonExistentId = UUID.randomUUID();
            when(notesRepository.findNoteById(nonExistentId)).thenReturn(Optional.empty());

            // Act & Assert
            BadRequestException exception = assertThrows(BadRequestException.class,
                    () -> noteService.getNoteById(testUser, nonExistentId));

            assertTrue(exception.getMessage().contains("Note not found with id"));
            verify(notesRepository, times(1)).findNoteById(nonExistentId);
            verify(objectMapper, never()).readTree(anyString());
        }

        @Test
        @DisplayName("Should handle note with null content")
        void shouldHandleNoteWithNullContent() throws JsonProcessingException {
            // Arrange
            Note noteWithNullContent = Note.builder()
                    .id(noteId)
                    .title("Test Note")
                    .content(null)
                    .tags(Arrays.asList("tag1"))
                    .user(testUser)
                    .build();

            when(notesRepository.findNoteById(noteId)).thenReturn(Optional.of(noteWithNullContent));

            // Act
            NoteResponse response = noteService.getNoteById(testUser, noteId);

            // Assert
            assertNotNull(response);
            assertNull(response.content());
            verify(objectMapper, never()).readTree(anyString());
        }

        @Test
        @DisplayName("Should throw GenericException when JSON deserialization fails")
        void shouldThrowExceptionWhenDeserializationFails() throws JsonProcessingException {
            // Arrange
            when(notesRepository.findNoteById(noteId)).thenReturn(Optional.of(testNote));
            when(objectMapper.readTree(contentJson))
                    .thenThrow(new JsonProcessingException("Deserialization failed") {});

            // Act & Assert
            GenericException exception = assertThrows(GenericException.class,
                    () -> noteService.getNoteById(testUser, noteId));

            assertTrue(exception.getMessage().contains("Failed to process JSON content"));
        }
    }

    @Nested
    @DisplayName("Delete Note Tests")
    class DeleteNoteTests {

        @Test
        @DisplayName("Should delete note successfully")
        void shouldDeleteNoteSuccessfully() {
            // Arrange
            when(notesRepository.findActiveNoteByIdAndUser(noteId, testUser))
                    .thenReturn(Optional.of(testNote));
            doNothing().when(notesRepository).delete(any(Note.class));

            // Act
            GenericMessageResponse response = noteService.deleteNote(testUser, noteId);

            // Assert
            assertNotNull(response);
            assertEquals("Note deleted successfully", response.message());
            assertNotNull(testNote.getDeletedAt());
            verify(notesRepository, times(1)).findActiveNoteByIdAndUser(noteId, testUser);
            verify(notesRepository, times(1)).delete(testNote);
        }

        @Test
        @DisplayName("Should throw BadRequestException when note not found")
        void shouldThrowExceptionWhenNoteNotFound() {
            // Arrange
            when(notesRepository.findActiveNoteByIdAndUser(noteId, testUser))
                    .thenReturn(Optional.empty());

            // Act & Assert
            BadRequestException exception = assertThrows(BadRequestException.class,
                    () -> noteService.deleteNote(testUser, noteId));

            assertEquals("Note not found or already deleted", exception.getMessage());
            verify(notesRepository, times(1)).findActiveNoteByIdAndUser(noteId, testUser);
            verify(notesRepository, never()).delete(any(Note.class));
        }

        @Test
        @DisplayName("Should throw BadRequestException when note already deleted")
        void shouldThrowExceptionWhenNoteAlreadyDeleted() {
            // Arrange
            Note deletedNote = Note.builder()
                    .id(noteId)
                    .title("Deleted Note")
                    .deleted(true)
                    .deletedAt(Instant.now())
                    .user(testUser)
                    .build();

            when(notesRepository.findActiveNoteByIdAndUser(noteId, testUser))
                    .thenReturn(Optional.empty());

            // Act & Assert
            BadRequestException exception = assertThrows(BadRequestException.class,
                    () -> noteService.deleteNote(testUser, noteId));

            assertEquals("Note not found or already deleted", exception.getMessage());
            verify(notesRepository, never()).delete(any(Note.class));
        }
    }

    @Nested
    @DisplayName("Restore Note Tests")
    class RestoreNoteTests {

        @Test
        @DisplayName("Should restore note successfully")
        void shouldRestoreNoteSuccessfully() {
            // Arrange
            when(notesRepository.restoreNote(noteId, testUser)).thenReturn(1);

            // Act
            GenericMessageResponse response = noteService.restoreNote(testUser, noteId);

            // Assert
            assertNotNull(response);
            assertEquals("Note restored successfully", response.message());
            verify(notesRepository, times(1)).restoreNote(noteId, testUser);
        }

        @Test
        @DisplayName("Should throw BadRequestException when note not found for restore")
        void shouldThrowExceptionWhenNoteNotFoundForRestore() {
            // Arrange
            when(notesRepository.restoreNote(noteId, testUser)).thenReturn(0);

            // Act & Assert
            BadRequestException exception = assertThrows(BadRequestException.class,
                    () -> noteService.restoreNote(testUser, noteId));

            assertEquals("Note not found or not deleted", exception.getMessage());
            verify(notesRepository, times(1)).restoreNote(noteId, testUser);
        }

        @Test
        @DisplayName("Should throw BadRequestException when note is not deleted")
        void shouldThrowExceptionWhenNoteIsNotDeleted() {
            // Arrange
            when(notesRepository.restoreNote(noteId, testUser)).thenReturn(0);

            // Act & Assert
            BadRequestException exception = assertThrows(BadRequestException.class,
                    () -> noteService.restoreNote(testUser, noteId));

            assertEquals("Note not found or not deleted", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Update Note Tests")
    class UpdateNoteTests {

        @Test
        @DisplayName("Should update note successfully")
        void shouldUpdateNoteSuccessfully() throws JsonProcessingException {
            // Arrange
            NotesRequest updateRequest = NotesRequest.builder()
                    .title("Updated Note")
                    .content(contentNode)
                    .tags(Arrays.asList("newTag1", "newTag2"))
                    .build();

            when(notesRepository.findActiveNoteByIdAndUser(noteId, testUser))
                    .thenReturn(Optional.of(testNote));
            when(objectMapper.writeValueAsString(contentNode)).thenReturn(contentJson);
            when(notesRepository.save(any(Note.class))).thenReturn(testNote);

            // Act
            GenericMessageResponse response = noteService.updateNote(testUser, noteId, updateRequest);

            // Assert
            assertNotNull(response);
            assertEquals("Note updated successfully", response.message());
            assertEquals("Updated Note", testNote.getTitle());
            assertEquals(contentJson, testNote.getContent());
            assertEquals(2, testNote.getTags().size());

            verify(notesRepository, times(1)).findActiveNoteByIdAndUser(noteId, testUser);
            verify(objectMapper, times(1)).writeValueAsString(contentNode);
            verify(notesRepository, times(1)).save(testNote);
        }

        @Test
        @DisplayName("Should trim title when updating note")
        void shouldTrimTitleWhenUpdatingNote() throws JsonProcessingException {
            // Arrange
            NotesRequest updateRequest = NotesRequest.builder()
                    .title("  Updated Note  ")
                    .content(contentNode)
                    .tags(Arrays.asList("tag1"))
                    .build();

            when(notesRepository.findActiveNoteByIdAndUser(noteId, testUser))
                    .thenReturn(Optional.of(testNote));
            when(objectMapper.writeValueAsString(contentNode)).thenReturn(contentJson);
            when(notesRepository.save(any(Note.class))).thenReturn(testNote);

            // Act
            noteService.updateNote(testUser, noteId, updateRequest);

            // Assert
            assertEquals("Updated Note", testNote.getTitle());
        }

        @Test
        @DisplayName("Should throw BadRequestException when note not found")
        void shouldThrowExceptionWhenNoteNotFound() {
            // Arrange
            when(notesRepository.findActiveNoteByIdAndUser(noteId, testUser))
                    .thenReturn(Optional.empty());

            // Act & Assert
            BadRequestException exception = assertThrows(BadRequestException.class,
                    () -> noteService.updateNote(testUser, noteId, notesRequest));

            assertEquals("Note not found or has been deleted", exception.getMessage());
            verify(notesRepository, times(1)).findActiveNoteByIdAndUser(noteId, testUser);
            verify(notesRepository, never()).save(any(Note.class));
        }

        @Test
        @DisplayName("Should throw ConflictException on ObjectOptimisticLockingFailureException")
        void shouldThrowConflictExceptionOnOptimisticLockFailure() throws JsonProcessingException {
            // Arrange
            when(notesRepository.findActiveNoteByIdAndUser(noteId, testUser))
                    .thenReturn(Optional.of(testNote));
            when(objectMapper.writeValueAsString(contentNode)).thenReturn(contentJson);
            when(notesRepository.save(any(Note.class)))
                    .thenThrow(new ObjectOptimisticLockingFailureException("Optimistic lock", new Exception()));

            // Act & Assert
            ConflictException exception = assertThrows(ConflictException.class,
                    () -> noteService.updateNote(testUser, noteId, notesRequest));

            assertTrue(exception.getMessage().contains("This note was modified by someone else"));
            verify(notesRepository, times(1)).findActiveNoteByIdAndUser(noteId, testUser);
            verify(notesRepository, times(1)).save(any(Note.class));
        }

        @Test
        @DisplayName("Should throw ConflictException on OptimisticLockException")
        void shouldThrowConflictExceptionOnJpaOptimisticLock() throws JsonProcessingException {
            // Arrange
            when(notesRepository.findActiveNoteByIdAndUser(noteId, testUser))
                    .thenReturn(Optional.of(testNote));
            when(objectMapper.writeValueAsString(contentNode)).thenReturn(contentJson);
            when(notesRepository.save(any(Note.class)))
                    .thenThrow(new OptimisticLockException("Optimistic lock"));

            // Act & Assert
            ConflictException exception = assertThrows(ConflictException.class,
                    () -> noteService.updateNote(testUser, noteId, notesRequest));

            assertTrue(exception.getMessage().contains("This note was modified by someone else"));
            verify(notesRepository, times(1)).save(any(Note.class));
        }

        @Test
        @DisplayName("Should throw GenericException when JSON serialization fails during update")
        void shouldThrowExceptionWhenSerializationFailsDuringUpdate() throws JsonProcessingException {
            // Arrange
            when(notesRepository.findActiveNoteByIdAndUser(noteId, testUser))
                    .thenReturn(Optional.of(testNote));
            when(objectMapper.writeValueAsString(contentNode))
                    .thenThrow(new JsonProcessingException("Serialization failed") {});

            // Act & Assert
            GenericException exception = assertThrows(GenericException.class,
                    () -> noteService.updateNote(testUser, noteId, notesRequest));

            assertTrue(exception.getMessage().contains("Failed to process JSON content"));
            verify(notesRepository, never()).save(any(Note.class));
        }

        @Test
        @DisplayName("Should update note with null tags")
        void shouldUpdateNoteWithNullTags() throws JsonProcessingException {
            // Arrange
            NotesRequest updateRequest = NotesRequest.builder()
                    .title("Updated Note")
                    .content(contentNode)
                    .tags(null)
                    .build();

            when(notesRepository.findActiveNoteByIdAndUser(noteId, testUser))
                    .thenReturn(Optional.of(testNote));
            when(objectMapper.writeValueAsString(contentNode)).thenReturn(contentJson);
            when(notesRepository.save(any(Note.class))).thenReturn(testNote);

            // Act
            GenericMessageResponse response = noteService.updateNote(testUser, noteId, updateRequest);

            // Assert
            assertNotNull(response);
            assertNull(testNote.getTags());
            verify(notesRepository, times(1)).save(testNote);
        }

        @Test
        @DisplayName("Should update note with empty tags")
        void shouldUpdateNoteWithEmptyTags() throws JsonProcessingException {
            // Arrange
            NotesRequest updateRequest = NotesRequest.builder()
                    .title("Updated Note")
                    .content(contentNode)
                    .tags(new ArrayList<>())
                    .build();

            when(notesRepository.findActiveNoteByIdAndUser(noteId, testUser))
                    .thenReturn(Optional.of(testNote));
            when(objectMapper.writeValueAsString(contentNode)).thenReturn(contentJson);
            when(notesRepository.save(any(Note.class))).thenReturn(testNote);

            // Act
            GenericMessageResponse response = noteService.updateNote(testUser, noteId, updateRequest);

            // Assert
            assertNotNull(response);
            assertTrue(testNote.getTags().isEmpty());
            verify(notesRepository, times(1)).save(testNote);
        }
    }

    @Nested
    @DisplayName("JSON Serialization/Deserialization Edge Cases")
    class JsonEdgeCaseTests {

        @Test
        @DisplayName("Should handle null content in serialization")
        void shouldHandleNullContentInSerialization() throws JsonProcessingException {
            // Arrange
            NotesRequest requestWithNullContent = NotesRequest.builder()
                    .title("Test Note")
                    .content(null)
                    .tags(Arrays.asList("tag1"))
                    .build();

            when(notesRepository.save(any(Note.class))).thenReturn(testNote);

            // Act
            GenericMessageResponse response = noteService.createNote(testUser, requestWithNullContent);

            // Assert
            assertNotNull(response);
            verify(objectMapper, never()).writeValueAsString(any());
            ArgumentCaptor<Note> noteCaptor = ArgumentCaptor.forClass(Note.class);
            verify(notesRepository).save(noteCaptor.capture());
            assertNull(noteCaptor.getValue().getContent());
        }

        @Test
        @DisplayName("Should deserialize complex JSON content")
        void shouldDeserializeComplexJsonContent() throws JsonProcessingException {
            // Arrange
            String complexJson = "{\"text\":\"Note\",\"items\":[1,2,3],\"nested\":{\"key\":\"value\"}}";
            Note complexNote = Note.builder()
                    .id(noteId)
                    .title("Complex Note")
                    .content(complexJson)
                    .tags(Arrays.asList("tag1"))
                    .user(testUser)
                    .build();

            JsonNode complexNode = mock(JsonNode.class);
            when(notesRepository.findNoteById(noteId)).thenReturn(Optional.of(complexNote));
            when(objectMapper.readTree(complexJson)).thenReturn(complexNode);

            // Act
            NoteResponse response = noteService.getNoteById(testUser, noteId);

            // Assert
            assertNotNull(response);
            assertEquals(complexNode, response.content());
            verify(objectMapper, times(1)).readTree(complexJson);
        }
    }
}

