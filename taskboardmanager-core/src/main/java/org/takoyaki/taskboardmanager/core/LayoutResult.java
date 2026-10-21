package org.takoyaki.taskboardmanager.core;

import java.util.Map;
import java.util.UUID;
import org.takoyaki.taskboardmanager.model.Position;

/** 階層レイアウトの一時座標と外接矩形です。 */
public record LayoutResult(Map<UUID, Position> positions, LayoutBounds bounds) {

    public LayoutResult {
        positions = Map.copyOf(positions);
    }

    /** レイアウト結果の外接矩形です。 */
    public record LayoutBounds(double x, double y, double width, double height) {
    }
}
