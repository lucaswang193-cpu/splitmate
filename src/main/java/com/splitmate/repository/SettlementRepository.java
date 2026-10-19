package com.splitmate.repository;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.splitmate.entity.Settlement;

public interface SettlementRepository extends JpaRepository<Settlement, Long> {

    Page<Settlement> findByGroup_Id(UUID groupId, Pageable pageable);

    Page<Settlement> findByFromUser_Id(UUID fromUserId, Pageable pageable);

    Page<Settlement> findByToUser_Id(UUID toUserId, Pageable pageable);

    // 這位成員在指定群組中，已付出的結算款項
    @Query("""
            select coalesce(sum(s.amount), 0)
            from Settlement s
            where s.group.id = :groupId
              and s.fromUser.id = :userId
            """)
    BigDecimal sumSentAmount(
            @Param("groupId") UUID groupId,
            @Param("userId") UUID userId
    );

    // 這位成員在指定群組中，已收到的結算款項
    @Query("""
            select coalesce(sum(s.amount), 0)
            from Settlement s
            where s.group.id = :groupId
              and s.toUser.id = :userId
            """)
    BigDecimal sumReceivedAmount(
            @Param("groupId") UUID groupId,
            @Param("userId") UUID userId
    );
}
