package orc.ast.extended.type;

import java.util.LinkedList;
import java.util.List;

import orc.error.compiletime.typing.ArgumentArityException;
import orc.error.compiletime.typing.SubtypeFailureException;
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
	public orc.ast.simple.type.Type simplify() {
		
		List<orc.ast.simple.type.Type> newitems = new LinkedList<orc.ast.simple.type.Type>();
		for (Type T : items) {
			newitems.add(T.simplify());
		}
		
		return new orc.ast.simple.type.TupleType(newitems);
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
