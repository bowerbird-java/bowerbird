// verify that compilation succeeded and the class file exists
File classFile = new File(basedir, "target/classes/com/example/App.class")
assert classFile.exists() : "App.class should exist"

// verify the preprocessed source retains the debugOnly method (DEBUG flag is active)
File preprocessedSource = new File(basedir, "target/generated-sources/bowerbird/com/example/App.java")
assert preprocessedSource.exists() : "preprocessed source should exist"

String source = preprocessedSource.text
assert source.contains("debugOnly") : "debugOnly method should be present (DEBUG is active)"
assert source.contains("alwaysPresent") : "alwaysPresent method should always be present"
assert !source.contains("@IfDef") : "Bowerbird annotations should be stripped"
assert !source.contains("import io.github.bowerbird") : "Bowerbird imports should be removed"
