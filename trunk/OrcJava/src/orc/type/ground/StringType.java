package orc.type.ground;

import java.util.List;

import orc.ast.oil.expression.argument.Arg;
import orc.env.Env;
import orc.error.compiletime.typing.TypeException;
import orc.type.Type;
import orc.type.java.ClassTycon;

public class StringType extends Type {

	public String toString() { return "String"; }
	
	public Type call(Env<Type> ctx, Env<Type> typectx, List<Arg> args, List<Type> typeActuals) throws TypeException {
		return (new ClassTycon(java.lang.String.class)).instance().call(ctx, typectx, args, typeActuals);
	}
	
	public Class javaCounterpart() {
		return java.lang.String.class;
	}
}
