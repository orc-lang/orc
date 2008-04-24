package orc.runtime.nodes.result;

import java.io.IOException;
import java.io.ObjectOutputStream;

import orc.runtime.values.Value;

/**
 * A special node that writes its output values on the given ObjectOutputStream.
 * Equivalent to (where F is the output file)
 * <pre>
 *    P >x> F.write(x)
 * </pre>
 * @author mbickford, dkitchin
 */
public class WriteResult extends Result {

	ObjectOutputStream out;
	
	public WriteResult(ObjectOutputStream out){
		this.out = out;
	}
	
	@Override
	public void emit(Value v) {
		try {
			out.writeObject(v);
		} catch (IOException e) {
			System.out.println("Couldn't write a value to the output file.");
			e.printStackTrace();
		}

	}

}
