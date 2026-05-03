package com.example.demo.pattern.factory;

import com.example.demo.dto.TaskRequest;
import com.example.demo.model.Priority;
import com.example.demo.model.Task;
import com.example.demo.model.TaskStatus;
import com.example.demo.model.TaskType;

public interface TaskCreator {

    Task create(TaskRequest request);

    default Task buildBase(TaskRequest request) {
        return Task.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .priority(request.getPriority() != null ? request.getPriority() : Priority.MEDIUM)
                .taskType(request.getTaskType() != null ? request.getTaskType() : TaskType.PERSONAL)
                .dueDate(request.getDueDate())
                .status(TaskStatus.TODO)
                .build();
    }
}
