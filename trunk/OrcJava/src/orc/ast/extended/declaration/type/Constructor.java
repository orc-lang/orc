package orc.ast.extended.declaration.type;

import java.util.List;

import orc.ast.extended.Expression;
import orc.type.Type;


/**
 * 
 * A single constructor in a variant type.
 * 
 * @author dkitchin
 *
 */
public class Constructor {

	public String name;
	public List<Type> args;
	
	public Constructor(String name, List<Type> args) {
		this.name = name;
		this.args = args;
	}
	
	public String toString() {
		return name + "(" + Expression.join(args, ",") + ")";
	}
	
}
