package org.planner.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.METHOD })
public @interface Visible {

	/**
	 * @return zusätzlicher Pfad zu Properties, die nicht per depth erreicht werden oder keine Visible Annotation haben.
	 *         Dieses Attribut wird nur ausgewertet, wenn die {@link Visibilities}-Annotation verwendet wird.
	 */
	String path() default "";

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

	/**
	 * @return Spalte, nach der gruppiert werden soll, wenn die zugehörige Tabellenspalte über mehrere Zeilen durch
	 *         einen Group-Join in einen Wert überführt werden soll.
	 */
	String multiRowGroup() default "";
}
