// build should succeed in lenient mode — file is copied unchanged
File classFile = new File(basedir, "target/classes/com/example/InvalidGroup.class")
assert classFile.exists() : "InvalidGroup.class should exist (lenient mode copies file unchanged)"

// verify the build log contains a warning diagnostic
File buildLog = new File(basedir, "build.log")
assert buildLog.exists() : "build log should exist"

String log = buildLog.text
assert log.contains("BWB-") : "build log should contain a Bowerbird diagnostic code"
