package orc.type.structured;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import orc.ast.oil.expression.argument.Argument;

import orc.env.Env;
import orc.error.compiletime.typing.ArgumentArityException;
import orc.error.compiletime.typing.SubtypeFailureException;
import orc.error.compiletime.typing.TypeArityException;
import orc.error.compiletime.typing.TypeException;
import orc.error.compiletime.typing.UncallableTypeException;
import orc.error.compiletime.typing.UnrepresentableTypeException;
import orc.type.Type;
import orc.type.TypingContext;
import orc.type.ground.Top;
import orc.type.inference.Constraint;
import orc.type.inference.InferenceRequest;
import orc.type.tycon.Variance;

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

	
	public boolean subtype(Type that) throws TypeException {
		ArrowType thatArrow = forceArrow(that);
		if (thatArrow != null) {
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
		} else {
			return super.subtype(that);
		}
	}
	
	/* 
	 * A join of two arrow types is a meet of their arg types
	 * and a join of their result type.
	 */
	public Type join(Type that) throws TypeException {	
		
		ArrowType thatArrow = forceArrow(that);
		if (thatArrow != null) { 
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
		} else {
			return super.join(that);
		}
	}
	
	/* 
	 * A meet of two arrow types is a join of their arg types
	 * and a meet of their result type.
	 */
	public Type meet(Type that) throws TypeException {
		
		ArrowType thatArrow = forceArrow(that);
		if (thatArrow != null) { 
		
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
		} else {
			return super.meet(that);
		}

	}
	
	
	public Type call(TypingContext ctx, List<Argument> args, List<Type> typeActuals) throws TypeException {
		
		/* Arity check */
		if (argTypes.size() != args.size()) {
			throw new ArgumentArityException(argTypes.size(), args.size());
		}
		
		/* Inference request check */
		if (typeActuals == null) {
			if (typeArity > 0) {
				throw new InferenceRequest(this);
			}
			else {
				/* Just use an empty list */
				typeActuals = new LinkedList<Type>();
			}
		}
		/* Type arity check */
		else {
			if (typeArity != typeActuals.size()) {
				throw new TypeArityException(typeArity, typeActuals.size());
			}
		}
		
		/* Add each type argument to the type context */
		for(Type targ : typeActuals) {
			ctx = ctx.bindType(targ);
		}
		
		/* Check each argument against its respective argument type */
		for(int i = 0; i < argTypes.size(); i++) {
			Type thisType = ctx.subst(argTypes.get(i));
			Argument thisArg = args.get(i);
			thisArg.typecheck(ctx, thisType);
		}
		
		return ctx.subst(resultType);
	}
	
	
	public Type subst(Env<Type> ctx) throws TypeException {
		
		Env<Type> newctx = ctx;
		
		/* Add empty entries in the context for each bound type parameter */
		for(int i = 0; i < typeArity; i++) { newctx = newctx.extend(null); }
		
		return new ArrowType(Type.substAll(argTypes, newctx), resultType.subst(newctx), typeArity);
	}
	
	
	public Variance findVariance(Integer var) {
		
		Variance result = resultType.findVariance(var);
		
		for (Type T : argTypes) {
			Variance v = T.findVariance(var).invert();
			result = result.and(v);
		}

		return result;
	}
	
	public Type promote(Env<Boolean> V) throws TypeException { 
		
		// Exclude newly bound variables from the set V
		for(int i = 0; i < typeArity; i++) {
			V = V.extend(false);
		}
		
		Type newResultType = resultType.promote(V);
		
		List<Type> newArgTypes = new LinkedList<Type>();
		for (Type T : argTypes) {
			newArgTypes.add(T.demote(V));
		}
		
		return new ArrowType(newArgTypes, newResultType, typeArity);
	}
	
	public Type demote(Env<Boolean> V) throws TypeException { 

		// Exclude newly bound variables from the set V
		for(int i = 0; i < typeArity; i++) {
			V = V.extend(false);
		}
		
		Type newResultType = resultType.demote(V);
		
		List<Type> newArgTypes = new LinkedList<Type>();
		for (Type T : argTypes) {
			newArgTypes.add(T.promote(V));
		}
		
		return new ArrowType(newArgTypes, newResultType, typeArity);
	}
	
	
	public void addConstraints(Env<Boolean> VX, Type T, Constraint[] C) throws TypeException {
		
		if (T instanceof ArrowType) {
			ArrowType other = (ArrowType)T;
			
			if (other.argTypes.size() != argTypes.size() || other.typeArity != typeArity) {
				throw new SubtypeFailureException(this, T);
			}
			
			for(int i = 0; i < typeArity; i++) {
				VX = VX.extend(true);
			}
			
			for(int i = 0; i < argTypes.size(); i++) {
				Type A = argTypes.get(i);
				Type B = other.argTypes.get(i);
		
				B.addConstraints(VX, A, C);		
			}
			resultType.addConstraints(VX, other.resultType, C);
			
		}
		else {
			super.addConstraints(VX, T, C);
		}
	}
	
	
	
	public String toString() {
		
		StringBuilder s = new StringBuilder();
		
		s.append('(');

		s.append("lambda ");
		if (typeArity > 0) {
			s.append("[_");
			for (int i = 1; i < typeArity; i++) {
				s.append(",_");
			}
			s.append("]");
		}
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

	@Override
	public orc.ast.xml.type.Type marshal() throws UnrepresentableTypeException {
		orc.ast.xml.type.Type[] newArgTypes = new orc.ast.xml.type.Type[argTypes.size()];
		int i = 0;
		for (Type t : argTypes) {
			newArgTypes[i] = t.marshal();
			++i;
		}
		orc.ast.xml.type.Type newResultType = null;
		if (resultType != null) newResultType = resultType.marshal();
		return new orc.ast.xml.type.ArrowType(newArgTypes, newResultType, typeArity);
	}
	
	public Set<Integer> freeVars() {
				
		Set<Integer> vars = Type.allFreeVars(argTypes);
		vars.addAll(resultType.freeVars());
		
		return Type.shiftFreeVars(vars, typeArity);
	}
}
