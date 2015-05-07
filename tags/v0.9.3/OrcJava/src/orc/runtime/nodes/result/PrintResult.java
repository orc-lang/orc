package orc.runtime.nodes.result;

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
	public void emit(Object v) {
		System.out.println(v.toString());
		System.out.flush();
	}

}
