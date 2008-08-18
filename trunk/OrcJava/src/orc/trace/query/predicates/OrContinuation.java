/**
 * 
 */
package orc.trace.query.predicates;

public class OrContinuation implements Continuation {
	private final Continuation left;
	private final Continuation right;
	public OrContinuation(Continuation left, Continuation right) {
		this.left = left;
		this.right = right;
	}
	public Result evaluate() {
		Result r1 = left.evaluate();
		if (r1 == Result.NO) return right.evaluate();
		else return new Result(r1.frame, new OrContinuation(r1.failure, right));
	}
}