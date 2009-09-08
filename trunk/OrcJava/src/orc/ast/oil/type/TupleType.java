package orc.ast.oil.type;

import java.util.LinkedList;
import java.util.List;

import orc.error.compiletime.typing.ArgumentArityException;
import orc.error.compiletime.typing.SubtypeFailureException;
import orc.error.compiletime.typing.TypeException;
import orc.error.compiletime.typing.UncallableTypeException;
import orc.type.TypingContext;

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
	public orc.type.Type transform(TypingContext ctx) throws TypeException {
		List<orc.type.Type> newitems = new LinkedList<orc.type.Type>();
		
		for (Type T : items) {
			newitems.add(T.transform(ctx));
		}
		
		return new orc.type.structured.TupleType(newitems);
	}
	
	public orc.ast.xml.type.Type marshal() {
		return new orc.ast.xml.type.TupleType(Type.marshalAll(items));
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
