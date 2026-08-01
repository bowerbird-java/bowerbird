// DebugController should be entirely excluded (DEBUG flag not active)
File debugClass = new File(basedir, "target/classes/com/example/DebugController.class")
assert !debugClass.exists() : "DebugController.class should NOT exist (whole-file exclusion)"

File debugSource = new File(basedir, "target/generated-sources/bowerbird/com/example/DebugController.java")
assert !debugSource.exists() : "DebugController.java should NOT exist in preprocessed output"

// MainApp should compile normally
File mainClass = new File(basedir, "target/classes/com/example/MainApp.class")
assert mainClass.exists() : "MainApp.class should exist"
