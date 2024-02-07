package com.lad.uploader;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import com.lad.server.SessionTool;
import com.lad.ui.OutPutTool;
import com.lad.StringTool;
import javafx.scene.control.TextArea;

import java.io.File;
import java.util.List;
import java.util.Vector;

/**
 * @author Andy
 * @date 2024-2-6 006 15:28
 */
public class DirUploader {

    // 用于记录上传了几个文件
    static int num = 0;

    /**
     * 处理上层传来的数据，并创建SSH会话，上传本地目录到服务器。
     *
     * @param values  五个字段的ComboBox
     * @param outputArea 输出区域，用于显示操作信息
     */
    public static void upload(List<String> values, TextArea outputArea) {
        // 获取输入的值
        String host = values.get(0);
        String username = values.get(1);
        String password = values.get(2);
        String localPath = values.get(3);
        String serverPath = values.get(4);

        // 获取本地上传的文件夹名称
        String localDirName = StringTool.getWindowsDirName(localPath);
        OutPutTool.appendText(outputArea, "开始上传本地" + localDirName + "文件夹到服务器的" + serverPath + "目录下...");

        File distDir = new File(localPath);
        if (distDir.exists() && distDir.isDirectory()) {
            try {
                // 创建 SSH 会话
                Session session = SessionTool.createSession(username, password, host);
                uploadDirectoryToServer(localPath, serverPath, outputArea, session);
                SessionTool.closeSession(session);
            } catch (JSchException e) {
                OutPutTool.appendText(outputArea, "SSH会话创建失败: " + e.getMessage());
            }
        } else {
            OutPutTool.appendText(outputArea, "未在本地找到名为" + localDirName + "的目录");
            OutPutTool.appendText(outputArea, "程序退出");
        }
    }


    /**
     * 开启通道并通过递归方法uploadDirectory来上传文件夹到服务器
     *
     * @param localPath  本地文件夹路径
     * @param serverPath 服务器存储路径（例如"/var/www/test/"或者"/var/www/test")
     * @param outputArea 输出区域
     * @param session    SSH 会话
     */
    public static void uploadDirectoryToServer(String localPath, String serverPath, TextArea outputArea, Session session) {
        try {
            ChannelSftp channelSftp = (ChannelSftp) session.openChannel("sftp");
            channelSftp.connect();

            // 获取本地目录名
            File localDir = new File(localPath);
            String dirName = localDir.getName();

            // 构建服务器上的目录路径
            String serverDirPath = serverPath + "/" + dirName;

            // 尝试删除服务器上现有的目录
            OutPutTool.appendText(outputArea, "尝试删除服务器上的旧 '" + dirName + "' 目录...");
            deleteDirectory(channelSftp, serverDirPath, outputArea);
            OutPutTool.appendText(outputArea, "已删除服务器上的旧 '" + dirName + "' 目录");

            // 创建新的目录
            channelSftp.mkdir(serverDirPath);
            // 递归上传目录内容
            OutPutTool.appendText(outputArea, "文件上传中，请耐心等待...");
            uploadDirectory(channelSftp, localPath, serverDirPath, outputArea);
            // 重置计数器num
            num = 0;
            // 关闭连接
            channelSftp.disconnect();

            OutPutTool.appendNewLine(outputArea);
            OutPutTool.appendText(outputArea, "上传完成");


        } catch (JSchException | SftpException e) {
            OutPutTool.appendText(outputArea, "上传目录失败: " + e.getMessage() + " : " + serverPath);
        }
    }


    /**
     * 上传目录，这个方法能够处理复杂的目录结构，自动创建服务器上缺失的目录，并将本地目录及其所有子目录和文件上传到服务器上的指定位置，同时将上传进度实时显示到GUI界面上
     *
     * @param channelSftp sftp 通道
     * @param localPath   本地路径（指向一个目录。如果不是，抛出一个SftpException异常，表示本地路径不是一个有效的目录）
     * @param serverPath  服务器路径
     * @param outputArea  输出区域
     */
    public static void uploadDirectory(ChannelSftp channelSftp, String localPath, String serverPath, TextArea outputArea) throws SftpException {
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
                try {
                    // 创建服务器上的目录并递归上传
                    channelSftp.mkdir(serverPath + "/" + file.getName());
                } catch (SftpException e) {
                    // 如果目录已经存在，则忽略错误
                }
                uploadDirectory(channelSftp, file.getAbsolutePath(), serverPath + "/" + file.getName(), outputArea);
            } else {
                // 上传文件
                channelSftp.put(file.getAbsolutePath(), serverPath + "/" + file.getName());
                OutPutTool.replaceText(outputArea, "已上传"+ ++num +"个文件，"+"最近一个上传的文件: " + file.getName());
            }
        }

    }

    /**
     * 删除服务器上的目录
     *
     * @param channelSftp   sftp 通道
     * @param serverDirPath 服务器目录路径
     * @param outputArea    输出
     */
    private static void deleteDirectory(ChannelSftp channelSftp, String serverDirPath, TextArea outputArea) {
        // 移除末尾的斜杠（如果有的话）后，获取到最后一级目录的目录名
        String dirName = StringTool.getLinuxDirName(serverDirPath);
        try {
            Vector<ChannelSftp.LsEntry> list = channelSftp.ls(serverDirPath);
            for (ChannelSftp.LsEntry entry : list) {
                String name = entry.getFilename();
                if (!".".equals(name) && !"..".equals(name)) {
                    String fullPath = serverDirPath + "/" + name;
                    if (entry.getAttrs().isDir()) {
                        deleteDirectory(channelSftp, fullPath, outputArea);
                    } else {
                        channelSftp.rm(fullPath);
                    }
                }
            }
            channelSftp.rmdir(serverDirPath);
        } catch (SftpException e) {
            OutPutTool.appendText(outputArea, "无法删除服务器上的 '" + dirName + "' 目录: " + e.getMessage());

        }
    }
}
