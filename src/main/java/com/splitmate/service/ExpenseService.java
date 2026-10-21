package com.splitmate.service;

import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.Objects;
import java.util.HashSet;
import com.splitmate.exception.ConflictException;
import com.splitmate.repository.ExpenseGroupRepository;
import jakarta.persistence.EntityManager;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.splitmate.dto.balance.BalanceResponse;
import com.splitmate.dto.expense.CreateExpenseRequest;
import com.splitmate.dto.expense.ExpenseResponse;
import com.splitmate.dto.expense.ExpenseSplitRequest;
import com.splitmate.dto.expense.UpdateExpenseRequest;
import com.splitmate.entity.Currency;
import com.splitmate.entity.Expense;
import com.splitmate.entity.ExpenseCategory;
import com.splitmate.entity.ExpenseGroup;
import com.splitmate.entity.ExpenseShare;
import com.splitmate.entity.GroupMember;
import com.splitmate.entity.User;
import com.splitmate.exception.BusinessException;
import com.splitmate.mapper.ExpenseMapper;
import com.splitmate.repository.ExpenseRepository;
import com.splitmate.repository.ExpenseShareRepository;
import com.splitmate.repository.GroupMemberRepository;
import com.splitmate.repository.SettlementRepository;
import com.splitmate.response.PageResponse;


@Service
@RequiredArgsConstructor
public class ExpenseService {

    private static final String STATUS_PAID = "PAID";
    private static final String STATUS_OWING = "OWING";

    private final ExpenseRepository expenseRepository;
    private final ExpenseGroupRepository expenseGroupRepository;
    private final EntityManager entityManager;
    private final ExpenseShareRepository expenseShareRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final ExpenseGroupService expenseGroupService;
    private final UserService userService;
    private final CurrencyService currencyService;
    private final ExpenseCategoryService expenseCategoryService;
    private final GroupMemberService groupMemberService;
    private final SettlementRepository settlementRepository;
    private final DataPermissionService dataPermissionService;
    // 建立支出，並建立對應的分帳紀錄
    @Transactional
    public ExpenseResponse createExpense(CreateExpenseRequest request) {
        dataPermissionService.requireGroupMember(request.groupId());
        // Serialize creation within a group so concurrent retries see the committed
        // expense.
        ExpenseGroup group = expenseGroupRepository.findForExpenseCreationById(request.groupId())
                .orElseThrow(() -> new BusinessException("Group not found"));
        Expense existing = expenseRepository.findById(request.id()).orElse(null);
        if (existing != null) {
            if (!sameExpense(existing, request)) {
                throw new ConflictException("Expense id already exists with different content");
            }
            return ExpenseMapper.toDto(existing);
        }
        User payer = userService.getUserEntityById(request.payerId());
        Currency currency = currencyService.getCurrencyEntityByCode(request.currency());
        ExpenseCategory category = request.categoryId() == null
                ? null
                : expenseCategoryService.getCategoryEntityById(request.categoryId());

        groupMemberService.validateUserInGroup(group.getId(), payer.getId());
        validateSplits(group.getId(), request.baseAmount(), request.splits());

        Expense expense = Expense.builder()
                .id(request.id())
                .group(group)
                .payer(payer)
                .title(request.title())
                .category(category)
                .currency(currency)
                .amount(request.amount())
                .exchangeRate(request.exchangeRate())
                .baseAmount(request.baseAmount())
                .expenseAt(request.expenseAt())
                .note(request.note())
                .build();

        // Assigned IDs must be inserted, never merged into an existing expense.
        entityManager.persist(expense);
        entityManager.flush();
        Expense savedExpense = expense;

        List<ExpenseShare> shares = request.splits()
                .stream()
                .map(split -> toExpenseShare(savedExpense, split))
                .toList();

        expenseShareRepository.saveAll(shares);
        savedExpense.setShares(shares);

        return ExpenseMapper.toDto(savedExpense);
    }

    // 檢查現有支出與請求是否相同，避免重複建立支出
    private boolean sameExpense(Expense expense, CreateExpenseRequest request) {
        if (!expense.getGroup().getId().equals(request.groupId())
                || !expense.getPayer().getId().equals(request.payerId())
                || !Objects.equals(expense.getTitle(), request.title())
                || !Objects.equals(expense.getCategory() == null ? null : expense.getCategory().getId(),
                        request.categoryId())
                || !expense.getCurrency().getCode().equals(request.currency())
                || expense.getAmount().compareTo(request.amount()) != 0
                || expense.getExchangeRate().compareTo(request.exchangeRate()) != 0
                || expense.getBaseAmount().compareTo(request.baseAmount()) != 0
                || !expense.getExpenseAt().equals(request.expenseAt())
                || !Objects.equals(expense.getNote(), request.note())
                || expense.getShares().size() != request.splits().size()) {
            return false;
        }
        var users = new HashSet<UUID>();
        return request.splits().stream().allMatch(split -> users.add(split.userId())
                && expense.getShares().stream().anyMatch(share -> share.getUser().getId().equals(split.userId())
                        && share.getShareAmount().compareTo(split.shareAmount()) == 0
                        && share.getPaidAmount().compareTo(split.paidAmount() == null
                                ? BigDecimal.ZERO
                                : split.paidAmount()) == 0));
    }

    // 更新支出時會重建分帳紀錄，避免舊 splits 殘留造成餘額計算錯誤。
    @Transactional
    public ExpenseResponse updateExpense(UUID expenseId, UpdateExpenseRequest request) {
        Expense expense = getExpenseEntityById(expenseId);
        dataPermissionService.requireExpenseEditor(expense);
        ExpenseGroup group = expense.getGroup();
        User payer = userService.getUserEntityById(request.payerId());
        Currency currency = currencyService.getCurrencyEntityByCode(request.currency());
        ExpenseCategory category = request.categoryId() == null
                ? null
                : expenseCategoryService.getCategoryEntityById(request.categoryId());

        groupMemberService.validateUserInGroup(group.getId(), payer.getId());
        validateSplits(group.getId(), request.baseAmount(), request.splits());

        expense.setPayer(payer);
        expense.setTitle(request.title());
        expense.setCategory(category);
        expense.setCurrency(currency);
        expense.setAmount(request.amount());
        expense.setExchangeRate(request.exchangeRate());
        expense.setBaseAmount(request.baseAmount());
        expense.setExpenseAt(request.expenseAt());
        expense.setNote(request.note());

        expenseShareRepository.deleteAllByExpenseId(expenseId);

        List<ExpenseShare> shares = request.splits()
                .stream()
                .map(split -> toExpenseShare(expense, split))
                .toList();

        expenseShareRepository.saveAll(shares);
        expense.setShares(shares);

        return ExpenseMapper.toDto(expense);
    }

    @Transactional
    public void deleteExpense(UUID expenseId) {
        Expense expense = getExpenseEntityById(expenseId);
        dataPermissionService.requireExpenseEditor(expense);
        expenseShareRepository.deleteAllByExpenseId(expenseId);
        expenseRepository.delete(expense);
    }

    @Transactional(readOnly = true)
    public ExpenseResponse getExpenseById(UUID expenseId) {
        Expense expense = getExpenseEntityById(expenseId);
        dataPermissionService.requireGroupMember(expense.getGroup().getId());
        return ExpenseMapper.toDto(expense);
    }

    // 取得某群組的所有支出，依照支出日期排序
    @Transactional(readOnly = true)
    public PageResponse<ExpenseResponse> getExpensesByGroupId(UUID groupId, Pageable pageable) {
        dataPermissionService.requireGroupMember(groupId);
        Page<ExpenseResponse> page = expenseRepository.findByGroup_Id(groupId, pageable)
                .map(ExpenseMapper::toDto);
        return PageResponse.of(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements());
    }

    // 取得某使用者的所有支出，依照支出日期排序
    @Transactional(readOnly = true)
    public PageResponse<ExpenseResponse> getExpensesByPayerId(UUID payerId, Pageable pageable) {
        dataPermissionService.requireSelf(payerId);
        Page<ExpenseResponse> page = expenseRepository.findByPayer_Id(payerId, pageable)
                .map(ExpenseMapper::toDto);
        return PageResponse.of(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements());
    }

    // 取得某使用者在某群組的所有支出，依照支出日期排序
    @Transactional(readOnly = true)
    public Expense getExpenseEntityById(UUID expenseId) {
        return expenseRepository.findById(expenseId)
                .orElseThrow(() -> new BusinessException("Expense not found"));
    }

    @Transactional(readOnly = true)
    public PageResponse<BalanceResponse> getGroupBalances(UUID groupId, Pageable pageable) {
        dataPermissionService.requireGroupMember(groupId);
        Page<BalanceResponse> page = groupMemberRepository.findByGroup_Id(groupId, pageable)
                .map(member -> toBalanceResponse(groupId, member));
        return PageResponse.of(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements());
    }

    // 取得某使用者在某群組的餘額
    private void validateSplits(UUID groupId, BigDecimal baseAmount, List<ExpenseSplitRequest> splits) {
        BigDecimal totalShareAmount = BigDecimal.ZERO;
        var users = new HashSet<UUID>();

        for (ExpenseSplitRequest split : splits) {
            if (!users.add(split.userId())) {
                throw new BusinessException("Duplicate split user");
            }
            groupMemberService.validateUserInGroup(groupId, split.userId());
            totalShareAmount = totalShareAmount.add(split.shareAmount());
        }

        // 分帳總額必須等於基準貨幣金額，避免後續餘額計算對不起來。
        if (totalShareAmount.compareTo(baseAmount) != 0) {
            throw new BusinessException("Total share amount must equal base amount");
        }
    }

    // 將支出與分帳紀錄轉換為ExpenseShare物件
    private ExpenseShare toExpenseShare(Expense expense, ExpenseSplitRequest split) {
        User user = userService.getUserEntityById(split.userId());
        BigDecimal paidAmount = split.paidAmount() == null ? BigDecimal.ZERO : split.paidAmount();

        return ExpenseShare.builder()
                .expense(expense)
                .user(user)
                .shareAmount(split.shareAmount())
                .paidAmount(paidAmount)
                .status(paidAmount.compareTo(split.shareAmount()) >= 0 ? STATUS_PAID : STATUS_OWING)
                .build();
    }

    // 計算成員餘額，包含支出分攤與結算款項
    private BalanceResponse toBalanceResponse(UUID groupId, GroupMember member) {
        UUID userId = member.getUser().getId();

        List<ExpenseShare> shares = expenseShareRepository
                .findByUser_Id(userId, Pageable.unpaged())
                .getContent()
                .stream()
                .filter(share -> share.getExpense().getGroup().getId().equals(groupId))
                .toList();

        BigDecimal totalShareAmount = shares.stream()
                .map(ExpenseShare::getShareAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalPaidAmount = shares.stream()
                .map(ExpenseShare::getPaidAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 沒有匯率資料的舊結算不能混入基本幣別加總
        boolean mixedCurrencies = settlementRepository
                .findByGroup_Id(groupId, Pageable.unpaged())
                .getContent()
                .stream()
                .anyMatch(settlement -> !settlement.getCurrency().getCode()
                        .equals(member.getGroup().getBaseCurrency().getCode()));

        if (mixedCurrencies) {
            throw new BusinessException(
                    "Settlement currency mismatch: resolve historical records first");
        }

        BigDecimal sentAmount = settlementRepository.sumSentAmount(groupId, userId);

        BigDecimal receivedAmount = settlementRepository.sumReceivedAmount(groupId, userId);

        BigDecimal netAmount = totalPaidAmount
                .subtract(totalShareAmount)
                .add(sentAmount)
                .subtract(receivedAmount);

        return new BalanceResponse(
                userId,
                member.getUser().getName(),
                totalShareAmount,
                totalPaidAmount,
                netAmount);
    }
}
