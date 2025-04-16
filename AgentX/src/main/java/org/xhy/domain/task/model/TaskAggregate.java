package org.xhy.domain.task.model;

import java.util.List;

public class TaskAggregate {
    private TaskEntity task;
    private List<TaskEntity> subTasks;

    public TaskAggregate(TaskEntity task, List<TaskEntity> subTasks) {
        this.task = task;
        this.subTasks = subTasks;
    }

    public TaskEntity getTask() {
        return task;
    }

    public List<TaskEntity> getSubTasks() {
        return subTasks;
    }
}