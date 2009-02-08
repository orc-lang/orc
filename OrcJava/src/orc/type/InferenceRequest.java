package orc.type;

import orc.error.compiletime.typing.TypeException;

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
