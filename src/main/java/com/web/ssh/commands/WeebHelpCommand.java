package com.web.ssh.commands;

import com.web.annotation.CommandInfo; // 自定义注解，用于标记命令名称及描述
import com.web.ssh.CommandManager; // 命令管理器类，用于管理所有命令
import com.web.ssh.CustomCommand; // 自定义命令基类，所有命令继承此类
import com.web.ssh.InteractionConnect; // 用户交互连接类，用于处理用户的输入和输出

/**
 * 自定义命令类，用于显示可用命令的列表及其用法。
 * 通过 @CommandInfo 注解标记命令名称和描述。
 */
@CommandInfo(name = "Weeb help", description = "获取weeb命令列表及其用法")
public class WeebHelpCommand extends CustomCommand {

    /**
     * 执行命令的逻辑。
     *
     * @param content        用户输入的命令内容
     * @param username       执行命令的用户名
     * @param args           命令参数数组
     * @param commandManager 命令管理器，用于获取所有命令的信息
     */
    @Override
    public void execute(String content, String username, String[] args, CommandManager commandManager) {
        // 获取当前用户的交互连接
        InteractionConnect connect = ONLINE_USERS.get(username);

        // 向用户输出命令列表标题
        connect.getWriter().println(ANSI_YELLOW + "[weeb命令列表]");

        // 遍历命令管理器中的所有命令，逐个输出名称和描述
        commandManager.getDetailsMap().forEach((name, info) -> {
            // 输出命令名称
            connect.getWriter().print(ANSI_YELLOW + name + " - ");
            // 输出命令描述
            connect.getWriter().println(ANSI_YELLOW2 + info);
        });

        // 重置输出颜色
        connect.getWriter().print(ANSI_RESET);

        // 调用基类的 echo 方法，处理额外的输出逻辑
        echo(username, connect);
    }
}
