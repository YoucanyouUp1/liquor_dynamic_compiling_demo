package com.demo.dynamic.script;

import com.demo.service.IScriptService;
import com.demo.util.ScriptUtil;

import java.util.Map;

public class ScriptError2 implements IScriptService {
    @Override
    public Map<String, Object> runScript(Map<String, Object> context) {
        context.put("tag1",context);
        return context;
    }
}
