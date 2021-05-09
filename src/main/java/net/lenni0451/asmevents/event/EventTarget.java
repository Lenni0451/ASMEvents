package net.lenni0451.asmevents.event;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface EventTarget {

    /**
     * The priority of the event in the pipeline
     */
    EnumEventPriority priority() default EnumEventPriority.NORMAL;

    /**
     * The type of event which should handled by the method
     */
    EnumEventType type() default EnumEventType.ALL;

    /**
     * The method also targets cancelled methods
     */
    boolean skipCancelled() default false;

}
