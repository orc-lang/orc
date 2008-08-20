package orc.trace.query;


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
	public Event current() throws EndOfStream;
	/**
	 * Return a cursor to the next event in the stream.
	 */
	public EventCursor forward() throws EndOfStream;
	/**
	 * Return a cursor to the previous event in the stream.
	 */
	public EventCursor back() throws EndOfStream;
}