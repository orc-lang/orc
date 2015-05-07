package orc.runtime.nodes.result;

import orc.runtime.values.Value;

/**
 * A special node that prints its output.
 * Equivalent to
 * <pre>
 *    P >x> println(x)
 * </pre>
 * @author wcook, dkitchin
 */
public class PrintResult extends Result {

	@Override
	public void emit(Value v) {
		System.out.println(v.toString());
		System.out.flush();
	}

}
