package com.lad.business.ui;

import com.lad.business.history.History;
import javafx.scene.control.ComboBox;

import java.util.Collections;
import java.util.List;

/**
 * @author Andy
 * @date 2024-2-6 006 17:55
 */
public class ComponentCreator {

    /**
     * 创建一个hostField
     *
     * @param name      字段英文名
     * @param cnName    字段中文名
     * @param histories
     * @return ComboBox<String>
     */
    public static ComboBox<String> createComboBox(String name, String cnName, List<History> histories) {
        ComboBox<String> field = new ComboBox<>();
        field.setEditable(true);
        field.setPromptText("请输入" + cnName);
        field.setMinWidth(750);
        // 获取对应历史记录
        List<String> values = histories.stream()
                .filter(history -> history.getName().equals(name))
                .findFirst()
                .map(History::getValues)
                .orElse(Collections.emptyList());
        if (!values.isEmpty()) {
            field.setValue(values.get(0));
        }
        field.getItems().addAll(values);
        return field;
    }

}
