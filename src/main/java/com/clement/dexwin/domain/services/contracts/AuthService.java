package com.clement.dexwin.domain.services.contracts;

import com.clement.dexwin.domain.dtos.users.LoginRequestDto;
import com.clement.dexwin.domain.dtos.users.SignedUpSucessResponse;
import com.clement.dexwin.domain.dtos.users.signinResponse;
import com.clement.dexwin.domain.dtos.users.SignupRequestDto;
import jakarta.validation.Valid;


public interface AuthService {

    SignedUpSucessResponse register(SignupRequestDto request);

    void logout();

    signinResponse login(@Valid LoginRequestDto request);
}