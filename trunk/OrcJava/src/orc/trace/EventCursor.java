package orc.trace;


import orc.trace.events.Event;

/**
 * Functional cursor in a stream of trace events. You'd probably implement this
 * as a <a href="http://en.wikipedia.org/wiki/Zipper_(data_structure)">Zipper</a>.
 * 
 * @author quark
 */
public interface EventCursor {
	public static class EndOfStream extends Exception {}
	/**
	 * Return the current event in the stream.
	 */
	public Event current();
	/**
	 * Return a cursor to the next event in the stream.
	 * Why "forward" instead of "next"? To avoid confusion
	 * with the temporal logic "next" operator which evaluates
	 * a predicate at the next location. Also because I prefer
	 * the symmetry of forward/backward to next/previous.
	 */
	public EventCursor forward() throws EndOfStream;
	/**
	 * Return a cursor to the previous event in the stream.
	 */
	public EventCursor backward() throws EndOfStream;
}