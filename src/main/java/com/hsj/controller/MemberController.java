package com.hsj.controller;

import com.hsj.dto.common.ApiResponse;
import com.hsj.dto.member.MemberResponse;
import com.hsj.security.CustomUserDetails;
import com.hsj.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MemberResponse>> getMyInfo(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        MemberResponse response = memberService.getMyInfo(userDetails.getMemberId());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
