package orc.trace;

import java.awt.event.InputEvent;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.NoSuchElementException;

import orc.trace.events.CallEvent;
import orc.trace.events.Event;
import orc.trace.events.ResumeEvent;
import orc.trace.handles.Handle;
import orc.trace.handles.HandleInputStream;
import orc.trace.query.EventStream;
import orc.trace.query.FilteredEventStream;
import orc.trace.query.Frame;
import orc.trace.query.InputEventStream;
import orc.trace.query.patterns.PropertyPattern;
import orc.trace.query.patterns.Variable;
import orc.trace.query.predicates.AndPredicate;
import orc.trace.query.predicates.CurrentEventPredicate;
import orc.trace.query.predicates.EqualPredicate;
import orc.trace.query.predicates.InstanceOfPredicate;
import orc.trace.query.predicates.NotPredicate;
import orc.trace.query.predicates.PlusPredicate;
import orc.trace.query.predicates.Predicate;
import orc.trace.query.predicates.TruePredicate;

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
		Variable callevent = new Variable();
		Predicate callp = AndPredicate.and(
				new CurrentEventPredicate(callevent),
				new InstanceOfPredicate(callevent, CallEvent.class));
		Predicate plusany = new PlusPredicate(TruePredicate.singleton);
		Variable resumeevent = new Variable();
		Predicate resumep = AndPredicate.and(
				new CurrentEventPredicate(resumeevent),
				new EqualPredicate(
						new PropertyPattern(callevent, "thread"),
						new PropertyPattern(resumeevent, "thread")),
				new InstanceOfPredicate(resumeevent, ResumeEvent.class));
		Predicate p = AndPredicate.and(callp, plusany, resumep);
		FilteredEventStream in = new FilteredEventStream(new InputEventStream(in0), p);
		try {
			while (true) {
				Frame frame = in.frame();
				System.out.println(frame.get(callevent));
				System.out.println(frame.get(resumeevent));
				in = in.tail();
			}
		} catch (NoSuchElementException _) {}
	}
}
