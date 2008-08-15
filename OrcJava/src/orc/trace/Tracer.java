package orc.trace;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import orc.error.runtime.TokenException;
import orc.runtime.values.Visitor;
import orc.trace.events.CallEvent;
import orc.trace.events.DieEvent;
import orc.trace.events.Event;
import orc.trace.events.ResumeEvent;
import orc.trace.events.ForkEvent;
import orc.trace.values.ConsValue;
import orc.trace.values.ConstantValue;
import orc.trace.values.NilValue;
import orc.trace.values.NoneValue;
import orc.trace.values.NullValue;
import orc.trace.values.ObjectValue;
import orc.trace.values.SomeValue;
import orc.trace.values.TraceableValue;
import orc.trace.values.TupleValue;
import orc.trace.values.Value;

/**
 * Interface for writing traces from a single Orc thread.
 * There are methods corresponding to trace events.
 * <b>Not thread safe.</b>
 * 
 * @author quark
 */
public interface Tracer {
	/**
	 * Start a new engine.
	 */
	public void start();
	/**
	 * Create a new thread. By convention the new thread should
	 * evaluate the right side of the combinator.
	 */
	public Tracer fork();
	/**
	 * Call a site.
	 */
	public void call(Object site, Object[] arguments);
	/**
	 * Terminate a thread.
	 */
	public void die();
	/**
	 * Return from a site call. Should be called after
	 * {@link #call(Object, Object[])}.
	 */
	public void resume(Object value);
	/**
	 * Block a thread waiting for a group cell value.
	 */
	public void block();
	/**
	 * Receive a group cell value we were waiting for.
	 */
	public void unblock();
	/**
	 * Print to stdout.
	 */
	public void print(String value, boolean newline);
	/**
	 * Publish a value from the program.
	 */
	public void publish(Object value);
	/**
	 * Report an error.
	 */
	public void error(TokenException error);
}
