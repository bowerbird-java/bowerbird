package com.example;

import io.github.bowerbird.java.api.IfDef;

public class App {

    @IfDef("METRICS")
    public void reportMetrics() {
        System.out.println("metrics enabled");
    }

    @IfDef("DISABLED_FLAG")
    public void neverCompiled() {
        System.out.println("should not appear");
    }

    public void alwaysPresent() {
    }
}
