package orc.lib.state.types;

import java.util.List;

import orc.error.compiletime.typing.TypeException;
import orc.type.ArrowType;
import orc.type.DotType;
import orc.type.ListType;
import orc.type.MutableContainerType;
import orc.type.Type;

public class ArrayType extends MutableContainerType {

	public String toString() {
		return "Array";
	}
	
	public Type makeCallableInstance(List<Type> params) throws TypeException {
		Type T = params.get(0);
		
		DotType dt = new DotType(/* no default behavior */);

		Type ArrayOfT = (new ArrayType()).instance(T);
		dt.addField("get", new ArrowType(Type.INTEGER, T));
		dt.addField("set", new ArrowType(Type.INTEGER, T, Type.TOP));
		dt.addField("slice", new ArrowType(Type.INTEGER, Type.INTEGER, ArrayOfT));
		dt.addField("length", new ArrowType(Type.INTEGER));
		dt.addField("fill", new ArrowType(T, Type.TOP));
		return dt;
	}
	
}
