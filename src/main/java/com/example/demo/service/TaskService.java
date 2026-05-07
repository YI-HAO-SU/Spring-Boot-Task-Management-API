package com.example.demo.service;

import com.example.demo.config.TaskStatisticsCache;
import com.example.demo.dto.*;
import com.example.demo.exception.TaskNotFoundException;
import com.example.demo.model.Task;
import com.example.demo.model.TaskStatus;
import com.example.demo.pattern.factory.TaskFactory;
import com.example.demo.pattern.observer.TaskStatusChangedEvent;
import com.example.demo.pattern.strategy.*;
import com.example.demo.repository.TaskRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class TaskService {

    private final TaskRepository taskRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final TaskStatisticsCache statsCache;

    public TaskService(TaskRepository taskRepository,
                       ApplicationEventPublisher eventPublisher,
                       TaskStatisticsCache statsCache) {
        this.taskRepository = taskRepository;
        this.eventPublisher = eventPublisher;
        this.statsCache = statsCache;
    }

    public TaskResponse createTask(TaskRequest request) {
        Task task = TaskFactory.createTask(request);
        TaskResponse response = TaskResponse.from(taskRepository.save(task));
        statsCache.invalidate();
        return response;
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getAllTasks(String sortBy) {
        List<Task> tasks = taskRepository.findAll();
        TaskSortingStrategy strategy = resolveSortingStrategy(sortBy);
        return strategy.sort(tasks).stream()
                .map(TaskResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public TaskResponse getTaskById(Long id) {
        return TaskResponse.from(findOrThrow(id));
    }

    public TaskResponse updateTask(Long id, TaskRequest request) {
        Task task = findOrThrow(id);
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        if (request.getPriority() != null) task.setPriority(request.getPriority());
        if (request.getTaskType() != null) task.setTaskType(request.getTaskType());
        task.setDueDate(request.getDueDate());
        TaskResponse response = TaskResponse.from(taskRepository.save(task));
        statsCache.invalidate();
        return response;
    }

    public TaskResponse updateTaskStatus(Long id, TaskStatusUpdateRequest request) {
        Task task = findOrThrow(id);
        TaskStatus previousStatus = task.getStatus();
        task.setStatus(request.getStatus());
        Task saved = taskRepository.save(task);
        statsCache.invalidate();
        eventPublisher.publishEvent(new TaskStatusChangedEvent(this, saved, previousStatus));
        return TaskResponse.from(saved);
    }

    public void deleteTask(Long id) {
        taskRepository.delete(findOrThrow(id));
        statsCache.invalidate();
    }

    @Transactional(readOnly = true)
    public TaskStatsResponse getStats() {
        return statsCache.compute(taskRepository.findAll());
    }

    private Task findOrThrow(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException(id));
    }

    private TaskSortingStrategy resolveSortingStrategy(String sortBy) {
        if (sortBy == null) return tasks -> tasks;
        return switch (sortBy.toLowerCase()) {
            case "priority" -> new PrioritySortingStrategy();
            case "duedate"  -> new DueDateSortingStrategy();
            case "status"   -> new StatusSortingStrategy();
            default         -> tasks -> tasks;
        };
    }
}
