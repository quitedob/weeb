package com.web.annotation;

// 引入Java注解相关的包
import java.lang.annotation.*;

/**
 * 自定义注解 @CommandInfo，用于为类提供元信息，例如描述和名称。
 */

@Target(ElementType.TYPE) // 指定注解的作用目标为类或接口
@Retention(RetentionPolicy.RUNTIME) // 指定注解在运行时有效，可以通过反射获取
@Documented // 指定此注解会被 Javadoc 工具处理，生成的文档会包含注解信息
public @interface CommandInfo {

    /**
     * 用于指定命令的描述信息。
     * 这是一个必须指定的属性，没有默认值。
     */
    String description();

    /**
     * 用于指定命令的名称。
     * 这是一个必须指定的属性，没有默认值。
     */
    String name();
}
