package org.planner.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.METHOD })
public @interface Visible {

	/**
	 * @return true, wenn die Spalte per Voreinstellung angezeigt werden soll
	 */
	boolean initial() default true;

	/**
	 * @return true, wenn der Wert von der Datenbank geholt werden soll, unabhängig von der Sichtbarkeit der Spalte
	 */
	boolean mandatory() default false;

	/**
	 * @return die Rollen, auf die die Sichtbarkeit eingeschränkt ist
	 */
	String[] roles() default {};

	/**
	 * @return die Tiefe, in die in komplexe Typen abgestiegen werden soll
	 */
	int depth() default 1;

	/**
	 * @return true, wenn diese Spalte exportiert werden soll
	 */
	boolean export() default false;

	/**
	 * @return die Reihenfolge der Anzeige
	 */
	int order() default -1;
}
