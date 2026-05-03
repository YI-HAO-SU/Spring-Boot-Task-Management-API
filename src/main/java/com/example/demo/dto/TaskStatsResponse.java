package com.example.demo.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskStatsResponse {
    private long total;
    private long todo;
    private long inProgress;
    private long done;
    private long cancelled;
    private long highPriority;
    private long criticalPriority;
}
