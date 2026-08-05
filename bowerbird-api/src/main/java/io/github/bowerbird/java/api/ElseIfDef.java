package io.github.bowerbird.java.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Intermediate conditional branch within a group.
 *
 * <p>Evaluated only when all preceding {@link IfDef}/{@link IfNotDef}/{@link ElseIfDef}
 * branches in the same group evaluated to {@code false}. If this branch's expression
 * evaluates to {@code true}, it is included and all subsequent branches in the group
 * are excluded.</p>
 *
 * <p>The {@code group} key is <em>required</em> and must reference a group whose head
 * ({@link IfDef} or {@link IfNotDef}) is declared in the same enclosing type.</p>
 *
 * <p><b>Example</b></p>
 * <pre>{@code
 * @IfDef(value = "USE_REDIS", group = "cache")
 * public Cache createCache() { return new RedisCache(); }
 *
 * @ElseIfDef(value = "USE_MEMCACHED", group = "cache")
 * public Cache createCache() { return new MemcachedCache(); }
 *
 * @ElseDef(group = "cache")
 * public Cache createCache() { return new InMemoryCache(); }
 * }</pre>
 *
 * @see IfDef
 * @see IfNotDef
 * @see ElseDef
 */
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.METHOD, ElementType.FIELD})
public @interface ElseIfDef {

    /**
     * Boolean expression evaluated against the active flag set.
     *
     * @return the condition expression
     */
    String value();

    /**
     * Grouping key — must match a preceding {@link IfDef} or {@link IfNotDef} in the
     * same enclosing type.
     *
     * @return the group identifier
     */
    String group();
}
