package com.example.demo.dto;

import com.example.demo.model.Priority;
import com.example.demo.model.TaskType;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    private Priority priority;

    private TaskType taskType;

    private LocalDate dueDate;
}
