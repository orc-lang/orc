package orc.trace;

import java.io.EOFException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;

import orc.trace.events.Event;
import orc.trace.handles.Handle;
import orc.trace.handles.HandleInputStream;

public class DumpTrace {
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		InputStream in0;
		if (args.length > 0) {
			in0 = new FileInputStream(args[0].toString());
		} else {
			in0 = System.in;
		}
		HandleInputStream in = new HandleInputStream(in0);
		Handle<Event> event;
		PrintWriter out = new PrintWriter(System.out);
		try {
			while (true) {
				event = in.readHandle();
				event.get().prettyPrint(out);
				out.println();
				out.flush();
			}
		} catch (EOFException _) {}
		out.close();
	}
}
