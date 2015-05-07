package orc.type;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import orc.ast.oil.arg.Arg;
import orc.ast.oil.arg.Field;
import orc.env.Env;
import orc.error.compiletime.typing.TypeException;


/**
 * Composite type for sites which can receive messages (using the . notation)
 * 
 * A DotType is created with an optional default type (to be used when the
 * site is called with something other than a message), and then type entries
 * for each understood message are added using addField.
 * 
 * @author dkitchin
 *
 */
public class DotType extends Type {

	public static final Type NODEFAULT = new NoDefaultType();
	Type defaultType;
	Map<String,Type> fieldMap;
	
	public DotType() {
		this.defaultType = NODEFAULT;
		fieldMap = new TreeMap<String,Type>();
	}
	
	public DotType(Type defaultType) {
		this.defaultType = defaultType;
		fieldMap = new TreeMap<String,Type>();
	}
	
	public void addField(String key, Type T) {
		fieldMap.put(key, T);
	}
	
	
	public Type call(Env<Type> ctx, Env<Type> typectx, List<Arg> args, List<Type> typeActuals) throws TypeException {
	
		if (args.size() == 1 && args.get(0) instanceof Field) {
			Field f = (Field)args.get(0);
			return fieldMap.get(f.key);
		}
		else {
			return defaultType.call(ctx,typectx,args,typeActuals);
		}
	}
		
	
	public boolean subtype(Type that) {
		return defaultType.subtype(that);
	}
	
	/* A call without explicit args passed is assumed to be a call to the default type */
	public Type call(List<Type> args) throws TypeException {
		return defaultType.call(args);
	}
	
	
	public String toString() {
		
		StringBuilder s = new StringBuilder();
		
		s.append('(');
		s.append(defaultType);
		for (String f : fieldMap.keySet()) {
			s.append(" & ");
			s.append("." + f + " :: ");
			s.append(fieldMap.get(f));
		}
		s.append(')');
		
		return s.toString();
	}
}
	
class NoDefaultType extends Type {

	public Type call(Env<Type> ctx, Env<Type> typectx, List<Arg> args, List<Type> typeActuals) throws TypeException {
		throw new TypeException("This site has no default behavior; it can only be called via messages");
	}
	
	public Type call(List<Type> args) throws TypeException {
		throw new TypeException("This site has no default behavior; it can only be called via messages");
	}
	
	public String toString() {
		return "no_default_type";
	}
	
	public boolean subtype(Type that) {
		return false;
	}
}
