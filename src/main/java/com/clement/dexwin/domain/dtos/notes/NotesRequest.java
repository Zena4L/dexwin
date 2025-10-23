package com.clement.dexwin.domain.dtos.notes;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.List;

import static com.clement.dexwin.utils.ConstantMessages.NOTESNOTBLANKORNULL;

@Builder
public record NotesRequest(
        @NotNull(message = NOTESNOTBLANKORNULL)
        @NotBlank(message = NOTESNOTBLANKORNULL)
        String title,

        @NotNull(message = NOTESNOTBLANKORNULL)
        JsonNode content,

        List<String> tags
) {
}
