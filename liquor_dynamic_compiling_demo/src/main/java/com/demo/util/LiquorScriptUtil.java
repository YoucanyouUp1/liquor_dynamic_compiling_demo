package com.demo.util;


import cn.hutool.extra.spring.SpringUtil;
import com.demo.init.DynamicClassLoaderRunner;
import com.demo.service.IScriptService;
import lombok.extern.slf4j.Slf4j;
import org.noear.liquor.DynamicCompiler;
import org.noear.liquor.eval.CodeSpec;
import org.noear.liquor.eval.Scripts;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author noear 2025/4/19 created
 */
@Slf4j
public final class LiquorScriptUtil {
    public static IScriptService get(String code) {
        String script = obtainLiquorScript(code);
        DynamicClassLoaderRunner dynamicClassLoaderRunner = SpringUtil.getBean(DynamicClassLoaderRunner.class);
        Thread.currentThread().setContextClassLoader(dynamicClassLoaderRunner.getDynamicClassLoader());
        // 内部已有缓存（容时为 10000）//也可以在外部构建缓存
        return (IScriptService) Scripts.eval(new CodeSpec(script).returnType(IScriptService.class));
    }

    private static String obtainLiquorScript(String code) {
        // 去掉包名
        if (code.startsWith("package ")) {
            int lineEnd = code.indexOf("\n");
            code = code.substring(lineEnd + 1);
        }

        // 把 public class 替换为 class
        int classStartIdx = code.indexOf("public class ") + 13;
        int classEndIdx = code.indexOf(" ", classStartIdx);

        // 构建脚本
        String className = code.substring(classStartIdx, classEndIdx);
        String script = code.replace("public class ", "class ");

        script = script + "\n\n return new " + className + "();";
        return script;
    }

    public static DynamicCompiler compileScript(String script) {
        // 校验是否实现了 IScriptService 接口
        if (!isImplementInterface(script)) {
            throw new RuntimeException("脚本必须实现 IScriptService 接口");
        }
        String newClassName = "NewNameForCompile_" + System.currentTimeMillis();
        DynamicCompiler compiler = new DynamicCompiler();
        compiler.setClassLoader(compiler.newClassLoader());
        // 设置 classpath
        DynamicClassLoaderRunner dynamicClassLoaderRunner = SpringUtil.getBean(DynamicClassLoaderRunner.class);
        try {
            setupCompilerWithClasspath(compiler, dynamicClassLoaderRunner.getClassDir());
        } catch (IOException e) {
            throw new RuntimeException("设置 classpath 失败", e);
        }
        String newScript = replaceClassName(newClassName, script);
        log.debug("newScript:" + newScript);
        compiler.addSource(newClassName, newScript);
        compiler.build();
        return compiler;
    }

    private static void setupCompilerWithClasspath(DynamicCompiler compiler, Path classDir) throws IOException {
        // 添加动态类输出目录
        compiler.addClassPath(classDir.toFile());

        // 添加系统 classpath（IDE 启动时的所有依赖 jar）
        String runtimeClasspath = System.getProperty("java.class.path");
        for (String pathStr : runtimeClasspath.split(File.pathSeparator)) {
            File path = new File(pathStr);
            if (path.exists()) {
                compiler.addClassPath(path);
            }
        }
    }

    private static boolean isImplementInterface(String script) {
        String regex = "implements\\s+IScriptService";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(script);
        return matcher.find();
    }

    private static String replaceClassName(String newClassName, String classCode) {
        Pattern pattern = Pattern.compile("(public\\s+class|class)\\s+([A-Za-z_][A-Za-z0-9_]*)");
        Matcher matcher = pattern.matcher(classCode);
        if (matcher.find()) {
            String replacement = matcher.group(1) + " " + newClassName;
            return matcher.replaceFirst(replacement);
        } else {
            return classCode;
        }
    }

    public static void main(String[] args) {
        DynamicCompiler compiler = new DynamicCompiler();

        String className = "HelloWorld1";
        String classCode = "import java.util.HashMap;\n\n" +
                "public class HelloWorld{ " +
                "   public static void helloWorld() { " +
                "       System.out.println(\"Hello, world!\"); " +
                "   } " +
                "}";
        classCode = replaceClassName(className, classCode);
        // Add source code (more than one) and build
        compiler.addSource(className, classCode).build();
        System.out.println("没有报错");
    }

}
