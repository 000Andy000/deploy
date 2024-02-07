package com.lad.business.server;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

/**
 * @author Andy
 * @date 2024-2-6 006 15:25
 */
public class SessionTool {
    /**
     * 创建SSH会话
     *
     * @param username 用户名
     * @param password 密码
     * @param host 主机地址
     * @return SSH会话对象
     * @throws JSchException 创建会话失败时抛出异常
     */
    public static Session createSession(String username, String password, String host) throws JSchException {
        JSch jsch = new JSch();
        Session session = jsch.getSession(username, host);
        session.setPassword(password);
        session.setConfig("StrictHostKeyChecking", "no");
        session.connect();
        return session;
    }

    /**
     * 关闭SSH会话
     */
    public static void closeSession(Session session) {
        if (session != null) {
            session.disconnect();
        }
    }
}
