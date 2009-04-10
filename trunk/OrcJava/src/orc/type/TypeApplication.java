package orc.type;

import java.util.LinkedList;
import java.util.List;

import orc.ast.oil.arg.Arg;
import orc.env.Env;
import orc.env.LookupFailureException;
import orc.error.OrcError;
import orc.error.compiletime.typing.ArgumentArityException;
import orc.error.compiletime.typing.SubtypeFailureException;
import orc.error.compiletime.typing.TypeArityException;
import orc.error.compiletime.typing.TypeException;
import orc.error.compiletime.typing.UncallableTypeException;
import orc.type.ground.Top;
import orc.type.inference.Constraint;
import orc.type.tycon.Tycon;
import orc.type.tycon.Variance;

/**
 * An unevaluated type application.
 * 
 * @author dkitchin
 *
 */
public class TypeApplication extends Type {

	public Type ty;
	public List<Type> params;
		
	public TypeApplication(Type ty, List<Type> params) {
		this.ty = ty;
		assert(params.size() > 0);
		this.params = params;
	}

	public Type subst(Env<Type> ctx) throws TypeException {
		
		Type newty = ty.subst(ctx);
		List<Type> newparams = Type.substAll(params, ctx);
		
		/* If the constructor is now bound, create an instance */
		if (newty.freeVars().isEmpty()) {
			return newty.asTycon().instance(newparams);
		}
		else {
			return new TypeApplication(newty, newparams);
		}

	}

	public String toString() {
		
		StringBuilder s = new StringBuilder();
		
		s.append(ty);
		s.append('[');
		for (int i = 0; i < params.size(); i++) {
			if (i > 0) { s.append(", "); }
			s.append(params.get(i));
		}
		s.append(']');
		
		return s.toString();
	}
	
}
