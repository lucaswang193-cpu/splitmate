package com.splitmate.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.splitmate.entity.ExpenseCategory;

public interface ExpenseCategoryRepository extends JpaRepository<ExpenseCategory, Long> {

    Optional<ExpenseCategory> findByName(String name);

    boolean existsByName(String name);
}
