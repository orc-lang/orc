package orc.runtime.nodes.result;

import java.util.concurrent.BlockingQueue;

import orc.runtime.values.Value;

/**
 * A special node that adds its output values to the given value queue.
 * Equivalent to (where Q is the output channel):
 * <pre>
 *    P >x> Q.put(x)
 * </pre>
 * @author dkitchin
 */
public class QueueResult extends Result {

	BlockingQueue<Value> q;
	
	public QueueResult(BlockingQueue<Value> q) {
		this.q = q;
	}
	
	@Override
	public void emit(Value v) {
		q.add(v);
	}

}
