package com.web.campus;

import java.util.List;

public record CampusPage<T>(List<T> list, long total, int page, int size) {
    public CampusPage { list = List.copyOf(list); offset(page, size); }
    public static int offset(int page, int size) {
        if (page < 0 || size < 1 || size > 100 || ((long) page + 1) * size > 10_000) {
            throw new CampusException(400, "分页参数无效，单页最多100条，查询窗口最多10000条");
        }
        return page * size;
    }
    public static String query(String value) {
        String result = value == null ? "" : value.trim();
        if (result.length() > 100) throw new CampusException(400, "搜索内容最多100个字符");
        return result;
    }
}
