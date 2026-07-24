package com.example.fixtures;

import io.github.bowerbird.java.api.IfDef;

public class StandaloneIfDef {

    @IfDef("DEBUG")
    public void debugMethod() {
        System.out.println("debug");
    }

    public void normalMethod() {
        System.out.println("always present");
    }
}
