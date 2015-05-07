package orc.type;

import java.util.LinkedList;
import java.util.List;

import orc.env.Env;
import orc.error.compiletime.typing.ArgumentArityException;
import orc.error.compiletime.typing.SubtypeFailureException;
import orc.error.compiletime.typing.TypeException;
import orc.error.compiletime.typing.UncallableTypeException;

public class OptionType extends ImmutableContainerType {
		
	public String toString() {
		return "Option";
	}

}
