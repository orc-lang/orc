package orc.ast.oil.type;

import java.util.LinkedList;
import java.util.List;

import orc.error.compiletime.typing.ArgumentArityException;
import orc.error.compiletime.typing.SubtypeFailureException;
import orc.error.compiletime.typing.TypeException;
import orc.error.compiletime.typing.UncallableTypeException;
import orc.type.TypingContext;

/**
 * A syntactic container for a type inferred during typechecking.
 * Such types are temporary, and cannot be marshalled to XML;
 * they marshal to null, since they were originally null before
 * being inferred.
 * 
 * @author dkitchin
 *
 */
public class InferredType extends Type {

	public orc.type.Type inferredType;
	
	public InferredType(orc.type.Type inferredType) {
		this.inferredType = inferredType;
	}
	
	@Override
	public orc.type.Type transform(TypingContext ctx) throws TypeException {
		return inferredType;
	}
	
	public orc.ast.xml.type.Type marshal() {
		return null;
	}
	
	
	public String toString() {
		return inferredType.toString() + "{- inferred -}";
	}
	
}
