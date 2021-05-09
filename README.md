# ASMEvents
A fast and feature rich Event library for Java using ASM to dynamically generate executor classes.

## Usage
### EventTarget annotation
To register an event listener method you must mark it with the `@EventTarget` annotation. The name of the method and parameters are not fixed. Name them as you want.
```Java
@EventTarget
public void onEvent(final Event event) {
}
```
The `@EventTarget` annotation has some useful fields you can use.  

| Field         | Description                                                                                |
| ------------- | ------------------------------------------------------------------------------------------ |
| priority      | Give listener more priority in the pipeline and ensure they are called ad the correct spot |
| type          | Choose only which type of event should get passed to the method (`PRE`, `POST` or both)    |
| skipCancelled | Skip events which are already cancelled to speed up the call                               |
| noParamEvents | Some events to listen to without getting their instance                                    |

### EventManager
The EventManager is the main class you will be working with.  
It handles event listener registering, unregistering and calling.  
To register an event listener method call.
```Java
//Register a listener
EventManager.register(Listener.call); //Static listener
EventManager.register(new Listener()); //Non static listener

//Unregister a listener
EventManager.unregister(Listener.call); //Static listener
EventManager.unregister(new Listener()); //Non static listener
```
You can either pass an listener class or instance to the `register` method.  
If you pass a class only static methods get registered.  
If you pass an instance only non static methods get registered.

To call an event just invoke the `call` method.
```Java
//The event needs to implement the IEvent interface
EventManager.call(new Event());
```

If an exception occurs in the generated event pipeline it is passed to the error listener.  
By default the error listener rethrows all exception as a runtime exception but you can set your own handler if you just want to print the exception it or do something else with it.  
To do that just call the `setErrorListener` method.
```Java
//The error listener needs to implement the IErrorListener interface
EventManager.addErrorListener();
```

### Event types
There are 4 types of events

| Type              | Description                                                                                                                                                                                  |
| ----------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| IEvent            | The default event which itself has no special usage                                                                                                                                          |
| ICancellableEvent | The cancellable event can be cancelled (as the name suggest). It can be used if a handler should be able to prevent some code from executing. You need to handle the cancelled code yourself |
| IStoppableEvent   | The stoppable event is the same as the cancelled but all following listeners get skipped                                                                                                     |
| ITypedEvent       | The typed event can have two types `PRE` and `POST`. It is useful if an event is called at the beginning and end of a method                                                                 |

All events have an already wrapped class to just extend which just contains the basic needed methods.

## Other code snippets
```Java
//It is possible to listen to all events if you just put the IEvent interface into the paramter
@EventTarget
public void onEvent(final IEvent event) {
    //Here you should check which event it is
}
```
```Java
//It is possible to listen for more than 1 event in a method
//The other event instances are null
@EventTarget
public void onEvent(final Event1 event1, final Event2 event2) {
    //If event1 is called event2 == null
}
```
```Java
//You can listen to events without needing them in your parameter
//The instance of other events in the parameters (if there are any) are null
@EventTarget(noParamEvents = Event2.class)
public void onEvent(final Event1 event1) {
    //If event2 is called event1 == null
}

//You do not need any parameter if you listen to events using the annotation
@EventTarget(noParamEvents = Event.class)
public void onEvent() {
}
```
```Java
//Here an example of all fields in the @EventTarget annotation
//Default: @EventTarget(priority = EnumEventPriority.NORMAL, type = EnumEventType.ALL, skipCancelled = false, noParamEvents = {})
@EventTarget(priority = EnumEventPriority.LOW, type = EnumEventType.PRE, skipCancelled = true, noParamEvents = Event.class)
public void onEvent() {
}
```
```Java
//Something like this is also "legal" but this will not listen to any events obviously
@EventTarget
public void onEvent() {
}

//This method does not have the @EventTarget annotation so this will also not listen to anything
public void onEvent(final Event event) {
}
```

## Contribute
If you want to contribute code please make sure it is kept in the same style as the original code:  
 - Method parameter should be final except you modify them.  
 - Local fields can be final if it makes the code more readable but usually I don't really care about this much.  
 - Global fields should be final except you modify them.  
 - Global fields should generally be private and only accessed using getter and setter. Exceptions for this are static fields.  
 - Static fields always have an upper case name and spaces are replaced with underscores.  
 - Please avoid using streams. I don't like them (they always seem so slow in comparison with for loops).  
 - Inlined if statements (both `boolean ? true : false` and `if(boolean) method();`) are ok as long as they do not get too long. And please avoid stacking them. It really makes code unreadable.  

Just please keep it in the style as the other code.

You do not need to mark changes but please document them well so it is easier for others to understand why you chose that code style and not others.
