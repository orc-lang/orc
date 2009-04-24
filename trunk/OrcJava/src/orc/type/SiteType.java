package orc.type;

import java.util.List;
import java.util.Set;

import orc.Config;
import orc.ast.oil.TypeDecl;
import orc.ast.oil.arg.Arg;
import orc.env.Env;
import orc.error.OrcError;
import orc.error.compiletime.typing.MissingTypeException;
import orc.error.compiletime.typing.TypeException;
import orc.type.inference.Constraint;
import orc.type.tycon.Tycon;
import orc.type.tycon.Variance;

/**
 * HACK: this is a dummy type which may only appear in {@link TypeDecl}, and is replaced when {@link #resolveSites(Config)} is run.
 * @author quark
 */
public class SiteType extends Type {
	public String classname;
	
	public SiteType(String classname) {
		this.classname = classname;
	}

	@Override
	public void addConstraints(Env<Boolean> VX, Type T, Constraint[] C) throws TypeException {
		throw new OrcError("Unexpected SiteType");
	}

	@Override
	public void assertSubtype(Type that) throws TypeException {
		throw new OrcError("Unexpected SiteType");
	}

	@Override
	public Tycon asTycon() throws TypeException {
		throw new OrcError("Unexpected SiteType");
	}

	@Override
	public Type call(Env<Type> ctx, Env<Type> typectx, List<Arg> args, List<Type> typeActuals) throws TypeException {
		throw new OrcError("Unexpected SiteType");
	}

	@Override
	public Type call(List<Type> args) throws TypeException {
		throw new OrcError("Unexpected SiteType");
	}

	@Override
	public boolean closed() {
		throw new OrcError("Unexpected SiteType");
	}

	@Override
	public Type demote(Env<Boolean> V) throws TypeException {
		throw new OrcError("Unexpected SiteType");
	}

	@Override
	public boolean equal(Type that) throws TypeException {
		throw new OrcError("Unexpected SiteType");
	}

	@Override
	public Variance findVariance(Integer var) throws TypeException {
		throw new OrcError("Unexpected SiteType");
	}

	@Override
	public Set<Integer> freeVars() {
		throw new OrcError("Unexpected SiteType");
	}

	@Override
	public boolean isBot() {
		throw new OrcError("Unexpected SiteType");
	}

	@Override
	public boolean isTop() {
		throw new OrcError("Unexpected SiteType");
	}

	@Override
	public Type join(Type that) throws TypeException {
		throw new OrcError("Unexpected SiteType");
	}

	@Override
	public Type meet(Type that) throws TypeException {
		throw new OrcError("Unexpected SiteType");
	}

	@Override
	public Type promote(Env<Boolean> V) throws TypeException {
		throw new OrcError("Unexpected SiteType");
	}

	@Override
	public Type subst(Env<Type> ctx) throws TypeException {
		throw new OrcError("Unexpected SiteType");
	}

	@Override
	public boolean subtype(Type that) throws TypeException {
		throw new OrcError("Unexpected SiteType");
	}

	@Override
	public boolean supertype(Type that) throws TypeException {
		throw new OrcError("Unexpected SiteType");
	}

	@Override
	public Type unwrapAs(Type T) throws TypeException {
		throw new OrcError("Unexpected SiteType");
	}

	@Override
	public List<Variance> variances() throws TypeException {
		throw new OrcError("Unexpected SiteType");
	}
	
	@Override
	public Type resolveSites(Config config) throws MissingTypeException {
		orc.type.Type t;
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
			t = (orc.type.Type)(cls.newInstance());
		} catch (InstantiationException e) {
			throw new MissingTypeException("Failed to load class " + cls + " as an external type. Instantiation error.", e);
		} catch (IllegalAccessException e) {
			throw new MissingTypeException("Failed to load class " + cls + " as an external type. Constructor is not accessible.");
		}
		
		return t;
	}
}
