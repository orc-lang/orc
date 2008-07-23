package orc.ast.oil.arg;

import java.util.Set;

import orc.ast.oil.Visitor;
import orc.env.Env;
import orc.runtime.values.Future;
import orc.runtime.values.Value;


/**
 * Field access argument. Embeds a String key.
 * 
 * @author dkitchin
 */

public class Field extends Arg {
	private static final long serialVersionUID = 1L;
	public String key;
	
	public Field(String key)
	{
		this.key = key;
	}
	
	public Future resolve(Env env) {
		return new orc.runtime.values.Field(key);
	}
	
	public String toString() {
		return "[." + key + "]";
	}
	
	@Override
	public <E> E accept(Visitor<E> visitor) {
		return visitor.visit(this);
	}
}