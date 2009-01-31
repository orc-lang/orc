package orc.type;

import java.util.LinkedList;
import java.util.List;

import orc.ast.oil.arg.Arg;
import orc.env.Env;
import orc.error.compiletime.typing.ArgumentArityException;
import orc.error.compiletime.typing.SubtypeFailureException;
import orc.error.compiletime.typing.TypeArityException;
import orc.error.compiletime.typing.TypeException;
import orc.error.compiletime.typing.UncallableTypeException;
import orc.type.ground.Top;

/**
 * A type application, e.g. List[Boolean]
 * 
 * @author dkitchin
 *
 */
public class TypeApplication extends Type {

	public Type type;
	public List<Type> params;
		
	public TypeApplication(Type type, List<Type> params) {
		this.type = type;
		assert(params.size() > 0);
		this.params = params;
	}
	
	
	public boolean subtype(Type that) {
		
		/* The other type must also be an application */
		if (that instanceof TypeApplication) {
			TypeApplication thatApp = (TypeApplication)that;
			
			/* The head type of that application must be equal to this type */
			if (type.equal(thatApp.type)) {
				
				List<Type> otherParams = thatApp.params;
				List<Variance> vs = type.variances();
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
		
		/* Otherwise, the only other supertype of a type application is Top */
		return (that instanceof Top);
	}	
		
	public Type join(Type that) {
		
		/* The other type must also be an application */
		if (that instanceof TypeApplication) {
			TypeApplication thatApp = (TypeApplication)that;
			
			/* The head type of that application must be equal to this type */
			if (type.equal(thatApp.type)) {
				
				List<Type> otherParams = thatApp.params;
				List<Variance> vs = type.variances();
				List<Type> joinParams = new LinkedList<Type>();
				for (int i = 0; i < vs.size(); i++) {
					Variance v = vs.get(i);
					Type joinType = v.join(params.get(i), otherParams.get(i));
					joinParams.add(joinType);
				}
				
				return new TypeApplication(type, joinParams);
			}
		}
		
		/* If we cannot find a join, delegate to super */
		return super.join(that);
	}
	
	public Type meet(Type that) {
		
		/* The other type must also be an application */
		if (that instanceof TypeApplication) {
			TypeApplication thatApp = (TypeApplication)that;
			
			/* The head type of that application must be equal to this type */
			if (type.equal(thatApp.type)) {
				
				List<Type> otherParams = thatApp.params;
				List<Variance> vs = type.variances();
				List<Type> meetParams = new LinkedList<Type>();
				for (int i = 0; i < vs.size(); i++) {
					Variance v = vs.get(i);					
					meetParams.add(v.meet(params.get(i), otherParams.get(i)));
				}
				
				return new TypeApplication(type, meetParams);
			}
		}
		
		/* If we cannot find a meet, delegate to super */
		return super.meet(that);
	}
	
	/* Call the type as a type instance using the params */
	public Type call(Env<Type> ctx, Env<Type> typectx, List<Arg> args, List<Type> typeActuals) throws TypeException {
		return type.makeCallableInstance(params).call(ctx,typectx,args,typeActuals);
	}
	
	public Type call(List<Type> args) throws TypeException {
		return type.makeCallableInstance(params).call(args);
	}

	public Type subst(Env<Type> ctx) {
		return new TypeApplication(type.subst(ctx), Type.substAll(params, ctx));
	}
	
	
	
	public Variance findVariance(Integer var) {
		
		Variance result = Variance.CONSTANT;
		
		List<Variance> vs = type.variances();
		for(int i = 0; i < vs.size(); i++) {
			Variance v = vs.get(i);
			Type p = params.get(i);
			result = result.and(v.apply(p.findVariance(var)));
		}

		return result;
	}
	
	public Type promote(Env<Boolean> V) throws TypeException { 
		
		List<Type> newParams = new LinkedList<Type>();
		
		List<Variance> vs = type.variances();
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
		
		return new TypeApplication(type, newParams);
	}
	
	public Type demote(Env<Boolean> V) throws TypeException { 

		List<Type> newParams = new LinkedList<Type>();
		
		List<Variance> vs = type.variances();
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
		
		return new TypeApplication(type, newParams);
	}
	
	public void addConstraints(Env<Boolean> VX, Type T, Constraint[] C) throws TypeException {
		
		if (T instanceof TypeApplication) {
			TypeApplication otherApp = (TypeApplication)T;
			
			if (!otherApp.type.equal(type) || otherApp.params.size() != params.size()) {
				throw new SubtypeFailureException(this, T);
			}
			
			List<Variance> vs = type.variances();
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
		
		if (type.subtype(T)) {
			if (params.size() == 1) {
				return params.get(0);
			}
			else {
				throw new TypeArityException(1, params.size());
			}
		}
		else {
			throw new SubtypeFailureException(T, type);
		}
		
	}
	
		
	public String toString() {
		
		StringBuilder s = new StringBuilder();
		
		s.append(type);
		s.append('[');
		for (int i = 0; i < params.size(); i++) {
			if (i > 0) { s.append(", "); }
			s.append(params.get(i));
		}
		s.append(']');
		
		return s.toString();
	}
	
}
