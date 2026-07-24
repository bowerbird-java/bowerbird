package com.example.fixtures;

import io.github.bowerbird.java.api.IfDef;
import io.github.bowerbird.java.api.ElseDef;

public class DuplicateElseDef {

    @IfDef(value = "A", group = "grp")
    public void method1() {
    }

    @ElseDef(group = "grp")
    public void method2() {
    }

    @ElseDef(group = "grp")
    public void method3() {
    }
}
