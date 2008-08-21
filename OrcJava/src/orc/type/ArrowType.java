package orc.type;

import java.util.LinkedList;
import java.util.List;

import orc.error.compiletime.typing.ArgumentArityException;
import orc.error.compiletime.typing.SubtypeFailureException;
import orc.error.compiletime.typing.TypeException;
import orc.error.compiletime.typing.UncallableTypeException;

public class ArrowType extends Type {

	public List<Type> argTypes;
	public Type resultType;
	
	
	// A zero-argument function
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

	
	
	/*
	 * Checks that the given type is in fact an
	 * ArrowType of the same arity as this arrow type.
	 * If so, returns that type, cast to ArrowType.
	 * Otherwise, returns null. 
	 * 
	 */
	protected ArrowType forceArrow(Type that) {
		
		if (that instanceof ArrowType) {
			ArrowType thatArrow = (ArrowType)that;
			if (arity() == thatArrow.arity()) {
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
		for(int i = 0; i < arity(); i++) {
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
		
		for(int i = 0; i < arity(); i++) {
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
		
		for(int i = 0; i < arity(); i++) {
			Type thisArg = argTypes.get(i);
			Type otherArg = otherArgTypes.get(i);
			meetArgTypes.add(thisArg.join(otherArg));
		}
		
		meetResultType = this.resultType.meet(thatArrow.resultType);
		
		return new ArrowType(meetArgTypes, meetResultType);

	}
	
	
	public Type call(List<Type> args) throws TypeException {
		
		if (arity() != args.size()) {
			throw new ArgumentArityException(arity(), args.size());
		}
		
		for(int i = 0; i < arity(); i++) {
			Type thisArg = argTypes.get(i);
			Type otherArg = args.get(i);
			if (!(otherArg.subtype(thisArg))) { throw new SubtypeFailureException(otherArg, thisArg); }
		}
		
		return resultType;
	}
	
	
	public String toString() {
		
		StringBuilder s = new StringBuilder();
		
		s.append('(');
		for (Type T : argTypes) {
			s.append(T);
			s.append(' ');
		}
		s.append("-> ");
		s.append(resultType);
		s.append(')');
		
		return s.toString();
	}

	public int arity() {
		return argTypes.size();
	}
	
	
	
}
