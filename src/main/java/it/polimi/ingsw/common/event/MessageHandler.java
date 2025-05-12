package it.polimi.ingsw.common.event;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark methods that should handle specific message types.
 * The annotated method must have exactly one parameter, which is the
 * type of the Message it handles.
 */
@Retention(RetentionPolicy.RUNTIME) // Needs to be available at runtime for reflection
@Target(ElementType.METHOD)         // Can only be applied to methods
public @interface MessageHandler {
}