package org.takoyaki.taskboardmanager.persistence;

import java.io.IOException;

/** 読み込んだボードファイルが形式または整合性要件を満たさないことを表します。 */
public final class InvalidBoardFileException extends IOException {

    public InvalidBoardFileException(String message) {
        super(message);
    }

    public InvalidBoardFileException(String message, Throwable cause) {
        super(message, cause);
    }
}
