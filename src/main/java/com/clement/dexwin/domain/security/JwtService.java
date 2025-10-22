package com.clement.dexwin.domain.security;


import com.clement.dexwin.domain.models.User;

public interface JwtService {
    String generateToken(User user);
}