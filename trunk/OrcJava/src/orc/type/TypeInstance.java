package orc.type;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import orc.ast.oil.expression.argument.Argument;
import orc.env.Env;
import orc.error.compiletime.typing.ArgumentArityException;
import orc.error.compiletime.typing.SubtypeFailureException;
import orc.error.compiletime.typing.TypeArityException;
import orc.error.compiletime.typing.TypeException;
import orc.error.compiletime.typing.UncallableTypeException;
import orc.type.ground.Top;
import orc.type.inference.Constraint;
import orc.type.java.ClassTycon;
import orc.type.tycon.Tycon;
import orc.type.tycon.Variance;

/**
 * A type constructor instantiated at particular types, 
 * e.g. a List of Integers.
 * 
 * The type params may be empty in the special case that a type instance
 * is constructed and substituted by the compiler itself, for example
 * when binding a declared Java class with no generic parameters 
 * in Orc type space. At present, users should not be able to write types
 * using type applications to an empty parameter list.
 * 
 * @author dkitchin
 *
 */
public class TypeInstance extends Type {

	public Tycon tycon;
	public List<Type> params;
		
	public TypeInstance(Tycon tycon, List<Type> params) {
		this.tycon = tycon;
		this.params = params;
	}
	
	
	public boolean subtype(Type that) throws TypeException {
		
		/* The other type must also be an instance */
		if (that instanceof TypeInstance) {
			TypeInstance thatInstance = (TypeInstance)that;
			
			/* If this type instance actually has no parameters, then it suffices for
			 * the tycons to be in a subtype relationship.
			 */
			if (tycon.variances().size() == 0) {
				return tycon.subtype(thatInstance.tycon);
			}
			
			/* Otherwise, the tycon of that instance must be equal to this tycon */
			if (tycon.equals(thatInstance.tycon)) {
				
				List<Type> otherParams = thatInstance.params;
				List<Variance> vs = tycon.variances();
				
				for (int i = 0; i < vs.size(); i++) {
					Variance v = vs.get(i);
					/* Make sure none of the type parameters fail to obey their variance restrictions */
					if (!v.subtype(params.get(i), otherParams.get(i))) {
						return false;
					}
				}
				/* If the other type is another instance of the same type, whose
				 * parameters vary appropriately, then this is a subtype of that type.
				 */
				return true;
			}
		}
		
		/* Otherwise, the only other supertype of a type instance is Top */
		return (that instanceof Top);
	}	
		
	public Type join(Type that) throws TypeException {
		
		/* The other type must also be an instance */
		if (that instanceof TypeInstance) {
			TypeInstance thatInstance = (TypeInstance)that;
			
			/* The tycon of that instance must be equal to this tycon */
			if (tycon.equals(thatInstance.tycon)) {
				
				List<Type> otherParams = thatInstance.params;
				List<Variance> vs = tycon.variances();
				List<Type> joinParams = new LinkedList<Type>();
				for (int i = 0; i < vs.size(); i++) {
					Variance v = vs.get(i);
					Type joinType = v.join(params.get(i), otherParams.get(i));
					joinParams.add(joinType);
				}
				
				return new TypeInstance(tycon, joinParams);
			}
		}
		
		/* If we cannot find a join, delegate to super */
		return super.join(that);
	}
	
	public Type meet(Type that) throws TypeException {
		
		/* The other type must also be an application */
		if (that instanceof TypeInstance) {
			TypeInstance thatInstance = (TypeInstance)that;
			
			/* The head type of that application must be equal to this type */
			if (tycon.equals(thatInstance.tycon)) {
				
				List<Type> otherParams = thatInstance.params;
				List<Variance> vs = tycon.variances();
				List<Type> meetParams = new LinkedList<Type>();
				for (int i = 0; i < vs.size(); i++) {
					Variance v = vs.get(i);					
					meetParams.add(v.meet(params.get(i), otherParams.get(i)));
				}
				
				return new TypeInstance(tycon, meetParams);
			}
		}
		
		/* If we cannot find a meet, delegate to super */
		return super.meet(that);
	}
	
	/* Call the type as a type instance using the params */
	public Type call(TypingContext ctx, List<Argument> args, List<Type> typeActuals) throws TypeException {
		return tycon.makeCallableInstance(params).call(ctx,args,typeActuals);
	}
	
	public Type call(List<Type> args) throws TypeException {
		return tycon.makeCallableInstance(params).call(args);
	}

	public Type subst(Env<Type> ctx) throws TypeException {
		return new TypeInstance(tycon, Type.substAll(params, ctx));
	}
	
	
	public Variance findVariance(Integer var) {
		
		Variance result = Variance.CONSTANT;
		
		List<Variance> vs = tycon.variances();
		for(int i = 0; i < vs.size(); i++) {
			Variance v = vs.get(i);
			Type p = params.get(i);
			result = result.and(v.apply(p.findVariance(var)));
		}

		return result;
	}

	
	
	public Type promote(Env<Boolean> V) throws TypeException { 
		
		List<Type> newParams = new LinkedList<Type>();
		
		List<Variance> vs = tycon.variances();
		for(int i = 0; i < vs.size(); i++) {
			Variance v = vs.get(i);
			Type p = params.get(i);
			
			Type newp;
			if (v.equals(Variance.INVARIANT)) {
				if (p.equals(p.promote(V))) {
					newp = p;
				}
				else {
					// TODO: Make this less cryptic
					throw new TypeException("Could not infer type parameters; an invariant position is overconstrained");
				}
			}
			else if (v.equals(Variance.CONTRAVARIANT)) {
				newp = p.demote(V);
			}
			else {
				newp = p.promote(V);
			}
			
			newParams.add(newp);
		}
		
		return new TypeInstance(tycon, newParams);
	}
	
	public Type demote(Env<Boolean> V) throws TypeException { 

		List<Type> newParams = new LinkedList<Type>();
		
		List<Variance> vs = tycon.variances();
		for(int i = 0; i < vs.size(); i++) {
			Variance v = vs.get(i);
			Type p = params.get(i);
			
			Type newp;
			if (v.equals(Variance.INVARIANT)) {
				if (p.equals(p.demote(V))) {
					newp = p;
				}
				else {
					// TODO: Make this less cryptic
					throw new TypeException("Could not infer type parameters; an invariant position is overconstrained");
				}
			}
			else if (v.equals(Variance.CONTRAVARIANT)) {
				newp = p.promote(V);
			}
			else {
				newp = p.demote(V);
			}
			
			newParams.add(newp);
		}
		
		return new TypeInstance(tycon, newParams);
	}
	
	public void addConstraints(Env<Boolean> VX, Type T, Constraint[] C) throws TypeException {
		
		if (T instanceof TypeInstance) {
			TypeInstance otherApp = (TypeInstance)T;
			
			if (!otherApp.tycon.equals(tycon) || otherApp.params.size() != params.size()) {
				throw new SubtypeFailureException(this, T);
			}
			
			List<Variance> vs = tycon.variances();
			for(int i = 0; i < vs.size(); i++) {
				Variance v = vs.get(i);
				Type A = params.get(i);
				Type B = otherApp.params.get(i);
			
				if (v.equals(Variance.COVARIANT)) {
					A.addConstraints(VX, B, C);
				}
				else if (v.equals(Variance.CONTRAVARIANT)) {
					B.addConstraints(VX, A, C);
				}
				else if (v.equals(Variance.INVARIANT)) {
					A.addConstraints(VX, B, C);
					B.addConstraints(VX, A, C);
				}
			}
			
		}
		else {
			super.addConstraints(VX, T, C);
		}
	}
	
	
	
	/* Make sure that this type is an application of the given type 
	 * (or some subtype) to exactly one type parameter. If so, return the parameter, and
	 * if not raise an error.
	 */
	public Type unwrapAs(Type T) throws TypeException {
		
		if (tycon.subtype(T)) {
			if (params.size() == 1) {
				return params.get(0);
			}
			else {
				throw new TypeArityException(1, params.size());
			}
		}
		else {
			throw new SubtypeFailureException(T, tycon);
		}
		
	}
	
	public Class javaCounterpart() {
		
		if (tycon instanceof ClassTycon) {
			ClassTycon ct = (ClassTycon)tycon;
			if (ct.cls.getTypeParameters().length == 0) {
				return ct.cls;
			}
		}
		
		return null;
	}
	
	public Set<Integer> freeVars() {
		
		Set<Integer> vars = Type.allFreeVars(params);
		vars.addAll(tycon.freeVars());
		
		return vars;
	}
		
	public String toString() {
		
		StringBuilder s = new StringBuilder();
		
		s.append(tycon.toString());
		if (params.size() > 0) 
		{
			s.append('[');
			for (int i = 0; i < params.size(); i++) {
				if (i > 0) { s.append(", "); }
				s.append(params.get(i));
			}
			s.append(']');
		}
		
		return s.toString();
	}
	
}
