package orc.type.ground;

import java.util.List;

import orc.error.compiletime.typing.TypeException;
import orc.type.Type;

public class StringType extends Type {

	public String toString() { return "String"; }
	
	public Type call(List<Type> args) throws TypeException {
		// HACK: allow String method calls
		return Type.BOT;
	}
}
