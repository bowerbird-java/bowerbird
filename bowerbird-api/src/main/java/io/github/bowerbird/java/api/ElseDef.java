package io.github.bowerbird.java.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Default/fallback branch within a conditional group.
 *
 * <p>Included only when every preceding {@link IfDef}/{@link IfNotDef}/{@link ElseIfDef}
 * branch in the same group evaluated to {@code false}. At most one {@code @ElseDef} may
 * appear per group, and it must be the <em>last</em> branch.</p>
 *
 * <p>The {@code group} key is <em>required</em> and must reference a group whose head
 * ({@link IfDef} or {@link IfNotDef}) is declared in the same enclosing type.</p>
 *
 * <p><b>Example</b></p>
 * <pre>{@code
 * @IfDef(value = "DEBUG", group = "logging")
 * public Logger createLogger() { return Logger.getLogger(Level.TRACE); }
 *
 * @ElseDef(group = "logging")
 * public Logger createLogger() { return Logger.getLogger(Level.WARN); }
 * }</pre>
 *
 * @see IfDef
 * @see IfNotDef
 * @see ElseIfDef
 */
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.METHOD, ElementType.FIELD})
public @interface ElseDef {

    /**
     * Grouping key — must match a preceding {@link IfDef} or {@link IfNotDef} in the
     * same enclosing type.
     *
     * @return the group identifier
     */
    String group();
}
