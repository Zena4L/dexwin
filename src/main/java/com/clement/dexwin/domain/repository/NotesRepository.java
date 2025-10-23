package com.clement.dexwin.domain.repository;

import com.clement.dexwin.domain.models.notes.Note;
import com.clement.dexwin.domain.models.users.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface NotesRepository extends JpaRepository<Note, UUID>, JpaSpecificationExecutor<Note> {
    @Query("select n from Note n where n.id = :noteId and n.deleted = false")
    Optional<Note> findNoteById(UUID noteId);


    @Query("SELECT n FROM Note n WHERE n.id = :id AND n.user = :user AND n.deleted = false")
    Optional<Note> findActiveNoteByIdAndUser(@Param("id") UUID id, @Param("user") User user);

    @Query("SELECT n FROM Note n WHERE n.id = :id AND n.user = :user AND n.deleted = true")
    Optional<Note> findDeletedNoteByIdAndUser(@Param("id") UUID id, @Param("user") User user);

    @Modifying
    @Query("UPDATE Note n SET n.deleted = false, n.deletedAt = null WHERE n.id = :id AND n.user = :user")
    int restoreNote(@Param("id") UUID id, @Param("user") User user);
}
