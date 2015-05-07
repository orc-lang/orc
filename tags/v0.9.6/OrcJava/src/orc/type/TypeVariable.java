package orc.type;

import java.util.LinkedList;
import java.util.List;

import orc.env.Env;
import orc.env.EnvException;
import orc.env.LookupFailureException;
import orc.env.SearchFailureException;
import orc.error.OrcError;
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
		
		Type t;
		
		try {
			t = ctx.lookup(index);
		} catch (LookupFailureException e) {
			throw new OrcError(e);
		}
		
		/* If t is null, then this is a bound variable and should not be replaced */
		return (t != null ? t : this);
	}
	
	public Variance findVariance(Integer var) {
		return (var == index ? Variance.COVARIANT : Variance.CONSTANT);
	}
	
	public Type promote(Env<Boolean> V) { 
		try {
			return (V.lookup(index) ? Type.TOP : this);
		} catch (LookupFailureException e) {
			return this;
		}
	}
	
	public Type demote(Env<Boolean> V) { 
		try {
			return (V.lookup(index) ? Type.BOT : this);
		} catch (LookupFailureException e) {
			return this;
		}
	}
	
	public void addConstraints(Env<Boolean> VX, Type T, Constraint[] C) throws TypeException {
		
		
		try {
		if (!VX.lookup(index)) {
			/* this is in X */
			
			// Find Z, the index of this variable in the outer context
			int Z;
			try {
			 	Z = index - VX.search(false);
			}
			catch (SearchFailureException e) {
				throw new OrcError(e);
			}
			
			/* Demote the type to remove the variables in V,
			 * and then add it as an upper bound of Z.
			 */
			C[Z].atMost(T.demote(VX));
			return;
		}
		else {
			/* this is in V */
			super.addConstraints(VX, T, C);
		}
		}
		/* It is also possible that this variable is not in V or X;
		 * it is a bound type variable from an enclosing scope.
		 * In this case, just treat it as an opaque type.
		 */
		catch (LookupFailureException e) {
			/* this is bound outside of V or X */
			/* This occurs when checking under a type binder */
			// TODO: Add bounded polymorphism support.
			super.addConstraints(VX, T, C);
		}
	}
	
	
	public String toString() {
		return "#" + index;
	}
	
}
