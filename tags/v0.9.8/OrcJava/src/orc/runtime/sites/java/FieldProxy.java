package orc.runtime.sites.java;

import java.lang.reflect.Field;

import orc.error.runtime.JavaException;
import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.sites.DotSite;
import orc.runtime.sites.EvalSite;

public class FieldProxy extends DotSite {
	private Object instance;
	private Field field;
	
	public FieldProxy(Object instance, Field field) {
		this.instance = instance;
		this.field = field;
	}

	@Override
	protected void addMembers() {
		addMember("read", new EvalSite() {
			@Override
			public Object evaluate(Args args) throws TokenException {
				try {
					return field.get(instance);
				} catch (IllegalArgumentException e) {
					throw new JavaException(e);
				} catch (IllegalAccessException e) {
					throw new JavaException(e);
				}
			}
		});
		addMember("write", new EvalSite() {
			@Override
			public Object evaluate(Args args) throws TokenException {
				try {
					field.set(instance, args.getArg(0));
					return signal();
				} catch (IllegalArgumentException e) {
					throw new JavaException(e);
				} catch (IllegalAccessException e) {
					throw new JavaException(e);
				}
			}
		});
	}
}
