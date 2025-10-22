package com.clement.dexwin.domain.services.contracts;

import com.clement.dexwin.domain.dtos.SignedUpSucessResponse;
import com.clement.dexwin.domain.dtos.SignupRequestDto;


public interface AuthService {

    SignedUpSucessResponse register(SignupRequestDto request);

    void logout();
}