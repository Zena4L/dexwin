package com.clement.dexwin.domain.services.contracts;

import com.clement.dexwin.domain.dtos.LoginRequestDto;
import com.clement.dexwin.domain.dtos.SignedUpSucessResponse;
import com.clement.dexwin.domain.dtos.signinResponse;
import com.clement.dexwin.domain.dtos.SignupRequestDto;
import jakarta.validation.Valid;


public interface AuthService {

    SignedUpSucessResponse register(SignupRequestDto request);

    void logout();

    signinResponse login(@Valid LoginRequestDto request);
}