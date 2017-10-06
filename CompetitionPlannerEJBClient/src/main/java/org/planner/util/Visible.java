package org.planner.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.METHOD })
public @interface Visible {

	boolean initial() default true;

	String[] roles() default {};

	int depth() default 1;

	boolean export() default false;

	int order() default -1;
}
