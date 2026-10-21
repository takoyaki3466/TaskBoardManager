package org.takoyaki.taskboardmanager.core;

import org.takoyaki.taskboardmanager.model.ElementSize;
import org.takoyaki.taskboardmanager.model.ViewSettings;

/** 要素作成とキャンバス操作に利用する設定値を集約します。 */
public final class BoardConfiguration {

    public static final ElementSize DEFAULT_NODE_SIZE = new ElementSize(180.0, 76.0);
    public static final ElementSize DEFAULT_BOX_SIZE = new ElementSize(260.0, 170.0);
    public static final double MINIMUM_BOX_WIDTH = 120.0;
    public static final double MINIMUM_BOX_HEIGHT = 80.0;
    public static final double MINIMUM_ZOOM = ViewSettings.MINIMUM_ZOOM;
    public static final double MAXIMUM_ZOOM = ViewSettings.MAXIMUM_ZOOM;
    public static final double ZOOM_STEP = 1.12;
    public static final double GRID_SPACING = 28.0;
    public static final double HIERARCHY_HORIZONTAL_GAP = 110.0;
    public static final double HIERARCHY_VERTICAL_GAP = 42.0;

    private BoardConfiguration() {
    }
}
