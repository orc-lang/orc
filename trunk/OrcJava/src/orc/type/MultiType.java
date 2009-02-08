package orc.type;

import java.util.LinkedList;
import java.util.List;

import orc.ast.oil.arg.Arg;
import orc.env.Env;
import orc.error.compiletime.typing.TypeException;

/**
 * A composite type supporting ad-hoc polymorphic calls.
 * 
 * Contains a list of types; when this type is used in call position,
 * it will be typechecked using each type in the list sequentially until
 * one succeeds.
 * 
 * 
 * @author dkitchin
 *
 */
public class MultiType extends Type {

	List<Type> alts;
	
	
	public MultiType(List<Type> alts) {
		this.alts = alts;
	}
	
	// binary case
	public MultiType(Type A, Type B) {
		this.alts = new LinkedList<Type>();
		alts.add(A);
		alts.add(B);
	}
	
	public boolean subtype(Type that) {
		
		for(Type alt : alts) {
			if (alt.subtype(that)) return true;
		}
		
		return super.subtype(that);
	}
	
	public Type call(Env<Type> ctx, Env<Type> typectx, List<Arg> args, List<Type> typeActuals) throws TypeException {
		
		for(Type alt : alts) {
			try {
				return alt.call(ctx, typectx, args, typeActuals);
			}
			catch (TypeException e) {}
		}
		
		// TODO: Make this more informative
		throw new TypeException("Typing failed for call; no alternatives matched.");
	}
	
}
