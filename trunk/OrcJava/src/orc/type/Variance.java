package orc.type;

import java.util.List;


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
	
	/* Find the inverse of this variance */
	public abstract Variance invert();
	
	/* Test whether S is a subtype of T under this variance */
	public abstract boolean subtype(Type S, Type T);
	
	/* Find the join of S and T under this variance. */
	public abstract Type join(Type S, Type T);
		
	/* Find the meet of S and T under this variance */
	public abstract Type meet(Type S, Type T);
	
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
	
	public boolean subtype(Type S, Type T) {
		return true;
	}

	public Type join(Type S, Type T) {
		/* We could choose any type here, but for convenience we'll choose the actual join */
		return S.join(T);
	}

	public Type meet(Type S, Type T) {
		/* We could choose any type here, but for convenience we'll choose the actual meet */
		return S.meet(T);
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
	
	public boolean subtype(Type S, Type T) {
		return S.subtype(T);
	}
	
	public Type join(Type S, Type T) {
		return S.join(T);
	}

	public Type meet(Type S, Type T) {
		return S.meet(T);
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
	
	public boolean subtype(Type S, Type T) {
		return T.subtype(S);
	}
	
	public Type join(Type S, Type T) {
		return S.meet(T);
	}

	public Type meet(Type S, Type T) {
		return S.join(T);
	}
	
}
final class Invariant extends Variance {
	public Variance and(Variance that) {
		return this;
	}
	
	public Variance invert() {
		return INVARIANT;
	}
	
	public boolean subtype(Type S, Type T) {
		return S.subtype(T) && T.subtype(S);
	}
	
	public Type join(Type S, Type T) {
		if (S.equal(T)) {
			return S;
		}
		else {
			return Type.TOP;
		}
	}

	public Type meet(Type S, Type T) {
		if (S.equal(T)) {
			return S;
		}
		else {
			return Type.BOT;
		}
	}
}
