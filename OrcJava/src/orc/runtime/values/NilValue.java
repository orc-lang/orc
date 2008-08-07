package orc.runtime.values;

import java.util.LinkedList;
import java.util.List;

import orc.runtime.Token;

public class NilValue extends ListValue {

	public String toString() { return "[]"; }
	
	@Override
	public void uncons(Token caller) {
		caller.die();
	}
	
	@Override
	public void unnil(Token caller) {
		caller.resume(new NilValue());
	}

	@Override
	public List<Object> enlist() {
		
		return new LinkedList<Object>();
	}
	
	
}
