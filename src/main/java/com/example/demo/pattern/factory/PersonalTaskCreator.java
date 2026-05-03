package com.example.demo.pattern.factory;

import com.example.demo.dto.TaskRequest;
import com.example.demo.model.Task;

public class PersonalTaskCreator implements TaskCreator {

    @Override
    public Task create(TaskRequest request) {
        return buildBase(request);
    }
}
