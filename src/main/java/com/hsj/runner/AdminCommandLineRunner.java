package com.hsj.runner;

import com.hsj.entity.Member;
import com.hsj.entity.enums.MemberRole;
import com.hsj.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 개발 환경(@Profile("dev"))에서만 실행되는 기본 관리자 계정 생성기.
 * 애플리케이션이 완전히 기동된 후 CommandLineRunner.run()이 호출된다.
 * DB에 ADMIN 계정이 존재하지 않을 경우에만 기본 관리자를 INSERT한다.
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class AdminCommandLineRunner implements CommandLineRunner {

    private static final String DEFAULT_ADMIN_EMAIL = "hsj0403@admin.com";
    private static final String DEFAULT_ADMIN_NAME  = "hsj0403";
    private static final String DEFAULT_ADMIN_RAW_PASSWORD = "runner12!@";

    private final MemberRepository memberRepository;
    private final PasswordEncoder  passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (memberRepository.existsByRoleAndDeletedFalse(MemberRole.ADMIN)) {
            log.info("[AdminCommandLineRunner] ADMIN 계정이 이미 존재합니다. 기본 계정 생성을 건너뜁니다.");
            return;
        }

        Member admin = Member.builder()
                .email(DEFAULT_ADMIN_EMAIL)
                .password(passwordEncoder.encode(DEFAULT_ADMIN_RAW_PASSWORD))
                .name(DEFAULT_ADMIN_NAME)
                .role(MemberRole.ADMIN)
                .build();

        memberRepository.save(admin);

        log.info("[AdminCommandLineRunner] 기본 관리자 계정이 생성되었습니다. email={}", DEFAULT_ADMIN_EMAIL);
    }
}
