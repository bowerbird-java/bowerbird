File preprocessed = new File(basedir, "target/generated-sources/bowerbird/com/example/App.java")
assert preprocessed.exists() : "preprocessed source should exist"

String source = preprocessed.text
assert source.contains("reportMetrics") : "METRICS is in the flag file, method should be retained"
assert !source.contains("neverCompiled") : "DISABLED_FLAG is not in the flag file, method should be removed"
assert source.contains("alwaysPresent") : "unconditional method should remain"
