package com.splitmate.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.splitmate.entity.GroupMember;

public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {

    List<GroupMember> findByGroup_Id(UUID groupId);

    Page<GroupMember> findByGroup_Id(UUID groupId, Pageable pageable);

    List<GroupMember> findByUser_Id(UUID userId);

    Page<GroupMember> findByUser_Id(UUID userId, Pageable pageable);

    Optional<GroupMember> findByGroup_IdAndUser_Id(UUID groupId, UUID userId);

    boolean existsByGroup_IdAndUser_Id(UUID groupId, UUID userId);
}
