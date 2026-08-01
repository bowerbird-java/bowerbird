package com.example;

import io.github.bowerbird.java.api.IfDef;
import io.github.bowerbird.java.api.ElseIfDef;
import io.github.bowerbird.java.api.ElseDef;

public class CacheFactory {

    @IfDef(value = "USE_REDIS", group = "cache")
    public String createCache() {
        return "redis";
    }

    @ElseIfDef(value = "USE_MEMCACHED", group = "cache")
    public String createCache() {
        return "memcached";
    }

    @ElseDef(group = "cache")
    public String createCache() {
        return "in-memory";
    }
}
