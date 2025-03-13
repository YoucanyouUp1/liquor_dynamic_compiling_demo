package com.demo.controller;

import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.message.AsynchronouslyFormattable;
import org.noear.liquor.DynamicCompiler;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

@RestController
@Slf4j
@RequestMapping("/demo")
public class DemoController {
    @RequestMapping("/hello")
    public String hello() {
        return "Hello, World!";
    }

    private DynamicCompiler compiler;
    private String directoryPath = "D:\\my-workspace\\liquor_dynamic_compiling_demo\\liquor_dynamic_compiling_demo\\src\\main\\java\\com\\demo\\dynamic\\script\\";
    public DemoController() {
        this.compiler = new DynamicCompiler();
        loadAndCompileFiles();
        startFileWatcher();
    }

    private void loadAndCompileFiles() {
        try {
            // 清除之前的类定义
            compiler.reset();
            try (Stream<Path> paths = Files.walk(Paths.get(directoryPath))) {
                paths.filter(Files::isRegularFile)
                        .filter(path -> path.toString().endsWith(".java"))
                        .forEach(path -> {
                            try {
                                String classCode = new String(Files.readAllBytes(path));
                                String className = path.getFileName().toString().replace(".java", "");
                                compiler.addSource(className, classCode);
                            } catch (IOException e) {
                                log.error("Failed to read file: " + path, e);
                            }
                        });
                compiler.build();
            }
        } catch (Exception e) {
            log.error("Failed to load and compile files", e);
        }
    }

    private void startFileWatcher() {
        try {
            Thread watchThread = new Thread(() -> {
                while (true) {
                    try {
                        WatchService watchService = FileSystems.getDefault().newWatchService();
                        Path path = Paths.get(directoryPath);
                        path.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
                        WatchKey key = watchService.take();
                        log.info("File watcher is active and waiting for events...");
                        for (WatchEvent<?> event : key.pollEvents()) {
                            Thread.sleep(10);
                            if (event.kind() == StandardWatchEventKinds.ENTRY_MODIFY) {
                                log.info("File modified: " + event.context());
                                loadAndCompileFiles();
                            }
                        }

                    } catch (Exception e) {
                        log.error("File watcher interrupted", e);
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            });
            watchThread.setDaemon(true);
            watchThread.start();
            log.info("File watcher started successfully.");
        } catch (Exception e) {
            log.error("Failed to start file watcher", e);
        }
    }

    @RequestMapping("/testJavaScript")
    public String test() throws Exception {
        // 获取请求报文参数
        Map<String,Object> params=new HashMap<>();
        Class<?> clazz = compiler.getClassLoader().loadClass("com.demo.dynamic.script.DynamicDemoScript");
        Object instance = clazz.getDeclaredConstructor().newInstance();
        return clazz.getMethod("hello", Map.class).invoke(instance, params).toString();
    }
}
