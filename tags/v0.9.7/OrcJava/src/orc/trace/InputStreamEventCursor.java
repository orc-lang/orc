package orc.trace;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import orc.error.OrcError;
import orc.trace.events.Event;
import orc.trace.handles.Handle;
import orc.trace.handles.HandleInputStream;

public class InputStreamEventCursor implements EventCursor {
	private HandleInputStream in;
	private Event head;
	private InputStreamEventCursor tail;
	/**
	 * Sequence counter for events.
	 * FIXME: this won't reset if you open multiple event streams.
	 */
	private static long seq = 0;
	public InputStreamEventCursor(InputStream in) throws IOException {
		this(new HandleInputStream(new GZIPInputStream(in)));
	}
	private InputStreamEventCursor(HandleInputStream in) throws IOException {
		Handle<Event> handle = in.readHandle();
		this.in = in;
		this.head = handle.get();
		this.head.setCursor(this);
		this.head.setSeq(seq++);
	}
	
	public Event current() {
		return head;
	}
	
	public InputStreamEventCursor forward() throws EndOfStream {
		if (tail != null) return tail;
		try {
			tail = new InputStreamEventCursor(in);
			return tail;
		} catch (EOFException e) {
			throw new EndOfStream();
		} catch (IOException e) {
			// FIXME: is there a better way to handle this?
			throw new OrcError(e);
		}
	}
	
	public InputStreamEventCursor backward() throws EndOfStream {
		throw new EndOfStream();
	}
}
