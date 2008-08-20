package orc.trace.query;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

import orc.error.OrcError;
import orc.trace.events.Event;
import orc.trace.handles.Handle;
import orc.trace.handles.HandleInputStream;

public class InputStreamEventCursor implements EventCursor {
	private HandleInputStream in;
	private Handle<Event> head;
	private InputStreamEventCursor tail;
	public InputStreamEventCursor(InputStream in) throws IOException {
		this(new HandleInputStream(in));
	}
	private InputStreamEventCursor(HandleInputStream in) {
		this.in = in;
	}
	
	private void force() throws EndOfStream {
		if (in == null) return;
		try {
			head = in.readHandle();
			tail = new InputStreamEventCursor(in);
			in = null;
		} catch (EOFException e) {
			throw new EndOfStream();
		} catch (IOException e) {
			// FIXME: is there a better way to handle this?
			throw new OrcError(e);
		}
	}
	
	public Event current() throws EndOfStream {
		force();
		return head.get();
	}
	
	public InputStreamEventCursor forward() throws EndOfStream {
		force();
		return tail;
	}
	public EventCursor back() throws EndOfStream {
		throw new EndOfStream();
	}
}
