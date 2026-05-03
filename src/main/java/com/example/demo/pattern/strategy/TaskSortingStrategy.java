package com.example.demo.pattern.strategy;

import com.example.demo.model.Task;

import java.util.List;

public interface TaskSortingStrategy {
    List<Task> sort(List<Task> tasks);
}
