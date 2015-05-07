package orc.type;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import orc.ast.oil.arg.Arg;
import orc.env.Env;
import orc.env.EnvException;
import orc.env.LookupFailureException;
import orc.error.OrcError;
import orc.error.compiletime.typing.ArgumentArityException;
import orc.error.compiletime.typing.SubtypeFailureException;
import orc.error.compiletime.typing.TypeArityException;
import orc.error.compiletime.typing.TypeException;
import orc.error.compiletime.typing.UncallableTypeException;
import orc.type.ground.BooleanType;
import orc.type.ground.Bot;
import orc.type.ground.IntegerType;
import orc.type.ground.LetType;
import orc.type.ground.NumberType;
import orc.type.ground.StringType;
import orc.type.ground.Top;

/**
 * 
 * Abstract superclass of all types for the Orc typechecker.
 * 
 * This typechecker is based on the local type inference algorithms
 * described by Benjamin Pierce and David Turner in their paper
 * entitled "Local Type Inference".
 * 
 * It extends that inference strategy with tuples, library-defined 
 * type constructors, user-defined datatypes, variance annotations, 
 * and other small changes.
 * 
 * @author dkitchin
 *
 */

public abstract class Type {

	/* Create singleton representatives for some common types */
	public static final Type TOP = new Top();
	public static final Type BOT = new Bot();
	public static final Type NUMBER = new NumberType();
	public static final Type STRING = new StringType();
	public static final Type BOOLEAN = new BooleanType();
	public static final Type INTEGER = new IntegerType();
	public static final Type LET = new LetType();
	
	/* Placeholder for an unspecified type */
	public static final Type BLANK = null;

	
	/* Check if a type is Top */
	public boolean isTop() {
		return false;
	}
	
	/* Check if a type is Bot */
	public boolean isBot() {
		return false;
	}
	
	
	/* We use the Java inheritance hierarchy as a default */
	/* We also require the two types to have the same kind */
	public boolean subtype(Type that) {
		
		return that.isTop() || 
			(that.getClass().isAssignableFrom(this.getClass())
				&& this.sameVariances(that));
	}

	public void assertSubtype(Type that) throws SubtypeFailureException {
		if (!this.subtype(that)) {
			throw new SubtypeFailureException(this, that);
		}
	}
	
	public boolean supertype(Type that) {
		return that.subtype(this);
	}
	
	/* By default, equality is based on mutual subtyping.
	 * TODO: This may not be correct in the presence of bounded quantification.
	 */
	public boolean equal(Type that) {
		return this.subtype(that) && that.subtype(this);
	}
	
	/* Find the join (least upper bound) in the subtype lattice
	 * of this type and another type.
	 */
	public Type join(Type that) {		
		if (this.subtype(that)) {
			return that;
		}
		else if (that.subtype(this)) {
			return this;
		}
		else {
			return TOP;
		}
	}
	
	/* Find the meet (greatest lower bound) in the subtype lattice
	 * of this type and another type.
	 */
	public Type meet(Type that) {
		if (this.subtype(that)) {
			return this;
		}
		else if (that.subtype(this)) {
			return that;
		}
		else {
			return BOT;
		}
		
	}
	
	/* By default, try to synthesize the argument types
	 * and call a different version of this method using
	 * just those types. Assert zero type arity.
	 * 
	 * This may be overridden by types which want to handle calls
	 * in their own way (for example, arrow types).
	 */
	public Type call(Env<Type> ctx, Env<Type> typectx, List<Arg> args, List<Type> typeActuals) throws TypeException {
		
		/* Default type arity is 0 */
		if (typeActuals != null && typeActuals.size() > 0) {
			throw new TypeArityException(0, typeActuals.size());
		}
		
		/* Synthesize the argument types */
		List<Type> argTypes = new LinkedList<Type>();
		for (Arg a : args) {
			argTypes.add(a.typesynth(ctx, typectx));
		}
		
		/* Attempt to use the simplified call form */
		return call(argTypes);
	}
	
	/* 
	 * Types which use normal argument synthesis, and have no type parameters,
	 * will usually override this simpler method instead.
	 * 
	 * By default, a type is not callable. 
	 */
	public Type call(List<Type> args) throws TypeException {
		throw new UncallableTypeException(this);
	}
	
	
	/* By default, use the class name as the type's string representation */
	public String toString() {
		return this.getClass().toString();
	}
	
	
	/* Replace all free type variables with the types given for them in the context.
	 * 
	 * By default, perform no substitutions.
	 * Types which contain variables or other types will override this.
	 */
	public Type subst(Env<Type> ctx) {
		return this;
	}
	
	/* Perform substitution on a list of types */
	public static List<Type> substAll(List<Type> ts, Env<Type> ctx) {
		
		List<Type> newts = new LinkedList<Type>();
		
		for (Type t : ts) {
			newts.add(t.subst(ctx));
		}
		
		return newts;
	}
	
	
	
	/* Type operators */
	
	/* Note that this checker does not separately check kinding; type operators
	 * are in the space of types, distinguished only by overriding
	 * the variances method to return a nonempty type parameter list.
	 */
	
	// TODO: Migrate variances method to Tycon, fix TypeApplication accordingly

	/* Get the variances of each type parameters for this type (by default, an empty list) */
	public List<Variance> variances() {
		return new LinkedList<Variance>();
	}
	
	/* Check that this type has the same variances as another type */
	private boolean sameVariances(Type that) {
		
		List<Variance> thisVariances = this.variances();
		List<Variance> thatVariances = that.variances();
		
		for (int i = 0; i < thisVariances.size(); i++) {
			if (!thisVariances.get(i).equals(thatVariances.get(i))) {
				return false;
			}
		}
		
		return true;
	}
	
	/* Make a callable instance of this type */
	/* By default, types do not have callable instances */
	public Type makeCallableInstance(List<Type> params) throws TypeException {
		throw new TypeException("Cannot create a callable instance of type " + this);
	}
	
	/* Make sure that this type is an application of the given type
	 * (or some subtype) to exactly one type parameter. If so, return the parameter, and
	 * if not raise an error.
	 */
	public Type unwrapAs(Type T) throws TypeException {
		throw new SubtypeFailureException(T, this);
	}
	
	
	// INFERENCE AND CONSTRAINT SATISFACTION
	
	
	/* Find the set of free type variable indices in this type */
	public Set<Integer> freeVars() {
		return new TreeSet<Integer>();
	}
	
	/* Determine whether this is a closed type (i.e. it has no free type variables) */
	public boolean closed() {
		return freeVars().isEmpty();
	}
	

	/* Find the variance of the given variable in this type.
	 * 
	 * The default implementation assumes that the variable does not occur
	 * and thus reports constant variance.
	 */
	public Variance findVariance(Integer var) {
		return Variance.CONSTANT;
	}

	/*
	 * Promote this type until all occurrences of the given variables
	 * have been eliminated.
	 * 
	 * A set of type variables is implemented here by an Env, where
	 * true indicates membership and false indicates non-membership.
	 * 
	 * The default implementation assumes none of the variables occur
	 * and returns the type unchanged.
	 */
	public Type promote(Env<Boolean> V) throws TypeException { return this; }

	/*
	 * Demote this type until all occurrences of the given variables
	 * have been eliminated.
	 * 
	 * A set of type variables is implemented here by an Env, where
	 * true indicates membership and false indicates non-membership.
	 * 
	 * The default implementation assumes none of the variables occur
	 * and returns the type unchanged.
	 */
	public Type demote(Env<Boolean> V) throws TypeException { return this; }
	
	
	/*
	 * Add all constraints imposed by the relation this <: T to the
	 * set of constraints C.
	 * 
	 * The environment VX subsumes the sets V and X in Pierce and
	 * Turner's original algorithm. Variables in V map to true,
	 * whereas variables in X map to false. This allows VX to be
	 * used as V in the promote/demote methods without modification.
	 * 
	 * This could also be implemented functionally, but the imperative
	 * solution is simpler in this setting.
	 * 
	 */
	public void addConstraints(Env<Boolean> VX, Type T, Constraint[] C) throws TypeException {
		
		try {
		if (T instanceof TypeVariable) {
			int Y = ((TypeVariable)T).index;
		
			/* If Y is in X */
			try {
			if (!VX.lookup(Y)) {
				
				// Find Z, the index of Y in the outer context
				int Z = Y - VX.search(false);
				
				/* Promote this type to remove the variables in V,
				 * and then add it as a lower bound of Z.
				 */
				C[Z].atLeast(this.promote(VX));
				return;
			}
			}
			catch (LookupFailureException e) {
				/* This variable is bound further out than X; it is not in X */
				/* This occurs when checking under a type binder */
				// TODO: Add bounded polymorphism support.		
			}
			/* The subtype assertion below will take care of these cases */
		}
		
		/* This is just a direct subtype assertion,
		 * and generates no constraints. 
		 */
		if (!this.subtype(T)) {
			throw new SubtypeFailureException(this, T);
		}
		
		}
		catch (EnvException e) {
			throw new OrcError(e);
		}
	}
	
}
