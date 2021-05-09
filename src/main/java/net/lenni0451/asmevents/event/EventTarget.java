package net.lenni0451.asmevents.event;

import net.lenni0451.asmevents.event.enums.EnumEventPriority;
import net.lenni0451.asmevents.event.enums.EnumEventType;

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

    /**
     * Events to listen to which do not need a parameter passed
     */
    Class<? extends IEvent>[] noParamEvents() default {};

}
