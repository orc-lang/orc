package orc.runtime.nodes.result;

import java.util.concurrent.BlockingQueue;

/**
 * A special node that adds its output values to the given value queue.
 * Equivalent to (where Q is the output channel):
 * <pre>
 *    P >x> Q.put(x)
 * </pre>
 * @author dkitchin
 */
public class QueueResult extends Result {

	BlockingQueue<Object> q;
	
	public QueueResult(BlockingQueue<Object> q) {
		this.q = q;
	}
	
	@Override
	public void emit(Object v) {
		q.add(v);
	}

}
