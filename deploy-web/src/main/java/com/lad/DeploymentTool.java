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
import java.util.Vector;


/**
 * Hello world!
 *
 * @author AD
 */
public class DeploymentTool extends Application {

    // 服务器信息
    private final String USERNAME = "root";
    private final String PASSWORD = "_WC5H3vnWQpgGMb";
    private final String HOST = "150.158.22.85";
    private final int PORT = 22; // SSH 默认端口是 22


    // 本地项目路径默认值
    private String LOCAL_PATH_DEFAULT = "D:\\mine\\Project\\skyherb-qims\\skyhub-qims-front";
    // 服务器项目路径默认值
    private String SERVER_PATH_DEFAULT = "/var/www/river-skyherb/skyherb-qims-front/";

    @Override
    public void start(Stage primaryStage) {


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
            String localPath = localPathField.getText();
            String serverPath = serverPathField.getText();

            new Thread(() -> buttonFunction(outputArea, localPath, serverPath)).start();
        });

        // 创建布局并添加组件
        VBox layout = new VBox(10);
        layout.setPadding(new Insets(20));
        layout.getChildren().addAll(localPathLabel, localPathField, serverPathLabel, serverPathField, outputArea, deployButton);

        // 设置场景
        Scene scene = new Scene(layout, 600, 500);

        // 配置舞台
        primaryStage.setTitle("前端自动化部署工具");
        primaryStage.setScene(scene);
        primaryStage.show();
    }


    /**
     * 按钮点击事件
     *
     * @param outputArea 输出区域
     * @param localPath  本地路径
     * @param serverPath 服务器路径
     */
    private void buttonFunction(TextArea outputArea, String localPath, String serverPath) {
        outputArea.appendText("部署开始...\n");

        // 1. maven 打包
        outputArea.appendText("开始执行 Maven 打包...\n");
        int i = executeNpmBuild(localPath, outputArea);
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
        outputArea.appendText("开始上传dist到服务器...\n");
        String localDistPath = localPath + "/dist"; // 前端打包后的目录
        File distDir = new File(localDistPath);
        if (distDir.exists() && distDir.isDirectory()) {
            try {
                Session session = createSession();
                uploadDirectoryToServer(localDistPath, serverPath, outputArea, session);
                // 等待0.2秒
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                outputArea.appendText("文件上传完成\n");
                session.disconnect();
            } catch (JSchException e) {
                Platform.runLater(() -> outputArea.appendText("SSH会话创建失败: " + e.getMessage() + "\n"));
            }
        } else {
            outputArea.appendText("未找到 'dist' 目录。\n");
            outputArea.appendText("程序退出\n");
        }

    }


    /**
     * 执行 Maven 打包
     *
     * @param projectPath 项目路径
     * @param outputArea  输出区域
     * @return 执行结果 1-失败 0-成功
     */
    private int executeNpmBuild(String projectPath, TextArea outputArea) {
        try {
            ProcessBuilder builder = new ProcessBuilder();
            builder.command("cmd.exe", "/c", "npm run build");
            builder.directory(new File(projectPath));
            builder.redirectErrorStream(true);

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
     * 创建 SSH 会话
     *
     * @return SSH 会话
     */
    private Session createSession() throws JSchException {
        JSch jsch = new JSch();
        Session session = jsch.getSession(USERNAME, HOST, PORT);
        session.setPassword(PASSWORD);
        session.setConfig("StrictHostKeyChecking", "no");
        session.connect();
        return session;
    }


    /**
     * 上传文件到服务器
     *
     * @param localDistPath 本地dist路径
     * @param serverPath    服务器路径
     * @param outputArea    输出区域
     */
    private void uploadDirectoryToServer(String localDistPath, String serverPath, TextArea outputArea, Session session) {
        try {
            ChannelSftp channelSftp = (ChannelSftp) session.openChannel("sftp");
            channelSftp.connect();

            // 获取本地目录名（这里是 "dist"）
            File localDir = new File(localDistPath);
            String dirName = localDir.getName();

            // 构建服务器上的目录路径
            String serverDirPath = serverPath + "/" + dirName;

            // 尝试删除服务器上现有的目录
            try {
                deleteDirectory(channelSftp, serverDirPath);
                Platform.runLater(() -> outputArea.appendText("删除服务器上的旧 'dist' 目录\n"));
            } catch (SftpException e) {
                Platform.runLater(() -> outputArea.appendText("无法删除服务器上的 'dist' 目录: " + e.getMessage() + "\n"));
            }

            // 等待0.2秒
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // 创建新的目录
            channelSftp.mkdir(serverDirPath);
            // 递归上传目录内容
            outputArea.appendText("文件上传中...\n");
            uploadDirectory(channelSftp, localDistPath, serverDirPath, outputArea);

            channelSftp.disconnect();
        } catch (JSchException | SftpException e) {
            Platform.runLater(() -> outputArea.appendText("上传目录失败: " + e.getMessage() + "\n"));
        }
    }

    /**
     * 上传目录
     *
     * @param channelSftp sftp 通道
     * @param localPath   本地路径
     * @param serverPath  服务器路径
     * @param outputArea  输出区域
     */
    private void uploadDirectory(ChannelSftp channelSftp, String localPath, String serverPath, TextArea outputArea) throws SftpException {
        File localDir = new File(localPath);
        if (!localDir.isDirectory()) {
            throw new SftpException(ChannelSftp.SSH_FX_NO_SUCH_FILE, "本地路径不是目录");
        }

        // 确保服务器上有对应的目录
        try {
            channelSftp.cd(serverPath);
        } catch (SftpException e) {
            // 目录不存在，需要创建
            channelSftp.mkdir(serverPath);
            channelSftp.cd(serverPath);
        }

        for (File file : localDir.listFiles()) {
            if (file.isDirectory()) {
                // 创建服务器上的目录并递归上传
                try {
                    channelSftp.mkdir(serverPath + "/" + file.getName());
                } catch (SftpException e) {
                    // 如果目录已经存在，则忽略错误
                }
                uploadDirectory(channelSftp, file.getAbsolutePath(), serverPath + "/" + file.getName(), outputArea);
            } else {
                // 上传文件
                channelSftp.put(file.getAbsolutePath(), serverPath + "/" + file.getName());
                Platform.runLater(() -> outputArea.appendText("上传文件: " + file.getName() + "\n"));
            }
        }
    }


    /**
     * 删除服务器上的目录
     *
     * @param channelSftp   sftp 通道
     * @param serverDirPath 服务器目录路径
     */
    private void deleteDirectory(ChannelSftp channelSftp, String serverDirPath) throws SftpException {
        Vector<ChannelSftp.LsEntry> list = channelSftp.ls(serverDirPath);
        for (ChannelSftp.LsEntry entry : list) {
            String name = entry.getFilename();
            if (!".".equals(name) && !"..".equals(name)) {
                String fullPath = serverDirPath + "/" + name;
                if (entry.getAttrs().isDir()) {
                    deleteDirectory(channelSftp, fullPath);
                } else {
                    channelSftp.rm(fullPath);
                }
            }
        }
        channelSftp.rmdir(serverDirPath);
    }


    public static void main(String[] args) {
        launch(args);
    }
}