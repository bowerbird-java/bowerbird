package com.example.fixtures;

import io.github.bowerbird.java.api.IfDef;
import io.github.bowerbird.java.api.ElseDef;

public class GroupedIfElse {

    @IfDef(value = "DEBUG", group = "logging")
    public String getLogLevel() {
        return "TRACE";
    }

    @ElseDef(group = "logging")
    public String getLogLevel() {
        return "WARN";
    }

    public void unrelated() {
    }
}
