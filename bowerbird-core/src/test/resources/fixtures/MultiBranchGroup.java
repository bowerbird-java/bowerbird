package com.example.fixtures;

import io.github.bowerbird.java.api.IfDef;
import io.github.bowerbird.java.api.ElseIfDef;
import io.github.bowerbird.java.api.ElseDef;

public class MultiBranchGroup {

    @IfDef(value = "USE_REDIS", group = "cache")
    public String getCacheType() {
        return "redis";
    }

    @ElseIfDef(value = "USE_MEMCACHED", group = "cache")
    public String getCacheType() {
        return "memcached";
    }

    @ElseDef(group = "cache")
    public String getCacheType() {
        return "in-memory";
    }
}
