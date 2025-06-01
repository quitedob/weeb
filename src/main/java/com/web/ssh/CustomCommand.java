package com.web.ssh;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 抽象类 `CustomCommand`，定义自定义命令的基础功能。
 * 提供命令执行、消息回显和错误提示等功能。
 */
public abstract class CustomCommand {

    // 存储所有在线用户的连接对象，线程安全
    public static final Map<String, InteractionConnect> ONLINE_USERS = new ConcurrentHashMap<>();

    // ANSI 控制符，用于终端输出颜色
    public static final String ANSI_RESET = "\033[0m"; // 重置颜色
    public static final String ANSI_RED = "\033[38;2;255;76;76m"; // 红色
    public static final String ANSI_GREEN = "\033[38;2;105;189;68m"; // 绿色
    public static final String ANSI_BLUE = "\033[38;2;76;155;255m"; // 蓝色
    public static final String ANSI_YELLOW = "\033[38;2;255;184;0m"; // 黄色
    public static final String ANSI_YELLOW2 = "\033[38;2;255;145;99m"; // 淡黄色
    public static final String ANSI_PINK = "\033[38;2;255;160;207m"; // 粉色

    /**
     * 抽象方法，定义命令的具体执行逻辑。
     * 每个命令类需要实现此方法。
     *
     * @param content        用户输入的命令内容
     * @param username       执行命令的用户名
     * @param args           命令参数
     * @param commandManager 命令管理器对象
     */
    public abstract void execute(String content, String username, String[] args, CommandManager commandManager);

    /**
     * 消息回显方法。
     * 用于在终端中显示当前用户状态（群聊或私聊）。
     *
     * @param username 执行命令的用户名
     * @param connect  用户的交互连接对象
     */
    public void echo(String username, InteractionConnect connect) {
        boolean isCurrentUser = username.equals(connect.getUsername()); // 判断是否为当前用户
        InteractionConnect currentConnect = ONLINE_USERS.get(username); // 获取当前用户的连接对象
        String title = "[群聊]"; // 默认状态为群聊

        if (connect != null) {
            // 检查是否处于私聊状态
            String privateChatUserName = currentConnect.getPrivateChatUserName();
            if (privateChatUserName != null && privateChatUserName.length() > 0) {
                title = String.format("[私聊:%s]", privateChatUserName); // 设置私聊标题
            }
        }

        // 根据是否为当前用户调整显示内容
        if (!isCurrentUser) {
            connect.getWriter().printf(ANSI_RESET + title + " > %s", connect.getCurrentInput().toString());
        } else {
            connect.getWriter().printf(ANSI_RESET + title + " > ");
        }
    }

    /**
     * 错误提示方法。
     * 在终端中显示错误消息。
     *
     * @param connect 用户的交互连接对象
     * @param message 错误消息内容
     */
    public void error(InteractionConnect connect, String message) {
        connect.getWriter().println(ANSI_RED + "[错误] " + message + ANSI_RESET); // 输出带颜色的错误消息
        connect.getWriter().flush(); // 刷新输出流
    }
}
