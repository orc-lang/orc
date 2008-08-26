package orc.ford;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import orc.error.Located;
import orc.error.SourceLocation;
import orc.trace.EventCursor;
import orc.trace.EventCursor.EndOfStream;
import orc.trace.events.*;

public class Debugger {
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
		public boolean isBlocked() {
			return lastEvent instanceof BlockEvent;
		}
	}
	
	private interface SourceFile {
		public void printLocation(Writer out, SourceLocation location) throws IOException;
	}
	
	private static class UnavailableSourceFile implements SourceFile {
		public void printLocation(Writer out, SourceLocation location) throws IOException {
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
			if (location == SourceLocation.UNKNOWN) return;
			if (location.line == location.endLine) {
				printMarkedLine(out, location.line, location.column, location.endColumn);
			} else {
				printMarkedLine(out, location.line, location.column, 0);
				for (int line = location.line+1; line < location.endLine; ++line) {
					out.write(lines.get(line-1));
					out.write("\n");
				}
				printMarkedLine(out, location.line, 0, location.endColumn);
			}
		}
		private void printMarkedLine(Writer out, int line, int start, int end) throws IOException {
			assert(start != 0 || end != 0);
			String text = lines.get(line-1);
			if (start != 0) {
				out.write(text.substring(0, start-1));
				out.write("{");
			}
			if (end != 0) {
				out.write(text.substring(start, end-1));
				out.write("}");
				out.write(text.substring(end));
			} else {
				out.write(text.substring(start));
			}
			out.write("\n");
		}
	}
	
	private Map<ForkEvent, ThreadState> threadStates = new TreeMap<ForkEvent, ThreadState>(); 
	private List<ForkEvent> threads = new LinkedList<ForkEvent>();
	private EventCursor cursor;
	private Map<File, SourceFile> sourceFiles = new TreeMap<File, SourceFile>();
	private Simulator FORWARD = new ForwardSimulator();
	private Simulator BACKWARD = new BackwardSimulator();
	
	public Debugger(EventCursor cursor) {
		Event current = cursor.current();
		this.cursor = cursor;
		addThread(current.getThread(), current);
		simulate(current, FORWARD);
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
		getSourceFile(location.file).printLocation(out, location);
	}
	
	private ThreadState getThreadState(ForkEvent thread) {
		return threadStates.get(thread);
	}
	
	private void addThread(ForkEvent thread, Event event) {
		threadStates.put(thread, new ThreadState(event));
		threads.add(thread);
	}
	
	protected void printThreadState(Writer out, ThreadState state) throws IOException {
		printLocation(out, state.getSourceLocation());
		state.lastEvent.prettyPrint(out, 0);
	}
	
	public void forward() throws EndOfStream {
		Event current;
		// cannot move a dead thread forward
		if (getThreadState(threads.get(0)).isDead()) {
			throw new EndOfStream();
		}
		removeDeadThreads();
		do {
			cursor = cursor.forward();
			current = cursor.current();
		} while (simulate(current, FORWARD));
	}
	
	public void backward() throws EndOfStream {
		Event current;
		do {
			cursor = cursor.backward();
			current = cursor.current();
		} while (simulate(current, BACKWARD));
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
	
	private abstract class Simulator implements Visitor<Boolean> {
		public Boolean visit(AfterEvent event) {
			return false;
		}

		public Boolean visit(BeforeEvent event) {
			return true;
		}

		public Boolean visit(BlockEvent event) {
			return false;
		}

		public Boolean visit(ChokeEvent event) {
			return false;
		}

		public Boolean visit(DieEvent event) {
			return false;
		}

		public Boolean visit(ErrorEvent event) {
			return false;
		}

		public Boolean visit(FreeEvent event) {
			return true;
		}

		public Boolean visit(PrintEvent event) {
			return false;
		}

		public Boolean visit(PublishEvent event) {
			return false;
		}

		public Boolean visit(PullEvent event) {
			return true;
		}

		public Boolean visit(ReceiveEvent event) {
			return false;
		}

		public Boolean visit(RootEvent event) {
			throw new AssertionError("Unexpected RootEvent");
		}

		public Boolean visit(SendEvent event) {
			return false;
		}

		public Boolean visit(StoreEvent event) {
			return false;
		}

		public Boolean visit(UnblockEvent event) {
			return false;
		}
	}

	private class ForwardSimulator extends Simulator {
		public Boolean visit(ForkEvent event) {
			if (getThreadState(event.getThread()) != null) {
				addThread(event, event);
			}
			return false;
		}
	}
	
	private class BackwardSimulator extends Simulator {
		public Boolean visit(ForkEvent event) {
			if (getThreadState(event) != null) {
				unforkThread(event);
			}
			return false;
		}
	}
	
	private boolean simulate(Event event, Simulator sim) {
		final ThreadState state = getThreadState(event.getThread());
		boolean skip = event.accept(sim);
		if (state != null) {
			state.lastEvent = event;
		}
		return skip || event.getThread() != threads.get(0);
	}
}
