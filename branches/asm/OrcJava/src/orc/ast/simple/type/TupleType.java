package orc.ast.simple.type;

import java.util.LinkedList;
import java.util.List;

import orc.env.Env;
import orc.error.compiletime.typing.ArgumentArityException;
import orc.error.compiletime.typing.SubtypeFailureException;
import orc.error.compiletime.typing.TypeException;
import orc.error.compiletime.typing.UncallableTypeException;

/**
 * A syntactic type tuple: (T,...,T)
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
	public orc.type.Type convert(Env<String> env) throws TypeException {
		List<orc.type.Type> newitems = new LinkedList<orc.type.Type>();
		
		for (Type T : items) {
			newitems.add(T.convert(env));
		}
		
		return new orc.type.TupleType(newitems);
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
