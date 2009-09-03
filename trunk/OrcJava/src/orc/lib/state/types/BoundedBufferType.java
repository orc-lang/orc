package orc.lib.state.types;

import java.util.List;

import orc.error.compiletime.typing.TypeException;
import orc.type.Type;
import orc.type.structured.ArrowType;
import orc.type.structured.DotType;
import orc.type.structured.ListType;
import orc.type.tycon.MutableContainerType;

public class BoundedBufferType extends MutableContainerType {

	public String toString() {
		return "BoundedBuffer";
	}
	
	public Type makeCallableInstance(List<Type> params) throws TypeException {
		/* We know that Buffer has exactly one type parameter */
		Type T = params.get(0);
		
		DotType dt = new DotType(/* no default behavior */);
		dt.addField("get", new ArrowType(T));
		dt.addField("getnb", new ArrowType(T));
		dt.addField("put", new ArrowType(T, Type.TOP));
		dt.addField("putnb", new ArrowType(T, Type.TOP));
		dt.addField("close", new ArrowType(Type.TOP));
		dt.addField("closenb", new ArrowType(Type.TOP));
		dt.addField("isClosed", new ArrowType(Type.BOOLEAN));
		dt.addField("getOpen", new ArrowType(Type.INTEGER));
		dt.addField("getBound", new ArrowType(Type.INTEGER));
		dt.addField("getAll", new ArrowType(ListType.listOf(T)));
		return dt;
	}
	
}
