package com.web.ssh.commands;

import com.web.ssh.CommandManager;
import com.web.ssh.CustomCommand;
import com.web.ssh.InteractionConnect;

import java.util.Objects;

/**
 * 改进版消息命令类。
 * 支持私聊、公共广播和系统通知功能，增强了鲁棒性和扩展性。
 */
public class MessageCommand extends CustomCommand {

    /**
     * 广播消息方法。
     * 根据用户状态选择发送模式（私聊或公共广播）。
     *
     * @param message  消息内容
     * @param username 发送消息的用户名
     */
    public void broadcast(String message, String username) {
        InteractionConnect senderConnect = ONLINE_USERS.get(username);

        // 检查发送方是否在线
        if (senderConnect == null) {
            System.err.println("用户连接不存在: " + username);
            return;
        }

        // 检查用户是否处于私聊模式
        String targetUsername = senderConnect.getPrivateChatUserName();
        if (targetUsername != null) {
            // 私聊逻辑
            InteractionConnect targetConnect = ONLINE_USERS.get(targetUsername);
            if (targetConnect == null) {
                sendErrorMessage(senderConnect, "目标用户不在线");
                return;
            }

            // 构建私聊内容
            String content = String.format("[%s -> %s]: %s", username, targetUsername, message);
            sendMsg(content, username, senderConnect, ANSI_PINK); // 回显发送方
            sendMsg(content, targetUsername, targetConnect, ANSI_PINK); // 发送给目标用户
        } else {
            // 公共广播逻辑
            ONLINE_USERS.values().forEach(connect -> {
                boolean isCurrentUser = Objects.equals(username, connect.getUsername());
                String content = String.format("%s: %s", username, message);
                String color = isCurrentUser ? ANSI_GREEN : ANSI_RESET;
                sendMsg(content, connect.getUsername(), connect, color);
            });
        }
    }

    /**
     * 发送消息的方法。
     * 将消息发送到指定用户终端，并进行格式化和颜色设置。
     *
     * @param message  消息内容
     * @param username 接收消息的用户名
     * @param connect  用户的交互连接对象
     * @param color    消息颜色
     */
    private void sendMsg(String message, String username, InteractionConnect connect, String color) {
        connect.getWriter().print("\r\033[K"); // 清除当前行内容
        connect.getWriter().println(color + message); // 输出消息
        echo(username, connect); // 回显用户名
    }

    /**
     * 发送错误消息。
     *
     * @param connect 用户交互连接对象
     * @param error   错误消息内容
     */
    private void sendErrorMessage(InteractionConnect connect, String error) {
        sendMsg("错误: " + error, connect.getUsername(), connect, ANSI_RED);
    }

    /**
     * 系统通知方法。
     * 用于向所有在线用户发送系统通知消息。
     *
     * @param message 系统消息内容
     */
    public void SystemNotify(String message) {
        ONLINE_USERS.values().forEach(connect -> {
            sendMsg("[系统通知] " + message, connect.getUsername(), connect, ANSI_BLUE);
        });
    }

    /**
     * 重写的命令执行方法。
     * 每次执行命令时，都会调用此方法。
     *
     * @param message       用户输入的消息内容
     * @param username      执行命令的用户名
     * @param args          命令参数
     * @param commandManager 命令管理器对象
     */
    @Override
    public void execute(String message, String username, String[] args, CommandManager commandManager) {
        broadcast(message, username); // 调用广播方法
    }
}
