package orc.trace.query.predicates;

import java.util.NoSuchElementException;

import orc.trace.query.Frame;
import orc.trace.query.Term;

public class NextEventPredicate implements Predicate {
	public static NextEventPredicate singleton = new NextEventPredicate();
	private NextEventPredicate() {}
	public Result evaluate(Frame frame) {
		try {
			return new Result(frame.nextEvent());
		} catch (NoSuchElementException e) {
			return null;
		}
	}
}
