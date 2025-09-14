package com.web.util;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 改进版 UrlPermitUtil，支持更灵活的 URL 权限管理。
 */
@Component
public class UrlPermitUtil {

    // 使用线程安全的集合存储免验证 URL 列表
    private final List<String> urls = new CopyOnWriteArrayList<>();

    // 使用线程安全的集合存储需要验证角色的 URL 资源
    private final ConcurrentHashMap<String, List<String>> roleUrl = new ConcurrentHashMap<>();

    // 初始化块，设置默认免验证 URL
    {
        urls.add("/ws/**");
    }

    /**
     * 验证 URL 是否匹配指定的 URL 列表，支持通配符匹配。
     *
     * @param permitUrl 被验证的 URL
     * @param urlArr URL 列表
     * @return true 如果匹配成功，否则 false
     */
    public boolean verifyUrl(String permitUrl, List<String> urlArr) {
        for (String url : urlArr) {
            // 使用正则表达式进行通配符匹配
            String regex = url.replace("**", ".*").replace("*", "[^/]*");
            if (permitUrl.matches(regex)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查 URL 是否在免验证的 URL 列表中。
     *
     * @param url 被验证的 URL
     * @return true 如果是免验证 URL，否则 false
     */
    public boolean isPermitUrl(String url) {
        return verifyUrl(url, urls);
    }

    /**
     * 获取所有免验证的 URL 列表。
     *
     * @return 免验证的 URL 列表
     */
    public List<String> getPermitAllUrl() {
        return new ArrayList<>(urls); // 返回副本，保证线程安全
    }

    /**
     * 添加多个免验证 URL 到列表中。
     *
     * @param urls 新的免验证 URL 列表
     */
    public void addUrls(List<String> urls) {
        this.urls.addAll(urls);
    }

    /**
     * 为指定角色添加需要验证的 URL 资源。
     *
     * @param role 角色名称
     * @param url URL 路径
     */
    public void addRoleUrl(String role, String url) {
        roleUrl.computeIfAbsent(url, k -> new CopyOnWriteArrayList<>()).add(role);
    }

    /**
     * 检查角色是否有访问指定 URL 的权限。
     *
     * @param role 角色名称
     * @param url URL 路径
     * @return true 如果角色有权限访问，否则 false
     */
    public boolean isRoleUrl(String role, String url) {
        List<String> roles = roleUrl.get(url);
        return roles == null || roles.contains(role);
    }
}
