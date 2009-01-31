package orc.type;

import java.util.LinkedList;
import java.util.List;

import orc.env.Env;
import orc.error.compiletime.typing.SubtypeFailureException;
import orc.error.compiletime.typing.TypeException;
import orc.type.ground.Top;

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
	
	public Variance findVariance(Integer var) {
		return (var == index ? Variance.COVARIANT : Variance.CONSTANT);
	}
	
	public Type promote(Env<Boolean> V) { 
		return (V.lookup(index) ? Type.TOP : this);
	}
	
	public Type demote(Env<Boolean> V) { 
		return (V.lookup(index) ? Type.BOT : this);
	}
	
	public void addConstraints(Env<Boolean> VX, Type T, Constraint[] C) throws TypeException {
		
		/* If this variable is in X */
		if (!VX.lookup(index)) {
			
			// Find Z, the index of this variable in the outer context
			int Z = index - VX.search(false);
			
			/* Demote this type to remove the variables in V,
			 * and then add it as an upper bound of Z.
			 */
			C[Z].atMost(this.demote(VX));
			return;
		}
		/* If this variable is not in X,
		 * then T must be identical to this variable.
		 */
		else {
			if (!equal(T)) {
				throw new SubtypeFailureException(this, T);
			}
		}
		
	}
	
	
	public String toString() {
		return "#" + index;
	}
	
}
