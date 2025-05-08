package com.demo.service;

import java.util.Map;

public interface IScriptService {
    Map<String, Object> runScript(Map<String, Object> context);
}
