package com.hsj.repository;

import com.hsj.entity.Member;
import com.hsj.entity.enums.MemberRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByEmailAndDeletedFalse(String email);

    boolean existsByEmailAndDeletedFalse(String email);

    boolean existsByRoleAndDeletedFalse(MemberRole role);
}
