package com.web.util;

// 导入所需的类和注解
import org.lionsoul.ip2region.xdb.Searcher; // ip2region 库，用于 IP 地区信息查询
import org.springframework.core.io.ClassPathResource; // 用于从 classpath 加载资源文件
import org.springframework.util.FileCopyUtils; // 用于文件和流之间的复制
import jakarta.annotation.PostConstruct; // 用于在类实例化后执行初始化方法
import jakarta.servlet.http.HttpServletRequest; // 用于处理 HTTP 请求
import java.io.InputStream; // 输入流，用于读取文件内容
import java.util.regex.Matcher; // 正则匹配类
import java.util.regex.Pattern; // 正则模式类

public class IpUtil {

    // 定义本地 IP 地址常量
    private static final String LOCAL_IP = "127.0.0.1";
    // 定义一个 Searcher 对象，用于 IP 查询
    private static Searcher searcher;

    /**
     * 获取客户端的真实 IP 地址
     *
     * @param request HttpServletRequest 请求对象
     * @return 客户端 IP 地址
     */
    public static String getIpAddr(HttpServletRequest request) {
        if (request == null) {
            return "unknown"; // 如果请求对象为空，返回 "unknown"
        }
        // 从请求头中获取 x-forwarded-for（经过代理的客户端 IP）
        String ip = request.getHeader("x-forwarded-for");
        // 如果为空或未知，尝试从其他头部字段中获取 IP 地址
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Forwarded-For");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }

        // 如果所有头部字段都获取不到，则获取远程地址
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        // 如果是本地回环地址 "::1"，返回 127.0.0.1
        return "0:0:0:0:0:0:0:1".equals(ip) ? LOCAL_IP : ip;
    }

    /**
     * 判断给定的 IP 地址是否合法
     *
     * @param ipAddress 要检查的 IP 地址
     * @return 合法返回 true，否则返回 false
     */
    public static boolean checkIp(String ipAddress) {
        // 定义 IPv4 的正则表达式
        String ip = "([1-9]|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])(\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])){3}";
        Pattern pattern = Pattern.compile(ip); // 编译正则表达式
        Matcher matcher = pattern.matcher(ipAddress); // 匹配输入的 IP 地址
        return matcher.matches(); // 返回匹配结果
    }

    /**
     * 加载 ip2region 数据库，用于查询 IP 地址的地理位置信息
     */
    @PostConstruct // Spring 生命周期回调，在实例化后调用
    private static void initIp2Region() {
        try {
            // 从 classpath 加载 ip2region 数据库文件
            InputStream inputStream = new ClassPathResource("/ip2region.xdb").getInputStream();
            // 将文件内容读入字节数组
            byte[] bytes = FileCopyUtils.copyToByteArray(inputStream);
            // 用字节数组初始化 Searcher 对象
            searcher = Searcher.newWithBuffer(bytes);
        } catch (Exception exception) {
            // 捕获异常并打印堆栈信息
            exception.printStackTrace();
        }
    }

    /**
     * 根据 IP 地址获取地理位置信息
     *
     * @param ip IP 地址
     * @return 所属地理位置
     */
    public static String getIpRegion(String ip) {
        // 检查 IP 地址是否合法
        boolean isIp = checkIp(ip);
        if (isIp) {
            // 初始化 ip2region（确保 searcher 可用）
            initIp2Region();
            try {
                // 使用 searcher 查询 IP 地址信息
                String searchIpInfo = searcher.search(ip);
                // 按 "|" 分隔返回的查询结果
                String[] splitIpInfo = searchIpInfo.split("\\|");
                if (splitIpInfo.length > 0) {
                    // 如果是中国 IP，返回省份信息
                    if ("中国".equals(splitIpInfo[0])) {
                        return splitIpInfo[2];
                    } else if ("0".equals(splitIpInfo[0])) {
                        // 如果是内网 IP，返回 "内网"
                        if ("内网IP".equals(splitIpInfo[4])) {
                            return "内网";
                        } else {
                            return "未知"; // 否则返回 "未知"
                        }
                    } else {
                        if ("0".equals(splitIpInfo[0])) {
                            return "未知"; // 无法解析的情况返回 "未知"
                        }
                        return splitIpInfo[0]; // 返回国家信息
                    }
                }
            } catch (Exception e) {
                // 捕获查询过程中的异常并打印
                e.printStackTrace();
            }
            return "未知"; // 默认返回 "未知"
        } else {
            return ip; // 如果 IP 不合法，直接返回输入的 IP
        }
    }
}
