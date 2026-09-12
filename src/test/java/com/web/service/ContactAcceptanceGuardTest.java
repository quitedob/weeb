package com.web.service;

import com.web.exception.WeebException;
import com.web.mapper.ContactMapper;
import com.web.model.Contact;
import com.web.service.impl.ContactServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ContactAcceptanceGuardTest {
    @Test
    void expiredOrConcurrentlyProcessedRequestsHaveNoReverseRelationOrNotificationSideEffects() {
        ContactMapper contacts = mock(ContactMapper.class);
        NotificationService notifications = mock(NotificationService.class);
        ContactServiceImpl service = service(contacts, notifications);
        Contact contact = new Contact(1L, 2L, "request");
        contact.setId(10L);
        when(contacts.selectById(10L)).thenReturn(contact);
        when(contacts.acceptPendingRequest(eq(10L), eq(2L), any(), any())).thenReturn(0);
        assertThrows(WeebException.class, () -> service.accept(10L, 2L));
        verify(contacts, never()).updateById(any(Contact.class));
        verify(contacts, never()).insert(any(Contact.class));
        verify(contacts, never()).selectList(any());
        verifyNoInteractions(notifications);
    }

    @Test
    void aSuccessfulAtomicAcceptanceCreatesTheReverseRelationAfterTheGuard() {
        ContactMapper contacts = mock(ContactMapper.class);
        NotificationService notifications = mock(NotificationService.class);
        ContactServiceImpl service = service(contacts, notifications);
        Contact contact = new Contact(1L, 2L, "request");
        contact.setId(10L);
        when(contacts.selectById(10L)).thenReturn(contact);
        when(contacts.acceptPendingRequest(eq(10L), eq(2L), any(), any())).thenReturn(1);
        when(contacts.selectList(any())).thenReturn(List.of());
        service.accept(10L, 2L);
        var ordered = inOrder(contacts);
        ordered.verify(contacts).acceptPendingRequest(eq(10L), eq(2L), any(LocalDateTime.class), any(LocalDateTime.class));
        ordered.verify(contacts).selectList(any());
        ordered.verify(contacts).insert(argThat((Contact reverse) -> reverse.getUserId().equals(2L)
                && reverse.getFriendId().equals(1L) && reverse.getStatus() == 1));
    }

    private ContactServiceImpl service(ContactMapper mapper, NotificationService notifications) {
        var service = new ContactServiceImpl();
        ReflectionTestUtils.setField(service, "contactMapper", mapper);
        ReflectionTestUtils.setField(service, "notificationService", notifications);
        return service;
    }
}
