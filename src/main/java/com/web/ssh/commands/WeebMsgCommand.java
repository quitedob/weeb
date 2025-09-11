package com.web.ssh.commands;

import com.web.annotation.CommandInfo;
import com.web.ssh.CommandManager;
import com.web.ssh.CustomCommand;
import com.web.ssh.InteractionConnect;

import java.util.Objects;

/**
 * 自定义命令类，用于设置聊天范围。
 * 支持两种模式：
 * - 私聊：weeb msg user [用户名]
 * - 群聊：weeb msg group
 */
@CommandInfo(
        name = "weeb msg",
        description = "设置聊天范围，私聊 - weeb msg user [用户名]，群聊 - weeb msg group"
)
public class WeebMsgCommand extends CustomCommand {

    /**
     * 执行命令的逻辑。
     *
     * @param content        用户输入的命令内容
     * @param username       执行命令的用户名
     * @param args           命令参数数组
     * @param commandManager 命令管理器，用于获取命令信息
     */
    @Override
    public void execute(String content, String username, String[] args, CommandManager commandManager) {
        // 获取当前用户连接
        InteractionConnect connect = ONLINE_USERS.get(username);
        if (connect == null) {
            System.err.println("用户连接不存在: " + username);
            return;
        }

        // 验证参数长度是否足够
        if (args.length < 3) {
            sendErrorAndEcho(connect, username, "该命令需要参数~");
            return;
        }

        // 获取模式参数
        String mode = args[2];
        switch (mode) {
            case "group": // 群聊模式
                connect.setPrivateChatUserName(null);
                break;

            case "user": // 私聊模式
                if (args.length < 4) { // 检查是否缺少目标用户名
                    sendErrorAndEcho(connect, username, "用户名称不能缺失~");
                    return;
                }

                String targetUsername = args[3]; // 获取目标用户名
                InteractionConnect targetConnect = ONLINE_USERS.get(targetUsername);

                if (targetConnect == null) { // 检查目标用户是否在线
                    sendErrorAndEcho(connect, username, "用户不在线~");
                    return;
                }

                connect.setPrivateChatUserName(targetUsername); // 设置私聊用户名
                break;

            default: // 无效模式
                sendErrorAndEcho(connect, username, "参数错误~");
        }

        // 输出当前状态
        echo(username, connect);
    }

    /**
     * 通用方法：发送错误信息并输出当前状态。
     *
     * @param connect  当前用户连接
     * @param username 用户名
     * @param errorMsg 错误信息
     */
    private void sendErrorAndEcho(InteractionConnect connect, String username, String errorMsg) {
        error(connect, errorMsg); // 发送错误信息
        echo(username, connect); // 输出当前状态
    }
}
