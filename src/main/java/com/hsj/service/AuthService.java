package com.hsj.service;

import com.hsj.dto.auth.LoginResponse;
import com.hsj.dto.auth.TokenRefreshRequest;
import com.hsj.dto.auth.TokenResponse;
import com.hsj.dto.member.MemberJoinRequest;
import com.hsj.dto.member.MemberLoginRequest;
import com.hsj.dto.member.MemberResponse;
import com.hsj.entity.Member;
import com.hsj.exception.BusinessException;
import com.hsj.exception.DuplicateException;
import com.hsj.exception.ErrorCode;
import com.hsj.exception.NotFoundException;
import com.hsj.repository.MemberRepository;
import com.hsj.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public MemberResponse signup(MemberJoinRequest request) {
        if (memberRepository.existsByEmailAndDeletedFalse(request.getEmail())) {
            throw new DuplicateException(ErrorCode.DUPLICATE_EMAIL);
        }

        Member member = Member.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .phone(request.getPhone())
                .address(request.getAddress())
                .build();

        Member saved = memberRepository.save(member);
        log.info("회원가입 완료: id={}, email={}", saved.getId(), saved.getEmail());

        return MemberResponse.from(saved);
    }

    public LoginResponse login(MemberLoginRequest request) {
        Member member = memberRepository.findByEmailAndDeletedFalse(request.getEmail())
                .orElseThrow(() -> new NotFoundException(ErrorCode.MEMBER_NOT_FOUND));

        if (!passwordEncoder.matches(request.getPassword(), member.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD);
        }

        TokenResponse token = issueTokens(member);
        MemberResponse memberResponse = MemberResponse.from(member);

        log.info("로그인 성공: id={}, email={}", member.getId(), member.getEmail());
        return LoginResponse.of(token, memberResponse);
    }

    public TokenResponse refresh(TokenRefreshRequest request) {
        String refreshToken = request.getRefreshToken();

        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        Long memberId = jwtTokenProvider.getMemberIdFromToken(refreshToken);

        if (!jwtTokenProvider.validateRefreshToken(memberId, refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN,
                    "저장된 리프레시 토큰과 일치하지 않습니다.");
        }

        Member member = memberRepository.findById(memberId)
                .filter(m -> !m.isDeleted())
                .orElseThrow(() -> new NotFoundException(ErrorCode.MEMBER_NOT_FOUND));

        log.info("토큰 갱신 완료: memberId={}", memberId);
        return issueTokens(member);
    }

    public void logout(Long memberId, String accessToken) {
        jwtTokenProvider.deleteRefreshToken(memberId);

        long remaining = jwtTokenProvider.getRemainingExpiration(accessToken);
        jwtTokenProvider.addToBlacklist(accessToken, remaining);

        log.info("로그아웃 완료: memberId={}", memberId);
    }

    private TokenResponse issueTokens(Member member) {
        String accessToken = jwtTokenProvider.createAccessToken(
                member.getId(), member.getEmail(), member.getRole().name()
        );
        String refreshToken = jwtTokenProvider.createRefreshToken(member.getId());

        return TokenResponse.of(
                accessToken,
                refreshToken,
                jwtTokenProvider.getAccessTokenExpiration() / 1000
        );
    }
}
