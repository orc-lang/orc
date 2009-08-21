package orc.type;

import java.util.List;
import java.util.Set;

import orc.Config;
import orc.ast.oil.expression.DeclareType;
import orc.ast.oil.expression.argument.Argument;
import orc.env.Env;
import orc.error.OrcError;
import orc.error.compiletime.typing.MissingTypeException;
import orc.error.compiletime.typing.TypeException;
import orc.error.compiletime.typing.UnrepresentableTypeException;
import orc.type.inference.Constraint;
import orc.type.tycon.Tycon;
import orc.type.tycon.Variance;

/**
 * This exists to mark types which can be represented syntactically.
 * @author quark
 */
public class SiteType extends Type {
	public String classname;
	private Type type;
	
	public SiteType(String classname, Type type) {
		this.classname = classname;
		this.type = type;
	}
	
	public SiteType(String classname) {
		this.classname = classname;
	}
	
	@Override
	public Type resolveSites(Config config) throws MissingTypeException {
		Class<?> cls;
			
		try {
			cls = config.loadClass(classname);
		}
		catch (ClassNotFoundException e) {
			throw new MissingTypeException("Failed to load class " + classname + " as an Orc external type. Class not found.");
		}
			
		if (!orc.type.Type.class.isAssignableFrom(cls)) {
			throw new MissingTypeException("Class " + cls + " cannot be used as an Orc external type because it is not a subtype of orc.type.Type."); 
		}
		
		try
		{
			type = (orc.type.Type)(cls.newInstance());
		} catch (InstantiationException e) {
			throw new MissingTypeException("Failed to load class " + cls + " as an external type. Instantiation error.", e);
		} catch (IllegalAccessException e) {
			throw new MissingTypeException("Failed to load class " + cls + " as an external type. Constructor is not accessible.");
		}
		
		return this;
	}
	
	@Override
	public orc.ast.xml.type.Type marshal() throws UnrepresentableTypeException {
		return new orc.ast.xml.type.SiteType(classname);
	}


	@Override
	public void addConstraints(Env<Boolean> VX, Type T, Constraint[] C) throws TypeException {
		// TODO Auto-generated method stub
		type.addConstraints(VX, T, C);
	}

	@Override
	public void assertSubtype(Type that) throws TypeException {
		// TODO Auto-generated method stub
		type.assertSubtype(that);
	}

	@Override
	public Tycon asTycon() throws TypeException {
		// TODO Auto-generated method stub
		return type.asTycon();
	}

	@Override
	public Type call(Env<Type> ctx, Env<Type> typectx, List<Argument> args, List<Type> typeActuals) throws TypeException {
		// TODO Auto-generated method stub
		return type.call(ctx, typectx, args, typeActuals);
	}

	@Override
	public Type call(List<Type> args) throws TypeException {
		// TODO Auto-generated method stub
		return type.call(args);
	}

	@Override
	public boolean closed() {
		// TODO Auto-generated method stub
		return type.closed();
	}

	@Override
	public Type demote(Env<Boolean> V) throws TypeException {
		// TODO Auto-generated method stub
		return type.demote(V);
	}

	@Override
	public boolean equal(Type that) throws TypeException {
		// TODO Auto-generated method stub
		return type.equal(that);
	}

	@Override
	public Variance findVariance(Integer var) throws TypeException {
		// TODO Auto-generated method stub
		return type.findVariance(var);
	}

	@Override
	public Set<Integer> freeVars() {
		// TODO Auto-generated method stub
		return type.freeVars();
	}

	@Override
	public boolean isBot() {
		// TODO Auto-generated method stub
		return type.isBot();
	}

	@Override
	public boolean isTop() {
		// TODO Auto-generated method stub
		return type.isTop();
	}

	@Override
	public Type join(Type that) throws TypeException {
		// TODO Auto-generated method stub
		return type.join(that);
	}

	@Override
	public Type meet(Type that) throws TypeException {
		// TODO Auto-generated method stub
		return type.meet(that);
	}

	@Override
	public Type promote(Env<Boolean> V) throws TypeException {
		// TODO Auto-generated method stub
		return type.promote(V);
	}

	@Override
	public Type subst(Env<Type> ctx) throws TypeException {
		// TODO Auto-generated method stub
		return type.subst(ctx);
	}

	@Override
	public boolean subtype(Type that) throws TypeException {
		// TODO Auto-generated method stub
		return type.subtype(that);
	}

	@Override
	public boolean supertype(Type that) throws TypeException {
		// TODO Auto-generated method stub
		return type.supertype(that);
	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return type.toString();
	}

	@Override
	public Type unwrapAs(Type T) throws TypeException {
		// TODO Auto-generated method stub
		return type.unwrapAs(T);
	}

	@Override
	public List<Variance> variances() throws TypeException {
		// TODO Auto-generated method stub
		return type.variances();
	}
}
