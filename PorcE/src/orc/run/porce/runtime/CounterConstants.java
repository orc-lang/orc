
package orc.run.porce.runtime;

public abstract class CounterConstants {
    // Due to inlining, changing this will likely require a full rebuild.
    public static final boolean tracingEnabled = false;
    
    // FIXME: Make this configurable.
    public static final int maxCounterDepth = 8000;
}
