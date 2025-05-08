package com.demo.controller;

import com.demo.service.IScriptService;
import com.demo.util.LiquorScriptUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@Slf4j
public class DynamicClassController {
    private static String getScript() {
        String script = "package com.demo.dynamic.script;\n" +
                "\n" +
                "import java.util.Map;\n" +
                "import im.ccs.engine.custom_entity.qiuyd_t01.Test1;\n" +
                "import com.demo.service.IScriptService;\n" +
                "\n" +
                "public class DynamicDemoScript implements IScriptService {\n" +
                "    @Override\n" +
                "    public Map<String, Object> runScript(Map<String, Object> params) {\n" +
                "        System.out.println(\"Hello, Dynamic Script!1\");\n" +
                "        System.out.println(\"Hello, Dynamic Script!2\");\n" +
                "        Test1 test1=new Test1();\n" +
                "        test1.setName(\"sdfasdf\");\n" +
                "        params.put(\"test1\",test1);\n" +
                "        return params;\n" +
                "    }\n" +
                "}";
        return script;
    }

    @RequestMapping("/compileScript")
    public Map<String, Object> compileScript() {

        try {
            LiquorScriptUtil.compileScript(getScript());
            return Map.of("code", "ok");
        } catch (Exception ex) {
            return Map.of("code", "error" + ex);
        }
    }

    @RequestMapping("/runScript")
    public Map<String, Object> runScript() {
        String script = getScript();
        IScriptService iScriptService = LiquorScriptUtil.get(script);
        Map<String, Object> params = new HashMap<>();
        return (Map<String, Object>) iScriptService.runScript(params);
    }
}
