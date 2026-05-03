package com.example.demo.pattern.observer;

import com.example.demo.model.TaskStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationObserver {

    private static final Logger log = LoggerFactory.getLogger(NotificationObserver.class);

    @EventListener
    public void onStatusChanged(TaskStatusChangedEvent event) {
        if (event.getTask().getStatus() == TaskStatus.DONE) {
            log.info("[NOTIFICATION] Task '{}' completed! Email notification sent.",
                    event.getTask().getTitle());
        } else {
            log.info("[NOTIFICATION] Task '{}' updated to {}. Push notification sent.",
                    event.getTask().getTitle(),
                    event.getTask().getStatus());
        }
    }
}
