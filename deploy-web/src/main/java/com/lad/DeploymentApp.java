package com.lad;

import com.lad.history.History;
import com.lad.history.HistoryTool;
import com.lad.ui.ComponentCreator;
import com.lad.ui.OutPutTool;
import com.lad.uploader.DirUploader;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;


import java.io.*;

import java.util.*;


/**
 * Hello world!
 *
 * @author AD
 */
public class DeploymentApp extends Application {

    // 五个变量对应的英文名（也是history.txt文件中的标识）
    private static final String HOST = HistoryTool.HOST;
    private static final String USERNAME = HistoryTool.USERNAME;
    private static final String PASSWORD = HistoryTool.PASSWORD;
    private static final String LOCAL_PATH = HistoryTool.LOCAL_PATH;
    private static final String SERVER_PATH = HistoryTool.SERVER_PATH;

    @Override
    public void start(Stage primaryStage) {
        // 读取历史记录
        List<History> histories = HistoryTool.loadHistory();

        // 舞台相关配置
        // 创建标签和文本框
        Label hostLabel = new Label("主机地址:");
        ComboBox<String> hostField = ComponentCreator.createComboBox(HOST, "主机地址", histories);
        Label usernameLabel = new Label("用户名:");
        ComboBox<String> usernameField = ComponentCreator.createComboBox(USERNAME, "用户名", histories);
        Label passwordLabel = new Label("密码:");
        ComboBox<String> passwordField = ComponentCreator.createComboBox(PASSWORD, "密码", histories);
        Label localPathLabel = new Label("本地项目路径:");
        ComboBox<String> localPathField = ComponentCreator.createComboBox(LOCAL_PATH, "本地项目路径", histories);
        Label serverPathLabel = new Label("服务器项目路径:");
        ComboBox<String> serverPathField = ComponentCreator.createComboBox(SERVER_PATH, "服务器项目路径", histories);
        // 创建文本区域用于输出信息
        TextArea outputArea = new TextArea();
        outputArea.setEditable(false);
        // 创建按钮
        Button deployButton = new Button("一键部署");
        // 创建布局并添加组件
        VBox layout = new VBox(10);
        layout.setPadding(new Insets(20));
        layout.getChildren().addAll(hostLabel, hostField, usernameLabel, usernameField, passwordLabel, passwordField, localPathLabel, localPathField, serverPathLabel, serverPathField, outputArea, deployButton);
        // 设置场景
        Scene scene = new Scene(layout, 600, 650);
        // 配置舞台
        primaryStage.setTitle("前端自动化部署工具");
        primaryStage.setScene(scene);
        primaryStage.show();

        // 五个字段的ComboBox List
        List<ComboBox<String>> fields = Arrays.asList(hostField, usernameField, passwordField, localPathField, serverPathField);

        // 设置按钮的点击事件
        deployButton.setOnAction(e -> {
            new Thread(() -> buttonFunction(outputArea, histories, fields)).start();
        });

    }



    /**
     * 按钮点击事件
     *
     * @param outputArea 输出区域
     * @param histories  历史记录
     * @param fields     五个字段的ComboBox
     */
    private void buttonFunction(TextArea outputArea, List<History> histories, List<ComboBox<String>> fields) {

        // 判断输入是否为空
        for (ComboBox<String> field : fields) {
            if (field.getValue() == null || field.getValue().isEmpty()) {
                OutPutTool.appendText(outputArea, "请填写所有字段\n");
                return;
            }
        }

        OutPutTool.appendDivider(outputArea);
        OutPutTool.appendText(outputArea, "开始部署！\n");

        // 更新历史记录
        HistoryTool.updateHistory(histories, fields);

        //  上传文件到服务器
        DirUploader.upload(fields, outputArea);

        OutPutTool.appendText(outputArea, "部署完成！\n");
        OutPutTool.appendDivider(outputArea);
    }

    public static void main(String[] args) throws IOException {
        launch(args);

    }

}
