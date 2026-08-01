package com.example;

import io.github.bowerbird.java.api.IfDef;

public class App {

    @IfDef("BASELINE")
    public void baselineMethod() {
        System.out.println("baseline");
    }

    @IfDef("OVERRIDE")
    public void overrideMethod() {
        System.out.println("override");
    }

    public void alwaysPresent() {
    }
}
