package com.lad.business.uploader;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import com.lad.business.server.SessionTool;
import com.lad.business.StringTool;
import com.lad.business.ui.OutPutTool;
import javafx.scene.control.TextArea;

import java.io.File;
import java.util.List;

/**
 * @author Andy
 * @date 2024-2-6 006 16:34
 */
public class SingleFileUploader {

    /**
     * 上传单个文件到服务器。
     *
     * @param values 五个字段的value，分别是主机、用户名、密码、本地文件路径和服务器路径
     * @param outputArea 输出区域，用于显示操作信息
     * @return 上传成功返回1，否则返回0
     */
    public static int upload(List<String> values, TextArea outputArea) {
        // 获取输入的值
        String host = values.get(0);
        String username = values.get(1);
        String password = values.get(2);
        String localPath = values.get(3);
        String serverPath = values.get(4);

        // 获取本地上传的文件名
        String localFileName = StringTool.getWindowsFileName(localPath);
        OutPutTool.appendText(outputArea, "开始上传文件 " + localFileName + " 到服务器路径 " + serverPath + "目录下...");

        File localFile = new File(localPath);
        if (localFile.exists() && localFile.isFile()) {
            try {
                // 创建 SSH 会话
                Session session = SessionTool.createSession(username, password, host);
                uploadFileToServer(localPath, serverPath, outputArea, session);
                SessionTool.closeSession(session);
                return 1;
            } catch (RuntimeException e) {
                OutPutTool.appendText(outputArea, e.getMessage() );
                return 0;
            } catch (JSchException e) {
                OutPutTool.appendText(outputArea, "SSH会话创建失败: " + e.getMessage());
                return 0;
            }
        } else {
            OutPutTool.appendText(outputArea, "未在本地找到文件 " + localFileName);
            OutPutTool.appendText(outputArea, "程序退出");
            return 0;
        }
    }

    /**
     * 通过SSH会话上传文件到服务器。（同名文件会自动覆盖）
     *
     * @param localFilePath 本地文件全路径
     * @param serverPath    服务器路径（例如"/var/www/test/"或者"/var/www/test")
     * @param outputArea    输出区域
     * @param session       SSH会话
     */
    private static void uploadFileToServer(String localFilePath, String serverPath, TextArea outputArea, Session session) {
        try {
            ChannelSftp channelSftp = (ChannelSftp) session.openChannel("sftp");
            channelSftp.connect();

            // 上传文件
            File file = new File(localFilePath);
            String serverFilePath = serverPath + file.getName();
            channelSftp.put(localFilePath, serverFilePath);
            OutPutTool.appendText(outputArea, "文件 " + file.getName() + " 已成功上传到 " + serverFilePath);

            // 关闭连接
            channelSftp.disconnect();

        } catch (JSchException | SftpException e) {
            throw new RuntimeException("上传文件失败: " + e.getMessage() + ": "+ serverPath);
        }
    }
}