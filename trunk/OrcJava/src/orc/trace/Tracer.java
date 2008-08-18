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
import orc.trace.events.StoreEvent;
import orc.trace.query.predicates.Predicate;
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
 * Methods correspond to events which may be traced.
 * 
 * <p>What is a thread? Intuitively, it corresponds to:
 * <ul>
 * <li>a token in the execution DAG
 * <li>a connected sequence of evaluation steps
 * </ul>
 * 
 * @author quark
 */
public interface Tracer {
	/**
	 * Events must satisfy this predicate to be traced.
	 */
	public void setFilter(Predicate filter);
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
	 * Terminate a thread.
	 */
	public void die();
	/**
	 * Call a site.
	 */
	public void call(Object site, Object[] arguments);
	/**
	 * Store a value for a future. The return value should be used when tracing
	 * the results of this store. When all events related to the store have been
	 * traced, call {@link #free(Event)}.
	 * 
	 * @see #choke(StoreEvent)
	 * @see #unblock(StoreEvent)
	 * @see #free(Event)
	 */
	public StoreEvent store(Object value);
	/**
	 * Killed through the setting of a future.
	 * Should be followed by {@link #die()}.
	 */
	public void choke(StoreEvent store);
	/**
	 * Return from a site call. Should be called after
	 * {@link #call(Object, Object[])}.
	 */
	public void resume(Object value);
	/**
	 * Block a thread waiting for a future.
	 */
	public void block();
	/**
	 * Receive a future we were waiting for.
	 */
	public void unblock(StoreEvent store);
	/**
	 * Print to stdout.
	 */
	public void print(String value, boolean newline);
	/**
	 * Publish a value from the program.
	 * Should be followed by {@link #die()}.
	 */
	public void publish(Object value);
	/**
	 * Indicate that an event will not appear after
	 * the current point in the trace.
	 * @param store
	 */
	public void free(Event event);
	/**
	 * Report an error.
	 * Should be followed by {@link #die()}.
	 */
	public void error(TokenException error);
}
