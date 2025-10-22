package com.clement.dexwin.domain.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import static com.clement.dexwin.utils.ConstantMessages.EMAIL_NOT_BLANK;
import static com.clement.dexwin.utils.ConstantMessages.EMAIL_NOT_NULL;
import static com.clement.dexwin.utils.ConstantMessages.FIRST_NAME_NOT_BLANK;
import static com.clement.dexwin.utils.ConstantMessages.FIRST_NAME_NOT_NULL;
import static com.clement.dexwin.utils.ConstantMessages.LAST_NAME_NOT_BLANK;
import static com.clement.dexwin.utils.ConstantMessages.LAST_NAME_NOT_NULL;
import static com.clement.dexwin.utils.ConstantMessages.PASSWORD_NOT_BLANK;
import static com.clement.dexwin.utils.ConstantMessages.PASSWORD_NOT_NULL;
import static com.clement.dexwin.utils.ConstantMessages.PASSWORD_SIZE;


@Builder
public record SignupRequestDto(

    @NotNull(message = FIRST_NAME_NOT_NULL)
    @NotBlank(message = FIRST_NAME_NOT_BLANK)
    String firstName,

    String middleName,

    @NotNull(message = LAST_NAME_NOT_NULL)
    @NotBlank(message = LAST_NAME_NOT_BLANK)
    String lastName,
    @NotNull(message = EMAIL_NOT_NULL)
    @Email(message = EMAIL_NOT_BLANK)
    String email,

    @NotNull(message = PASSWORD_NOT_NULL)
    @NotBlank(message = PASSWORD_NOT_BLANK)
    @Size(min = 8, max = 32, message = PASSWORD_SIZE)
    String password
) {
}