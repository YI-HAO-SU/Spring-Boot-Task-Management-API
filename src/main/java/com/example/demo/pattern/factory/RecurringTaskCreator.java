package com.example.demo.pattern.factory;

import com.example.demo.dto.TaskRequest;
import com.example.demo.model.Task;

public class RecurringTaskCreator implements TaskCreator {

    @Override
    public Task create(TaskRequest request) {
        Task task = buildBase(request);
        String original = task.getDescription() != null ? task.getDescription() : "";
        task.setDescription("[RECURRING] " + original);
        return task;
    }
}
