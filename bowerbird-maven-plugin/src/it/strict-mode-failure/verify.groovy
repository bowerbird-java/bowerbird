// the build should have failed — verify the log contains the expected diagnostic
File buildLog = new File(basedir, "build.log")
assert buildLog.exists() : "build log should exist"

String log = buildLog.text
assert log.contains("BWB-001") || log.contains("BWB-004") || log.contains("Bowerbird preprocessing failed") :
        "build log should contain a Bowerbird diagnostic code or failure message"
