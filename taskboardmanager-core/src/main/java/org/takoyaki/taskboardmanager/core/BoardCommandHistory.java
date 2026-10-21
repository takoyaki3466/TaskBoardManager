package org.takoyaki.taskboardmanager.core;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import org.takoyaki.taskboardmanager.model.BoardDocument;
import org.takoyaki.taskboardmanager.model.BoardSnapshot;

/** ボード編集をスナップショット単位で安全にUndo/Redoします。 */
public final class BoardCommandHistory {

    private final BoardDocument document;
    private final Deque<HistoryEntry> undoEntries = new ArrayDeque<>();
    private final Deque<HistoryEntry> redoEntries = new ArrayDeque<>();

    public BoardCommandHistory(BoardDocument document) {
        this.document = Objects.requireNonNull(document, "document");
    }

    /** 変更処理を実行し、実際に状態が変化した場合だけ履歴へ記録します。 */
    public void execute(String description, Runnable change) {
        Objects.requireNonNull(change, "change");
        BoardSnapshot before = document.snapshot();
        try {
            change.run();
            recordAppliedChange(description, before);
        } catch (RuntimeException exception) {
            document.restore(before);
            throw exception;
        }
    }

    /** ドラッグなど既にモデルへ反映済みの一連の変更を1履歴として記録します。 */
    public void recordAppliedChange(String description, BoardSnapshot before) {
        BoardSnapshot after = document.snapshot();
        if (before.equals(after)) {
            return;
        }
        undoEntries.push(new HistoryEntry(description, before, after));
        redoEntries.clear();
    }

    public boolean canUndo() {
        return !undoEntries.isEmpty();
    }

    public boolean canRedo() {
        return !redoEntries.isEmpty();
    }

    public String undo() {
        if (!canUndo()) {
            return "";
        }
        HistoryEntry entry = undoEntries.pop();
        document.restore(entry.before());
        redoEntries.push(entry);
        return entry.description();
    }

    public String redo() {
        if (!canRedo()) {
            return "";
        }
        HistoryEntry entry = redoEntries.pop();
        document.restore(entry.after());
        undoEntries.push(entry);
        return entry.description();
    }

    private record HistoryEntry(
            String description, BoardSnapshot before, BoardSnapshot after) {

        private HistoryEntry {
            Objects.requireNonNull(description, "description");
            Objects.requireNonNull(before, "before");
            Objects.requireNonNull(after, "after");
        }
    }
}
