package com.lad.server;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.lad.ui.OutPutTool;
import javafx.application.Platform;
import javafx.scene.control.TextArea;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author Andy
 * @date 2024-2-7 007 13:16
 */
public class RemoteCommandExecutor {
    // SSH会话对象
    private final Session session;

    /**
     * 构造函数，初始化RemoteCommandExecutor实例。
     *
     * @param session 一个已经通过认证的SSH会话。
     */
    public RemoteCommandExecutor(Session session) {
        this.session = session;
    }

    /**
     * 在通过SSH会话连接的服务器上执行一个指定的命令。
     *
     * @param command 需要在远端服务器上执行的命令。
     * @return 命令执行的输出结果作为字符串返回。
     * @throws JSchException 如果在执行命令或连接过程中出现了问题。
     * @throws IOException 如果读取命令执行结果时出现IO错误。
     */
    public String executeCommand(String command) throws JSchException, IOException {
        if (session == null || !session.isConnected()) {
            // 确保会话是活动的
            throw new IllegalStateException("SSH会话未连接或已关闭。");
        }

        ChannelExec channel = null;
        try {
            // 打开执行通道并设置命令
            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command);
            channel.setInputStream(null);
            channel.setErrStream(System.err);

            // 获取命令执行结果的输入流
            InputStream in = channel.getInputStream();
            channel.connect(); // 开始执行命令

            StringBuilder output = new StringBuilder();
            byte[] tmp = new byte[1024];

            // 循环读取命令的输出
            while (true) {
                while (in.available() > 0) {
                    int i = in.read(tmp, 0, 1024);
                    if (i < 0) {
                        break;
                    }
                    output.append(new String(tmp, 0, i));
                }
                if (channel.isClosed()) {
                    if (in.available() > 0) {
                        continue;
                    }
                    break;
                }
                // 等待一段时间再次检查输出
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); // 重新设置中断状态
                }
            }
            // 返回收集到的输出结果
            return output.toString();
        } finally {
            if (channel != null) {
                // 最后确保通道被关闭
                channel.disconnect();
            }
        }
    }

    /**
     * 批量执行一组命令。
     * @param commands 一组命令。
     * @param outputArea 用于输出命令执行结果的输出区域。
     */
    public void executeCommands(String[] commands, TextArea outputArea) throws JSchException, IOException {
        for (String command : commands) {
            OutPutTool.appendText(outputArea, "执行命令：" + command);
            executeCommand(command);
        }
    }

}
