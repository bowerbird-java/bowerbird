// plain project should compile normally
File classFile = new File(basedir, "target/classes/com/example/PlainApp.class")
assert classFile.exists() : "PlainApp.class should exist"

// preprocessed source should be a copy of the original (no modifications)
File preprocessed = new File(basedir, "target/generated-sources/bowerbird/com/example/PlainApp.java")
assert preprocessed.exists() : "preprocessed source should exist (passthrough copy)"

String source = preprocessed.text
assert source.contains("getName") : "all methods should be present"
assert source.contains("run") : "all methods should be present"
