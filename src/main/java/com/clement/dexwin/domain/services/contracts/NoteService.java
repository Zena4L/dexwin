package com.clement.dexwin.domain.services.contracts;

import com.clement.dexwin.domain.dtos.GenericMessageResponse;
import com.clement.dexwin.domain.dtos.notes.NoteResponse;
import com.clement.dexwin.domain.dtos.notes.NotesRequest;
import com.clement.dexwin.domain.models.users.User;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.UUID;

public interface NoteService {
    GenericMessageResponse createNote(User user, NotesRequest request);

    Page<NoteResponse> getAllNotes(User user, int page, int size, String search, List<String> filterTags);

    NoteResponse getNoteById(User user, UUID noteId);

    GenericMessageResponse deleteNote(User user, UUID noteId);

    GenericMessageResponse restoreNote(User user, UUID noteId);

    GenericMessageResponse updateNote(User user, UUID noteId, NotesRequest request);
}
