package com.clement.dexwin.domain.dtos.notes;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record NoteResponse(
        UUID id, String title, JsonNode content, List<String> tags, Instant createdAt, String createdBy) {
}
