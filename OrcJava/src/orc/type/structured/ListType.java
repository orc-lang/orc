package orc.type.structured;

import java.util.LinkedList;
import java.util.List;

import orc.env.Env;
import orc.error.compiletime.typing.ArgumentArityException;
import orc.error.compiletime.typing.SubtypeFailureException;
import orc.error.compiletime.typing.TypeException;
import orc.error.compiletime.typing.UncallableTypeException;
import orc.type.Type;
import orc.type.tycon.ImmutableContainerType;

public class ListType extends ImmutableContainerType {
		
	public String toString() {
		return "List";
	}
	
	public static Type listOf(Type T) throws TypeException {
		return (new ListType()).instance(T);
	}

}
