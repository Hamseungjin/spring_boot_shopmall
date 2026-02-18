package com.hsj.dto.auth;

import com.hsj.dto.member.MemberResponse;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoginResponse {

    private TokenResponse token;
    private MemberResponse member;

    public static LoginResponse of(TokenResponse token, MemberResponse member) {
        return LoginResponse.builder()
                .token(token)
                .member(member)
                .build();
    }
}
