package orc.lib.net;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import orc.Orc;
import orc.error.runtime.JavaException;
import orc.error.runtime.TokenException;
import orc.runtime.Args;
import orc.runtime.sites.ThreadedSite;
import orc.runtime.sites.java.ThreadedObjectProxy;
import orc.runtime.values.Value;

import org.apache.axis.wsdl.toJava.Emitter;
import org.apache.axis.wsdl.toJava.GeneratedFileInfo;
import org.apache.axis.wsdl.toJava.GeneratedFileInfo.Entry;

/**
 * JAX-RPC-based webservice site.
 * Stubs for the service are generated and compiled on the fly.
 * 
 * <p>This example should get you started:
 * <pre>
 * val Google = Webservice("http://api.google.com/GoogleSearch.wsdl")
 * val GoogleDevKey = "YOURDEVKEY"
 * def GoogleSearch(keywords) = Google.doGoogleSearch(
 * 	GoogleDevKey, keywords, 0, 10, true, "", true, "", "", "")
 *
 * each(GoogleSearch("test").getResultElements()) >r>
 * 	r.getURL()
 * </pre>
 * 
 * <p>TODO: allow webservices to provide constructors for complex objects.
 * @author quark, unknown
 */
public class Webservice extends ThreadedSite {
	/**
	 * Compile class files.
	 */
	@SuppressWarnings("unchecked")
	private static void javac(File tmpdir, List sourcesList) {
        // build argument list
		String[] sources = ((List<String>)sourcesList).toArray(new String[]{});
		String[] args = new String[sources.length + 2];
		args[0] = "-cp";
		args[1] = classpath;
		int i = 2;
		for (String source : sources) {
			args[i++] = source;
		}
		com.sun.tools.javac.Main.compile(args);
	}

	/** Cache the classpath on load. */
	private static String classpath = inferClasspath();
	/**
	 * Infer the classpath based on classloader information. This is a total
	 * hack which I borrowed from CXF's DynamicClientFactory, with minor
	 * changes.
	 */
	private static String inferClasspath() {
		String pathsep = System.getProperty("path.separator");
		ClassLoader leaf = Webservice.class.getClassLoader();
		ClassLoader root = ClassLoader.getSystemClassLoader().getParent();
		StringBuffer out = new StringBuffer();
		for (; leaf != null && !leaf.equals(root); leaf = leaf.getParent()) {
			if (!(leaf instanceof URLClassLoader)) continue;
			URL[] urls = ((URLClassLoader)leaf).getURLs();
			if (urls == null) continue;
			for (URL url : urls) {
				if (!url.getProtocol().startsWith("file")) continue;
				File file = new File(url.getPath());
				if (file.exists()) {
					out.append(file.getAbsolutePath());
					out.append(pathsep);
					// If the file is a JAR we should
					// include its classpath as well,
					// but since I know none of the libraries
					// we need use this feature I'll ignore it.
				}
			}
		}
		return out.toString();
	}
	
	public Value evaluate(Args args) throws TokenException {
		// Create a temporary directory to host compilation of stubs
		final File tmpdir;
		try {
			tmpdir = Orc.createTmpdir(new Integer(hashCode()).toString());
		} catch (IOException e) {
			throw new JavaException(e);
		}
		try {
			// Generate stub source files.
			// Emitter is the class that does all of the file creation for the
			// WSDL2Java tool. Using Emitter will allow us to get immediate
			// access to the class names that it generates.
			Emitter emitter = new Emitter();
			emitter.setOutputDir(tmpdir.toString());
			emitter.run(args.stringArg(0));
			GeneratedFileInfo info = emitter.getGeneratedFileInfo();
			
			// Compile stub source files
			javac(tmpdir, info.getFileNames());

			URLClassLoader cl = new URLClassLoader(new URL[]{tmpdir.toURL()},
					Webservice.class.getClassLoader());
			// ensure all of the service's classes are loaded into the VM
			// FIXME: is this necessary?
			for (Object name : info.getClassNames()) cl.loadClass((String)name);
			List<Entry> stubs = (ArrayList<Entry>) info.findType("interface");
			Class stub = null;
			Class locator = null;
			for (Entry e : stubs) {
				Class c = cl.loadClass(e.className);
				if (c.getName().endsWith("Port")
						|| c.getName().endsWith("PortType")) {
					stub = cl.loadClass(e.className);
					break;
				}
				for (Class iface : c.getInterfaces()) {
					if (iface.getName().equals("java.rmi.Remote")) {
						stub = cl.loadClass(e.className);
						break;
					}
				}
			}

			if (stub == null) {
				throw new Error("Unable to find stub among port interfaces");
			}

			List<Entry> services = (ArrayList<Entry>) info.findType("service");

			for (Entry e : services) {
				if (e.className.endsWith("Locator")) {
					locator = cl.loadClass(e.className);
				}
			}

			if (locator == null) {
				throw new Error("Unable to find Locator among services");
			}

			Method[] locatorMethods = locator.getMethods();
			Method getStub = null;

			// use the stub with the default no-arg constructor
			for (int i = 0; i < locatorMethods.length; i++) {
				if (locatorMethods[i].getReturnType().equals(stub)
						&& locatorMethods[i].getParameterTypes().length == 0) {
					getStub = locatorMethods[i];
				}
			}

			if (getStub == null) {
				throw new Error("Unable to find getStub method within Locator");
			}

			Object arglist[] = new Object[0];
			Object locatorObject = locator.newInstance();
			Object stubObject = getStub.invoke(locatorObject, arglist);

			return new ThreadedObjectProxy(stubObject) {
				// delete the temporary directory when the
				// service is no longer accessible
				public void finalize() {
					Orc.deleteDirectory(tmpdir);
				}
			};
		} catch (Exception e) {
			// TODO Auto-generated catch block
			throw new JavaException(e);
		}
	}
}
