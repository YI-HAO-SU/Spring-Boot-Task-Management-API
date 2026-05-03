package com.example.demo.pattern.strategy;

import com.example.demo.model.Task;

import java.util.Comparator;
import java.util.List;

public class StatusSortingStrategy implements TaskSortingStrategy {

    @Override
    public List<Task> sort(List<Task> tasks) {
        return tasks.stream()
                .sorted(Comparator.comparing(Task::getStatus))
                .toList();
    }
}
