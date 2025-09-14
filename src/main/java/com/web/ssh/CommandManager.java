package com.web.ssh;

import com.web.annotation.CommandInfo;
import com.web.ssh.commands.MessageCommand;
import lombok.Data;
import org.reflections.Reflections;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 改进版命令管理器类。
 * 增强了鲁棒性、性能和扩展性，支持动态注册命令和日志记录。
 */
@Data
public class CommandManager {

    private static final Logger LOGGER = Logger.getLogger(CommandManager.class.getName());

    // 单例模式的实例
    private static CommandManager instance;

    // 使用线程安全的集合存储命令名称和对应的命令实例
    private Map<String, CustomCommand> commandMap = new ConcurrentHashMap<>();

    // 使用线程安全的集合存储命令名称和对应的描述信息
    private Map<String, String> detailsMap = new ConcurrentHashMap<>();

    /**
     * 构造方法，初始化命令管理器并加载命令。
     */
    public CommandManager() {
        commandMap.put("message", new MessageCommand()); // 添加默认命令
        loadCommands(); // 自动加载命令
    }

    /**
     * 自动加载标注了 @CommandInfo 注解的命令类。
     */
    public void loadCommands() {
        LOGGER.info("开始加载命令...");
        Reflections reflections = new Reflections("com.web.ssh.commands");
        Set<Class<?>> commandClasses = reflections.getTypesAnnotatedWith(CommandInfo.class);

        for (Class<?> clazz : commandClasses) {
            CommandInfo commandAnnotation = clazz.getAnnotation(CommandInfo.class);
            String name = commandAnnotation.name();
            String description = commandAnnotation.description();

            try {
                CustomCommand commandInstance = (CustomCommand) clazz.getDeclaredConstructor().newInstance();
                commandMap.put(name, commandInstance);
                detailsMap.put(name, description);
                LOGGER.info("加载命令成功: " + name);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "加载命令失败: " + name, e);
            }
        }
        LOGGER.info("命令加载完成");
    }

    /**
     * 根据用户输入执行命令。
     *
     * @param content  用户输入的命令内容
     * @param username 执行命令的用户名
     */
    public void executeCommand(String content, String username) {
        String[] args = content.split(" ");
        CustomCommand command = null;

        if (args.length >= 2) {
            command = commandMap.get(args[0] + " " + args[1]);
        }

        if (command == null) {
            LOGGER.warning("未找到命令: " + content + "，默认执行 message 命令");
            commandMap.get("message").execute(content, username, null, getInstance());
        } else {
            LOGGER.info("执行命令: " + content + " by " + username);
            command.execute(content, username, args, getInstance());
        }
    }

    /**
     * 向所有在线用户发送系统通知。
     *
     * @param content 系统通知内容
     */
    public void systemNotify(String content) {
        LOGGER.info("发送系统通知: " + content);
        MessageCommand message = (MessageCommand) commandMap.get("message");
        message.SystemNotify(content);
    }

    /**
     * 获取命令管理器的单例实例。
     *
     * @return 单例的 CommandManager 实例
     */
    public static CommandManager getInstance() {
        if (instance == null) {
            instance = new CommandManager();
        }
        return instance;
    }
}
