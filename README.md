# Bowerbird

> **Source-level conditional compilation for Java, inspired by the C preprocessor.**
>
> *Just as the Satin Bowerbird curates and selects objects to build its bower,
> Bowerbird curates and selects which code elements survive into the final build.*

---

## Table of Contents

- [The Problem](#the-problem)
- [How C++ Does It: Preprocessor Directives](#how-c-does-it-preprocessor-directives)
- [How Java Annotations Work — And Why They Are Different](#how-java-annotations-work--and-why-they-are-different)
- [Bowerbird's Approach: Bridging the Gap](#bowerbirds-approach-bridging-the-gap)
- [Why the `group` Property Exists](#why-the-group-property-exists)
- [Annotation Reference](#annotation-reference)
- [Quick Start](#quick-start)
- [Configuration](#configuration)
- [Multi-Module Projects](#multi-module-projects)
- [IDE Support](#ide-support)
- [Error Handling](#error-handling)
- [Modules](#modules)
- [Building from Source](#building-from-source)
- [License](#license)

---

## The Problem

Conditional compilation — the ability to include or exclude code based on build-time
feature flags — is a fundamental capability in C and C++. It allows a single codebase
to target multiple platforms, enable debug instrumentation, toggle experimental features,
and strip diagnostics from production builds. The key property is that **excluded code
never reaches the compiler**: it is physically absent from the compilation unit.

Java has no equivalent mechanism. The language deliberately omitted a preprocessor, and
the alternatives available today — `if (false)` dead-code patterns, annotation processors
that can only generate new files, or Lombok-style internal compiler API hacking — all
fall short of true source-level exclusion.

Bowerbird fills this gap. It is a **Maven plugin** that reads Java source files annotated
with conditional compilation annotations, evaluates conditions against a set of feature
flags, and produces a **modified source tree** with excluded elements physically removed
**before `javac` ever sees them**.

---

## How C++ Does It: Preprocessor Directives

The C/C++ preprocessor operates on a **textual, line-oriented** model. Directives like
`#ifdef`, `#elif`, `#else`, and `#endif` form **lexically-scoped blocks** that the
preprocessor includes or strips before the compiler parses anything.

### Scoping by Directive Delimiters

The critical property of the C++ model is that `#ifdef` and `#endif` act as **block
delimiters**. Everything between them is a single conditional region, regardless of
what the content is — it can span type definitions, function bodies, individual
statements, variable declarations, or even fragments of expressions:

```cpp
#ifdef ENABLE_METRICS
// this entire block is included or excluded as a unit
class MetricsCollector {
public:
    void record(const std::string& name, double value);
private:
    std::unordered_map<std::string, double> counters_;
};
#endif
```

### Branching with `#elif` and `#else`

The preprocessor supports multi-way branching within a single `#ifdef`/`#endif` region.
The pairing is **implicit and positional** — the preprocessor knows which `#else` belongs
to which `#ifdef` because they are lexically nested:

```cpp
#ifdef USE_OPENSSL
    #include <openssl/ssl.h>
    using TlsContext = SSL_CTX;
#elif defined(USE_BORINGSSL)
    #include <openssl/ssl.h>  // BoringSSL-compatible header
    using TlsContext = bssl::UniquePtr<SSL_CTX>;
#else
    // fallback: no TLS support
    struct TlsContext { /* no-op stub */ };
#endif
```

Several properties make this model powerful:

- **Arbitrary granularity** — directives can wrap entire files, single functions, individual
  statements, or even partial expressions. There is no requirement that the conditional
  region aligns with any language construct.
- **Implicit pairing** — `#else` and `#elif` are automatically associated with the
  nearest preceding `#ifdef`/`#if`. No explicit labeling is needed.
- **Nesting** — `#ifdef` blocks can nest arbitrarily. The preprocessor tracks a stack
  of open conditional regions.
- **True exclusion** — stripped code is never tokenized, parsed, or type-checked. It can
  contain syntax errors, references to undefined types, or platform-specific constructs
  that would not compile in the current environment.

### The `#endif` Closing Marker

Every conditional region must be explicitly closed with `#endif`. This closing marker is
what allows the preprocessor to know precisely where the conditional block ends:

```cpp
#ifdef DEBUG
    void debugDump() { /* ... */ }
#endif  // ← the preprocessor knows the block ends here
```

Without `#endif`, the preprocessor could not determine the extent of the conditional
region.

---

## How Java Annotations Work — And Why They Are Different

Java annotations are **metadata attached to declarations**. They are defined by the
language specification (JLS §9.6, §9.7) and have three fundamental properties that
distinguish them from C++ preprocessor directives.

### Annotations Attach to Declarations, Not Blocks

A Java annotation is placed on a specific declaration — a class, method, field,
parameter, local variable, or package. It **cannot** wrap an arbitrary block of code:

```java
// valid: annotation on a method declaration
@Deprecated
public void oldMethod() { /* ... */ }

// not possible: there is no way to annotate "the next 15 lines"
// or "everything between here and some end marker"
```

This is a fundamental structural difference from `#ifdef`/`#endif`, which delimit
a **region of text** rather than decorating a **single declaration**.

### Target Constraints

Every annotation type declares which element kinds it can annotate via `@Target`:

```java
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})
public @interface MyAnnotation { }
```

This annotation can appear on types (classes, interfaces, enums, records), methods, and
fields. It **cannot** appear on an arbitrary statement, an `if` block, or a code region.
The Java language provides `ElementType` values for types, methods, fields, parameters,
constructors, local variables, annotation types, packages, type parameters, type uses,
modules, and record components — but not for "regions" or "blocks."

### Retention Policies

Annotations have a lifecycle controlled by `@Retention`:

| Retention Policy | Availability | Use Case |
|-----------------|--------------|----------|
| `RetentionPolicy.SOURCE` | Discarded after compilation; not present in bytecode | Compile-time processing, code generation |
| `RetentionPolicy.CLASS` | Present in bytecode but not available at runtime via reflection | Bytecode analysis tools |
| `RetentionPolicy.RUNTIME` | Available at runtime via reflection | Frameworks like Spring, JPA, JUnit |

Bowerbird annotations use `RetentionPolicy.SOURCE` because they are consumed by the
preprocessor **before compilation** and have no purpose in the compiled output.

### No Implicit Pairing

Unlike `#ifdef`/`#else` which are implicitly paired by their lexical position,
two annotations on two separate method declarations have **no inherent relationship**:

```java
@IfDef("DEBUG")
public void logDebug() { /* ... */ }

// this annotation has no structural connection to the one above —
// it is simply another annotated declaration in the same class
@IfDef("!DEBUG")
public void logNoop() { /* ... */ }
```

The Java compiler (and any tool reading the source) sees two independently annotated
methods. There is no language mechanism to express "these two methods are alternatives —
exactly one should survive." This is the impedance mismatch that the `group` property
solves.

---

## Bowerbird's Approach: Bridging the Gap

Bowerbird maps C++ preprocessor semantics onto Java's annotation model. The translation
is not one-to-one — it is a **semantic adaptation** that respects Java's constraints
while preserving the core conditional compilation capability.

### What Maps Directly

| C++ Concept | Bowerbird Equivalent |
|-------------|---------------------|
| `#ifdef FLAG` | `@IfDef("FLAG")` |
| `#ifndef FLAG` | `@IfNotDef("FLAG")` |
| `#elif defined(FLAG)` | `@ElseIfDef(value = "FLAG", group = "...")` |
| `#else` | `@ElseDef(group = "...")` |
| `#endif` | Implicit — the annotated declaration is the scope |
| Boolean expressions: `#if defined(A) && !defined(B)` | `@IfDef("A && !B")` |

### What Changes

| C++ Property | Bowerbird Adaptation | Reason |
|--------------|---------------------|--------|
| Arbitrary block granularity | Types, methods, and fields only | Java annotations cannot target arbitrary code regions |
| Implicit `#else` pairing | Explicit `group` key | Java has no positional pairing between annotations on separate declarations |
| `#endif` closing marker | Not needed | Each annotation's scope is exactly the declaration it annotates |
| Code can contain syntax errors | Excluded code must be syntactically valid | Bowerbird parses the full source AST before stripping; the source file must parse as valid Java |

### End-to-End Example

**C++ original:**

```cpp
#ifdef USE_REDIS
    Cache* createCache() {
        return new RedisCache("localhost", 6379);
    }
#elif defined(USE_MEMCACHED)
    Cache* createCache() {
        return new MemcachedCache("localhost", 11211);
    }
#else
    Cache* createCache() {
        return new InMemoryCache();
    }
#endif
```

**Bowerbird equivalent:**

```java
@IfDef(value = "USE_REDIS", group = "cache-factory")
public Cache createCache() {
    return new RedisCache("localhost", 6379);
}

@ElseIfDef(value = "USE_MEMCACHED", group = "cache-factory")
public Cache createCache() {
    return new MemcachedCache("localhost", 11211);
}

@ElseDef(group = "cache-factory")
public Cache createCache() {
    return new InMemoryCache();
}
```

With the flag `USE_REDIS` active, the preprocessed output sent to `javac` contains only:

```java
public Cache createCache() {
    return new RedisCache("localhost", 6379);
}
```

The other two methods are physically absent from the source that reaches the compiler.

---

## Why the `group` Property Exists

The `group` property is the key design element that bridges the structural gap between
C++ preprocessor blocks and Java annotation-based conditionals. Understanding **why** it
is necessary requires examining what C++ gets for free and what Java does not provide.

### What C++ Gets for Free: Lexical Scoping

In C++, the `#ifdef`/`#else`/`#endif` directives form a **lexically nested block
structure**. The preprocessor maintains a stack:

```
#ifdef A          ← push A onto stack
    ...
    #ifdef B      ← push B onto stack
        ...
    #else         ← belongs to B (top of stack)
        ...
    #endif        ← pop B
    ...
#else             ← belongs to A (now top of stack)
    ...
#endif            ← pop A
```

The pairing is implicit and unambiguous. The preprocessor never needs to ask "which
`#ifdef` does this `#else` belong to?" — the answer is always "the most recent
unclosed one." This is possible because the directives are **block delimiters** that
define a textual region with a clear start (`#ifdef`) and end (`#endif`).

### What Java Does Not Provide: No Block Structure

Java annotations attach to **individual declarations**. Two annotated methods sitting
next to each other in a class body are structurally independent:

```java
@IfDef("DEBUG")
void methodA() { }

@ElseDef       // ← which @IfDef does this belong to?
void methodB() { }
```

Without the `group` property, the preprocessor would need a **positional adjacency
rule**: "`@ElseDef` belongs to the immediately preceding `@IfDef`." This approach
has serious problems:

1. **Fragility** — reordering methods, inserting a new method between the branches,
   or running a code formatter that sorts methods alphabetically would silently
   break the pairing. The developer would get no error — the `@ElseDef` would
   simply attach to the wrong `@IfDef`, producing incorrect compilation output.

2. **Ambiguity with multiple groups** — if a class has two independent conditional
   groups (say, one for cache selection and one for logger selection), positional
   rules cannot reliably associate each `@ElseDef` with its intended `@IfDef`
   when the groups are interleaved or separated by unconditional members.

3. **Readability** — a developer reading a large class would need to scan backwards
   to find the matching `@IfDef` for a given `@ElseDef`. The explicit `group` key
   makes the relationship immediately visible at the point of use.

### The `group` Key as an Explicit Scope Identifier

The `group` property restores the unambiguous pairing that C++ achieves through lexical
nesting, but does so through **explicit naming** rather than **positional convention**:

```java
@IfDef(value = "USE_REDIS", group = "cache-factory")
public Cache createCache() { return new RedisCache(); }

// ... other methods, fields, or even another conditional group can appear here ...

@IfDef(value = "TRACE", group = "logger-level")
public Logger createLogger() { return traceLogger(); }

@ElseDef(group = "logger-level")
public Logger createLogger() { return infoLogger(); }

// ... back to the cache group ...

@ElseDef(group = "cache-factory")
public Cache createCache() { return new InMemoryCache(); }
```

The `group` key provides:

- **Refactoring safety** — methods can be reordered freely. The pairing is by name,
  not by position.
- **Interleaving** — multiple conditional groups can coexist in the same class without
  ambiguity.
- **Explicitness** — the relationship is self-documenting. Any developer (or tool)
  reading the code can immediately see which branches belong together.
- **Validation** — the preprocessor can check at build time that every `@ElseIfDef`
  and `@ElseDef` references a group that has a valid `@IfDef`/`@IfNotDef` head,
  catching errors early.

### When `group` Is Not Needed

For simple standalone conditionals that have no `@ElseIfDef` or `@ElseDef` counterpart,
the `group` property can be omitted. It defaults to an empty string:

```java
@IfDef("DEBUG")
public class DiagnosticsController { /* ... */ }
```

This is the common case for feature-gated classes and fields that are simply present or
absent, with no alternative branch.

---

## Annotation Reference

### `@IfDef`

Includes the annotated element when the expression evaluates to `true`.

```java
@IfDef(value = "FEATURE_A && !LEGACY", group = "optional-group")
```

| Attribute | Type | Default | Description |
|-----------|------|---------|-------------|
| `value` | `String` | — | Boolean expression over feature flags |
| `group` | `String` | `""` | Grouping key for multi-branch conditionals |

Targets: types, methods, fields.

### `@IfNotDef`

Includes the annotated element when the expression evaluates to `false`.
Semantically equivalent to `@IfDef("!expression")` but improves readability.

```java
@IfNotDef(value = "LEGACY_MODE", group = "optional-group")
```

| Attribute | Type | Default | Description |
|-----------|------|---------|-------------|
| `value` | `String` | — | Boolean expression (included when `false`) |
| `group` | `String` | `""` | Grouping key for multi-branch conditionals |

Targets: types, methods, fields.

### `@ElseIfDef`

Intermediate branch within a group. Evaluated only when all preceding branches in
the same group evaluated to `false`.

```java
@ElseIfDef(value = "FEATURE_B", group = "required-group")
```

| Attribute | Type | Default | Description |
|-----------|------|---------|-------------|
| `value` | `String` | — | Boolean expression |
| `group` | `String` | — | **Required.** Must match a preceding `@IfDef`/`@IfNotDef` |

Targets: methods, fields.

### `@ElseDef`

Default/fallback branch. Included only when all preceding branches in the group
evaluated to `false`.

```java
@ElseDef(group = "required-group")
```

| Attribute | Type | Default | Description |
|-----------|------|---------|-------------|
| `group` | `String` | — | **Required.** Must match a preceding `@IfDef`/`@IfNotDef` |

Targets: methods, fields.

### Expression Syntax

Condition expressions support boolean logic over flag identifiers:

| Operator | Meaning | Precedence |
|----------|---------|------------|
| `!` | Logical NOT | Highest |
| `&&` | Logical AND | Medium |
| `\|\|` | Logical OR | Lowest |
| `( )` | Grouping | Overrides precedence |

Flag identifiers match `[A-Za-z_][A-Za-z0-9_]*`. A flag evaluates to `true` if present
in the active flag set, `false` otherwise.

Examples: `"DEBUG"`, `"!PRODUCTION"`, `"FEATURE_A && !LEGACY"`,
`"(USE_REDIS || USE_MEMCACHED) && !IN_MEMORY_ONLY"`.

### Group Rules

1. A group must start with exactly one `@IfDef` or `@IfNotDef`.
2. Zero or more `@ElseIfDef` may follow.
3. At most one `@ElseDef` may close the group.
4. All members must be the **same element kind** (all methods or all fields).
5. All members must reside in the **same enclosing type**.
6. Exactly **one branch** per group survives preprocessing.

---

## Quick Start

### 1. Add the plugin to your `pom.xml`

```xml
<plugin>
    <groupId>io.github.bowerbird.java</groupId>
    <artifactId>bowerbird-maven-plugin</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <executions>
        <execution>
            <goals>
                <goal>preprocess</goal>
            </goals>
        </execution>
    </executions>
    <configuration>
        <flags>
            <flag>DEBUG</flag>
            <flag>FEATURE_AUTH</flag>
        </flags>
    </configuration>
</plugin>
```

### 2. Add the annotation dependency

```xml
<dependency>
    <groupId>io.github.bowerbird.java</groupId>
    <artifactId>bowerbird-api</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <scope>provided</scope>
</dependency>
```

The `provided` scope is sufficient — annotations are `RetentionPolicy.SOURCE` and are
not needed at runtime.

### 3. Annotate your code

```java
import io.github.bowerbird.java.api.IfDef;
import io.github.bowerbird.java.api.ElseDef;

public class AppConfig {

    @IfDef(value = "DEBUG", group = "logging")
    public Logger createLogger() {
        return Logger.getLogger(Level.TRACE);
    }

    @ElseDef(group = "logging")
    public Logger createLogger() {
        return Logger.getLogger(Level.WARN);
    }
}
```

### 4. Build

```bash
mvn clean compile
```

The Bowerbird plugin runs at the `generate-sources` phase, producing preprocessed
sources in `target/generated-sources/preprocessed/`. The compiler compiles from this
directory instead of `src/main/java`.

---

## Configuration

### Plugin Configuration

```xml
<configuration>
    <!-- inline flags (lowest precedence) -->
    <flags>
        <flag>DEBUG</flag>
        <flag>FEATURE_AUTH</flag>
    </flags>

    <!-- external flag file (medium precedence), auto-detected format -->
    <flagFile>${project.basedir}/bowerbird-flags.yaml</flagFile>

    <!-- source file encoding (default: UTF-8) -->
    <sourceEncoding>UTF-8</sourceEncoding>

    <!-- strict | lenient (default: strict) -->
    <errorMode>strict</errorMode>
</configuration>
```

### Flag Sources and Precedence

Flags are resolved from three sources in ascending precedence order. Higher-precedence
sources override lower ones.

| Precedence | Source | Example |
|------------|--------|---------|
| 1 (lowest) | Plugin `<configuration>` in POM | `<flag>DEBUG</flag>` |
| 2 | External flag file (`.properties` or `.yaml`) | `bowerbird-flags.yaml` |
| 3 (highest) | System properties | `-Dbowerbird.flags=DEBUG,PRODUCTION` |

Flags at each layer are merged as a union. To explicitly **disable** a flag defined at
a lower level, prefix it with `!` in a flag file:

**`bowerbird-flags.yaml`**

```yaml
flags:
  - FEATURE_AUTH
  - METRICS
  - "!DEBUG"          # removes DEBUG even if defined in POM
```

**`bowerbird-flags.properties`**

```properties
flags=FEATURE_AUTH,METRICS,!DEBUG
```

### Error Modes

| Mode | Behavior |
|------|----------|
| `strict` | Any validation error (invalid group, malformed expression) fails the build |
| `lenient` | Validation errors produce warnings; affected elements are left untouched |

---

## Multi-Module Projects

Bowerbird supports per-module flag configuration with inheritance from parent POMs.

### Directory Layout

```
parent-pom/
├── bowerbird-flags.yaml          ← base flags for all modules
├── module-api/
│   └── bowerbird-flags.yaml      ← module-level overrides
├── module-service/
│   └── (no flag file — inherits parent)
└── module-admin/
    └── bowerbird-flags.yaml      ← different flag set
```

### Resolution Per Module

1. Start with flags from plugin `<configuration>` (inherits from parent POM).
2. Overlay with flags from the nearest `bowerbird-flags.yaml`/`.properties` (walk
   up to parent if none found locally).
3. Overlay with system properties (`-Dbowerbird.flags=...`), which apply globally.

---

## IDE Support

Bowerbird provides an IntelliJ plugin (`bowerbird-ide-plugin`) that reuses the core
evaluation engine to provide real-time feedback.

| Feature | Description |
|---------|-------------|
| Grayed-out code | Excluded elements appear dimmed based on current flag configuration |
| Flag switching | Toolbar widget to toggle flags live; instant re-evaluation |
| Group validation | Real-time inspections for invalid groups |
| Gutter icons | Markers showing which branch is active |
| Navigation | "Go to other branch" action from any grouped element |

The IDE plugin depends on `bowerbird-core`, ensuring evaluation logic is identical to
the build-time preprocessor.

---

## Error Handling

Bowerbird emits structured diagnostics with codes in the format `BWB-NNN`.

### Validation Errors

| Code | Description |
|------|-------------|
| `BWB-001` | Group has no `@IfDef`/`@IfNotDef` head |
| `BWB-002` | Duplicate `@ElseDef` in group |
| `BWB-003` | Mixed element kinds (methods and fields) in same group |
| `BWB-004` | Orphaned `@ElseIfDef`/`@ElseDef` referencing non-existent group |
| `BWB-005` | `@ElseDef` not last in group |
| `BWB-006` | Single-branch group (informational) |

### Expression Errors

| Code | Description |
|------|-------------|
| `BWB-007` | Malformed condition expression |
| `BWB-008` | Empty condition expression |
| `BWB-009` | Undefined flag reference (evaluates to `false`) |

### I/O and Configuration Errors

| Code | Description |
|------|-------------|
| `BWB-010` | Flag file not found |
| `BWB-011` | Flag file parse error |
| `BWB-012` | Source file parse error |
| `BWB-013` | Output write error |

### Import Cleanup

| Code | Description |
|------|-------------|
| `BWB-014` | Orphaned import removed after preprocessing |

---

## Modules

| Module | ArtifactId | Description |
|--------|-----------|-------------|
| **API** | `bowerbird-api` | Annotation definitions (`@IfDef`, `@IfNotDef`, `@ElseIfDef`, `@ElseDef`). Zero dependencies. |
| **Core** | `bowerbird-core` | Source parser, expression evaluator, group validator, source rewriter, and import cleaner. |
| **Maven Plugin** | `bowerbird-maven-plugin` | Maven Mojo binding at `generate-sources` phase. Orchestrates core processing and flag resolution. |
| **IDE Plugin** | `bowerbird-ide-plugin` | IntelliJ plugin for live flag-aware highlighting, validation, and navigation. |

### Dependency Graph

```
bowerbird-api              (zero dependencies)
    ▲
    │
bowerbird-core             (depends on api, JavaParser, SnakeYAML)
    ▲
    ├─────────────────────────┐
    │                         │
bowerbird-maven-plugin    bowerbird-ide-plugin
```

---

## Building from Source

### Prerequisites

- JDK 21 or later
- Maven 3.9 or later

### Build

```bash
git clone https://github.com/bowerbird-java/bowerbird.git
cd bowerbird
mvn clean verify
```

### Build with Quality Gates

```bash
mvn clean verify -Pquality          # SpotBugs + Checkstyle
mvn clean verify -Pmutation         # PIT mutation testing (bowerbird-core)
mvn clean verify -Pquality,mutation # all quality gates
```

### Run Integration Tests

```bash
mvn clean install -DskipTests
mvn verify -pl bowerbird-maven-plugin
```

---

## License

```
Copyright (c) 2025 Bowerbird Contributors

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
