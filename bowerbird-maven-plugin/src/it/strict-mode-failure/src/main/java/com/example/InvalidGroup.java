package com.example;

import io.github.bowerbird.java.api.ElseDef;

public class InvalidGroup {

    @ElseDef(group = "nonexistent")
    public void orphanedMethod() {
    }
}
