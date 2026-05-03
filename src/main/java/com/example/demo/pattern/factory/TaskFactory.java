package com.example.demo.pattern.factory;

import com.example.demo.dto.TaskRequest;
import com.example.demo.model.Task;
import com.example.demo.model.TaskType;

public class TaskFactory {

    public static Task createTask(TaskRequest request) {
        TaskType type = request.getTaskType() != null ? request.getTaskType() : TaskType.PERSONAL;
        TaskCreator creator = switch (type) {
            case PERSONAL -> new PersonalTaskCreator();
            case WORK -> new WorkTaskCreator();
            case RECURRING -> new RecurringTaskCreator();
        };
        return creator.create(request);
    }
}
