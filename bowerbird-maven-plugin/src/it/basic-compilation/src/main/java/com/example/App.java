package com.example;

import io.github.bowerbird.java.api.IfDef;

public class App {

    @IfDef("DEBUG")
    public void debugOnly() {
        System.out.println("debug mode");
    }

    public void alwaysPresent() {
        System.out.println("always");
    }
}
