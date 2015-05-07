package orc.type.tycon;

import java.util.List;

import orc.error.compiletime.typing.TypeException;
import orc.type.Type;


/**
 * 
 * Variances of type parameters.
 * 
 * @author dkitchin
 *
 */
public abstract class Variance {
	
	
	/* Find the conjunction of these two variances */
	public abstract Variance and(Variance that);
	
	/* Apply this variance to another variance,
	 * i.e. if a variable with that variance occurs in
	 * a context with this variance, what is the variable's
	 * variance in the larger context?
	 */
	public abstract Variance apply(Variance that);
	
	/* Invert this variance */
	public abstract Variance invert();
	
	/* Test whether S is a subtype of T under this variance */
	public abstract boolean subtype(Type S, Type T) throws TypeException;
	
	/* Find the join of S and T under this variance. */
	public abstract Type join(Type S, Type T) throws TypeException;
		
	/* Find the meet of S and T under this variance */
	public abstract Type meet(Type S, Type T) throws TypeException;
	
	/* Find the minimal substitution for X under this variance given S <: X <: T */
	public abstract Type minimum(Type S, Type T) throws TypeException;
	
	public static final Variance CONSTANT = new Constant();
	public static final Variance COVARIANT = new Covariant();
	public static final Variance CONTRAVARIANT = new Contravariant();
	public static final Variance INVARIANT = new Invariant();
	
	public boolean equals(Object that) {
		return this.getClass().equals(that.getClass());
	}

}

final class Constant extends Variance {
	
	public Variance and(Variance that) {
		return that;
	}
	
	public Variance invert() {
		return CONSTANT;
	}
	
	public Variance apply(Variance that) {
		return CONSTANT;
	}
	
	public boolean subtype(Type S, Type T) throws TypeException {
		return true;
	}

	public Type join(Type S, Type T) throws TypeException  {
		/* We could choose any type here, but for convenience we'll just choose S */
		return S;
	}

	public Type meet(Type S, Type T) throws TypeException {
		/* We could choose any type here, but for convenience we'll just choose S */
		return S;
	}

	public Type minimum(Type S, Type T) {
		return S;
	}
}

final class Covariant extends Variance {
	
	public Variance and(Variance that) {
		if (that instanceof Constant || that instanceof Covariant) {
			return this;
		}
		else {
			return INVARIANT;
		}
	}
	
	public Variance invert() {
		return CONTRAVARIANT;
	}
	
	public Variance apply(Variance that) {
		return that;
	}
	
	public boolean subtype(Type S, Type T) throws TypeException {
		return S.subtype(T);
	}
	
	public Type join(Type S, Type T) throws TypeException {
		return S.join(T);
	}

	public Type meet(Type S, Type T) throws TypeException {
		return S.meet(T);
	}
	
	public Type minimum(Type S, Type T) {
		return S;
	}
}

final class Contravariant extends Variance {
	
	public Variance and(Variance that) {
		if (that instanceof Constant || that instanceof Contravariant) {
			return this;
		}
		else {
			return INVARIANT;
		}
	}
	
	public Variance invert() {
		return COVARIANT;
	}
	
	public Variance apply(Variance that) {
		return that.invert();
	}
	
	public boolean subtype(Type S, Type T) throws TypeException {
		return T.subtype(S);
	}
	
	public Type join(Type S, Type T) throws TypeException {
		return S.meet(T);
	}

	public Type meet(Type S, Type T) throws TypeException {
		return S.join(T);
	}
	
	public Type minimum(Type S, Type T) {
		return T;
	}
	
}
final class Invariant extends Variance {
	public Variance and(Variance that) {
		return this;
	}
	
	public Variance invert() {
		return INVARIANT;
	}
	
	public Variance apply(Variance that) {
		return INVARIANT;
	}
	
	public boolean subtype(Type S, Type T) throws TypeException {
		return S.subtype(T) && T.subtype(S);
	}
	
	public Type join(Type S, Type T) throws TypeException {
		if (S.equal(T)) {
			return S;
		}
		else {
			return Type.TOP;
		}
	}

	public Type meet(Type S, Type T) throws TypeException {
		if (S.equal(T)) {
			return S;
		}
		else {
			return Type.BOT;
		}
	}
	
	public Type minimum(Type S, Type T) throws TypeException {
		if (S.equal(T)) {
			return S;
		}
		else {
			throw new TypeException("Couldn't infer type parameters; types " + S + " and " + T + " are not equal.");
		}
	}
}
