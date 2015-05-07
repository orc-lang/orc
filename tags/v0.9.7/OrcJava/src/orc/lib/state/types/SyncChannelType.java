package orc.lib.state.types;

import java.util.List;

import orc.error.compiletime.typing.TypeException;
import orc.type.ArrowType;
import orc.type.DotType;
import orc.type.ListType;
import orc.type.MutableContainerType;
import orc.type.Type;

public class SyncChannelType extends MutableContainerType {

	public String toString() {
		return "SyncChannel";
	}
	
	public Type makeCallableInstance(List<Type> params) throws TypeException {
		/* We know that SyncChannel has exactly one type parameter */
		Type T = params.get(0);
		
		DotType dt = new DotType(/* no default behavior */);
		dt.addField("get", new ArrowType(T));
		dt.addField("put", new ArrowType(T, Type.TOP));
		return dt;
	}
	
}
