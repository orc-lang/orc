package orc.ast.simple.type;

import java.util.LinkedList;
import java.util.List;

import orc.env.Env;
import orc.error.compiletime.typing.ArgumentArityException;
import orc.error.compiletime.typing.SubtypeFailureException;
import orc.error.compiletime.typing.TypeException;
import orc.error.compiletime.typing.UncallableTypeException;

/**
 * A type tuple: (T,...,T)
 * 
 * @author dkitchin
 *
 */
public class TupleType extends Type {

	public List<Type> items;
	
	public TupleType(List<Type> items) {
		this.items = items;
	}
	
	@Override
	public orc.ast.oil.type.Type convert(Env<TypeVariable> env) throws TypeException {
		return new orc.ast.oil.type.TupleType(Type.convertAll(items, env));
	}
	
	/* (non-Javadoc)
	 * @see orc.ast.simple.type.Type#subst(orc.ast.simple.type.Type, orc.ast.simple.type.FreeTypeVariable)
	 */
	@Override
	public Type subst(Type T, FreeTypeVariable X) {
		return new TupleType(Type.substAll(items, T, X));
	}
	
	
	public String toString() {
		
		StringBuilder s = new StringBuilder();
		
		s.append('(');
		for (int i = 0; i < items.size(); i++) {
			if (i > 0) { s.append(", "); }
			s.append(items.get(i));
		}
		s.append(')');
		
		return s.toString();
	}	
	
}
