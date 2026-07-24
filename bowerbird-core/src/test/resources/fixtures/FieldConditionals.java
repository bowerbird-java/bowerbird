package com.example.fixtures;

import io.github.bowerbird.java.api.IfDef;
import io.github.bowerbird.java.api.IfNotDef;
import io.github.bowerbird.java.api.ElseDef;

public class FieldConditionals {

    @IfDef(value = "METRICS", group = "registry")
    private String registry = "real";

    @ElseDef(group = "registry")
    private String registry = "noop";

    @IfNotDef("LEGACY")
    private String mode = "modern";

    private String alwaysPresent = "yes";
}
