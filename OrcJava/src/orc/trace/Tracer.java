package orc.trace;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import orc.error.Locatable;
import orc.error.runtime.TokenException;
import orc.runtime.values.GroupCell;
import orc.runtime.values.Visitor;
import orc.trace.events.BeforeEvent;
import orc.trace.events.SendEvent;
import orc.trace.events.DieEvent;
import orc.trace.events.Event;
import orc.trace.events.PullEvent;
import orc.trace.events.ReceiveEvent;
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
 * Interface for writing traces from a single Orc thread. Methods correspond to
 * events which may be traced. Some guidelines used to organize events:
 * <ul>
 * <li>Steps shared by several logical events are made explicit. So for example
 * when a thread encounters an error, this results in {@link #error(TokenException)}
 * followed by {@link #die()}. We could make the {@link #die()} implicit but making
 * it explicit facilitates code reuse in the client and simplifies queries.
 * <li>When two threads interact, at least two events are involved: one for the
 * cause and one for the effect. This ensures that we can reconstruct the behavior
 * of a thread looking only at events in that thread. The effect event includes
 * a pointer back to the cause.
 * </ul>
 * 
 * <p>FIXME: the event objects passed between trace methods should be more abstract,
 * so it's possible to write tracers that don't use our event classes.
 * 
 * @author quark
 */
public interface Tracer extends Locatable {
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
	public void send(Object site, Object[] arguments);
	/**
	 * Store a value for a future. The return value should be used when tracing
	 * the results of this store. When all events related to the store have been
	 * traced, call {@link #free(Event)}. If this returns null, clients are
	 * free to <i>not</i> call {@link #choke(StoreEvent)}.
	 * 
	 * @see #choke(StoreEvent)
	 * @see #unblock(StoreEvent)
	 * @see #free(Event)
	 */
	public StoreEvent store(PullEvent event, Object value);
	/**
	 * Killed through the setting of a future.
	 * Should be followed by {@link #die()}.
	 */
	public void choke(StoreEvent store);
	/**
	 * Return from a site call. Should be called after
	 * {@link #send(Object, Object[])}.
	 */
	public void receive(Object value);
	/**
	 * Block a thread waiting for a future.
	 */
	public void block(PullEvent pull);
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
	 * Indicate that an event will not appear after the current point in the
	 * trace. Currently this is only used for the event returned by
	 * {@link #store(PullEvent, Object)}, because there's no other easy way for
	 * the tracer to know when the last relate event has been recorded.
	 */
	public void free(Event event);
	/**
	 * Report an error.
	 * Should be followed by {@link #die()}.
	 */
	public void error(TokenException error);
	/**
	 * Create a new future for a pull.
	 * Should be followed by {@link #fork()}.
	 */
	public PullEvent pull();
	/**
	 * Leaving the left side of a semicolon combinator. If the thread is
	 * "leaving" because it is dying, this will be followed by a {@link #die()};
	 * otherwise it may be followed by any number of events which happen outside
	 * the scope of the semicolon.
	 * 
	 * @return a BeforeEvent which you can pass to {@link #after(BeforeEvent)}.
	 */
	public BeforeEvent before();
	/**
	 * Indicate that the right side of a semicolon combinator is continuing.
	 * @param before the BeforeEvent which triggered this event
	 */
	public void after(BeforeEvent before);
}
