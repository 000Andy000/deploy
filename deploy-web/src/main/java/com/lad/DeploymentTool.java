package com.lad;

import com.jcraft.jsch.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
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
public class DeploymentTool extends Application {

    // 历史记录文件名
    private static final String FILE_NAME = "history.txt";
    private static final String DIRECTORY_PATH = System.getProperty("user.dir");
    private static final String FULL_PATH = DIRECTORY_PATH + File.separator + FILE_NAME;

    // 五个变量对应的英文名
    private static final String HOST = "host";
    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";
    private static final String LOCAL_PATH = "localPath";
    private static final String SERVER_PATH = "serverPath";


    // 服务器信息
    private final int PORT = 22; // SSH 默认端口是 22


    @Override
    public void start(Stage primaryStage) {
        Map<String, List<String>> historyMap = loadHistory();

        Label hostLabel = new Label("主机地址:");
        ComboBox<String> hostField = createComboBox(HOST, "主机地址", historyMap);

        Label usernameLabel = new Label("用户名:");
        ComboBox<String> usernameField = createComboBox(USERNAME, "用户名", historyMap);


        Label passwordLabel = new Label("密码:");
        ComboBox<String> passwordField = createComboBox(PASSWORD, "密码", historyMap);

        Label localPathLabel = new Label("本地项目路径:");
        ComboBox<String> localPathField = createComboBox(LOCAL_PATH, "本地项目路径", historyMap);


        Label serverPathLabel = new Label("服务器项目路径:");
        ComboBox<String> serverPathField = createComboBox(SERVER_PATH, "服务器项目路径", historyMap);


        // 创建文本区域用于输出信息
        TextArea outputArea = new TextArea();
        outputArea.setEditable(false);

        // 创建按钮
        Button deployButton = new Button("一键部署");

        // 五个字段的ComboBox List
        List<ComboBox<String>> fields = Arrays.asList(hostField, usernameField, passwordField, localPathField, serverPathField);

        // 设置按钮的点击事件
        deployButton.setOnAction(e -> {

            new Thread(() -> buttonFunction(outputArea, historyMap, fields)).start();
        });

        // 创建布局并添加组件
        VBox layout = new VBox(10);
        layout.setPadding(new Insets(20));
        layout.getChildren().addAll(hostLabel, hostField, usernameLabel, usernameField, passwordLabel, passwordField, localPathLabel, localPathField, serverPathLabel, serverPathField, outputArea, deployButton);
        // 设置场景
        Scene scene = new Scene(layout, 600, 500);

        // 配置舞台
        primaryStage.setTitle("前端自动化部署工具");
        primaryStage.setScene(scene);
        primaryStage.show();
    }


    /**
     * 创建一个hostField
     *
     * @param name       字段英文名
     * @param cnName     字段中文名
     * @param historyMap
     * @return ComboBox<String>
     */
    private ComboBox<String> createComboBox(String name, String cnName, Map<String, List<String>> historyMap) {
        ComboBox<String> field = new ComboBox<>();
        field.setEditable(true);
        field.setPromptText("请输入" + cnName);
        // 获取对应历史记录
        List<String> history = historyMap.getOrDefault(name, FXCollections.observableArrayList());
        if (!history.isEmpty()) {
            field.setValue(history.get(0));
        }
        field.getItems().addAll(history);
        return field;
    }

    /**
     * 按钮点击事件
     *
     * @param outputArea 输出区域
     * @param historyMap 历史记录
     * @param fields     五个字段的ComboBox
     */
    private void buttonFunction(TextArea outputArea, Map<String, List<String>> historyMap, List<ComboBox<String>> fields) {

        // 判断输入是否为空
        for (ComboBox<String> field : fields) {
            if (field.getValue() == null || field.getValue().isEmpty()) {
                outputArea.appendText("请填写所有字段\n");
                return;
            }
        }

        // 获取输入的值
        String host = fields.get(0).getValue();
        String username = fields.get(1).getValue();
        String password = fields.get(2).getValue();
        String localPath = fields.get(3).getValue();
        String serverPath = fields.get(4).getValue();


        outputArea.appendText("部署开始...\n");

        // 更新历史记录
        updateHistory(historyMap, fields);


        //  上传文件到服务器
        outputArea.appendText("开始上传dist到服务器...\n");

        File distDir = new File(localPath);
        if (distDir.exists() && distDir.isDirectory()) {
            try {
                // 创建 SSH 会话
                Session session = createSession(username, password, host);
                uploadDirectoryToServer(localPath, serverPath, outputArea, session);
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
            outputArea.appendText("未在本地找到相应文件。\n");
            outputArea.appendText("程序退出\n");
        }


    }


    /**
     * 创建 SSH 会话
     *
     * @return SSH 会话
     */
    private Session createSession(String username, String password, String host) throws JSchException {
        JSch jsch = new JSch();
        Session session = jsch.getSession(username, host, PORT);
        session.setPassword(password);
        session.setConfig("StrictHostKeyChecking", "no");
        session.connect();
        return session;
    }


    /**
     * 上传文件到服务器
     *
     * @param localPath  本地dist路径
     * @param serverPath 服务器路径
     * @param outputArea 输出区域
     */
    private void uploadDirectoryToServer(String localPath, String serverPath, TextArea outputArea, Session session) {
        try {
            ChannelSftp channelSftp = (ChannelSftp) session.openChannel("sftp");
            channelSftp.connect();

            // 获取本地目录名（这里是 "dist"）
            File localDir = new File(localPath);
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
            uploadDirectory(channelSftp, localPath, serverDirPath, outputArea);

            channelSftp.disconnect();
        } catch (JSchException | SftpException e) {
            Platform.runLater(() -> outputArea.appendText("上传目录失败: " + e.getMessage() + " : " + serverPath + "\n"));
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






    /**
     * 读取历史记录
     *
     * @return 历史记录
     */
    private static Map<String, List<String>> loadHistory() {
        Map<String, List<String>> historyMap = new HashMap<>();
        File file = new File(FILE_NAME);
        if (!file.exists()) {
            return historyMap;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            List<String> currentList = null;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("#")) {
                    currentList = historyMap.computeIfAbsent(line.substring(1), k -> new ArrayList<>());
                } else if (currentList != null) {
                    currentList.add(line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return historyMap;
    }


    /**
     * 更新历史记录。
     *
     * @param historyMap 包含旧历史记录的Map。
     * @param fields     五个字段的ComboBox 需要按照 HOST, USERNAME, PASSWORD, LOCAL_PATH, SERVER_PATH 的顺序
     */
    public static void updateHistory(Map<String, List<String>> historyMap, List<ComboBox<String>> fields) {
        // 确保values列表中的顺序与historyMap中的键匹配
        List<String> keys = Arrays.asList(HOST, USERNAME, PASSWORD, LOCAL_PATH, SERVER_PATH);


        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            String value = fields.get(i).getValue();

            // 获取当前key的历史记录列表，如果不存在则创建新的列表
            List<String> historyList = historyMap.computeIfAbsent(key, k -> new ArrayList<>());

            // 如果列表已经包含当前值，则先移除
            historyList.remove(value);

            // 将当前值添加到列表的开头
            historyList.add(0, value);
        }
        // 保存历史记录到文件
        saveHistoryToFile(historyMap);
        // 更新fields中的历史记录
        for (int i = 0; i < keys.size(); i++) {
            final ComboBox<String> field = fields.get(i);
            List<String> history = historyMap.getOrDefault(keys.get(i), new ArrayList<>());

            // 将更新操作放在JavaFX主线程上执行
            Platform.runLater(() -> {
                field.getItems().clear(); // 首先清除现有的项
                if (!history.isEmpty()) {
                    field.setValue(history.get(0)); // 设置默认值为最新的历史记录
                }
                field.getItems().addAll(history); // 添加所有历史记录
            });
        }
    }


    /**
     * 将历史记录保存到文件中。
     *
     * @param history 包含历史记录的Map。
     */
    public static void saveHistoryToFile(Map<String, List<String>> history) {
        try {
            File file = new File(FULL_PATH);
            if (!file.exists()) {
                file.createNewFile(); // 如果文件不存在，则创建新文件
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, false))) {
                    writer.write("#host"+ "\n"+ "#username"+ "\n"+ "#password"+ "\n"+ "#localPath"+ "\n"+ "#serverPath");
                }
            }

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, false))) {
                for (Map.Entry<String, List<String>> entry : history.entrySet()) {
                    writer.write("#" + entry.getKey());
                    writer.newLine();
                    for (String value : entry.getValue()) {
                        writer.write(value);
                        writer.newLine();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    public static void main(String[] args) throws IOException {
        launch(args);

    }

}
