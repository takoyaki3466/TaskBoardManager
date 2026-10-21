package org.takoyaki.taskboardmanager.model;

/**
 * ボード上のワールド座標を表します。
 *
 * @param x X座標
 * @param y Y座標
 */
public record Position(double x, double y) {

    public Position {
        requireFinite(x, "x");
        requireFinite(y, "y");
    }

    private static void requireFinite(double value, String name) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(name + "は有限値である必要があります。");
        }
    }
}
