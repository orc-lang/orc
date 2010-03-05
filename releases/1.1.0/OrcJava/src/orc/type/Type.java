//
// Type.java -- Java class Type
// Project OrcJava
//
// $Id$
//
// Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.type;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.WildcardType;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import orc.Config;
import orc.ast.oil.expression.argument.Argument;
import orc.env.Env;
import orc.env.EnvException;
import orc.env.LookupFailureException;
import orc.error.OrcError;
import orc.error.compiletime.typing.MissingTypeException;
import orc.error.compiletime.typing.SubtypeFailureException;
import orc.error.compiletime.typing.TypeArityException;
import orc.error.compiletime.typing.TypeException;
import orc.error.compiletime.typing.UncallableTypeException;
import orc.error.compiletime.typing.UnrepresentableTypeException;
import orc.lib.state.types.ArrayType;
import orc.type.ground.BooleanType;
import orc.type.ground.Bot;
import orc.type.ground.IntegerType;
import orc.type.ground.LetType;
import orc.type.ground.NumberType;
import orc.type.ground.SignalType;
import orc.type.ground.StringType;
import orc.type.ground.Top;
import orc.type.inference.Constraint;
import orc.type.java.ClassTycon;
import orc.type.structured.ArrowType;
import orc.type.structured.MultiType;
import orc.type.tycon.Tycon;
import orc.type.tycon.Variance;

/**
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
	public static final Type SIGNAL = new SignalType();

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
	public boolean subtype(final Type that) throws TypeException {

		if (that.isTop()) {
			return true;
		}

		final Class thisCls = this.javaCounterpart();
		final Class thatCls = that.javaCounterpart();
		if (thisCls != null && thatCls != null) {
			return thatCls.isAssignableFrom(thisCls);
		}

		// As a last resort, check the Java hierarchy for the types' classes themselves
		if (that.getClass().isAssignableFrom(this.getClass()) && this.sameVariances(that)) {
			return true;
		}

		return false;
	}

	public void assertSubtype(final Type that) throws TypeException {
		if (!this.subtype(that)) {
			throw new SubtypeFailureException(this, that);
		}
	}

	public boolean supertype(final Type that) throws TypeException {
		return that.subtype(this);
	}

	/* By default, equality is based on mutual subtyping.
	 * TODO: This may not be correct in the presence of bounded quantification.
	 */
	public boolean equal(final Type that) throws TypeException {
		return this.subtype(that) && that.subtype(this);
	}

	/* Find the join (least upper bound) in the subtype lattice
	 * of this type and another type.
	 */
	public Type join(final Type that) throws TypeException {
		if (this.subtype(that)) {
			return that;
		} else if (that.subtype(this)) {
			return this;
		} else {
			return TOP;
		}
	}

	/* Find the meet (greatest lower bound) in the subtype lattice
	 * of this type and another type.
	 */
	public Type meet(final Type that) throws TypeException {
		if (this.subtype(that)) {
			return this;
		} else if (that.subtype(this)) {
			return that;
		} else {
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
	public Type call(final TypingContext ctx, final List<Argument> args, final List<Type> typeActuals) throws TypeException {

		/* Default type arity is 0 */
		if (typeActuals != null && typeActuals.size() > 0) {
			throw new TypeArityException(0, typeActuals.size());
		}

		/* Synthesize the argument types */
		final List<Type> argTypes = new LinkedList<Type>();
		for (final Argument a : args) {
			argTypes.add(a.typesynth(ctx));
		}

		/* Attempt to use the simplified call form */
		return call(argTypes);
	}

	/* 
	 * Types which use normal argument synthesis, and have no type parameters,
	 * will usually override this simpler method instead.
	 * 
	 * Normally, if the full call method is overridden,
	 * this simpler method will never be called.
	 */
	public Type call(final List<Type> args) throws TypeException {
		// By default, a type is not callable.
		// Override this call method or the full call method to change this behavior.
		throw new UncallableTypeException(this);
	}

	/* By default, use the class name as the type's string representation */
	@Override
	public String toString() {
		return this.getClass().toString();
	}

	/* Replace all free type variables with the types given for them in the context.
	 * 
	 * By default, perform no substitutions.
	 * Types which contain variables or other types will override this.
	 */
	public Type subst(final Env<Type> ctx) throws TypeException {
		return this;
	}

	/* Perform substitution on a list of types */
	public static List<Type> substAll(final List<Type> ts, final Env<Type> ctx) throws TypeException {

		final List<Type> newts = new LinkedList<Type>();

		for (final Type t : ts) {
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
	private boolean sameVariances(final Type that) throws TypeException {

		final List<Variance> thisVariances = this.variances();
		final List<Variance> thatVariances = that.variances();

		for (int i = 0; i < thisVariances.size(); i++) {
			if (!thisVariances.get(i).equals(thatVariances.get(i))) {
				return false;
			}
		}

		return true;
	}

	/* Make sure that this type is an application of the given type
	 * (or some subtype) to exactly one type parameter. If so, return the parameter, and
	 * if not raise an error.
	 */
	public Type unwrapAs(final Type T) throws TypeException {
		throw new SubtypeFailureException(T, this);
	}

	/* Attempt to use this type as a tycon. We examine the Java type to
	 * determine the kind, since this checker does not keep an explicit
	 * kinding context.
	 */
	public Tycon asTycon() throws TypeException {
		throw new TypeException("Kinding error: Type operator expected, found type " + this + "instead. ");
	}

	// INFERENCE AND CONSTRAINT SATISFACTION

	/* Find the set of free type variable indices in this type */
	public Set<Integer> freeVars() {
		return new TreeSet<Integer>();
	}

	/* Find the union of the free var sets of a list of types */
	public static Set<Integer> allFreeVars(final Collection<Type> collection) {

		final Set<Integer> vars = new TreeSet<Integer>();
		for (final Type t : collection) {
			vars.addAll(t.freeVars());
		}

		return vars;
	}

	/* Bind some variables in a set of free vars, removing
	 * all variables below a given depth and shifting down
	 * all remaining variables by that depth.
	 */
	public static Set<Integer> shiftFreeVars(final Set<Integer> vars, final Integer distance) {

		final Set<Integer> newvars = new TreeSet<Integer>();
		for (final Integer i : vars) {
			if (i >= distance) {
				newvars.add(i - distance);
			}
		}

		return newvars;
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
	public Variance findVariance(final Integer var) {
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
	public Type promote(final Env<Boolean> V) throws TypeException {
		return this;
	}

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
	public Type demote(final Env<Boolean> V) throws TypeException {
		return this;
	}

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
	public void addConstraints(final Env<Boolean> VX, final Type T, final Constraint[] C) throws TypeException {

		try {
			/* If T is a type variable Y, we may be defining
			 * a lower bound for that variable.
			 */
			if (T instanceof TypeVariable) {
				final int Y = ((TypeVariable) T).index;

				/* If Y is in X */
				try {
					if (!VX.lookup(Y)) {

						// Find Z, the index of Y in the outer context
						final int Z = Y - VX.search(false);

						/* Promote this type to remove the variables in V,
						 * and then add it as a lower bound of Z.
						 */
						C[Z].atLeast(this.promote(VX));
						return;
					}
				} catch (final LookupFailureException e) {
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

		} catch (final EnvException e) {
			throw new OrcError(e);
		}
	}

	/** 
	 * From a list of Java Methods, which are assumed to be
	 * the declarations of an overloaded method, create an
	 * Orc type, either an ArrowType for a singleton method
	 * or a MultiType for multiple methods, to typecheck
	 * the possible invocations of that method.
	 * 
	 * @param matchingMethods
	 * @param javaCtx
	 * @return The Orc arrow type for the methods
	 * @throws TypeException 
	 */
	public static Type fromJavaMethods(final List<Method> matchingMethods, final Map<java.lang.reflect.TypeVariable, Type> javaCtx) throws TypeException {

		if (matchingMethods.size() > 1) {
			final List<Type> methodTypes = new LinkedList<Type>();
			for (final Method mth : matchingMethods) {
				methodTypes.add(fromJavaMethod(mth, javaCtx));
			}
			return new MultiType(methodTypes);
		} else if (matchingMethods.size() == 1) {
			return fromJavaMethod(matchingMethods.get(0), javaCtx);
		} else {
			// This should never occur
			throw new OrcError("Attempted to create an Orc type from an empty method list");
		}
	}

	public static Type fromJavaMethod(final Method mth, final Map<java.lang.reflect.TypeVariable, Type> javaCtx) throws TypeException {

		final List<Type> argTypes = new LinkedList<Type>();
		for (final java.lang.reflect.Type T : mth.getGenericParameterTypes()) {
			argTypes.add(fromJavaType(T, javaCtx));
		}

		final Type returnType = fromJavaType(mth.getGenericReturnType(), javaCtx);

		return new ArrowType(argTypes, returnType);
	}

	/**
	 * From a Java type, possibly a generic type, create an Orc
	 * type. This conversion requires a context for all Java
	 * type variables that may occur in the type to the appropriate
	 * Orc types bound to those variables.
	 * 
	 * @param genericType
	 * @param javaCtx
	 * @return The Orc arrow type for the methods
	 * @throws TypeException 
	 */
	public static Type fromJavaType(final java.lang.reflect.Type genericType, final Map<java.lang.reflect.TypeVariable, Type> javaCtx) throws TypeException {
		// X
		if (genericType instanceof java.lang.reflect.TypeVariable) {
			return javaCtx.get(genericType);
		}
		// ?
		else if (genericType instanceof WildcardType) {
			final WildcardType wt = (WildcardType) genericType;

			// Is this an unbounded wildcard?
			if (wt.getUpperBounds().length == 1) {
				try {
					final Class cls = (Class) wt.getUpperBounds()[0];
					if (cls.isAssignableFrom(Object.class)) {
						return Type.TOP; // FIXME: This may not be the correct choice.
					}
				} catch (final ClassCastException e) {
				}
			}

			// If not, we can't handle it; Orc's typechecker does not yet implement bounded polymorphism.
			throw new TypeException("Can't handle nontrivial type bounds (... extends T) on Java types; bounded polymorphism is not supported in Orc.");

		}
		// C<D>
		else if (genericType instanceof ParameterizedType) {
			final ParameterizedType pt = (ParameterizedType) genericType;

			final List<Type> typeActuals = new LinkedList<Type>();
			for (final java.lang.reflect.Type T : pt.getActualTypeArguments()) {
				typeActuals.add(0, fromJavaType(T, javaCtx));
			}
			final Class cls = (Class) pt.getRawType();

			return fromJavaClass(cls).asTycon().instance(typeActuals);
		}
		// C[]
		else if (genericType instanceof GenericArrayType) {
			final GenericArrayType gat = (GenericArrayType) genericType;
			final Type T = fromJavaType(gat.getGenericComponentType(), javaCtx);

			return new ArrayType().instance(T);
		}
		// C
		else if (genericType instanceof Class) {

			final Class cls = (Class) genericType;
			return fromJavaClass(cls);
		} else {
			throw new TypeException("Can't convert Java type " + genericType + " to an Orc type.");
		}
	}

	public static Type fromJavaType(final java.lang.reflect.Type genericType) throws TypeException {
		return fromJavaType(genericType, makeJavaCtx());
	}

	/**
	 * Convert a Java class to an Orc type. Generic classes are
	 * converted to tycons. 
	 * 
	 * @param cls Java class to be converted.
	 * @return Resulting Orc type.
	 * @throws TypeException
	 */
	public static Type fromJavaClass(final Class cls) throws TypeException {

		if (Integer.class.isAssignableFrom(cls) || cls.equals(Integer.TYPE) || cls.equals(Byte.TYPE) || cls.equals(Short.TYPE) || cls.equals(Long.TYPE) || cls.equals(Character.TYPE)) {
			return Type.INTEGER;
		} else if (Boolean.class.isAssignableFrom(cls) || cls.equals(Boolean.TYPE)) {
			return Type.BOOLEAN;
		} else if (String.class.isAssignableFrom(cls)) {
			return Type.STRING;
		} else if (Number.class.isAssignableFrom(cls)) {
			return Type.NUMBER;
		} else if (cls.equals(Void.TYPE)) {
			return Type.TOP;
		}

		/*
		 * Technically, this weakens the metatheory of the
		 * typechecker, since it exposes the fact that all
		 * Orc values are representable as Java objects.
		 * However, this compromise is necessary in practice. 
		 * -dkitchin
		 */
		else if (cls.equals(Object.class)) {
			return Type.TOP;
		}

		else if (Double.class.isAssignableFrom(cls) || cls.equals(Double.TYPE)) {
			return new ClassTycon(Double.class).instance();
		} else if (Float.class.isAssignableFrom(cls) || cls.equals(Float.TYPE)) {
			return new ClassTycon(Float.class).instance();
		}
		// Check if this is actually a primitive array class
		else if (cls.isArray()) {
			final Type T = fromJavaClass(cls.getComponentType());
			return new ArrayType().instance(T);
		} else {
			if (cls.getTypeParameters().length > 0) {
				return new ClassTycon(cls);
			} else {
				return new ClassTycon(cls).instance();
			}
		}

	}

	/**
	 * From a class with Java type formals and a list
	 * of actual Orc type parameters, create a mapping
	 * from those Java variables to their appropriate
	 * Orc type bindings. 
	 * 
	 * @param cls
	 * @param typeActuals
	 * @return A new Java context, with the parameter mapping
	 * @throws TypeArityException 
	 */
	public static Map<java.lang.reflect.TypeVariable, Type> makeJavaCtx(final Class cls, final List<Type> typeActuals) throws TypeArityException {

		final Map<java.lang.reflect.TypeVariable, Type> ctx = makeJavaCtx();

		final java.lang.reflect.TypeVariable[] Xs = cls.getTypeParameters();

		if (Xs.length != typeActuals.size()) {
			throw new TypeArityException(Xs.length, typeActuals.size());
		}

		for (int i = 0; i < Xs.length; i++) {
			ctx.put(Xs[i], typeActuals.get(i));
			//System.out.println(Xs[i] + " -> " + typeActuals.get(i));
		}
		//System.out.println(".");

		return ctx;
	}

	/* Make an empty context */
	public static Map<java.lang.reflect.TypeVariable, Type> makeJavaCtx() {
		return new HashMap<java.lang.reflect.TypeVariable, orc.type.Type>();
	}

	/**
	 * 
	 * Determine whether this type has a counterpart
	 * as a non-generic class in the Java class hierarchy. 
	 * If not, return null. 
	 * 
	 * This is not a true inverse of fromJavaClass,
	 * though it behaves like one in most cases.
	 * 
	 */
	public Class javaCounterpart() {
		return null;
	}

	public Type resolveSites(final Config config) throws MissingTypeException {
		return this;
	}

	/**
	 * Convert to a syntactic type. May return null
	 * if the type is not representable.
	 * 
	 * @return The syntactic type corresponding to this type
	 * @throws UnrepresentableTypeException
	 */
	public orc.ast.xml.type.Type marshal() throws UnrepresentableTypeException {
		throw new UnrepresentableTypeException(this);
	}

}
