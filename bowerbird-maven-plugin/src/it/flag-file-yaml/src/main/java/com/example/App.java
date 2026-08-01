package com.example;

import io.github.bowerbird.java.api.IfDef;
import io.github.bowerbird.java.api.IfNotDef;

public class App {

    @IfDef("FEATURE_X")
    public void featureX() {
        System.out.println("feature x");
    }

    @IfNotDef("LEGACY")
    public void modernOnly() {
        System.out.println("modern path");
    }

    public void alwaysPresent() {
    }
}
