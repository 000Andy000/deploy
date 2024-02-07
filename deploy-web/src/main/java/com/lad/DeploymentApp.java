package com.lad;

import com.lad.history.History;
import com.lad.history.HistoryTool;
import com.lad.server.ServerManager;
import com.lad.ui.ComponentCreator;
import com.lad.ui.OutPutTool;
import com.lad.uploader.DirUploader;
import com.lad.uploader.SingleFileUploader;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;


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
        Label localPathLabel = new Label("本地路径:(前端选择dist文件夹，后端选择jar文件)");
        ComboBox<String> localPathField = ComponentCreator.createComboBox(LOCAL_PATH, "本地项目路径", histories);
        Label serverPathLabel = new Label("服务器项目路径:(请选择一个文件夹)");
        ComboBox<String> serverPathField = ComponentCreator.createComboBox(SERVER_PATH, "服务器项目路径", histories);
        // 创建部署类型选择框
        Label deploymentTypeLabel = new Label("部署类型:");
        ComboBox<String> deploymentTypeField = new ComboBox<>(FXCollections.observableArrayList("前端web部署", "后端jar部署"));
        deploymentTypeField.setValue("前端web部署"); // 设置默认值

        // 创建文本区域用于输出信息
        TextArea outputArea = new TextArea();
        outputArea.setEditable(false);
        outputArea.setPrefHeight(400);
        outputArea.setPrefWidth(800);
        // 创建按钮
        Button deployButton = new Button("一键部署");
        // 创建布局并添加组件
        VBox layout = new VBox(10);
        layout.setPadding(new Insets(20));
        layout.getChildren().addAll(
                deploymentTypeLabel, deploymentTypeField,
                hostLabel, hostField,
                usernameLabel, usernameField,
                passwordLabel, passwordField,
                localPathLabel, localPathField,
                serverPathLabel, serverPathField,
                outputArea,
                deployButton);


        // 设置场景
        Scene scene = new Scene(layout, 900, 850);
        // 配置舞台
        primaryStage.setTitle("自动化部署工具");
        primaryStage.setScene(scene);
        primaryStage.show();

        // 六个字段的ComboBox List
        List<ComboBox<String>> fields = Arrays.asList(hostField, usernameField, passwordField, localPathField, serverPathField, deploymentTypeField);

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
     * @param fields     六个字段的ComboBox
     */
    private void buttonFunction(TextArea outputArea, List<History> histories, List<ComboBox<String>> fields) {

        // 判断输入是否为空
        for (ComboBox<String> field : fields) {
            if (field.getValue() == null || field.getValue().isEmpty()) {
                OutPutTool.appendText(outputArea, "请填写所有字段");
                return;
            }
        }

        // 获取输入的值
        List<String> values = new ArrayList<>();
        // 第5个字段是服务器路径，需要在最后加上"/"（如果没有的话）
        for (ComboBox<String> field : fields) {
            if (field.equals(fields.get(4))) {
                String value = field.getValue();
                if (!value.endsWith("/")) {
                    value += "/";
                }
                values.add(value);
            } else {
                // 其他字段直接添加
                values.add(field.getValue());
            }
        }

        OutPutTool.appendDivider(outputArea);
        OutPutTool.appendText(outputArea, "开始部署！");

        // 更新历史记录
        HistoryTool.updateHistory(histories, fields);

        // 判断部署类型
        String deploymentType = fields.get(5).getValue();
        switch (deploymentType) {
            case "前端web部署":
                webDeploy(outputArea, values);
                break;
            case "后端jar部署":
                jarDeploy(outputArea, values);
                break;
        }
        OutPutTool.appendDivider(outputArea);
        OutPutTool.appendNewLine(outputArea);
    }

    /**
     * 前端部署
     *
     * @param outputArea 输出区域
     * @param values     五个字段的value
     */
    private void webDeploy(TextArea outputArea, List<String> values) {
        // 上传文件夹到服务器
        DirUploader.upload(values, outputArea);

    }

    /**
     * 后端部署
     *
     * @param outputArea 输出区域
     * @param values     五个字段的value
     */
    private void jarDeploy(TextArea outputArea, List<String> values) {
        // 上传文件到服务器
        int flag = SingleFileUploader.upload(values, outputArea);

        if (flag == 0) {
            return;
        }

        // 重启后端服务
        ServerManager.restartServer(values, outputArea);
    }



}