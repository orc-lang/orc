package orc.trace.query.predicates;

import java.util.NoSuchElementException;

import orc.trace.query.Frame;
import orc.trace.query.Term;

public class CurrentEventPredicate implements Predicate {
	private Term term;
	public CurrentEventPredicate(Term term) {
		this.term = term;
	}
	public Result evaluate(Frame frame) {
		try {
			frame =  term.unify(frame, frame.currentEvent());
			if (frame == null) return Result.NO;
			else return new Result(frame);
		} catch (NoSuchElementException e) {
			return Result.NO;
		}
	}
}
