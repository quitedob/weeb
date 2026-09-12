package com.web.task;

import com.web.service.MessageOutboxService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MessageOutboxTask {
    private final MessageOutboxService outbox;
    public MessageOutboxTask(MessageOutboxService outbox) { this.outbox = outbox; }
    @Scheduled(fixedDelayString = "${weeb.message-outbox.poll-ms:1000}")
    public void dispatch() { outbox.dispatchScheduled(); }
}
