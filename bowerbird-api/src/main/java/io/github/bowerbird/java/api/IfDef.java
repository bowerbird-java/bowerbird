package io.github.bowerbird.java.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Conditionally includes the annotated element when the expression evaluates to {@code true}.
 *
 * <p>Supports simple flags ({@code "DEBUG"}) and boolean expressions
 * ({@code "FEATURE_A && !PRODUCTION"}). Operators: {@code &&} (AND), {@code ||} (OR),
 * {@code !} (NOT), and parentheses for grouping.</p>
 *
 * <p>When used with a {@code group} key, this annotation marks the <em>head</em> of a
 * conditional group. Exactly one branch in the group survives preprocessing. The group
 * may contain zero or more {@link ElseIfDef} branches and at most one {@link ElseDef}
 * fallback.</p>
 *
 * <p><b>Example — standalone</b></p>
 * <pre>{@code
 * @IfDef("DEBUG")
 * public class DiagnosticsController { }
 * }</pre>
 *
 * <p><b>Example — grouped</b></p>
 * <pre>{@code
 * @IfDef(value = "USE_REDIS", group = "cache-factory")
 * public Cache createCache() { return new RedisCache(); }
 *
 * @ElseDef(group = "cache-factory")
 * public Cache createCache() { return new InMemoryCache(); }
 * }</pre>
 *
 * @see IfNotDef
 * @see ElseIfDef
 * @see ElseDef
 */
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})
public @interface IfDef {

    /**
     * Boolean expression evaluated against the active flag set.
     *
     * <p>A flag identifier evaluates to {@code true} if present in the active set,
     * {@code false} otherwise.</p>
     *
     * @return the condition expression
     */
    String value();

    /**
     * Grouping key for pairing with {@link ElseIfDef} and {@link ElseDef} branches.
     *
     * <p>When empty (the default), this annotation acts as a standalone conditional
     * with no alternative branches. When non-empty, the preprocessor matches all
     * annotations sharing the same group key within the enclosing type.</p>
     *
     * @return the group identifier, or empty string for standalone conditionals
     */
    String group() default "";
}
