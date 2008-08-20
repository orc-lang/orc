package orc.trace.query;


import orc.trace.events.Event;

/**
 * Stream of trace events.
 * @author quark
 */
public interface EventStream {
	public static class EndOfStream extends Exception {}
	/**
	 * Return the event at the head of the stream.
	 */
	public Event head() throws EndOfStream;
	/**
	 * Return the tail of the stream.
	 */
	public EventStream tail() throws EndOfStream;
}