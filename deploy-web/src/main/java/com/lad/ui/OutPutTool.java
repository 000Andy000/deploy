package com.lad.ui;

import javafx.application.Platform;
import javafx.scene.control.TextArea;

/**
 * @author Andy
 * @date 2024-2-6 006 16:21
 */
public class OutPutTool {
    /**
     * 在指定的输出区域中追加文本
     *
     * @param outputArea 输出区域
     * @param s          要追加的文本
     */
    public static void appendText(TextArea outputArea, String s) {
        Platform.runLater(() -> outputArea.appendText("► " + s + "\n"));
    }

    /**
     * 在指定的输出区域中追加分隔符
     *
     * @param outputArea 输出区域
     */
    public static void appendDivider(TextArea outputArea) {
        Platform.runLater(() -> outputArea.appendText("███████████████████████████████████████████████████████████████████████████\n"));
    }

    /**
     * 在指定的输出区域中追加换行符
     *
     * @param outputArea 输出区域
     */
    public static void appendNewLine(TextArea outputArea) {
        Platform.runLater(() -> outputArea.appendText("\n"));
    }

    /**
     * 在指定的输出区域用s替换最新一行的文本
     *
     * @param outputArea 输出区域
     */
    public static void replaceText(TextArea outputArea, String s) {
        Platform.runLater(() -> {
            String currentText = outputArea.getText();
            int lastNewLineIndex = currentText.lastIndexOf("\n");
            if (lastNewLineIndex == -1) {
                // 如果没有换行符，表示只有一行，直接替换
                outputArea.setText("█ " + s);
            } else {
                // 存在换行符，替换最后一行
                String beforeLastLine = currentText.substring(0, lastNewLineIndex + 1);
                outputArea.setText(beforeLastLine + "█ " + s);
            }
            scrollToBottom(outputArea);
        });
    }

    /**
     * 滚动到TextArea的底部
     *
     * @param outputArea 输出区域
     */
    private static void scrollToBottom(TextArea outputArea) {
        outputArea.setScrollTop(Double.MAX_VALUE);
    }


}
