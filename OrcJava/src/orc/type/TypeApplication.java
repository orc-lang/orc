package orc.type;

import java.util.LinkedList;
import java.util.List;

import orc.env.Env;
import orc.error.compiletime.typing.ArgumentArityException;
import orc.error.compiletime.typing.SubtypeFailureException;
import orc.error.compiletime.typing.TypeArityException;
import orc.error.compiletime.typing.TypeException;
import orc.error.compiletime.typing.UncallableTypeException;

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
				List<Variance> vs = variances();
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
		
		/* If any of these conditions fail to hold then this is not a subtype */
		return false;
	}	
		
	public Type join(Type that) {
		
		/* The other type must also be an application */
		if (that instanceof TypeApplication) {
			TypeApplication thatApp = (TypeApplication)that;
			
			/* The head type of that application must be equal to this type */
			if (type.equal(thatApp.type)) {
				
				List<Type> otherParams = thatApp.params;
				List<Variance> vs = variances();
				List<Type> joinParams = new LinkedList<Type>();
				for (int i = 0; i < vs.size(); i++) {
					Variance v = vs.get(i);					
					joinParams.add(v.join(params.get(i), otherParams.get(i)));
				}
				
				return new TypeApplication(type, joinParams);
			}
		}
		
		/* If we cannot find a join, just return Top */
		return Type.TOP;
	}
	
	public Type meet(Type that) {
		
		/* The other type must also be an application */
		if (that instanceof TypeApplication) {
			TypeApplication thatApp = (TypeApplication)that;
			
			/* The head type of that application must be equal to this type */
			if (type.equal(thatApp.type)) {
				
				List<Type> otherParams = thatApp.params;
				List<Variance> vs = variances();
				List<Type> meetParams = new LinkedList<Type>();
				for (int i = 0; i < vs.size(); i++) {
					Variance v = vs.get(i);					
					meetParams.add(v.meet(params.get(i), otherParams.get(i)));
				}
				
				return new TypeApplication(type, meetParams);
			}
		}
		
		/* If we cannot find a join, just return Bot */
		return Type.BOT;
	}
	
	/* Call the type as a type instance using the params */
	public Type call(List<Type> args) throws TypeException {
		return type.callInstance(args, params);
	}

	public Type subst(Env<Type> ctx) {
		return new TypeApplication(type.subst(ctx), Type.substAll(params, ctx));
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
