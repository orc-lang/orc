/**
 * 
 */
package orc.trace.query.predicates;


public class AndContinuation implements Continuation {
	private final Continuation left;
	private final Predicate right;
	public AndContinuation(final Continuation before, final Predicate after) {
		this.left = before;
		this.right = after;
	}
	public Result evaluate() {
		Result r1 = left.evaluate();
		if (r1 == Result.NO) return Result.NO;
		Result r2 = right.evaluate(r1.frame);
		Continuation failure = new AndContinuation(r1.failure, right);
		if (r2 == Result.NO) return failure.evaluate();
		return new Result(r2.frame, new OrContinuation(r2.failure, failure));
	}
}