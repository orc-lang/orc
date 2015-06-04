//
// Webservice.java -- Java class Webservice
// Project OrcSites
//
// $Id$
//
// Copyright (c) 2009 The University of Texas at Austin. All rights reserved.
//
// Use and redistribution of this file is governed by the license terms in
// the LICENSE file found in the project's top-level directory and also found at
// URL: http://orc.csres.utexas.edu/license.shtml .
//

package orc.lib.net;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.UUID;

import orc.Handle;
import orc.error.runtime.JavaException;
import orc.error.runtime.TokenException;
import orc.values.sites.compatibility.Args;
import orc.values.sites.compatibility.SiteAdaptor;

import org.apache.axis.wsdl.toJava.Emitter;
import org.apache.axis.wsdl.toJava.GeneratedFileInfo;

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
public class Webservice extends SiteAdaptor {
	/**
	 * Compile class files.
	 */
	private static void javac(final File tmpdir, final List<?> sourcesList) {
		// build argument list
		@SuppressWarnings("unchecked")
		final String[] sources = ((List<String>) sourcesList).toArray(new String[] {});
		final String[] args = new String[sources.length + 4];
		args[0] = "-cp";
		args[1] = classpath;
		args[2] = "-nowarn";
		args[3] = "-noExit";
		int i = 4;
		for (final String source : sources) {
			args[i++] = source;
		}
		org.eclipse.jdt.internal.compiler.batch.Main.main(args);
	}

	/** Cache the classpath on load. */
	private static String classpath = inferClasspath();

	/**
	 * Infer the classpath based on classloader information. This is a total
	 * hack which I borrowed from CXF's DynamicClientFactory, with minor
	 * changes.
	 */
	private static String inferClasspath() {
		final String pathsep = System.getProperty("path.separator");
		ClassLoader leaf = Webservice.class.getClassLoader();
		final ClassLoader root = ClassLoader.getSystemClassLoader().getParent();
		final StringBuffer out = new StringBuffer();
		for (; leaf != null && !leaf.equals(root); leaf = leaf.getParent()) {
			if (!(leaf instanceof URLClassLoader)) {
				continue;
			}
			final URL[] urls = ((URLClassLoader) leaf).getURLs();
			if (urls == null) {
				continue;
			}
			for (final URL url : urls) {
				if (!url.getProtocol().startsWith("file")) {
					continue;
				}
				final File file = new File(url.getPath());
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

	@Override
	public void callSite(final Args args, final Handle caller) {
		try {
			// Create a temporary directory to host compilation of stubs
			final File tmpdir = new File(System.getProperty("java.io.tmpdir"), "orc-" + UUID.randomUUID().toString());
			final Object out = evaluate(args, tmpdir);
			deleteDirectory(tmpdir);
			if (out == null) {
				caller.halt();
			} else {
				caller.publish(out);
			}
		} catch (final TokenException e) {
			caller.$bang$bang(e);
		}
	}

	/** Delete a directory recursively */
	private final boolean deleteDirectory(final File directory) {
		boolean out = true;
		final File[] fileArray = directory.listFiles();
		if (fileArray != null) {
			for (final File f : fileArray) {
				out = deleteDirectory(f) && out;
			}
		}
		return directory.delete() && out;
	}

	public Object evaluate(final Args args, final File tmpdir) throws TokenException {
		try {
			// Generate stub source files.
			// Emitter is the class that does all of the file creation for the
			// WSDL2Java tool. Using Emitter will allow us to get immediate
			// access to the class names that it generates.
			final Emitter emitter = new Emitter();
			emitter.setOutputDir(tmpdir.toString());
			emitter.run(args.stringArg(0));
			final GeneratedFileInfo info = emitter.getGeneratedFileInfo();

			// Compile stub source files
			javac(tmpdir, info.getFileNames());

			final URLClassLoader cl = new URLClassLoader(new URL[] { tmpdir.toURI().toURL() }, Webservice.class.getClassLoader());
			// ensure all of the service's classes are loaded into the VM
			// FIXME: is this necessary?
			for (final Object name : info.getClassNames()) {
				cl.loadClass((String) name);
			}
			final List<?> stubs = info.findType("interface");
			Class<?> stub = null;
			Class<?> locator = null;
			for (final Object o : stubs) {
				final GeneratedFileInfo.Entry e = (GeneratedFileInfo.Entry) o;
				final Class<?> c = cl.loadClass(e.className);
				if (c.getName().endsWith("Port") || c.getName().endsWith("PortType")) {
					stub = cl.loadClass(e.className);
					break;
				}
				for (final Class<?> iface : c.getInterfaces()) {
					if (iface.getName().equals("java.rmi.Remote")) {
						stub = cl.loadClass(e.className);
						break;
					}
				}
			}

			if (stub == null) {
				cl.close();
				throw new Exception("Unable to find stub among port interfaces");
			}

			final List<?> services = info.findType("service");

			for (final Object o : services) {
				final GeneratedFileInfo.Entry e = (GeneratedFileInfo.Entry) o;
				if (e.className.endsWith("Locator")) {
					locator = cl.loadClass(e.className);
				}
			}

			cl.close();

			if (locator == null) {
				throw new Exception("Unable to find Locator among services");
			}

			final Method[] locatorMethods = locator.getMethods();
			Method getStub = null;

			// use the stub with the default no-arg constructor
			for (final Method locatorMethod : locatorMethods) {
				if (locatorMethod.getReturnType().equals(stub) && locatorMethod.getParameterTypes().length == 0) {
					getStub = locatorMethod;
				}
			}

			if (getStub == null) {
				throw new Exception("Unable to find getStub method within Locator");
			}

			final Object arglist[] = new Object[0];
			final Object locatorObject = locator.newInstance();
			final Object stubObject = getStub.invoke(locatorObject, arglist);

			return stubObject;//new ThreadedObjectProxy(stubObject);
		} catch (final Exception e) {
			throw new JavaException(e);
		}
	}
}
