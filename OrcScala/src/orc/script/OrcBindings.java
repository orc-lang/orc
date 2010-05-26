//
// OrcBindings.java -- Java class OrcBindings
// Project OrcJava
//
// $Id$
//
// Created by jthywiss on May 26, 2010.
//
// Copyright (c) 2010 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.script;

import java.util.Map;

import javax.script.SimpleBindings;

/**
 * An extended implementation of <code>javax.script.Bindings</code>
 * with type-specific get and put methods.
 *
 * @author jthywiss
 */
public class OrcBindings extends SimpleBindings {

	/**
	 * Constructs an object of class OrcBindings.
	 */
	public OrcBindings() {
		super();
	}

	/**
	 * Constructs an object of class OrcBindings.
	 * 
	 * @param m
	 */
	public OrcBindings(final Map<String, Object> m) {
		super(m);
	}

	/**
	 * @param key
	 * @param value
	 */
	public void putInt(final String key, final int value) {
		put(key, Integer.toString(value));
	}

	/**
	 * @param key
	 * @param def
	 * @return
	 */
	public int getInt(final String key, final int def) {
		int result = def;
		try {
			final Object value = get(key);
			if (value != null && value instanceof String) {
				result = Integer.parseInt((String) value);
			}
		} catch (final NumberFormatException e) {
			// Ignoring exception causes specified default to be returned
		}

		return result;
	}

	/**
	 * @param key
	 * @param value
	 */
	public void putLong(final String key, final long value) {
		put(key, Long.toString(value));
	}

	/**
	 * @param key
	 * @param def
	 * @return
	 */
	public long getLong(final String key, final long def) {
		long result = def;
		try {
			final Object value = get(key);
			if (value != null && value instanceof String) {
				result = Long.parseLong((String) value);
			}
		} catch (final NumberFormatException e) {
			// Ignoring exception causes specified default to be returned
		}

		return result;
	}

	/**
	 * @param key
	 * @param value
	 */
	public void putBoolean(final String key, final boolean value) {
		put(key, String.valueOf(value));
	}

	/**
	 * @param key
	 * @param def
	 * @return
	 */
	public boolean getBoolean(final String key, final boolean def) {
		boolean result = def;
		final Object value = get(key);
		if (value != null && value instanceof String) {
			if (((String) value).equalsIgnoreCase("true")) {
				result = true;
			} else if (((String) value).equalsIgnoreCase("false")) {
				result = false;
			}
		}

		return result;
	}

	/**
	 * @param key
	 * @param value
	 */
	public void putFloat(final String key, final float value) {
		put(key, Float.toString(value));
	}

	/**
	 * @param key
	 * @param def
	 * @return
	 */
	public float getFloat(final String key, final float def) {
		float result = def;
		try {
			final Object value = get(key);
			if (value != null && value instanceof String) {
				result = Float.parseFloat((String) value);
			}
		} catch (final NumberFormatException e) {
			// Ignoring exception causes specified default to be returned
		}

		return result;
	}

	/**
	 * @param key
	 * @param value
	 */
	public void putDouble(final String key, final double value) {
		put(key, Double.toString(value));
	}

	/**
	 * @param key
	 * @param def
	 * @return
	 */
	public double getDouble(final String key, final double def) {
		double result = def;
		try {
			final Object value = get(key);
			if (value != null && value instanceof String) {
				result = Double.parseDouble((String) value);
			}
		} catch (final NumberFormatException e) {
			// Ignoring exception causes specified default to be returned
		}

		return result;
	}

}
