package orc.ast.extended.type;

import java.util.LinkedList;
import java.util.List;

import orc.ast.simple.type.FreeTypeVariable;
import orc.env.SearchFailureException;
import orc.error.compiletime.typing.ArgumentArityException;
import orc.error.compiletime.typing.SubtypeFailureException;
import orc.error.compiletime.typing.UnboundTypeException;
import orc.error.compiletime.typing.UncallableTypeException;
import orc.type.TypeVariable;

/**
 * A type instantiation with explicit type parameters: T[T,..,T]
 * 
 * @author dkitchin
 *
 */
public class TypeApplication extends Type {

	public String name;
	public List<Type> params;
	
	public TypeApplication(String name, List<Type> params) {
		this.name = name;
		this.params = params;
	}
	
	@Override
	public orc.ast.simple.type.Type simplify() {
		 
		List<orc.ast.simple.type.Type> ts = new LinkedList<orc.ast.simple.type.Type>();
		for (Type t : params) {
			ts.add(t.simplify());
		}
				
		return new orc.ast.simple.type.TypeApplication(new FreeTypeVariable(name), ts);
	}
	
	
	public String toString() {
		
		StringBuilder s = new StringBuilder();
		
		s.append(name);
		s.append('[');
		for (int i = 0; i < params.size(); i++) {
			if (i > 0) { s.append(", "); }
			s.append(params.get(i));
		}
		s.append(']');
		
		return s.toString();
	}	
	
}
