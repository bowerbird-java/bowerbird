package io.github.bowerbird.java.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Conditionally includes the annotated element when the expression evaluates to {@code false}.
 *
 * <p>Semantically equivalent to {@code @IfDef("!expression")} but improves readability
 * in flag-only scenarios where the intent is "include when this flag is absent."</p>
 *
 * <p>When used with a {@code group} key, this annotation marks the <em>head</em> of a
 * conditional group, just like {@link IfDef}.</p>
 *
 * <h3>Example</h3>
 * <pre>{@code
 * @IfNotDef("LEGACY_MODE")
 * public void enforceModernAuth(HttpRequest request) { }
 * }</pre>
 *
 * @see IfDef
 * @see ElseIfDef
 * @see ElseDef
 */
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})
public @interface IfNotDef {

    /**
     * Boolean expression that must evaluate to {@code false} for the element to be included.
     *
     * @return the condition expression
     */
    String value();

    /**
     * Grouping key for pairing with {@link ElseIfDef} and {@link ElseDef} branches.
     *
     * @return the group identifier, or empty string for standalone conditionals
     * @see IfDef#group()
     */
    String group() default "";
}
