package com.example.demo.pattern.factory;

import com.example.demo.dto.TaskRequest;
import com.example.demo.model.Priority;
import com.example.demo.model.Task;

public class WorkTaskCreator implements TaskCreator {

    @Override
    public Task create(TaskRequest request) {
        Task task = buildBase(request);
        if (request.getPriority() == null) {
            task.setPriority(Priority.HIGH);
        }
        return task;
    }
}
