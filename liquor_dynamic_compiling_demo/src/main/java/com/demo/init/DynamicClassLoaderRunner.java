package com.demo.init;


import com.demo.entity.CustomDataEntityBO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.beans.Introspector;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Order(1)
public class DynamicClassLoaderRunner implements ApplicationRunner {


    private final ApplicationContext applicationContext;
    private Path classDir;  // 保存 classDir
    private URLClassLoader dynamicClassLoader;

    public static String getFullClasspath() {
        return System.getProperty("java.class.path");
    }

    public URLClassLoader getDynamicClassLoader() {
        return dynamicClassLoader;
    }

    public Path getClassDir() {
        return classDir;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try {
            loadAndRegisterDynamicClasses();
        } catch (Exception e) {
            throw new RuntimeException("动态类加载失败，系统启动终止", e);
        }
    }

    private void loadAndRegisterDynamicClasses() throws Exception {
        List<CustomDataEntityBO> entities = selectByEnableEntity();

        Path tempDir = Files.createTempDirectory("dynamic-classes");
        this.classDir = tempDir.resolve("classes");  // 保存下来
        Path sourceDir = tempDir.resolve("sources");
        Path classDir = tempDir.resolve("classes");

        Files.createDirectories(sourceDir);
        Files.createDirectories(classDir);

        List<Path> sourceFiles = writeSourceFiles(entities, sourceDir);

        compileJavaSources(sourceDir, classDir, sourceFiles);

        URLClassLoader classLoader = new URLClassLoader(new URL[]{classDir.toUri().toURL()});
        Thread.currentThread().setContextClassLoader(classLoader);

        registerClassesAsSpringBeans(entities, classLoader, classDir);
        this.dynamicClassLoader = classLoader;
    }

    public List<CustomDataEntityBO> selectByEnableEntity() {
        List<CustomDataEntityBO> entities = new ArrayList<>();
        CustomDataEntityBO entity = new CustomDataEntityBO();
        entity.setClassFullyQualifiedName("im.ccs.engine.custom_entity.qiuyd_t01.Test1");
        entity.setEntityClassContent("package im.ccs.engine.custom_entity.qiuyd_t01;\n" +
                "import lombok.Data;\n" +
                "@Data\n" +
                "public class Test1 {\n" +
                "    private String name;\n" +
                "    private Integer age;\n" +
                "}\n");
        entities.add(entity);
        CustomDataEntityBO entity2 = new CustomDataEntityBO();
        entity2.setClassFullyQualifiedName("im.ccs.engine.custom_entity.qiuyd_t01.Test2");
        entity2.setEntityClassContent("package im.ccs.engine.custom_entity.qiuyd_t01;\n" +
                "import lombok.Data;\n" +
                "@Data\n" +
                "public class Test2 {\n" +
                "    private String name;\n" +
                "    private Integer age;\n" +
                "}\n");
        entities.add(entity2);
        return entities;
    }

    private List<Path> writeSourceFiles(List<CustomDataEntityBO> entities, Path sourceDir) throws Exception {
        List<Path> sourceFiles = new ArrayList<>();
        for (CustomDataEntityBO entity : entities) {
            String packageName = getPackageName(entity.getClassFullyQualifiedName());
            String className = getClassName(entity.getClassFullyQualifiedName());

            Path packagePath = sourceDir.resolve(packageName.replace(".", "/"));
            Files.createDirectories(packagePath);

            Path sourceFile = packagePath.resolve(className + ".java");
            Files.write(sourceFile, entity.getEntityClassContent().getBytes(StandardCharsets.UTF_8));
            sourceFiles.add(sourceFile);
        }
        return sourceFiles;
    }

    private void compileJavaSources(Path sourceDir, Path classDir, List<Path> sourceFiles) throws Exception {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        List<String> compileOptions = new ArrayList<>();

        compileOptions.add("-d");
        compileOptions.add(classDir.toString());

        compileOptions.add("-sourcepath");
        compileOptions.add(sourceDir.toString());

        compileOptions.add("-encoding");
        compileOptions.add("UTF-8");

        compileOptions.add("-cp");
        compileOptions.add(getFullClasspath()); // 给 -cp 指定值

        compileOptions.add("-processorpath");
        compileOptions.add(getFullClasspath()); // 给 -processorpath 指定值


        compileOptions.addAll(sourceFiles.stream().map(Path::toString).collect(Collectors.toList()));
// 打印编译命令，方便调试
        System.out.println("Compile command: javac " + String.join(" ", compileOptions));
        System.out.println("javac " + String.join(" ", compileOptions));
        int result = compiler.run(null, null, null, compileOptions.toArray(new String[0]));

        if (result != 0) {
            throw new RuntimeException("Compilation failed.");
        }
    }

    private void registerClassesAsSpringBeans(List<CustomDataEntityBO> entities, URLClassLoader classLoader, Path classDir) throws Exception {
        for (CustomDataEntityBO entity : entities) {
            Class<?> clazz = classLoader.loadClass(entity.getClassFullyQualifiedName());
            System.out.println("Loaded class: " + clazz.getName());
            registerAsSpringBean(clazz);
        }
    }

    private void registerAsSpringBean(Class<?> clazz) throws Exception {
        ConfigurableApplicationContext context = (ConfigurableApplicationContext) applicationContext;
        ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();

        String beanName = Introspector.decapitalize(clazz.getSimpleName());

        Constructor<?> constructor = clazz.getDeclaredConstructor();
        Object instance = constructor.newInstance();

        // 直接注册实例，不需要代理也能使用注解和依赖注入
        beanFactory.registerSingleton(beanName, instance);
    }

    private String getPackageName(String fqn) {
        int lastDotIndex = fqn.lastIndexOf(".");
        return (lastDotIndex > 0) ? fqn.substring(0, lastDotIndex) : "";
    }

    private String getClassName(String fqn) {
        int lastDotIndex = fqn.lastIndexOf(".");
        return (lastDotIndex > 0) ? fqn.substring(lastDotIndex + 1) : fqn;
    }

}
