package com.lad;

/**
 * @author Andy
 * @date 2024-2-6 006 17:19
 */
public class StringTool {
    /**
     * 移除末尾的斜杠（如果有的话）后，获取到最后一级目录的目录名
     * @param path 路径
     * @return 目录名
     */
    public static String getLinuxDirName(String path) {
        path = path.replaceAll("/+$", "");
        return path.substring(path.lastIndexOf("/") + 1);
    }

    /**
     * 移除末尾的反斜杠（如果有的话）后，获取到最后一级目录的目录名
     * @param path 路径
     * @return 目录名
     */
    public static String getWindowsDirName(String path) {
        path = path.replaceAll("\\\\+$", "");
        return path.substring(path.lastIndexOf("\\") + 1);
    }

    /**
     * 移除末尾的反斜杠（如果有的话）后，获取到最后一级目录的目录名
     * @param path 路径
     * @return 目录名
     */
    public static String getWindowsFileName(String path) {
        return path.substring(path.lastIndexOf("\\") + 1);
    }




}
