package com.clement.dexwin.domain.dtos.notes;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

public record NoteResponse(
        String title, JsonNode content, List<String> tags) {
}
