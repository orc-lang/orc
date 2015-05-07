package orc.type;

import java.util.LinkedList;
import java.util.List;

import orc.error.compiletime.typing.ArgumentArityException;
import orc.error.compiletime.typing.SubtypeFailureException;
import orc.error.compiletime.typing.TypeException;
import orc.error.compiletime.typing.UncallableTypeException;

public class TupleType extends Type {

	public List<Type> items;
		
	public TupleType(List<Type> items) {
		this.items = items;
	}
	
	public boolean subtype(Type that) {
		
		if (that instanceof Top) { return true; }
		
		
		if (that instanceof TupleType) {
		
			TupleType other = (TupleType)that;
			
			if (width() != other.width()) {
				return false;
			}
			
			List<Type> otherItems = other.items;
			for(int i = 0; i < width(); i++) {
				Type thisItem = items.get(i);
				Type otherItem = otherItems.get(i);
				
				if (!(thisItem.subtype(otherItem))) { return false; }
			}
		}
		
		return true;
	}
	
	public Type join(Type that) {	
		
		if (that instanceof TupleType) {
			
			TupleType other = (TupleType)that;
			
			if (width() != other.width()) {
				return Type.TOP;
			}
			
			List<Type> otherItems = other.items;
			List<Type> joinItems = new LinkedList<Type>();
			for(int i = 0; i < width(); i++) {
				Type thisItem = items.get(i);
				Type otherItem = otherItems.get(i);
				
				joinItems.add(thisItem.join(otherItem));
			}
			
			return new TupleType(joinItems);
		}
		else {
			return Type.TOP;
		}
			
	}
	
	/* 
	 * A meet of two arrow types is a join of their arg types
	 * and a meet of their result type.
	 */
	public Type meet(Type that) {
		
		if (that instanceof TupleType) {
			
			TupleType other = (TupleType)that;
			
			if (width() != other.width()) {
				return Type.BOT;
			}
			
			List<Type> otherItems = other.items;
			List<Type> meetItems = new LinkedList<Type>();
			for(int i = 0; i < width(); i++) {
				Type thisItem = items.get(i);
				Type otherItem = otherItems.get(i);
				
				meetItems.add(thisItem.meet(otherItem));
			}
			
			return new TupleType(meetItems);
		}
		else {
			return Type.BOT;
		}
	}
	
	
	public Type call(List<Type> args) throws TypeException {

		// TODO: Need a more general solution for messages; this is not quite correct.
		if (args.size() == 1) {
			Type T = args.get(0);
			if (T instanceof Message) {
				Message m = (Message)T;
				// TODO: Make a set of special compiler constants
				if (m.f.key.equals("fits")) {
					return new ArrowType(Type.INTEGER, Type.BOOLEAN);
				}
			}
		}

		
		if (args.size() == 1) {
			
			Type T = args.get(0);
			if (T instanceof ConstIntType) {
				Integer index = ((ConstIntType)T).i;
				if (index < width()) {
					return items.get(index);
				}
				else {
					// TODO: Make this a more specific exception type
					throw new TypeException("Can't access index " + index + " of a " + width() + " element tuple (indices start at 0)");
				}
			}
			else if (T instanceof IntegerType) {
				Type j = Type.BOT;
				for(Type iType : items) { j = j.join(iType); }
				return j;
			}
			else {
				throw new SubtypeFailureException(T, Type.INTEGER);
			}
		}
		else {
			throw new ArgumentArityException(1, args.size());
		}
		
	}
	
	
	public String toString() {
		
		StringBuilder s = new StringBuilder();
		
		s.append('(');
		for (int i = 0; i < width(); i++) {
			if (i > 0) { s.append(", "); }
			s.append(items.get(i));
		}
		s.append(')');
		
		return s.toString();
	}

	public int width() {
		return items.size();
	}
	
	
	
}
