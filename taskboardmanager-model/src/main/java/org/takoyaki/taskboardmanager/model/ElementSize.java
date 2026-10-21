package org.takoyaki.taskboardmanager.model;

/**
 * ボード要素の大きさを表します。
 *
 * @param width 幅
 * @param height 高さ
 */
public record ElementSize(double width, double height) {

    public ElementSize {
        requirePositiveFinite(width, "width");
        requirePositiveFinite(height, "height");
    }

    private static void requirePositiveFinite(double value, String name) {
        if (!Double.isFinite(value) || value <= 0.0) {
            throw new IllegalArgumentException(name + "は正の有限値である必要があります。");
        }
    }
}
