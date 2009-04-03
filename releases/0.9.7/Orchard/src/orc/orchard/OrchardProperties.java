package orc.orchard;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Store global properties. See uses of this class to see which properties are
 * supported.
 * 
 * @author quark
 */
public final class OrchardProperties {
	private static Properties props = new Properties();
	static {
		try {
			InputStream data = OrchardProperties.class.getResourceAsStream("orchard.properties");
			if (data == null) throw new FileNotFoundException("orchard.properties");
			props.load(data);
		} catch (IOException e) {
			throw new AssertionError(e);
		}
	}
	private OrchardProperties() {}
	public static void setProperty(String name, String value) {
		props.setProperty(name, value);
	}
	public static String getProperty(String name) {
		return props.getProperty(name);
	}
	public static Integer getInteger(String name) {
		String out = props.getProperty(name);
		if (out == null || out.equals("null")) return null;
		return Integer.parseInt(out);
	}
	public static int getInteger(String name, int defaultValue) {
		String out = props.getProperty(name);
		if (out == null || out.equals("null")) return defaultValue;
		return Integer.parseInt(out);
	}
	public static boolean getBoolean(String name, boolean defaultValue) {
		String out = props.getProperty(name);
		if (out == null || out.equals("null")) return defaultValue;
		return out.equals("true");
	}
}
