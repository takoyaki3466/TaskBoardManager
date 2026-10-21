package org.takoyaki.taskboardmanager.model;

/**
 * 保存可能なキャンバス表示状態を表します。
 *
 * @param zoom ズーム倍率
 * @param offsetX 水平方向の表示オフセット
 * @param offsetY 垂直方向の表示オフセット
 */
public record ViewSettings(double zoom, double offsetX, double offsetY) {

    public static final double MINIMUM_ZOOM = 0.25;
    public static final double MAXIMUM_ZOOM = 3.0;
    public static final ViewSettings DEFAULT = new ViewSettings(1.0, 0.0, 0.0);

    public ViewSettings {
        if (!Double.isFinite(zoom) || zoom < MINIMUM_ZOOM || zoom > MAXIMUM_ZOOM) {
            throw new IllegalArgumentException("ズーム倍率が対応範囲外です。");
        }
        if (!Double.isFinite(offsetX) || !Double.isFinite(offsetY)) {
            throw new IllegalArgumentException("表示オフセットは有限値である必要があります。");
        }
    }
}
