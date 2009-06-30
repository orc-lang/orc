package orc.type.inference;

import orc.error.compiletime.typing.TypeException;
import orc.type.ArrowType;

/**
 * 
 * A special exception raised and caught within the typechecker to request
 * the inference of missing type parameters on a call.
 * 
 * @author dkitchin
 *
 */
public class InferenceRequest extends TypeException {

	public ArrowType requestedType;
	
	public InferenceRequest(ArrowType requestedType) {
		super("Type parameters missing");
		this.requestedType = requestedType;
	}
}
