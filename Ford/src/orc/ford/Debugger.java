package orc.ford;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import orc.error.Located;
import orc.error.SourceLocation;
import orc.trace.BackwardEventCursor;
import orc.trace.EventCursor;
import orc.trace.InputStreamEventCursor;
import orc.trace.EventCursor.EndOfStream;
import orc.trace.events.AfterEvent;
import orc.trace.events.BeforeEvent;
import orc.trace.events.BlockEvent;
import orc.trace.events.ChokeEvent;
import orc.trace.events.DieEvent;
import orc.trace.events.ErrorEvent;
import orc.trace.events.Event;
import orc.trace.events.ForkEvent;
import orc.trace.events.FreeEvent;
import orc.trace.events.PrintEvent;
import orc.trace.events.PublishEvent;
import orc.trace.events.PullEvent;
import orc.trace.events.ReceiveEvent;
import orc.trace.events.SendEvent;
import orc.trace.events.StoreEvent;
import orc.trace.events.UnblockEvent;
import orc.trace.events.Visitor;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

public class Debugger {
	/**
	 * Track the current state of a thread in the debugger.
	 * Currently this is just the last event seen by the thread.
	 */
	private static class ThreadState implements Located {
		public Event lastEvent;
		public ThreadState(Event lastEvent) {
			this.lastEvent = lastEvent;
		}
		public SourceLocation getSourceLocation() {
			return lastEvent.getSourceLocation();
		}
		public boolean isDead() {
			return lastEvent instanceof DieEvent;
		}
	}
	
	/**
	 * Cached source file.
	 */
	private interface SourceFile {
		/**
		 * Print a location in a source file; this means the lines covered by
		 * the location and some markers to delimit the extent of the location.
		 */
		public void printLocation(Writer out, SourceLocation location) throws IOException;
		public void printShortLocation(Writer out, SourceLocation location) throws IOException;
	}
	
	/**
	 * Used if the source file cannot be loaded.
	 */
	private static class UnavailableSourceFile implements SourceFile {
		public void printLocation(Writer out, SourceLocation location) throws IOException {
			out.write(location.toString());
			out.write("\n");
		}
		public void printShortLocation(Writer out, SourceLocation location) throws IOException {
			out.write(location.toString());
			out.write("\n");
		}
	}
	
	private static class AvailableSourceFile implements SourceFile {
		ArrayList<String> lines = new ArrayList<String>();
		public AvailableSourceFile(File file) throws IOException {
			BufferedReader reader = new BufferedReader(new FileReader(file));
			for (String line = reader.readLine();
				line != null;
				line = reader.readLine())
			{
				lines.add(line);
			}
		}
		public void printLocation(Writer out, SourceLocation location) throws IOException {
			out.write(location.toString());
			out.write("\n");
			int startL = location.line;
			int endL = location.endLine;
			int startC = location.column;
			int endC = location.endColumn;
			if (startL == endL) {
				String text = lines.get(startL-1);
				out.write(text.substring(0, startC-1));
				out.write("{");
				out.write(text.substring(startC-1, endC-1));
				out.write("}");
				out.write(text.substring(endC-1));
			} else {
				String text;
				text = lines.get(startL-1);
				out.write(text.substring(0, startC-1));
				out.write("{");
				out.write(text.substring(startC-1));
				out.write("\n");
				for (int line = startL+1; line < endL; ++line) {
					text = lines.get(line-1);
					out.write(text);
					out.write("\n");
				}
				if (endL > lines.size()) {
					// not sure how this happens, I guess the parser
					// adds a newline at the end of the file which may
					// or may not be actually present
					out.write("}");
				} else {
					text = lines.get(endL-1);
					out.write(text.substring(0, endC-1));
					out.write("}");
					out.write(text.substring(endC-1));
				}
			}
			out.write("\n");
		}
		public void printShortLocation(Writer out, SourceLocation location) throws IOException {
			out.write(location.toString());
			out.write(": ");
			int startL = location.line;
			int endL = location.endLine;
			int startC = location.column;
			int endC = location.endColumn;
			if (startL == endL) {
				String text = lines.get(startL-1);
				out.write(text.substring(0, startC-1));
				out.write("{");
				out.write(text.substring(startC-1, endC-1));
				out.write("}");
				out.write(text.substring(endC-1));
			} else {
				String text;
				text = lines.get(startL-1);
				out.write(text.substring(0, startC-1));
				out.write("{...}");
				if (endL > lines.size()) {
					// not sure how this happens, I guess the parser
					// adds a newline at the end of the file which may
					// or may not be actually present
				} else {
					text = lines.get(endL-1);
					out.write(text.substring(endC-1));
				}
			}
			out.write("\n");
		}
	}
	
	/** Thread states indexed by thread event */
	private Map<ForkEvent, ThreadState> threadStates = new HashMap<ForkEvent, ThreadState>(); 
	/** List of currently debugged threads. The "primary" thread is the first in the list. */
	private List<ForkEvent> threads = new LinkedList<ForkEvent>();
	/** The current event cursor. */
	private EventCursor cursor;
	/** Cached source files. */
	private Map<File, SourceFile> sourceFiles = new HashMap<File, SourceFile>();
	private Simulator FORWARD = new ForwardSimulator();
	private Simulator BACKWARD = new BackwardSimulator();
	
	public Debugger() {}
	
	@Argument(metaVar="file", required=true, usage="Input file. Omit to use STDIN.")
	public void setInputFile(File file) throws CmdLineException {
		try {
			cursor = new BackwardEventCursor(
					new InputStreamEventCursor(
							new FileInputStream(file)));
			Event current = cursor.current();
			addThread((ForkEvent)current, current);
		} catch (FileNotFoundException e) {
			throw new CmdLineException("Could not find input file '"+file+"'");
		} catch (IOException e) {
			throw new CmdLineException(e);
		} catch (ClassCastException e) {
			throw new CmdLineException("That trace file is missing the expected RootEvent");
		}
	}
	
	private SourceFile getSourceFile(File file) {
		SourceFile out = sourceFiles.get(file);
		if (out != null) return out;
		try {
			out = new AvailableSourceFile(file);
		} catch (IOException e) {
			out = new UnavailableSourceFile();
		}
		sourceFiles.put(file, out);
		return out;
	}
	
	private void printLocation(Writer out, SourceLocation location) throws IOException {
		if (location.file == null) {
			out.write(location.toString());
			out.write("\n");
		} else {
			getSourceFile(location.file).printLocation(out, location);
		}
	}
	
	private void printShortLocation(Writer out, SourceLocation location) throws IOException {
		if (location.file == null) {
			out.write(location.toString());
			out.write("\n");
		} else {
			getSourceFile(location.file).printShortLocation(out, location);
		}
	}
	
	private ThreadState getThreadState(ForkEvent thread) {
		return threadStates.get(thread);
	}
	
	private void addThread(ForkEvent thread, Event event) {
		if (threadStates.containsKey(event)) return;
		threadStates.put(thread, new ThreadState(event));
		threads.add(thread);
	}
	
	protected void printPrimaryThreadState(Writer out) throws IOException {
		printLocation(out, cursor.current().getSourceLocation());
		cursor.current().prettyPrint(out, 0);
		out.write("\n");
	}
	
	protected void printThreadState(Writer out, ThreadState state) throws IOException {
		printShortLocation(out, state.getSourceLocation());
		state.lastEvent.prettyPrint(out, 0);
		out.write("\n");
	}
	
	public void print() throws IOException {
		Writer out = new OutputStreamWriter(System.out);
		String sep = "-------------------------------------------------\n";
		Iterator<ForkEvent> threadsi = threads.iterator();
		out.write(threadsi.next().toString());
		out.write(": ");
		printPrimaryThreadState(out);
		int i = 1;
		while (threadsi.hasNext()) {
			ForkEvent thread = threadsi.next();
			out.write(sep);
			out.write(Integer.toString(i) + ". ");
			out.write(thread.toString());
			out.write(": ");
			printThreadState(out, getThreadState(thread));
			++i;
		}
		out.write(sep);
		out.flush();
	}
	
	public void forward() throws EndOfStream {
		// cannot move a dead thread forward
		if (cursor.current().getThread() == threads.get(0)
				&& cursor.current() instanceof DieEvent) {
			throw new EndOfStream();
		}
		removeDeadThreads();
		do {
			simulate(cursor.current(), FORWARD);
			cursor = cursor.forward();
		} while (isBoring(cursor.current()));
	}
	
	public void backward() throws EndOfStream {
		do {
			cursor = cursor.backward();
			simulate(cursor.current(), BACKWARD);
		} while (isBoring(cursor.current()));
	}
	
	public void removeThread(int index) {
		threadStates.remove(threads.remove(index));
	}
	
	public void swapThreads(int index1, int index2) {
		if (index1 == index2) return;
		if (index1 > index2) {
			ForkEvent thread1 = threads.remove(index1);
			ForkEvent thread2 = threads.remove(index2);
			threads.add(index2, thread1);
			threads.add(index1, thread2);
		} else {
			ForkEvent thread2 = threads.remove(index2);
			ForkEvent thread1 = threads.remove(index1);
			threads.add(index1, thread2);
			threads.add(index2, thread1);
		}
	}
	
	public void unforkThread(ForkEvent thread) {
		int index = threads.indexOf(thread);
		assert(index >= 0);
		removeThread(index);
		if (getThreadState(thread.getThread()) == null) {
			threads.add(index, thread.getThread());
			threadStates.put(thread.getThread(), new ThreadState(thread));
		}
	}
	
	private void removeDeadThreads() {
		Iterator<ForkEvent> threadsi = threads.iterator();
		while (threadsi.hasNext()) {
			ForkEvent thread = threadsi.next();
			if (getThreadState(thread).isDead()) {
				threadsi.remove();
				threadStates.remove(thread);
			}
		}
	}
	
	/**
	 * Returns true if an event should be skipped.
	 */
	private static final Visitor<Boolean> SKIP = new Visitor<Boolean>() {
		public Boolean visit(AfterEvent event) { return false; }
		public Boolean visit(BeforeEvent event) { return true; }
		public Boolean visit(BlockEvent event) { return false; }
		public Boolean visit(ChokeEvent event) { return false; }
		public Boolean visit(DieEvent event) { return false; }
		public Boolean visit(ErrorEvent event) { return false; }
		public Boolean visit(ForkEvent event) { return false; }
		public Boolean visit(FreeEvent event) { return true; }
		public Boolean visit(PrintEvent event) { return false; }
		public Boolean visit(PublishEvent event) { return false; }
		public Boolean visit(PullEvent event) { return true; }
		public Boolean visit(ReceiveEvent event) { return false; }
		public Boolean visit(SendEvent event) { return false; }
		public Boolean visit(StoreEvent event) { return false; }
		public Boolean visit(UnblockEvent event) { return false; }
	};
	
	/**
	 * Simulate an event either forwards or backwards.
	 * Most events have no effect.
	 */
	private abstract class Simulator implements Visitor<Void> {
		public Void visit(AfterEvent event) { return null; }
		public Void visit(BeforeEvent event) { return null; }
		public Void visit(BlockEvent event) { return null; }
		public Void visit(ChokeEvent event) { return null; }
		public Void visit(DieEvent event) { return null; }
		public Void visit(ErrorEvent event) { return null; }
		public Void visit(FreeEvent event) { return null; }
		public Void visit(PrintEvent event) { return null; }
		public Void visit(PublishEvent event) { return null; }
		public Void visit(PullEvent event) { return null; }
		public Void visit(ReceiveEvent event) { return null; }
		public Void visit(SendEvent event) { return null; }
		public Void visit(StoreEvent event) { return null; }
		public Void visit(UnblockEvent event) { return null; }
	}

	private class ForwardSimulator extends Simulator {
		public Void visit(ForkEvent event) {
			if (getThreadState(event.getThread()) != null) {
				addThread(event, event);
			}
			return null;
		}
	}
	
	private class BackwardSimulator extends Simulator {
		public Void visit(ForkEvent event) {
			if (getThreadState(event) != null) {
				unforkThread(event);
			}
			return null;
		}
	}
	
	/**
	 * Return true if this event should be skipped when stepping.
	 */
	private boolean isBoring(Event event) {
		return event.accept(SKIP) || event.getThread() != threads.get(0);
	}
	
	/**
	 * Simulate an event and return false when simulation
	 * should stop.
	 */
	private void simulate(Event event, Simulator sim) {
		event.accept(sim);
		ThreadState state = getThreadState(event.getThread());
		if (state != null) {
			state.lastEvent = event;
		}
	}

	@Option(name="-help",usage="Show command-line argument usage")
	public void printUsage(boolean _) throws CmdLineException{
		throw new CmdLineException("");
	}
	
	public void processArgs(String[] args) {
		CmdLineParser parser = new CmdLineParser(this); 
		try {
			parser.parseArgument(args);
		} catch (CmdLineException e1) {
			System.err.println(e1.getMessage());
			System.err.println("Usage: java -cp orc.jar " +
					getClass().getName() +" [options] [file]");
			parser.printUsage(System.err);
			System.exit(1);
		}
	}
	
	public void run() throws IOException {
		BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
		print();
		loop: while (true) {
			System.out.print("> ");
			String command = r.readLine();
			if (command == null) break loop;
			if (command.length() == 0) command = "?";
			switch (command.charAt(0)) {
			case 'q':
				break loop;
			case 'p':
				print();
				break;
			case 'f':
				try {
					forward();
					print();
				} catch (EndOfStream e) {
					System.err.println("No more events.");
				}
				break;
			case 'b':
				try {
					backward();
					print();
				} catch (EndOfStream e) {
					System.err.println("No more events.");
				}
				break;
			case 't':
				try {
					int index = Integer.parseInt(command.substring(2));
					if (index >= threads.size()) {
						System.err.println("Bad index " + index);
					} else {
						swapThreads(0, index);
						forward();
						print();
					}
				} catch (NumberFormatException e) {
					System.err.println("Malformed index '" + command.substring(2));
				} catch (EndOfStream e) {
					System.err.println("No more events.");
				}
				break;
			default:
				System.out.println("Please enter one of the following commands:");
				System.out.println("q: quit");
				System.out.println("f: step forward");
				System.out.println("b: step backward");
				System.out.println("p: print current state");
				System.out.println("t N: switch to thread N");
				break;
			}
		}
	}
	
	public static void main(String[] args) throws IOException {
		Debugger d = new Debugger();
		d.processArgs(args);
		d.run();
	}
}
