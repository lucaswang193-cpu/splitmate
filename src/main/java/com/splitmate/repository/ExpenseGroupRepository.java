package com.splitmate.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.splitmate.entity.ExpenseGroup;

public interface ExpenseGroupRepository extends JpaRepository<ExpenseGroup, UUID> {

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select g from ExpenseGroup g where g.id = :id")
    Optional<ExpenseGroup> findForExpenseCreationById(@org.springframework.data.repository.query.Param("id") UUID id);

    List<ExpenseGroup> findByOwner_Id(UUID ownerId);

    Optional<ExpenseGroup> findByInviteCode(String inviteCode);

    boolean existsByInviteCode(String inviteCode);
}
