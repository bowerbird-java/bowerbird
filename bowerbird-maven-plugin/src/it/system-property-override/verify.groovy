File preprocessed = new File(basedir, "target/generated-sources/bowerbird/com/example/App.java")
assert preprocessed.exists() : "preprocessed source should exist"

String source = preprocessed.text
// BASELINE comes from plugin config, OVERRIDE comes from system property — both should be active
assert source.contains("baselineMethod") : "BASELINE flag from plugin config should be active"
assert source.contains("overrideMethod") : "OVERRIDE flag from system property should be active"
assert source.contains("alwaysPresent") : "unconditional method should remain"
