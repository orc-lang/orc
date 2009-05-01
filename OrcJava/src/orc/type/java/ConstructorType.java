package orc.type.java;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import orc.ast.oil.arg.Arg;
import orc.env.Env;
import orc.error.compiletime.typing.TypeException;
import orc.error.runtime.MessageNotUnderstoodException;
import orc.type.Type;


/**
 * Type associated with a class constructor. Note that this is
 * not a type operator (ClassTycon); it is the type of the
 * constructor site itself. Users cannot write this
 * type explicitly; it is syntesized as the type of the value
 * bound by a 'class ... =' declaration.
 * 
 * @author dkitchin
 *
 */
public class ConstructorType extends Type {

	public Class cls;
	
	public ConstructorType(Class cls) {
		this.cls = cls;
	}

	@Override
	public Type call(Env<Type> ctx, Env<Type> typectx, List<Arg> args,
			List<Type> typeActuals) throws TypeException {
		
		if (typeActuals == null) {
			typeActuals = new LinkedList<Type>();
		}
		
		String f = Arg.asField(args);
		Map<java.lang.reflect.TypeVariable, orc.type.Type> javaCtx = Type.makeJavaCtx(cls, typeActuals);
		
		// This is a call to a static method or member.
		// Return the appropriate method type, or try to resolve a static field.
		if (f != null) {
			List<Method> matchingMethods = new LinkedList<Method>();
			for (Method m : cls.getDeclaredMethods()) {
				if (Modifier.isStatic(m.getModifiers())
					&& Modifier.isPublic(m.getModifiers())
					&& m.getName().equals(f)) 
				{
					matchingMethods.add(m);	
				}
			}

			if (!matchingMethods.isEmpty()) {
				return Type.fromJavaMethods(matchingMethods, javaCtx);
			}
			else {
				// No static method matches. Try static fields.
				for (java.lang.reflect.Field fld : cls.getDeclaredFields()) {
					if (Modifier.isStatic(fld.getModifiers())
					&& Modifier.isPublic(fld.getModifiers())
					&& fld.getName().equals(f)) {
						return Type.fromJavaType(fld.getGenericType(), javaCtx);
					}
				}
				
				// Neither a method nor a field
				throw new TypeException(f + " is not a public static member of class " + cls);
			}
		}
		// This is a constructor call. Check the args, then return an instance of the class. 
		else {
			List<Constructor> matchingConstructors = new LinkedList<Constructor>();
			for (Constructor c : cls.getConstructors()) {
				
				// Check each parameter type to make sure it matches
				try {
					java.lang.reflect.Type[] javaTypes = c.getGenericParameterTypes();
					if (javaTypes.length != args.size()) { continue; }
					for(int i = 0; i < javaTypes.length; i++) {
						Type orcType = Type.fromJavaType(javaTypes[i], javaCtx);
						args.get(i).typecheck(orcType, ctx, typectx);
					}
				}
				catch (TypeException e) { continue; }
					
				// We found a matching constructor.
				
				/* If this is a parameterized class, instantiate it directly */
				if (typeActuals.size() > 0) {
					return (new ClassTycon(cls)).instance(typeActuals);
				}
				/* Otherwise convert the class from Java to Orc,
				 * in case it is a ground type.
				 */
				else {
					return Type.fromJavaType(cls);
				}
			}
			
			throw new TypeException("No appropriate constructor found for these arguments.");
		}
		
		
	}

	@Override
	public boolean subtype(Type that) throws TypeException {
		
		if (that instanceof ConstructorType 
			&& ((ConstructorType)that).cls.equals(cls)) {
			return true;
		}
		else {
			return that.isTop();
		}
	}

	@Override
	public String toString() {
		return "(constructor: " + cls + ")";
	}

}
