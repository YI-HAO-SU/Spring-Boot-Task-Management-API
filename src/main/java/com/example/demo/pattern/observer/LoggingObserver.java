package com.example.demo.pattern.observer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class LoggingObserver {

    private static final Logger log = LoggerFactory.getLogger(LoggingObserver.class);

    @EventListener
    public void onStatusChanged(TaskStatusChangedEvent event) {
        log.info("[AUDIT] Task '{}' (id={}) status changed: {} -> {}",
                event.getTask().getTitle(),
                event.getTask().getId(),
                event.getPreviousStatus(),
                event.getTask().getStatus());
    }
}
