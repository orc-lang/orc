package orc.type.ground;

import java.util.List;

import orc.ast.oil.arg.Arg;
import orc.env.Env;
import orc.error.compiletime.typing.TypeException;
import orc.type.Type;
import orc.type.java.ClassTycon;

public class IntegerType extends NumberType {
	
	public String toString() { return "Integer"; }
	
	public Type call(Env<Type> ctx, Env<Type> typectx, List<Arg> args, List<Type> typeActuals) throws TypeException {
		return (new ClassTycon(java.lang.Integer.class)).instance().call(ctx, typectx, args, typeActuals);
	}
	
	public Class javaCounterpart() {
		return java.lang.Integer.class;
	}
	
}
