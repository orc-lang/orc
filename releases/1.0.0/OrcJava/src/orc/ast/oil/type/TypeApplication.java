package orc.ast.oil.type;

import java.util.LinkedList;
import java.util.List;

import orc.env.SearchFailureException;
import orc.error.OrcError;
import orc.error.compiletime.typing.ArgumentArityException;
import orc.error.compiletime.typing.SubtypeFailureException;
import orc.error.compiletime.typing.TypeException;
import orc.error.compiletime.typing.UnboundTypeException;
import orc.error.compiletime.typing.UncallableTypeException;
import orc.type.TypingContext;

/**
 * A type instantiation with explicit type parameters: T[T,..,T]
 * 
 * @author dkitchin
 *
 */
public class TypeApplication extends Type {

	public Type typeOperator;
	public List<Type> params;
	
	public TypeApplication(Type typeOperator, List<Type> params) {
		this.typeOperator = typeOperator;
		this.params = params;
	}
	
	@Override
	public orc.type.Type transform(TypingContext ctx) throws TypeException {
		return new orc.type.TypeApplication(typeOperator.transform(ctx), Type.transformAll(params, ctx));
	}
	
	
	public String toString() {
		
		StringBuilder s = new StringBuilder();
		
		s.append(typeOperator);
		s.append('[');
		for (int i = 0; i < params.size(); i++) {
			if (i > 0) { s.append(", "); }
			s.append(params.get(i));
		}
		s.append(']');
		
		return s.toString();
	}

	/* (non-Javadoc)
	 * @see orc.ast.oil.type.Type#marshal()
	 */
	@Override
	public orc.ast.xml.type.Type marshal() {
		return new orc.ast.xml.type.TypeApplication(typeOperator.marshal(), Type.marshalAll(params));
	}	
	
}
