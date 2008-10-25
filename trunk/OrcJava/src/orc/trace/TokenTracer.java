package orc.trace;

import orc.error.Locatable;
import orc.error.runtime.TokenException;
import orc.runtime.values.Closure;

/**
 * Interface for writing traces from a single Orc thread. Methods correspond to
 * events which may be traced; in essence this is like a visitor of execution
 * events. Some guidelines used to organize events:
 * <ul>
 * <li>Steps shared by several logical events are made explicit. So for example
 * when a thread encounters an error, this results in
 * {@link #error(TokenException)} followed by {@link #die()}. We could make the
 * {@link #die()} implicit but making it explicit facilitates code reuse in the
 * client and simplifies queries.
 * <li>When two threads interact, at least two events are involved: one for the
 * cause and one for the effect. This ensures that we can reconstruct the
 * behavior of a thread looking only at events in that thread. The effect event
 * includes a pointer back to the cause event.
 * </ul>
 * 
 * <p>
 * "Trace" objects ({@link StoreTrace} et al) serve as abstract handles for
 * events and are used to record relationships between events in different
 * threads. Since Java doesn't have existential types, implementors have to cast
 * these objects to the appropriate concrete types internally. This is safe as
 * long as all the TokenTracers produced by a single {@link Tracer} use use
 * compatible concrete trace types.
 * 
 * @author quark
 */
public interface TokenTracer extends Locatable {
	/** Abstract handle for a store event */
	public interface StoreTrace {}
	/** Abstract handle for a pull event */
	public interface PullTrace {}
	/** Abstract handle for a before event */
	public interface BeforeTrace {}
	/**
	 * Create a new thread. By convention the new thread should
	 * evaluate the right side of the combinator.
	 */
	public TokenTracer fork();
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
	 * the results of this store. If this returns null, clients are free to
	 * <i>not</i> call {@link #choke(StoreTrace)}.
	 * 
	 * <p>
	 * The engine guarantees that all
	 * {@link #choke(orc.trace.TokenTracer.StoreTrace)} and
	 * {@link #unblock(orc.trace.TokenTracer.StoreTrace)} events will occur
	 * <i>before</i> the {@link #die()} event for this tracer.
	 * 
	 * @see #choke(orc.trace.TokenTracer.StoreTrace)
	 * @see #unblock(orc.trace.TokenTracer.StoreTrace)
	 */
	public StoreTrace store(PullTrace event, Object value);
	/**
	 * Killed through the setting of a future.
	 * Should be followed by {@link #die()}.
	 */
	public void choke(StoreTrace store);
	/**
	 * Return from a site call. Should be called after
	 * {@link #send(Object, Object[])}.
	 */
	public void receive(Object value);
	/**
	 * Block a thread waiting for a future.
	 */
	public void block(PullTrace pull);
	/**
	 * Receive a future we were waiting for.
	 */
	public void unblock(StoreTrace store);
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
	 * Report an error.
	 * Should be followed by {@link #die()}.
	 */
	public void error(TokenException error);
	/**
	 * Create a new future for a pull.
	 * Should be followed by {@link #fork()}.
	 */
	public PullTrace pull();
	/**
	 * Leaving the left side of a semicolon combinator. If the thread is
	 * "leaving" because it is dying, this will be followed by a {@link #die()};
	 * otherwise it may be followed by any number of events which happen outside
	 * the scope of the semicolon.
	 * 
	 * @return a tag which you can pass to {@link #after(BeforeTrace)}.
	 */
	public BeforeTrace before();
	/**
	 * Enter a closure.
	 * EXPERIMENTAL
	 */
	public void enter(Closure closure);
	/**
	 * Leave "depth" closures
	 * EXPERIMENTAL
	 */
	public void leave(int depth);
	/**
	 * Indicate that the right side of a semicolon combinator is continuing.
	 * @param before the BeforeEvent which triggered this event
	 */
	public void after(BeforeTrace before);
	/**
	 * Called when a token reads a value from a group cell which has
	 * already been stored.
	 * @param storeTrace the trace produced when {@link #store(orc.trace.TokenTracer.PullTrace, Object)} was called
	 */
	public void useStored(StoreTrace storeTrace);
}
