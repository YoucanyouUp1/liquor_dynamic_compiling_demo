package com.demo.util;

import com.demo.service.IScriptService;
import org.noear.liquor.DynamicCompiler;

import java.util.HashMap;
import java.util.Map;

public class ScriptUtil {
    public static Object runScript(String tag, Map<String, Object> context) {
        String scriptByTag = "package com.demo.dynamic.script;\n" +
                "\n" +
                "import com.demo.service.IScriptService;\n" +
                "import com.demo.util.ScriptUtil;\n" +
                "\n" +
                "import java.util.Map;\n" +
                "\n" +
                "public class ScriptError2 implements IScriptService {\n" +
                "    @Override\n" +
                "    public Map<String, Object> runScript(Map<String, Object> context) {\n" +
                "        context.put(\"tag1\",context);\n" +
                "        return context;\n" +
                "    }\n" +
                "}";
        IScriptService iScriptService = LiquorScriptUtil.get(scriptByTag);
        Map<String, Object> params = new HashMap<>();
        return (Map<String, Object>) iScriptService.runScript(params);
    }
}
