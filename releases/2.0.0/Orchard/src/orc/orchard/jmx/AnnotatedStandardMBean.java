//
// AnnotatedStandardMBean.java -- Java class AnnotatedStandardMBean
// Project Orchard
//
// $Id$
//
// Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.orchard.jmx;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;

import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;

/**
 * Mostly copied from Eamon McManus's java.net article "Adding information to a
 * Standard MBean interface using annotations."
 * http://weblogs.java.net/blog/emcmanus/archive/2005/07/adding_informat.html
 * 
 * @author quark
 */
public class AnnotatedStandardMBean extends StandardMBean {
	public <T> AnnotatedStandardMBean(final T impl, final Class<T> mbeanInterface) throws NotCompliantMBeanException {
		super(impl, mbeanInterface);
	}

	protected AnnotatedStandardMBean(final Class<?> mbeanInterface) throws NotCompliantMBeanException {
		super(mbeanInterface);
	}

	private static final HashMap<String, Class<?>> primitiveClasses = new HashMap<String, Class<?>>();
	static {
		final Class<?>[] prims = { byte.class, short.class, int.class, long.class, float.class, double.class, char.class, boolean.class, };
		for (final Class<?> c : prims) {
			primitiveClasses.put(c.getName(), c);
		}
	}

	static Class<?> classForName(final String name, final ClassLoader loader) throws ClassNotFoundException {
		Class<?> c = primitiveClasses.get(name);
		if (c == null) {
			c = Class.forName(name, false, loader);
		}
		return c;
	}

	private static Method methodFor(final Class<?> mbeanInterface, final MBeanOperationInfo op) {
		final MBeanParameterInfo[] params = op.getSignature();
		final String[] paramTypes = new String[params.length];
		for (int i = 0; i < params.length; i++) {
			paramTypes[i] = params[i].getType();
		}

		return findMethod(mbeanInterface, op.getName(), paramTypes);
	}

	private static Method findMethod(final Class<?> mbeanInterface, final String name, final String... paramTypes) {
		final ClassLoader loader = mbeanInterface.getClassLoader();
		final Class<?>[] paramClasses = new Class<?>[paramTypes.length];
		try {
			for (int i = 0; i < paramTypes.length; i++) {
				paramClasses[i] = classForName(paramTypes[i], loader);
			}
			return mbeanInterface.getMethod(name, paramClasses);
		} catch (final SecurityException e) {
			throw new AssertionError(e);
		} catch (final NoSuchMethodException e) {
			throw new AssertionError(e);
		} catch (final ClassNotFoundException e) {
			throw new AssertionError(e);
		}
	}

	private static <A extends Annotation> A getParameterAnnotation(final Method m, final int paramNo, final Class<A> annot) {
		for (final Annotation a : m.getParameterAnnotations()[paramNo]) {
			if (annot.isInstance(a)) {
				return annot.cast(a);
			}
		}
		return null;
	}

	@Override
	protected String getDescription(final MBeanOperationInfo op) {
		String descr = op.getDescription();
		final Method m = methodFor(getMBeanInterface(), op);
		if (m != null) {
			final JMXDescription d = m.getAnnotation(JMXDescription.class);
			if (d != null) {
				descr = d.value();
			}
		}
		return descr;
	}

	@Override
	protected String getParameterName(final MBeanOperationInfo op, final MBeanParameterInfo param, final int paramNo) {
		String name = param.getName();
		final Method m = methodFor(getMBeanInterface(), op);
		if (m != null) {
			final JMXParam pname = getParameterAnnotation(m, paramNo, JMXParam.class);
			if (pname != null) {
				name = pname.value();
			}
		}
		return name;
	}
}
