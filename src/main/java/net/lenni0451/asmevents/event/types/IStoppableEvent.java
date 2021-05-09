package net.lenni0451.asmevents.event.types;

/**
 * Stoppable events are the same as cancellable events but the pipeline does not call other listeners after it has been cancelled.<br>
 * Here the caller is also responsible for handling the cancelled state
 */
public interface IStoppableEvent extends ICancellableEvent {
}
