package orc.trace.query.predicates;

import orc.trace.query.Frame;
import orc.trace.query.patterns.BindingPattern;

public class InstanceOfPredicate implements Predicate {
	private final BindingPattern variable;
	private final Class class_;
	public InstanceOfPredicate(final BindingPattern variable, final Class class_) {
		this.variable = variable;
		this.class_ = class_;
	}
	public Result evaluate(Frame frame) {
		if (class_.isAssignableFrom(variable.evaluate(frame).getClass())) {
			return new Result(frame);
		} else return Result.NO;
	}
}