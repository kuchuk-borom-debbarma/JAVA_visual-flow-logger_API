package dev.kuku.vfl.impl.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as a sub block in VFL tracing.
 *
 * <p>Supports placeholders in {@link #blockName()}, {@link #startMessage()} and {@link #endMessage()}:
 * <ul>
 *   <li>{@code {0}}, {@code {1}}, ... → method arguments (0‑based index)</li>
 *   <li>{@code {r}} / {@code {return}} → return value (endMessage only)</li>
 * </ul>
 *
 * Example:
 * <pre>
 * {@code
 * @SubBlock(
 *     blockName="Process {0}",
 *     startMessage="Start {0}",
 *     endMessage="Done {0} -> {r}"
 * )
 * }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface SubBlock {

    /** Name of the sub block. Defaults to method name if blank. */
    String blockName() default "";

    /** Message logged on entry. Blank = no start message. */
    String startMessage() default "";

    /** Message logged on exit. Blank = no end message. */
    String endMessage() default "";
}
