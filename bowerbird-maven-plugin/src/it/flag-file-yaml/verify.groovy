File preprocessed = new File(basedir, "target/generated-sources/bowerbird/com/example/App.java")
assert preprocessed.exists() : "preprocessed source should exist"

String source = preprocessed.text
assert source.contains("featureX") : "FEATURE_X is in the YAML file, method should be retained"
assert source.contains("modernOnly") : "LEGACY is not active (negated in YAML), @IfNotDef should include"
assert source.contains("alwaysPresent") : "unconditional method should remain"
