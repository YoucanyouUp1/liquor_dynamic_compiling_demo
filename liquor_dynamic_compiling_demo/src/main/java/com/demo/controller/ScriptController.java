package com.demo.controller;

import com.demo.service.IScriptService;
import com.demo.util.LiquorScriptUtil;
import lombok.extern.slf4j.Slf4j;
import org.noear.liquor.DynamicCompiler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

@RestController
@Slf4j
@RequestMapping("/script")
public class ScriptController {


    @RequestMapping("/runErrorScript")
    public Map<String, Object> test() throws Exception {
        // 获取请求报文参数
        String script = "package com.demo.dynamic.script;\n" +
                "\n" +
                "import com.demo.service.IScriptService;\n" +
                "import com.demo.util.ScriptUtil;\n" +
                "\n" +
                "import java.util.Map;\n" +
                "\n" +
                "public class ScriptError1 implements IScriptService {\n" +
                "    @Override\n" +
                "    public Map<String, Object> runScript(Map<String, Object> context) {\n" +
                "        context.put(\"tag1\", ScriptUtil.runScript(\"readWriteTagDemo\", context));\n" +
                "        return context;\n" +
                "    }\n" +
                "}";
        IScriptService iScriptService = LiquorScriptUtil.get(script);
        Map<String, Object> params = new HashMap<>();
        return (Map<String, Object>) iScriptService.runScript(params);
    }
}
