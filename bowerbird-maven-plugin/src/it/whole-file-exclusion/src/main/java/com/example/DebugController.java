package com.example;

import io.github.bowerbird.java.api.IfDef;

@IfDef("DEBUG")
public class DebugController {

    public void dumpState() {
        System.out.println("state dump");
    }
}
