package com.web.Config;

import com.github.houbb.sensitive.word.bs.SensitiveWordBs;
import com.github.houbb.sensitive.word.support.ignore.SensitiveWordCharIgnores;
import com.github.houbb.sensitive.word.support.resultcondition.WordResultConditions;
import com.github.houbb.sensitive.word.support.tag.WordTags;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 敏感词配置类 (SensitiveWordConfig)。
 * 使用 houbb-sensitive-word 库进行敏感词过滤配置。
 * 提供全局配置的敏感词过滤器，支持多种过滤规则和选项。
 */
@Configuration // 标注为Spring配置类
public class SensitiveWordConfig {

    /**
     * 定义敏感词过滤器 (SensitiveWordBs) Bean。
     * 配置过滤规则，包括忽略大小写、字符风格、重复字符等选项。
     *
     * @return 配置好的 SensitiveWordBs 对象
     */
    @Bean
    public SensitiveWordBs sensitiveWordBs() {
        // 配置敏感词过滤器
        SensitiveWordBs wordBs = SensitiveWordBs.newInstance()
                .ignoreCase(true) // 忽略大小写，例如 "Test" 和 "test" 将被视为相同
                .ignoreWidth(true) // 忽略全角和半角的区别，例如 "ｔｅｓｔ" 和 "test" 将被视为相同
                .ignoreNumStyle(true) // 忽略数字格式差异，例如 "１23" 和 "123" 将被视为相同
                .ignoreChineseStyle(true) // 忽略中文样式差异，例如 "测试" 和 "測試" 将被视为相同
                .ignoreEnglishStyle(true) // 忽略英文样式差异，例如 "ｔｅｓｔ" 和 "test" 将被视为相同
                .ignoreRepeat(false) // 是否忽略重复字符，例如 "tessst" 是否视为 "test"
                .enableNumCheck(false) // 禁用数字校验
                .enableEmailCheck(false) // 禁用邮箱校验
                .enableUrlCheck(false) // 禁用URL校验
                .enableIpv4Check(false) // 禁用IPv4校验
                .enableWordCheck(true) // 启用敏感词校验
                .numCheckLen(8) // 数字校验的最小长度，设置为8（仅在启用时生效）
                .wordTag(WordTags.none()) // 设置敏感词标签，默认为无标签
                .charIgnore(SensitiveWordCharIgnores.defaults()) // 设置默认的字符忽略规则
                .wordResultCondition(WordResultConditions.alwaysTrue()) // 设置结果条件，始终返回匹配的敏感词
                .init(); // 初始化过滤器配置

        return wordBs; // 返回配置好的过滤器
    }
}
