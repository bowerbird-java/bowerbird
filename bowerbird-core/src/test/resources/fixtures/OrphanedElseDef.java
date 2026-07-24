package com.example.fixtures;

import io.github.bowerbird.java.api.ElseDef;

public class OrphanedElseDef {

    @ElseDef(group = "nonexistent")
    public void orphanedMethod() {
    }
}
