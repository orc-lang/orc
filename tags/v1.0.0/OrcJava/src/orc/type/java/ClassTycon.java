package orc.type.java;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.TypeVariable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import orc.ast.oil.expression.argument.Argument;
import orc.error.compiletime.typing.TypeArityException;
import orc.error.compiletime.typing.TypeException;
import orc.lib.state.types.ArrayType;
import orc.lib.state.types.RefType;
import orc.type.Type;
import orc.type.TypingContext;
import orc.type.tycon.Tycon;
import orc.type.tycon.Variance;

public class ClassTycon extends Tycon {

	public Class cls;
	
	/* This constructor is rarely used directly.
	 * Try Type.fromJavaClass instead.
	 */
	public ClassTycon(Class cls) {
		this.cls = cls;
	}
	
	@Override
	public boolean subtype(Type that) throws TypeException {

		// All tycons are subtypes of Top
		if (that.isTop()) { return true; }
		
		if (that instanceof ClassTycon) {
			ClassTycon ct = (ClassTycon)that;
			
			// If this is not a generic class, just check Java subtyping.
			if (cls.getTypeParameters().length == 0) {
				return ct.cls.isAssignableFrom(cls);
			}
			
			// Otherwise, check for class equality.
			return ct.cls.equals(cls);
		}
		
		return false;
	}

	@Override
	public List<Variance> variances() {
		/* 
		 * All Java type parameters should be considered invariant, to be safe.
		 */
		List<Variance> vs = new LinkedList<Variance>();
		for(int i = 0; i < cls.getTypeParameters().length; i++) {
			vs.add(Variance.INVARIANT);
		}
		return vs;
	}

	public Type makeCallableInstance(List<Type> params) throws TypeArityException {		
		return new CallableJavaInstance(cls, Type.makeJavaCtx(cls, params));
	}
	
	public String toString() {
		return cls.getName().toString();
	}

}





class CallableJavaInstance extends Type {
	
	Class cls;
	Map<java.lang.reflect.TypeVariable, Type> javaCtx;
	
	public CallableJavaInstance(Class cls, Map<java.lang.reflect.TypeVariable, Type> javaCtx) {
		this.cls = cls;
		this.javaCtx = javaCtx;
	}
	
	@Override
	public Type call(TypingContext ctx, List<Argument> args, List<Type> typeActuals) throws TypeException {
				
		String f = Argument.asField(args);
		
		if (f != null) {
			List<Method> matchingMethods = new LinkedList<Method>();
			for (Method m : cls.getMethods()) {
				if (m.getName().equals(f)) 
				{
					matchingMethods.add(m);	
				}
			}

			if (!matchingMethods.isEmpty()) {
				return Type.fromJavaMethods(matchingMethods, javaCtx);
			}
			else {
				// No method matches. Try fields.
				for (java.lang.reflect.Field fld : cls.getFields()) {
					if (fld.getName().equals(f)) {
						return (new RefType()).instance(Type.fromJavaType(fld.getGenericType(), javaCtx));
					}
				}
				
				// Neither a method nor a field
				throw new TypeException("'" + f + "' is not a member of " + cls.getName());
			}
		} 
		else {
			// Look for the 'apply' method
			
			List<Method> matchingMethods = new LinkedList<Method>();
			for (Method m : cls.getMethods()) {
				if (m.getName().equals("apply")) 
				{
					matchingMethods.add(m);	
				}
			}

			if (!matchingMethods.isEmpty()) {
				Type target = Type.fromJavaMethods(matchingMethods, javaCtx);
				return target.call(ctx, args, typeActuals);
			}
			else {
				throw new TypeException("This Java class does not implement the 'apply' method, so it has no default site behavior. Use a method call.");
			}
		}
		
	}
	
}
