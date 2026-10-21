package org.takoyaki.taskboardmanager.core;

import java.util.List;
import org.takoyaki.taskboardmanager.model.Connection;
import org.takoyaki.taskboardmanager.model.NodeElement;

/** 階層レイアウトへ渡すJavaFX非依存の入力です。 */
public record LayoutRequest(
        List<NodeElement> nodes,
        List<Connection> connections,
        double horizontalGap,
        double verticalGap) {

    public LayoutRequest {
        nodes = List.copyOf(nodes);
        connections = List.copyOf(connections);
        if (!Double.isFinite(horizontalGap) || horizontalGap < 0.0
                || !Double.isFinite(verticalGap) || verticalGap < 0.0) {
            throw new IllegalArgumentException("レイアウト間隔は0以上の有限値で指定してください。");
        }
    }
}
