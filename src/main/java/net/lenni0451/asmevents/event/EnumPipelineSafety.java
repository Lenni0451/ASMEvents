package net.lenni0451.asmevents.event;

public enum EnumPipelineSafety {

    /**
     * Call {@link Throwable#printStackTrace()}
     */
    PRINT,

    /**
     * Call {@link net.lenni0451.asmevents.IErrorListener#onException(Throwable)}
     */
    ERROR_LISTENER,

    /**
     * Ignore the exception and do nothing
     */
    IGNORE
    
}
