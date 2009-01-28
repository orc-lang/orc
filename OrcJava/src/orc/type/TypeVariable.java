package orc.type;

import java.util.LinkedList;
import java.util.List;

import orc.env.Env;

/**
 * A bound type variable.
 * 
 * Subtype comparisons may occur between types with bound variables (such as between
 * the type bodies of parametrized arrow types), so there is a subtype relation
 * specified for type variables: it is simply variable equality.
 * 
 * @author dkitchin
 */
public class TypeVariable extends Type {
	
	public int index;
	
	public TypeVariable(int index) {
		this.index = index;
	}

	public boolean subtype(Type that) {	
		
		if (that instanceof TypeVariable) {
			return this.index == ((TypeVariable)that).index;
		}
		else {
			return (that instanceof Top);
		}
	}

	public Type subst(Env<Type> ctx) {
		
		/* If t is null, then this is a bound variable and should not be replaced */
		Type t = ctx.lookup(index);
		
		return (t != null ? t : this);
	}
	
}
