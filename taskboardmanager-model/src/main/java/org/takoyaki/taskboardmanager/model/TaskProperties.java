package org.takoyaki.taskboardmanager.model;

import java.time.LocalDate;
import java.util.Objects;

/** TASKノード固有の進捗・優先度・期間を保持します。 */
public record TaskProperties(
        TaskStatus status,
        int progress,
        Priority priority,
        ProgressMode progressMode,
        LocalDate startDate,
        LocalDate dueDate) {

    public static final TaskProperties DEFAULT = new TaskProperties(
            TaskStatus.TODO, 0, Priority.NORMAL, ProgressMode.MANUAL, null, null);

    public TaskProperties {
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(priority, "priority");
        Objects.requireNonNull(progressMode, "progressMode");
        if (progress < 0 || progress > 100) {
            throw new IllegalArgumentException("進捗率は0から100の範囲で指定してください。");
        }
        if (status == TaskStatus.DONE && progress != 100) {
            progress = 100;
        } else if (progress == 100 && status != TaskStatus.DONE) {
            status = TaskStatus.DONE;
        }
        if (startDate != null && dueDate != null && dueDate.isBefore(startDate)) {
            throw new IllegalArgumentException("期限日は開始日より前にできません。");
        }
    }
}
