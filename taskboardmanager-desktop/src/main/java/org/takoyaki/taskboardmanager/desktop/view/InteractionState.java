package org.takoyaki.taskboardmanager.desktop.view;

/** キャンバス上で同時実行を許可しない操作状態です。 */
public enum InteractionState {
    IDLE,
    DRAGGING_ELEMENT,
    RESIZING_ELEMENT,
    CONNECTING_ELEMENTS,
    PANNING_CANVAS,
    SELECTING_RANGE,
    EDITING_TEXT
}
