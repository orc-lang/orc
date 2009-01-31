package orc.lib.state.types;

import java.util.List;

import orc.type.ArrowType;
import orc.type.DotType;
import orc.type.MutableContainerType;
import orc.type.Type;

public class BufferType extends MutableContainerType {

	public String toString() {
		return "Buffer";
	}
	
	public Type makeCallableInstance(List<Type> params) {
		/* We know that Buffer has exactly one type parameter */
		Type T = params.get(0);
		
		DotType dt = new DotType(/* no default behavior */);
		dt.addField("get", new ArrowType(T));
		dt.addField("put", new ArrowType(T, Type.TOP));
		
		return dt;
	}
	
}
