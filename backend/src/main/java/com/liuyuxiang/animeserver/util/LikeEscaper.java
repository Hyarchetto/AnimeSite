package com.liuyuxiang.animeserver.util;

/**
 * 把 LIKE 的通配符转义掉
 *
 * <p>不转的话，用户搜一个 `%` 就等于搜全部，搜 `_` 会匹配任意单字符，
 * 结果看着像「搜索坏了」
 *
 * <p>**这挡的不是 SQL 注入。** 参数一直走 `#{}` 传，注入不了。
 * 这里挡的是「用户输入里的通配符被当成语法」——是正确性问题，不是安全问题
 *
 * <p>搜番剧和管理列表都要用，所以单独放一个类而不是各写一份
 */
public final class LikeEscaper {

    private static final char ESCAPE_CHAR = '\\';

    private LikeEscaper() {
    }

    public static String escape(String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            return keyword;
        }
        // 反斜杠要先转，不然会把后面补上的转义符又转一遍
        return keyword
                .replace(String.valueOf(ESCAPE_CHAR), ESCAPE_CHAR + "" + ESCAPE_CHAR)
                .replace("%", ESCAPE_CHAR + "%")
                .replace("_", ESCAPE_CHAR + "_");
    }
}
