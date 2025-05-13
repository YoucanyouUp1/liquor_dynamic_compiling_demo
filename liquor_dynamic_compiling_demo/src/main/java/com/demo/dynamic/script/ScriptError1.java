package com.demo.dynamic.script;

import com.demo.service.IScriptService;
import com.demo.util.ScriptUtil;

import java.util.Map;

public class ScriptError1 implements IScriptService {
    @Override
    public Map<String, Object> runScript(Map<String, Object> context) {
        context.put("tag1", ScriptUtil.runScript("readWriteTagDemo", context));
        return context;
    }
}
