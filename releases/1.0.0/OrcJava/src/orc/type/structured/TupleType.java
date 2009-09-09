package orc.type.structured;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import orc.env.Env;
import orc.error.compiletime.typing.ArgumentArityException;
import orc.error.compiletime.typing.SubtypeFailureException;
import orc.error.compiletime.typing.TypeException;
import orc.error.compiletime.typing.UncallableTypeException;
import orc.error.compiletime.typing.UnrepresentableTypeException;
import orc.type.Type;
import orc.type.ground.ConstIntType;
import orc.type.ground.IntegerType;
import orc.type.ground.Message;
import orc.type.ground.Top;
import orc.type.inference.Constraint;
import orc.type.tycon.Variance;

public class TupleType extends Type {

	public List<Type> items;

	public TupleType(List<Type> items) {
		this.items = items;
	}
	
	/* Convenience function for constructing pairs */
	public TupleType(Type a, Type b) {
		this.items = new LinkedList<Type>();
		this.items.add(a);
		this.items.add(b);
	}
	
	public boolean subtype(Type that) throws TypeException {
		
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
			
			return true;
		}
		
		return false;
	}
	
	public Type join(Type that) throws TypeException {	
		
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
			return super.join(that);
		}
			
	}
	
	public Type meet(Type that) throws TypeException {
		
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
			return super.meet(that);
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
	
	public Type subst(Env<Type> ctx) throws TypeException {		
		return new TupleType(Type.substAll(items, ctx));
	}
	
	
	
	public Variance findVariance(Integer var) {
		
		Variance result = Variance.CONSTANT;
		
		for (Type T : items) {
			result = result.and(T.findVariance(var));
		}

		return result;
	}
	
	public Type promote(Env<Boolean> V) throws TypeException { 
		
		List<Type> newItems = new LinkedList<Type>();
		for (Type T : items) {
			newItems.add(T.promote(V));
		}
		
		return new TupleType(newItems);
	}
	
	public Type demote(Env<Boolean> V) throws TypeException { 

		List<Type> newItems = new LinkedList<Type>();
		for (Type T : items) {
			newItems.add(T.demote(V));
		}
		
		return new TupleType(newItems);
	}
	
	
	public void addConstraints(Env<Boolean> VX, Type T, Constraint[] C) throws TypeException {
		
		if (T instanceof TupleType) {
			TupleType other = (TupleType)T;
			
			if (other.items.size() != items.size()) {
				throw new SubtypeFailureException(this, T);
			}
			
			for(int i = 0; i < items.size(); i++) {
				Type A = items.get(i);
				Type B = other.items.get(i);
		
				A.addConstraints(VX, B, C);
			}
			
		}
		else {
			super.addConstraints(VX, T, C);
		}
	}
	
	public Set<Integer> freeVars() {
		return Type.allFreeVars(items);
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
	
	@Override
	public orc.ast.xml.type.Type marshal() throws UnrepresentableTypeException {
		orc.ast.xml.type.Type[] newItems = new orc.ast.xml.type.Type[items.size()];
		int i = 0;
		for (Type t : items) {
			newItems[i] = t.marshal();
			++i;
		}
		return new orc.ast.xml.type.TupleType(newItems);
	}
}
