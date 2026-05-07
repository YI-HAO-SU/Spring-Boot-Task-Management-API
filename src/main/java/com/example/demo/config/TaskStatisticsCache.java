package com.example.demo.config;

import com.example.demo.dto.TaskStatsResponse;
import com.example.demo.model.Priority;
import com.example.demo.model.Task;
import com.example.demo.model.TaskStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TaskStatisticsCache {

    private static final Logger log = LoggerFactory.getLogger(TaskStatisticsCache.class);
    private final Map<String, TaskStatsResponse> cache = new ConcurrentHashMap<>();

    public TaskStatsResponse compute(List<Task> tasks) {
        return cache.computeIfAbsent("stats", k -> {
            log.info("[SINGLETON] TaskStatisticsCache instance #{} computing stats",
                    System.identityHashCode(this));
            return calculate(tasks);
        });
    }

    public void invalidate() {
        cache.clear();
        log.info("[SINGLETON] TaskStatisticsCache instance #{} cache invalidated",
                System.identityHashCode(this));
    }

    private TaskStatsResponse calculate(List<Task> tasks) {
        return TaskStatsResponse.builder()
                .total(tasks.size())
                .todo(countByStatus(tasks, TaskStatus.TODO))
                .inProgress(countByStatus(tasks, TaskStatus.IN_PROGRESS))
                .done(countByStatus(tasks, TaskStatus.DONE))
                .cancelled(countByStatus(tasks, TaskStatus.CANCELLED))
                .highPriority(countByPriority(tasks, Priority.HIGH))
                .criticalPriority(countByPriority(tasks, Priority.CRITICAL))
                .build();
    }

    private long countByStatus(List<Task> tasks, TaskStatus status) {
        return tasks.stream().filter(t -> t.getStatus() == status).count();
    }

    private long countByPriority(List<Task> tasks, Priority priority) {
        return tasks.stream().filter(t -> t.getPriority() == priority).count();
    }
}
