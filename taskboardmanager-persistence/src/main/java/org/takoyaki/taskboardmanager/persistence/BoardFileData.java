package org.takoyaki.taskboardmanager.persistence;

import java.util.List;
import java.util.UUID;
import org.takoyaki.taskboardmanager.model.IdeaStatus;
import org.takoyaki.taskboardmanager.model.NodeType;
import org.takoyaki.taskboardmanager.model.Priority;
import org.takoyaki.taskboardmanager.model.ProgressMode;
import org.takoyaki.taskboardmanager.model.QuestionStatus;
import org.takoyaki.taskboardmanager.model.RelationType;
import org.takoyaki.taskboardmanager.model.TaskStatus;

/** Jacksonで読み書きするTaskBoardManagerファイルのデータ転送形式です。 */
public record BoardFileData(
        int schemaVersion,
        UUID documentId,
        String title,
        List<ElementData> elements,
        List<ConnectionData> connections,
        ViewSettingsData viewSettings) {

    /** 保存対象の要素種別です。 */
    public enum ElementKind {
        NODE,
        BOX
    }

    /** JSON内の要素表現です。 */
    public record ElementData(
            UUID id,
            ElementKind kind,
            String title,
            double x,
            double y,
            double width,
            double height,
            NodeType nodeType,
            String description,
            TaskData task,
            QuestionData question,
            IdeaData idea,
            DecisionData decision) {
    }

    /** JSON内の接続表現です。 */
    public record ConnectionData(
            UUID id,
            UUID sourceElementId,
            UUID targetElementId,
            RelationType relationType) {
    }

    /** JSON内の表示設定です。 */
    public record ViewSettingsData(double zoom, double offsetX, double offsetY) {
    }

    /** JSON内のタスク属性です。 */
    public record TaskData(
            TaskStatus status,
            int progress,
            Priority priority,
            ProgressMode progressMode,
            String startDate,
            String dueDate) {
    }

    /** JSON内の疑問属性です。 */
    public record QuestionData(QuestionStatus status, String resolution) {
    }

    /** JSON内のアイデア属性です。 */
    public record IdeaData(IdeaStatus status) {
    }

    /** JSON内の決定属性です。 */
    public record DecisionData(String decision, String rationale, String decidedAt) {
    }
}
