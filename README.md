# ASMEvents
A fast and feature rich Event library for Java using ASM to dynamically generate executor classes.  
The library has full obfuscation support and allows for multithreaded event access even for timing critical events.

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
To register an event listener method call the `EventManager.register()` method.
```Java
//Register a listener
EventManager.register(Listener.class); //Static listener
EventManager.register(new Listener()); //Non static listener

//Register a listener to only a specific event
EventManager.register(Event.class, Listener.class); //Static listener
EventManager.register(Event.class, new Listener()); //Non static listener


//Unregister a listener
EventManager.unregister(Listener.class); //Static listener
EventManager.unregister(new Listener()); //Non static listener

//Unregister a listener from only a specific event
EventManager.unregister(Event.class, Listener.class); //Static listener
EventManager.unregister(Event.class, new Listener()); //Non static listener
```
You can either pass an listener class or instance to the `register` method.  
If you pass a class only static methods get registered.  
If you pass an instance only non static methods get registered.  
If the listener is already registered/not registered nothing will happen. This prevents call stacking.  

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
EventManager.setErrorListener(new ErrorListenerImpl());
```

The generated pipeline classes for the events are loaded using a custom ClassLoader by default.  
You may want to change how they get loaded so there is an interface to provide class loading.  
To the set class load provider just call the `setClassLoadProvider` method.
```Java
//The class load provider needs to implement the IClassLoadProvider interface
EventManager.setClassLoadProvider(new ClassLoadProviderImpl());
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
If any event listener in the pipeline throws an exception the whole pipeline breaks and all following listener won't get called. If you need the event pipeline to continue after a thrown exception you can add the `@PipelineSafety` annotation to the event where you want this extra safety.  
There is the option to just print the catched exception or to do nothing with it.

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
```Java
//It is possible to add arguments to listener methods other than events
//You may find this useful for reusing methods
@EventTarget
public void onEvent(final int i, final Event event, final Object o) {
    //In this case all arguments other than the current event are null/their primitive counterpart
    //Here i == 0 and o == null
}

//This also works with noParamEvents
@EventTarget(noParamEvents = Event.class)
public void onEvent(final int i) {
    //Here also i == 0
}
```

```Java
//This is the basic event call
EventManager.call(new Event());
```
```Java
//This is an example for a cancellable event which breaks the method execution if cancelled
if (EventManager.call(new Event()).isCancelled()) return;
```
```Java
//This is an example for a cancellable event which breaks the method execution if cancelled and modifies a variable
Event event = EventManager.call(new Event(someField));
if (event.isCancelled()) return;
someField = event.getSomeField();
```
```Java
//Here an example for a full method using pre and post events
public static int multiply(int num) {
    if(EventManager.call(new MathEvent(EnumEventType.PRE, num)).isCancelled()) return num;

    num *= 2;

    num = EventManager.call(new MathEvent(EnumEventType.POST, num)).getNum();

    return num;
}
```

```Java
//This is an example event class for the MathEvent from above
public class MathEvent implements IEvent, ICancellableEvent, ITypedEvent {

    private final EnumEventType type;
    private int num;
    private boolean cancelled;

    public MathEvent(final EnumEventType type, final int num) {
        this.type = type;
        this.num = num;
        this.cancelled = false;
    }

    @Override
    public EnumEventType getType() {
        return this.type;
    }

    public int getNum() {
        return this.num;
    }

    public void setNum(final int num) {
        this.num = num;
    }

    @Override
    public boolean isCancelled() {
        return this.cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

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
