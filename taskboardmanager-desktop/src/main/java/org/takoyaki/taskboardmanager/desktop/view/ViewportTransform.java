package org.takoyaki.taskboardmanager.desktop.view;

import javafx.geometry.Point2D;
import javafx.scene.transform.Affine;
import org.takoyaki.taskboardmanager.core.BoardConfiguration;
import org.takoyaki.taskboardmanager.model.ViewSettings;

/** ワールド座標とキャンバス上の画面座標の相互変換を管理します。 */
public final class ViewportTransform {

    private final Affine affine = new Affine();
    private double zoom = 1.0;
    private double offsetX;
    private double offsetY;

    public ViewportTransform() {
        updateAffine();
    }

    public Affine affine() {
        return affine;
    }

    public double zoom() {
        return zoom;
    }

    public double offsetX() {
        return offsetX;
    }

    public double offsetY() {
        return offsetY;
    }

    public ViewSettings settings() {
        return new ViewSettings(zoom, offsetX, offsetY);
    }

    /** 保存された表示設定を適用します。 */
    public void apply(ViewSettings settings) {
        set(settings.zoom(), settings.offsetX(), settings.offsetY());
    }

    /** 検証済みの倍率と表示オフセットを直接設定します。 */
    public void set(double zoom, double offsetX, double offsetY) {
        ViewSettings settings = new ViewSettings(zoom, offsetX, offsetY);
        this.zoom = settings.zoom();
        this.offsetX = settings.offsetX();
        this.offsetY = settings.offsetY();
        updateAffine();
    }

    public Point2D worldToScreen(double worldX, double worldY) {
        requireFinite(worldX, worldY);
        double screenX = worldX * zoom + offsetX;
        double screenY = worldY * zoom + offsetY;
        requireFinite(screenX, screenY);
        return new Point2D(screenX, screenY);
    }

    public Point2D screenToWorld(double screenX, double screenY) {
        requireFinite(screenX, screenY);
        double worldX = (screenX - offsetX) / zoom;
        double worldY = (screenY - offsetY) / zoom;
        requireFinite(worldX, worldY);
        return new Point2D(worldX, worldY);
    }

    /** 表示だけを移動し、ワールド座標は変更しません。 */
    public void pan(double deltaX, double deltaY) {
        requireFinite(deltaX, deltaY);
        double nextOffsetX = offsetX + deltaX;
        double nextOffsetY = offsetY + deltaY;
        requireFinite(nextOffsetX, nextOffsetY);
        offsetX = nextOffsetX;
        offsetY = nextOffsetY;
        updateAffine();
    }

    /** 指定した画面座標の直下を固定したままズームします。 */
    public void zoomAt(double requestedZoom, double screenX, double screenY) {
        requireFinite(requestedZoom, screenX, screenY);
        double clampedZoom = Math.clamp(
                requestedZoom, BoardConfiguration.MINIMUM_ZOOM, BoardConfiguration.MAXIMUM_ZOOM);
        Point2D anchorWorld = screenToWorld(screenX, screenY);
        double nextOffsetX = screenX - anchorWorld.getX() * clampedZoom;
        double nextOffsetY = screenY - anchorWorld.getY() * clampedZoom;
        requireFinite(nextOffsetX, nextOffsetY);
        zoom = clampedZoom;
        offsetX = nextOffsetX;
        offsetY = nextOffsetY;
        updateAffine();
    }

    private void updateAffine() {
        affine.setToTransform(zoom, 0.0, offsetX, 0.0, zoom, offsetY);
    }

    private static void requireFinite(double... values) {
        for (double value : values) {
            if (!Double.isFinite(value)) {
                throw new IllegalArgumentException("座標変換には有限値のみ指定できます。");
            }
        }
    }
}
