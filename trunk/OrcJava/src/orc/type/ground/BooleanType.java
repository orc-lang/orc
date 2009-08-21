package orc.type.ground;

import java.util.List;

import orc.ast.oil.expression.argument.Argument;
import orc.env.Env;
import orc.error.compiletime.typing.TypeException;
import orc.type.Type;
import orc.type.java.ClassTycon;

public class BooleanType extends Type {

	public String toString() { return "Boolean"; }
	
	public Type call(Env<Type> ctx, Env<Type> typectx, List<Argument> args, List<Type> typeActuals) throws TypeException {
		return (new ClassTycon(java.lang.Boolean.class)).instance().call(ctx, typectx, args, typeActuals);
	}
	
	public Class javaCounterpart() {
		return java.lang.Boolean.class;
	}
}
