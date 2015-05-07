package orc.trace;

/**
 * Interface for tracing an Orc execution.
 * Most of the work is done by the {@link TokenTracer}
 * returned by {@link #start()}.
 * 
 * @author quark
 */
public abstract class Tracer {
	/**
	 * Begin an execution; return the tracer for the first token.
	 */
	public abstract TokenTracer start();
	/**
	 * End an execution.
	 */
	public abstract void finish();
}
