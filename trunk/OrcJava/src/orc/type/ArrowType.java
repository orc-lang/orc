package orc.type;

import java.util.LinkedList;
import java.util.List;

import orc.ast.oil.arg.Arg;
import orc.ast.simple.type.VariantTypeFormal;

import orc.env.Env;
import orc.error.compiletime.typing.ArgumentArityException;
import orc.error.compiletime.typing.SubtypeFailureException;
import orc.error.compiletime.typing.TypeArityException;
import orc.error.compiletime.typing.TypeException;
import orc.error.compiletime.typing.UncallableTypeException;

public class ArrowType extends Type {

	public List<Type> argTypes;
	public Type resultType;
	public int typeArity = 0;

	public ArrowType(Type resultType) {
		this.argTypes = new LinkedList<Type>();
		this.resultType = resultType;
	}
	
	public ArrowType(Type argType, Type resultType) {
		this.argTypes = new LinkedList<Type>();
		argTypes.add(argType);
		this.resultType = resultType;
	}
	
	public ArrowType(Type firstArgType, Type secondArgType, Type resultType) {
		this.argTypes = new LinkedList<Type>();
		argTypes.add(firstArgType);
		argTypes.add(secondArgType);
		this.resultType = resultType;
	}
	
	public ArrowType(List<Type> argTypes, Type resultType) {
		this.argTypes = argTypes;
		this.resultType = resultType;
	}
	
	public ArrowType(Type resultType, int typeArity) {
		this.argTypes = new LinkedList<Type>();
		this.resultType = resultType;
		this.typeArity = typeArity;
	}
	
	public ArrowType(Type argType, Type resultType, int typeArity) {
		this.argTypes = new LinkedList<Type>();
		argTypes.add(argType);
		this.resultType = resultType;
		this.typeArity = typeArity;
	}
	
	public ArrowType(Type firstArgType, Type secondArgType, Type resultType, int typeArity) {
		this.argTypes = new LinkedList<Type>();
		argTypes.add(firstArgType);
		argTypes.add(secondArgType);
		this.resultType = resultType;
		this.typeArity = typeArity;
	}
	
	public ArrowType(List<Type> argTypes, Type resultType, int typeArity) {
		this.argTypes = argTypes;
		this.resultType = resultType;
		this.typeArity = typeArity;
	}

	
	
	/*
	 * Checks that the given type is in fact an
	 * ArrowType of the same type and argument 
	 * arity as this arrow type.
	 * If so, returns that type, cast to ArrowType.
	 * Otherwise, returns null. 
	 * 
	 */
	protected ArrowType forceArrow(Type that) {
		
		if (that instanceof ArrowType) {
			ArrowType thatArrow = (ArrowType)that;
			if (argTypes.size() == thatArrow.argTypes.size() && typeArity == thatArrow.typeArity) {
				return thatArrow;
			}
		}

		return null;
	}

	
	public boolean subtype(Type that) {
		
		if (that instanceof Top) { return true; }
		
		ArrowType thatArrow = forceArrow(that);
		if (thatArrow == null) { return false; }
		
		List<Type> otherArgTypes = thatArrow.argTypes;
		
		/*
		 * Arguments are contravariant: make sure
		 * that each other arg type is a subtype
		 * of this arg type.
		 */
		for(int i = 0; i < argTypes.size(); i++) {
			Type thisArg = argTypes.get(i);
			Type otherArg = otherArgTypes.get(i);
			if (!(otherArg.subtype(thisArg))) { return false; }
		}
		
		/*
		 * Result type is covariant.
		 */
		return this.resultType.subtype(thatArrow.resultType);
	}
	
	/* 
	 * A join of two arrow types is a meet of their arg types
	 * and a join of their result type.
	 */
	public Type join(Type that) {	
		
		ArrowType thatArrow = forceArrow(that);
		if (thatArrow == null) { return Type.TOP; }
		
		List<Type> otherArgTypes = thatArrow.argTypes;
		
		List<Type> joinArgTypes = new LinkedList<Type>();
		Type joinResultType;
		
		for(int i = 0; i < argTypes.size(); i++) {
			Type thisArg = argTypes.get(i);
			Type otherArg = otherArgTypes.get(i);
			joinArgTypes.add(thisArg.meet(otherArg));
		}
		
		joinResultType = this.resultType.join(thatArrow.resultType);
		
		return new ArrowType(joinArgTypes, joinResultType);
	}
	
	/* 
	 * A meet of two arrow types is a join of their arg types
	 * and a meet of their result type.
	 */
	public Type meet(Type that) {
		
		ArrowType thatArrow = forceArrow(that);
		if (thatArrow == null) { return Type.BOT; }
		
		List<Type> otherArgTypes = thatArrow.argTypes;
		
		List<Type> meetArgTypes = new LinkedList<Type>();
		Type meetResultType;
		
		for(int i = 0; i < argTypes.size(); i++) {
			Type thisArg = argTypes.get(i);
			Type otherArg = otherArgTypes.get(i);
			meetArgTypes.add(thisArg.join(otherArg));
		}
		
		meetResultType = this.resultType.meet(thatArrow.resultType);
		
		return new ArrowType(meetArgTypes, meetResultType);

	}
	
	
	public Type call(Env<Type> ctx, Env<Type> typectx, List<Arg> args, List<Type> typeActuals) throws TypeException {
		
		/* Arity check */
		if (argTypes.size() != args.size()) {
			throw new ArgumentArityException(argTypes.size(), args.size());
		}
		
		/* Type arity check */
		if (typeArity != typeActuals.size()) {
			throw new TypeArityException(typeArity, typeActuals.size());
		}
		
		/* Add each type argument to the type context */
		for(Type targ : typeActuals) {
			typectx = typectx.extend(targ);
		}
		
		/* Check each argument against its respective argument type */
		for(int i = 0; i < argTypes.size(); i++) {
			Type thisType = argTypes.get(i).subst(typectx);
			Arg thisArg = args.get(i);
			thisArg.typecheck(thisType, ctx, typectx);
		}
		
		return resultType.subst(typectx);
	}
	
	
	public Type subst(Env<Type> ctx) {
		
		Env<Type> newctx = ctx;
		
		/* Add empty entries in the context for each bound type parameter */
		for(int i = 0; i < typeArity; i++) { newctx = newctx.extend(null); }
		
		return new ArrowType(Type.substAll(argTypes, newctx), resultType.subst(newctx), typeArity);
	}
	
	
	public String toString() {
		
		StringBuilder s = new StringBuilder();
		
		s.append('(');

		s.append("lambda ");
		s.append('[' + typeArity + ']');
		s.append('(');
		for (int i = 0; i < argTypes.size(); i++) {
			if (i > 0) { s.append(", "); }
			s.append(argTypes.get(i));
		}
		s.append(')');
		s.append(" :: ");
		s.append(resultType);
		
		s.append(')');
		
		return s.toString();
	}
	
}
