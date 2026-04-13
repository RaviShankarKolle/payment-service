package com.library.borrow.borrow;

import com.library.borrow.client.BookClient;
import com.library.borrow.client.NotificationClient;
import com.library.borrow.client.UserClient;
import com.library.borrow.common.ApiResponse;
import com.library.borrow.exception.ApplicationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BorrowServiceTest {
    private BorrowRepository borrowRepository;
    private UserClient userClient;
    private BookClient bookClient;
    private NotificationClient notificationClient;
    private ApplicationEventPublisher eventPublisher;
    private BorrowService borrowService;

    @BeforeEach
    void setUp() {
        borrowRepository = mock(BorrowRepository.class);
        userClient = mock(UserClient.class);
        bookClient = mock(BookClient.class);
        notificationClient = mock(NotificationClient.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        borrowService = new BorrowService(
                borrowRepository,
                userClient,
                bookClient,
                notificationClient,
                eventPublisher
        );
    }

    @Test
    void createBorrow_shouldRejectInvalidDateWindow() {
        BorrowDtos.BorrowRequest request = new BorrowDtos.BorrowRequest(
                1L, 10L, LocalDate.now().plusDays(5), LocalDate.now().plusDays(5)
        );
        ApplicationException ex = assertThrows(ApplicationException.class, () -> borrowService.createBorrow(request));
        assertEquals("INVALID_BORROW_WINDOW", ex.getCode());
    }

    @Test
    void createBorrow_shouldRejectDurationBeyondLimit() {
        BorrowDtos.BorrowRequest request = new BorrowDtos.BorrowRequest(
                1L, 10L, LocalDate.now(), LocalDate.now().plusDays(16)
        );
        ApplicationException ex = assertThrows(ApplicationException.class, () -> borrowService.createBorrow(request));
        assertEquals("BORROW_DURATION_EXCEEDED", ex.getCode());
    }

    @Test
    void createBorrow_shouldRejectWhenLimitReached() {
        BorrowDtos.BorrowRequest request = new BorrowDtos.BorrowRequest(1L, 10L, LocalDate.now(), LocalDate.now().plusDays(7));
        when(borrowRepository.countActiveLoans(1L)).thenReturn(3);

        ApplicationException ex = assertThrows(ApplicationException.class, () -> borrowService.createBorrow(request));

        assertEquals("BORROW_LIMIT_REACHED", ex.getCode());
    }

    @Test
    void createBorrow_shouldRejectIneligibleUser() {
        BorrowDtos.BorrowRequest request = new BorrowDtos.BorrowRequest(1L, 10L, LocalDate.now(), LocalDate.now().plusDays(7));
        when(borrowRepository.countActiveLoans(1L)).thenReturn(0);
        when(userClient.getEligibility(1L)).thenReturn(ApiResponse.success(new UserClient.UserEligibilityResponse(1L, false, "USER_NOT_ACTIVE"), Map.of()));

        ApplicationException ex = assertThrows(ApplicationException.class, () -> borrowService.createBorrow(request));

        assertEquals("USER_NOT_ELIGIBLE", ex.getCode());
    }

    @Test
    void createBorrow_shouldPersistPendingAndPublishEvent() {
        BorrowDtos.BorrowRequest request = new BorrowDtos.BorrowRequest(1L, 10L, LocalDate.now(), LocalDate.now().plusDays(7));
        BorrowRepository.BorrowRecord record = record(9L, "PENDING_PICKUP", null);
        when(borrowRepository.countActiveLoans(1L)).thenReturn(0);
        when(userClient.getEligibility(1L)).thenReturn(ApiResponse.success(new UserClient.UserEligibilityResponse(1L, true, "USER_ACTIVE"), Map.of()));
        when(borrowRepository.createPending(eq(1L), eq(10L), any(LocalDate.class), any(LocalDate.class))).thenReturn(9L);
        when(borrowRepository.findById(9L)).thenReturn(Optional.of(record));

        BorrowDtos.BorrowResponse response = borrowService.createBorrow(request);

        assertEquals("PENDING_PICKUP", response.status());
        verify(bookClient).reserve(10L);
        verify(eventPublisher).publishEvent(any(BorrowService.BorrowDomainEvent.class));
    }

    @Test
    void allocate_shouldRejectWhenMissing() {
        when(borrowRepository.findById(77L)).thenReturn(Optional.empty());

        ApplicationException ex = assertThrows(ApplicationException.class,
                () -> borrowService.allocate(77L, new BorrowDtos.AllocateRequest(LocalDate.now().plusDays(3))));

        assertEquals("BORROW_NOT_FOUND", ex.getCode());
    }

    @Test
    void allocate_shouldRejectInvalidState() {
        when(borrowRepository.findById(7L)).thenReturn(Optional.of(record(7L, "ALLOCATED", LocalDate.now().plusDays(5))));

        ApplicationException ex = assertThrows(ApplicationException.class,
                () -> borrowService.allocate(7L, new BorrowDtos.AllocateRequest(LocalDate.now().plusDays(2))));

        assertEquals("INVALID_BORROW_STATE", ex.getCode());
    }

    @Test
    void allocate_shouldRejectPastDueDate() {
        when(borrowRepository.findById(7L)).thenReturn(Optional.of(record(7L, "PENDING_PICKUP", null)));

        ApplicationException ex = assertThrows(ApplicationException.class,
                () -> borrowService.allocate(7L, new BorrowDtos.AllocateRequest(LocalDate.now())));

        assertEquals("INVALID_DUE_DATE", ex.getCode());
    }

    @Test
    void allocate_shouldIssueMarkAllocatedAndSendConfirmation() {
        LocalDate dueDate = LocalDate.now().plusDays(3);
        when(borrowRepository.findById(5L))
                .thenReturn(Optional.of(record(5L, "PENDING_PICKUP", null)))
                .thenReturn(Optional.of(record(5L, "ALLOCATED", dueDate)));
        when(userClient.getProfileSummary(1L))
                .thenReturn(ApiResponse.success(new UserClient.UserNotificationContact("user@example.com", "User"), Map.of()));

        BorrowDtos.BorrowResponse response = borrowService.allocate(5L, new BorrowDtos.AllocateRequest(dueDate));

        assertEquals("ALLOCATED", response.status());
        verify(bookClient).issue(10L);
        verify(borrowRepository).markAllocated(5L, dueDate);
        verify(notificationClient).sendBorrowConfirmation(any(NotificationClient.BorrowConfirmationRequest.class));
    }

    @Test
    void returnBook_shouldRejectAlreadyReturned() {
        when(borrowRepository.findById(9L)).thenReturn(Optional.of(record(9L, "RETURNED", LocalDate.now())));

        ApplicationException ex = assertThrows(ApplicationException.class, () -> borrowService.returnBook(9L));

        assertEquals("ALREADY_RETURNED", ex.getCode());
    }

    @Test
    void returnBook_shouldReturnAndPublishEvent() {
        when(borrowRepository.findById(9L))
                .thenReturn(Optional.of(record(9L, "ALLOCATED", LocalDate.now().plusDays(4))))
                .thenReturn(Optional.of(record(9L, "RETURNED", LocalDate.now().plusDays(4))));

        BorrowDtos.BorrowResponse response = borrowService.returnBook(9L);

        assertEquals("RETURNED", response.status());
        verify(bookClient).returnBook(10L);
        verify(borrowRepository).markReturned(9L);
    }

    @Test
    void processOverdueRecords_shouldMarkAndSkipMissingEmail() {
        BorrowRepository.BorrowRecord overdue = record(12L, "ALLOCATED", LocalDate.now().minusDays(1));
        when(borrowRepository.findAllocatedOverdue(any(LocalDate.class))).thenReturn(List.of(overdue));
        when(userClient.getProfileSummary(1L)).thenReturn(ApiResponse.success(new UserClient.UserNotificationContact("", "User"), Map.of()));

        int processed = borrowService.processOverdueRecords();

        assertEquals(1, processed);
        verify(borrowRepository).markOverdue(12L);
        verify(notificationClient, never()).sendOverdueNotification(any(NotificationClient.OverdueNotificationRequest.class));
    }

    @Test
    void listByUser_shouldClampPagination() {
        when(borrowRepository.listByUser(1L, 0, 100)).thenReturn(List.of(record(1L, "PENDING_PICKUP", null)));
        when(borrowRepository.countByUser(1L)).thenReturn(1L);

        BorrowDtos.BorrowListResponse response = borrowService.listByUser(1L, -2, 1000);

        assertEquals(0, response.page());
        assertEquals(100, response.size());
        assertEquals(1, response.total());
    }

    private static BorrowRepository.BorrowRecord record(Long id, String status, LocalDate dueDate) {
        return new BorrowRepository.BorrowRecord(
                id,
                1L,
                10L,
                LocalDate.now(),
                LocalDate.now().plusDays(7),
                dueDate,
                status,
                Instant.now()
        );
    }
}
