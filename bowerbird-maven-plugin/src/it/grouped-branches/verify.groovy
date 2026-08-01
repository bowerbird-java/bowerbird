// with USE_MEMCACHED active, only the memcached branch should survive
File preprocessed = new File(basedir, "target/generated-sources/bowerbird/com/example/CacheFactory.java")
assert preprocessed.exists() : "preprocessed source should exist"

String source = preprocessed.text
assert source.contains("memcached") : "memcached branch should be retained"
assert !source.contains("redis") : "redis branch should be removed"
assert !source.contains("in-memory") : "in-memory branch should be removed"
assert !source.contains("@IfDef") : "annotations should be stripped"
assert !source.contains("@ElseIfDef") : "annotations should be stripped"
assert !source.contains("@ElseDef") : "annotations should be stripped"

// should compile successfully
File classFile = new File(basedir, "target/classes/com/example/CacheFactory.class")
assert classFile.exists() : "CacheFactory.class should exist"
