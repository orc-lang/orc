package orc.orchard.interfaces;

import java.io.Serializable;

public interface Publication extends Serializable {
	/** Timestamp when the value was published. */
	public java.util.Date getTimestamp();
	/**
	 * Sequence number. This does not imply anything about when the value was
	 * actually published, but if a client observes publications 1 .. n and the
	 * next sequence is n+1, the client knows it didn't miss anything.
	 */
	public int getSequence();
	/**
	 * The value published. Orc tuples are encoded as arrays, Orc lists as
	 * java.util.List, and all other values as the obvious Java equivalents.
	 */
	public Object getValue();
}