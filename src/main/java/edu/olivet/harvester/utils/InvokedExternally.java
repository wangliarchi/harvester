package edu.olivet.harvester.utils;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used to indicator some classes or methods are used or invoked externally.
 * This is a workaround to tolerant unreasonable dependency chain, yet need to be
 * reserved for maintenance compatibility. However, such bad smell and habit should not
 * be kept in new implementations.
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 10/26/17 2:14 PM
 */

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})
public @interface InvokedExternally {

    String desc() default "";

}