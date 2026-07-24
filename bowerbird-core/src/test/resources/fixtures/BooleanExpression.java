package com.example.fixtures;

import io.github.bowerbird.java.api.IfDef;

public class BooleanExpression {

    @IfDef("FEATURE_A && !PRODUCTION")
    public void conditionalMethod() {
        System.out.println("only in non-prod with feature A");
    }

    public void alwaysPresent() {
    }
}
