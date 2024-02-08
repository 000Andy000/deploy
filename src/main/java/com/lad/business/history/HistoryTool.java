package com.lad.business.history;

import javafx.application.Platform;
import javafx.scene.control.ComboBox;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author Andy
 * @date 2024-2-6 006 15:38
 */
public class HistoryTool {
    // 特殊标识符
    public static final String SPECIAL_SYMBOL = "██████████";


    // 历史记录文件名
    private static final String FILE_NAME = "history.txt";
    private static final String DIRECTORY_PATH = System.getProperty("user.dir");
    private static final String FULL_PATH = DIRECTORY_PATH + File.separator + FILE_NAME;


    // 五个变量对应的英文名（也是history.txt文件中的标识）
    public static final String HOST = "host";
    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";
    public static final String LOCAL_PATH = "localPath";
    public static final String SERVER_PATH = "serverPath";


    /**
     * 读取历史记录
     *
     * @return 历史记录
     */
    public static List<History> loadHistory() {
        List<History> histories = new ArrayList<>();
        File file = new File(FILE_NAME);
        if (!file.exists()) {
            return histories;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            // 声明一个空的History对象
            History history = new History("", new ArrayList<>());

            while ((line = reader.readLine()) != null) {
                // 如果是以 SPECIAL_SYMBOL 开头，则表示是一个新的历史记录，需要创建新的History对象，并添加到histories列表中
                if (line.startsWith(SPECIAL_SYMBOL)) {
                    history = new History(line.substring(SPECIAL_SYMBOL.length()), new ArrayList<>());
                    histories.add(history);
                } else if (!line.isEmpty()) {
                    // 否则将当前行添加到最近一个History对象的values列表中
                    history.getValues().add(line);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return histories;
    }


    /**
     * 更新历史记录。
     *
     * @param histories 包含旧历史记录的List
     * @param fields    五个字段的ComboBox 需要按照 HOST, USERNAME, PASSWORD, LOCAL_PATH, SERVER_PATH 的顺序
     */
    public static void updateHistory(List<History> histories, List<ComboBox<String>> fields) {
        // 确保fields列表中的顺序与keys列表中的顺序一致
        List<String> keys = Arrays.asList(HOST, USERNAME, PASSWORD, LOCAL_PATH, SERVER_PATH);


        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            String value = fields.get(i).getValue();

            // 获取当前key的历史记录列表，如果不存在则创建新的列表
            List<String> values = histories.stream()
                    .filter(history -> history.getName().equals(key))
                    .findFirst()
                    .map(History::getValues)
                    .orElseGet(() -> {
                        List<String> list = new ArrayList<>();
                        histories.add(new History(key, list));
                        return list;
                    });

            // 如果列表已经包含当前值，则先移除
            values.remove(value);

            // 将当前值添加到列表的开头
            values.add(0, value);
        }
        // 保存历史记录到文件
        saveHistoryToFile(histories);
        // 更新fields中的历史记录
        for (int i = 0; i < keys.size(); i++) {
            final ComboBox<String> field = fields.get(i);
            int finalI = i;
            List<String> values = histories.stream()
                    .filter(history -> history.getName().equals(keys.get(finalI)))
                    .findFirst()
                    .map(History::getValues)
                    .orElse(Collections.emptyList());

            // 将更新操作放在JavaFX主线程上执行
            Platform.runLater(() -> {
                field.getItems().clear(); // 首先清除现有的项
                if (!values.isEmpty()) {
                    field.setValue(values.get(0)); // 设置默认值为最新的历史记录
                }
                field.getItems().addAll(values); // 添加所有历史记录
            });
        }
    }


    /**
     * 将历史记录保存到文件中。
     *
     * @param histories 包含历史记录的List。
     */
    public static void saveHistoryToFile(List<History> histories) {
        try {
            File file = new File(FULL_PATH);
            if (!file.exists()) {
                file.createNewFile(); // 如果文件不存在，则创建新文件
            }

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, false))) {
                for (History history : histories) {
                    writer.write(SPECIAL_SYMBOL + history.getName());
                    writer.newLine();
                    for (String value : history.getValues()) {
                        writer.write(value);
                        writer.newLine();
                    }
                    writer.newLine();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
