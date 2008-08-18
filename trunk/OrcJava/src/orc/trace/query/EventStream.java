package orc.trace.query;

import java.util.NoSuchElementException;

import orc.trace.events.Event;

/**
 * Stream of trace events.
 * @author quark
 */
public interface EventStream {
	/**
	 * Return the event at the head of the stream.
	 * @throws NoSuchElementException if there are no more events.
	 */
	public Event head() throws NoSuchElementException;
	/**
	 * Return the tail of the stream.
	 * @throws NoSuchElementException if there are no more events.
	 */
	public EventStream tail() throws NoSuchElementException;
}