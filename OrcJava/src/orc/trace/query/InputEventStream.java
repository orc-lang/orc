package orc.trace.query;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

import orc.error.OrcError;
import orc.trace.events.Event;
import orc.trace.handles.Handle;
import orc.trace.handles.HandleInputStream;

public class InputEventStream implements EventStream {
	private HandleInputStream in;
	private Handle<Event> head;
	private InputEventStream tail;
	public InputEventStream(InputStream in) throws IOException {
		this(new HandleInputStream(in));
	}
	private InputEventStream(HandleInputStream in) {
		this.in = in;
	}
	
	private void force() throws EndOfStream {
		if (in == null) return;
		try {
			head = in.readHandle();
			tail = new InputEventStream(in);
			in = null;
		} catch (EOFException e) {
			throw new EndOfStream();
		} catch (IOException e) {
			// FIXME: is there a better way to handle this?
			throw new OrcError(e);
		}
	}
	
	public Event head() throws EndOfStream {
		force();
		return head.get();
	}
	
	public InputEventStream tail() throws EndOfStream {
		force();
		return tail;
	}
}
