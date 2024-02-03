package com.lad;

import com.jcraft.jsch.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;


/**
 * Hello world!
 *
 */
public class DeploymentTool extends Application {

    // 服务器信息
    private final String USERNAME = "root";
    private final String PASSWORD = "_WC5H3vnWQpgGMb";
    private final String HOST = "150.158.22.85";
    private final int PORT = 22; // SSH 默认端口是 22

	// jar包名称
    private String JAR_NAME="qims-0.0.1-SNAPSHOT.jar";
    // 本地项目路径默认值
    private String LOCAL_PATH_DEFAULT = "D:\\mine\\Project\\skyherb-qims\\skyherb-qims";
    // 服务器项目路径默认值
    private String SERVER_PATH_DEFAULT = "/usr/local/river-skyherb/";

    @Override
    public void start(Stage primaryStage) {

        // 创建输入框
        Label jarNamelabel = new Label("jar包名称（确保和maven打包出来的jar包的名字一样）");
        TextField jarNameField = new TextField(JAR_NAME);
        jarNameField.setPromptText("请输入jar包名称");

        Label localPathLabel = new Label("本地项目路径:");
        TextField localPathField = new TextField(LOCAL_PATH_DEFAULT);
        localPathField.setPromptText("请输入本地项目路径");

        Label serverPathLabel = new Label("服务器项目路径:");
        TextField serverPathField = new TextField(SERVER_PATH_DEFAULT);
        serverPathField.setPromptText("请输入服务器项目路径");



        // 创建文本区域用于输出信息
        TextArea outputArea = new TextArea();
        outputArea.setEditable(false);

        // 创建按钮
        Button deployButton = new Button("一键部署");

        // 设置按钮的点击事件
        deployButton.setOnAction(e -> {
            String jarName = jarNameField.getText();
            String localPath = localPathField.getText();
            String serverPath = serverPathField.getText();

            new Thread(() -> buttonFunction(outputArea,localPath,serverPath,jarName)).start();
        });

        // 创建布局并添加组件
        VBox layout = new VBox(10);
        layout.setPadding(new Insets(20));
        layout.getChildren().addAll(jarNamelabel,jarNameField,localPathLabel, localPathField, serverPathLabel, serverPathField, outputArea, deployButton);

        // 设置场景
        Scene scene = new Scene(layout, 600, 500);

        // 配置舞台
        primaryStage.setTitle("后端自动化部署工具");
        primaryStage.setScene(scene);
        primaryStage.show();
    }


    /**
     * 按钮点击事件
     * @param outputArea 输出区域
     * @param localPath 本地路径
     * @param serverPath 服务器路径
     */
    private void buttonFunction(TextArea outputArea,String localPath,String serverPath,String jarName) {
        outputArea.appendText("部署开始...\n");

        // 1. maven 打包
        outputArea.appendText("开始执行 Maven 打包...\n");
        int i = executeMavenPackage(localPath, outputArea);
        // 等待0.2秒
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (i != 0) {
            outputArea.appendText("Maven 打包失败\n");
            outputArea.appendText("程序退出\n");
            return;
        }

        // 2. 上传文件到服务器
        outputArea.appendText("开始上传文件到服务器...\n");
        // 获取 target 目录下的 jar 文件
        File targetDir = new File(localPath + "/target");
        File[] jarFiles = targetDir.listFiles((dir, name) -> name.endsWith(".jar"));
        if (jarFiles != null && jarFiles.length > 0) {
            // 上传第一个找到的 jar 文件
            outputArea.appendText("文件上传中...\n");
            uploadFileToServer(jarFiles[0].getAbsolutePath(), serverPath, outputArea);
        } else {
            outputArea.appendText("未找到 jar 文件。\n");
            outputArea.appendText("程序退出\n");
            return;
        }

        // 3. 重启后端服务
        outputArea.appendText("开始重启后端服务...\n");
        manageRemoteProcess(serverPath, outputArea, jarName);
    }


    /**
     * 执行 Maven 打包
     * @param projectPath 项目路径
     * @param outputArea 输出区域
     * @return 执行结果 1-失败 0-成功
     */
    private int executeMavenPackage(String projectPath, TextArea outputArea) {
        try {
            ProcessBuilder builder = new ProcessBuilder();
            builder.command("cmd.exe", "/c", "mvn clean package -Dspring.profiles.active=home -DskipTests");
            builder.directory(new File(projectPath));

            builder.redirectErrorStream(true); // 将错误信息和输出信息合并

            Process process = builder.start();

            // 读取命令行输出
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                String finalLine = line;
                Platform.runLater(() -> outputArea.appendText(finalLine + "\n"));
            }

            int exitCode = process.waitFor();
            Platform.runLater(() -> outputArea.appendText("打包完成，退出码: " + exitCode + "\n"));
            return exitCode;
        } catch (IOException | InterruptedException e) {
            Platform.runLater(() -> outputArea.appendText("打包过程中出错: " + e.getMessage() + "\n"));
            return 1;
        }
    }


    /**
     * 上传文件到服务器
     * @param localFilePath 本地文件路径
     * @param serverPath 服务器路径
     * @param outputArea 输出区域
     */
    private void uploadFileToServer(String localFilePath, String serverPath, TextArea outputArea) {
        Session session = null;
        Channel channel = null;
        ChannelSftp channelSftp = null;

        try {
            JSch jsch = new JSch();
            session = jsch.getSession(USERNAME, HOST, PORT);
            session.setPassword(PASSWORD);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();

            channel = session.openChannel("sftp");
            channel.connect();
            channelSftp = (ChannelSftp) channel;

            channelSftp.put(localFilePath, serverPath);

            Platform.runLater(() -> outputArea.appendText("文件上传成功\n"));
        } catch (JSchException | SftpException e) {
            Platform.runLater(() -> outputArea.appendText("文件上传失败: " + e.getMessage() + "\n"));
        } finally {
            if (channelSftp != null) {
                channelSftp.exit();
            }
            if (channel != null) {
                channel.disconnect();
            }
            if (session != null) {
                session.disconnect();
            }
        }
    }


    /**
     * 远程进程重启
     * @param serverPath 项目所在路径
     * @param outputArea 输出区域
     */
    private void manageRemoteProcess(String serverPath, TextArea outputArea,String jarName) {

        Session session = null;
        ChannelExec channel = null;

        try {
            JSch jsch = new JSch();
            session = jsch.getSession(USERNAME, HOST, PORT);
            session.setPassword(PASSWORD);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();


            // 停止当前运行的进程
            // 启动新的 jar 进程
            // 执行 shell 脚本
            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand("sh " + serverPath+ "restart.sh");
            channel.connect();


            Platform.runLater(() -> outputArea.appendText("远程进程管理完成\n"));
        } catch (JSchException e) {
            Platform.runLater(() -> outputArea.appendText("远程进程管理失败: " + e.getMessage() + "\n"));
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
            if (session != null) {
                session.disconnect();
            }
        }
    }


    public static void main(String[] args) {
        launch(args);
    }
}