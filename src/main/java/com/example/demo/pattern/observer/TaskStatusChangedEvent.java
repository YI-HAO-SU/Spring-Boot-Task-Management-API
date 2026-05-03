package com.example.demo.pattern.observer;

import com.example.demo.model.Task;
import com.example.demo.model.TaskStatus;
import org.springframework.context.ApplicationEvent;

public class TaskStatusChangedEvent extends ApplicationEvent {

    private final Task task;
    private final TaskStatus previousStatus;

    public TaskStatusChangedEvent(Object source, Task task, TaskStatus previousStatus) {
        super(source);
        this.task = task;
        this.previousStatus = previousStatus;
    }

    public Task getTask() {
        return task;
    }

    public TaskStatus getPreviousStatus() {
        return previousStatus;
    }
}
