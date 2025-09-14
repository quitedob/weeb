package com.web.ssh;

import lombok.Data;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.apache.sshd.server.channel.ChannelSession;
import org.apache.sshd.server.command.Command;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * InteractionConnect类，用于实现SSH服务器的交互逻辑。
 * 提供用户命令处理、实时聊天功能和终端控制。
 */
@Data
public class InteractionConnect implements Command, Runnable {
    private InputStream inputStream;
    private OutputStream outputStream;
    private OutputStream errorStream;
    private ExitCallback exitCallback;
    private Thread thread;
    private String username;
    private BufferedReader reader;
    private PrintWriter writer;
    private StringBuilder currentInput = new StringBuilder();
    private CommandManager commandManager = CommandManager.getInstance();
    private String privateChatUserName = null;

    @Override
    public void setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
        this.reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
    }

    @Override
    public void setOutputStream(OutputStream outputStream) {
        this.outputStream = outputStream;
        this.writer = new PrintWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8), true);
    }

    @Override
    public void setErrorStream(OutputStream errorStream) {
        this.errorStream = errorStream;
    }

    @Override
    public void setExitCallback(ExitCallback exitCallback) {
        this.exitCallback = exitCallback;
    }

    @Override
    public void start(ChannelSession channelSession, Environment environment) {
        environment.getEnv().put(Environment.ENV_TERM, "vt100");
        this.username = channelSession.getSession().getUsername();
        CustomCommand.ONLINE_USERS.put(username, this);

        writer.println(CustomCommand.ANSI_YELLOW + "欢迎来到聊天室！");
        writer.print(CustomCommand.ANSI_YELLOW + "当前在线用户: ");
        writer.println(CustomCommand.ANSI_BLUE + String.join(", ", CustomCommand.ONLINE_USERS.keySet()) + CustomCommand.ANSI_RESET);
        writer.println(CustomCommand.ANSI_YELLOW + "输入 'quit' 退出聊天室");

        commandManager.systemNotify(String.format("[系统] %s 加入了聊天室", username));
        writer.flush();

        thread = new Thread(this);
        thread.start();
    }

    @Override
    public void destroy(ChannelSession channelSession) {
        CustomCommand.ONLINE_USERS.remove(username);
        commandManager.systemNotify(String.format("[系统] %s 离开了聊天室", username));

        if (thread != null && !thread.isInterrupted()) {
            thread.interrupt();
        }
    }

    @Override
    public void run() {
        try {
            int read;
            int cursorPosition = 0;
            int displayPosition = 0;

            while ((read = this.reader.read()) != -1) {
                char ch = (char) read;

                if (ch == '\033') { // 处理ANSI转义序列
                    read = this.reader.read();
                    if (read == '[') {
                        read = this.reader.read();
                        switch (read) {
                            case 'C': // 右方向键
                                handleCursorRight(cursorPosition, displayPosition);
                                continue;
                            case 'D': // 左方向键
                                handleCursorLeft(cursorPosition, displayPosition);
                                continue;
                        }
                    }
                    continue;
                }

                if (ch == '\n' || ch == '\r') { // 处理回车键
                    handleEnterKey();
                    cursorPosition = 0;
                    displayPosition = 0;
                    continue;
                }

                if (ch == '\b' || ch == '\u007f') { // 处理退格键
                    cursorPosition = handleBackspace(cursorPosition, displayPosition);
                    continue;
                }

                if (Character.isISOControl(ch)) { // 忽略不可打印字符
                    continue;
                }

                cursorPosition = handleCharacterInsertion(ch, cursorPosition, displayPosition);
            }
        } catch (IOException e) {
            e.printStackTrace();
            exitCallback.onExit(1, e.getMessage());
        }
    }

    private void handleCursorRight(int cursorPosition, int displayPosition) {
        if (cursorPosition < currentInput.length()) {
            String nextChar = String.valueOf(currentInput.charAt(cursorPosition));
            int charWidth = getCharDisplayWidth(nextChar);
            writer.print("\033[" + charWidth + "C");
            cursorPosition++;
            displayPosition += charWidth;
            writer.flush();
        }
    }

    private void handleCursorLeft(int cursorPosition, int displayPosition) {
        if (cursorPosition > 0) {
            String prevChar = String.valueOf(currentInput.charAt(cursorPosition - 1));
            int charWidth = getCharDisplayWidth(prevChar);
            writer.print("\033[" + charWidth + "D");
            cursorPosition--;
            displayPosition -= charWidth;
            writer.flush();
        }
    }

    private void handleEnterKey() {
        String line = currentInput.toString();
        if ("quit".equalsIgnoreCase(line.trim())) {
            writer.println("\r再见！");
            exitCallback.onExit(0);
        } else if (!line.trim().isEmpty()) {
            writer.print("\r\033[K");
            commandManager.executeCommand(line, username);
        }
        currentInput.setLength(0);
        writer.flush();
    }

    private int handleBackspace(int cursorPosition, int displayPosition) {
        if (cursorPosition > 0) {
            String deletedChar = String.valueOf(currentInput.charAt(cursorPosition - 1));
            int charWidth = getCharDisplayWidth(deletedChar);

            currentInput.deleteCharAt(cursorPosition - 1);
            cursorPosition--;
            displayPosition -= charWidth;

            writer.print(repeatChar('\b', charWidth));
            writer.print("\033[K");
            String remaining = currentInput.substring(cursorPosition);
            writer.print(remaining);

            int remainingWidth = getStringDisplayWidth(remaining);
            if (remainingWidth > 0) {
                writer.print("\033[" + remainingWidth + "D");
            }
            writer.flush();
        }
        return cursorPosition;
    }

    private int handleCharacterInsertion(char ch, int cursorPosition, int displayPosition) {
        String newChar = String.valueOf(ch);
        int charWidth = getCharDisplayWidth(newChar);

        if (cursorPosition == currentInput.length()) {
            currentInput.append(ch);
            writer.print(newChar);
            cursorPosition++;
            displayPosition += charWidth;
        } else {
            currentInput.insert(cursorPosition, ch);
            String restContent = currentInput.substring(cursorPosition);
            writer.print(restContent);

            int restWidth = getStringDisplayWidth(restContent);
            cursorPosition++;
            displayPosition += charWidth;

            writer.print("\033[" + (restWidth - charWidth) + "D");
        }
        writer.flush();
        return cursorPosition;
    }

    private int getCharDisplayWidth(String ch) {
        if (ch.matches("[\\u4e00-\\u9fa5]")) {
            return 2;
        }
        return 1;
    }

    private int getStringDisplayWidth(String str) {
        int width = 0;
        for (int i = 0; i < str.length(); i++) {
            width += getCharDisplayWidth(String.valueOf(str.charAt(i)));
        }
        return width;
    }

    private static String repeatChar(char ch, int count) {
        return String.valueOf(ch).repeat(count);
    }
}
