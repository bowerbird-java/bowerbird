package com.example.fixtures;

import io.github.bowerbird.java.api.IfDef;
import io.github.bowerbird.java.api.ElseDef;

public class MixedKinds {

    @IfDef(value = "A", group = "mixed")
    public void methodBranch() {
    }

    @ElseDef(group = "mixed")
    private String fieldBranch = "fallback";
}
