package orc.type.ground;

import java.util.List;

import orc.ast.oil.expression.argument.Argument;
import orc.error.compiletime.typing.TypeException;
import orc.type.Type;
import orc.type.TypingContext;
import orc.type.java.ClassTycon;

public class IntegerType extends NumberType {
	
	public String toString() { return "Integer"; }
	
	public Type call(TypingContext ctx, List<Argument> args, List<Type> typeActuals) throws TypeException {
		return (new ClassTycon(java.lang.Integer.class)).instance().call(ctx, args, typeActuals);
	}
	
	public Class javaCounterpart() {
		return java.lang.Integer.class;
	}
	
}
