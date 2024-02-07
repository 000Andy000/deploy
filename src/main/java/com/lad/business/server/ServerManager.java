package com.lad.business.server;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.lad.business.StringTool;
import com.lad.business.ui.OutPutTool;
import javafx.scene.control.TextArea;

import java.io.IOException;
import java.util.List;

/**
 * @author Andy
 * @date 2024-2-7 007 12:41
 */
public class ServerManager {
    /**
     * 重启服务器
     *
     * @param values     五个字段的ComboBox
     * @param outputArea 输出区域，用于显示操作信息
     */
    public static void restartServer(List<String> values, TextArea outputArea) {
        // 获取输入的值
        String host = values.get(0);
        String username = values.get(1);
        String password = values.get(2);
        String localPath = values.get(3);
        String serverPath = values.get(4);

        String jarName = StringTool.getWindowsFileName(localPath);
        String serverJarPath = serverPath + jarName;

        OutPutTool.appendText(outputArea, "开始重启服务...");

        try {
            // 创建 SSH 会话
            Session session = SessionTool.createSession(username, password, host);

            // 创建远程命令执行器
            RemoteCommandExecutor remoteCommandExecutor = new RemoteCommandExecutor(session);
            // 创建要执行的命令（每条命令对应一个字符串，且都是独立的）
            String[] commands = new String[]{
                    // 杀死原有的进程
                    "pkill -f '"+ jarName + "'",
                    // 启动新的进程
                    "nohup java -jar " + serverJarPath + " > "+ serverPath +"nohup.out 2>&1 &"
            };
            // 批量执行命令
            remoteCommandExecutor.executeCommands(commands, outputArea);
            // 关闭SSH会话
            SessionTool.closeSession(session);
            OutPutTool.appendText(outputArea, "服务重启成功");
        } catch (JSchException | IOException e) {
            OutPutTool.appendText(outputArea, "程序出错: " + e.getMessage());
        }
    }
}
