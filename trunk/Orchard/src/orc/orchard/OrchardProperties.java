//
// OrchardProperties.java -- Java class OrchardProperties
// Project Orchard
//
// $Id$
//
// Copyright (c) 2011 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.orchard;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

/**
 * Store global properties. See uses of this class to see which properties are
 * supported.
 * 
 * @author quark
 */
public final class OrchardProperties {
	private static Properties props = null;

	private static void initIfNeeded() {
		if (props == null) {
			props = new Properties();
			try {
				final InputStream data = OrchardProperties.class.getResourceAsStream("orchard.properties");
				if (data == null) {
					throw new FileNotFoundException("orchard.properties");
				}
				props.load(data);
			} catch (final IOException e) {
				throw new AssertionError(e);
			}
		}
	}

	private OrchardProperties() {
	}

	protected static Map getMap() {
		initIfNeeded();
		return (Map) props;
	}

	public static void setProperty(final String name, final String value) {
		initIfNeeded();
		props.setProperty(name, value);
	}

	public static String getProperty(final String name) {
		initIfNeeded();
		return props.getProperty(name);
	}

	public static Integer getInteger(final String name) {
		initIfNeeded();
		final String out = props.getProperty(name);
		if (out == null || out.equals("null")) {
			return null;
		}
		return Integer.parseInt(out);
	}

	public static int getInteger(final String name, final int defaultValue) {
		initIfNeeded();
		final String out = props.getProperty(name);
		if (out == null || out.equals("null")) {
			return defaultValue;
		}
		return Integer.parseInt(out);
	}

	public static boolean getBoolean(final String name, final boolean defaultValue) {
		initIfNeeded();
		final String out = props.getProperty(name);
		if (out == null || out.equals("null")) {
			return defaultValue;
		}
		return out.equals("true");
	}
}
