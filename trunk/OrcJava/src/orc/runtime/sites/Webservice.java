package orc.runtime.sites;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import orc.error.JavaException;
import orc.error.TokenException;
import orc.runtime.Args;
import orc.runtime.Token;
import orc.runtime.sites.java.ObjectProxy;
import orc.runtime.sites.java.ThreadedObjectProxy;
import orc.runtime.values.Value;

import org.apache.axis.wsdl.toJava.Emitter;
import org.apache.axis.wsdl.toJava.GeneratedFileInfo;
import org.apache.axis.wsdl.toJava.GeneratedFileInfo.Entry;

public class Webservice extends Site {
	// Emitter is the class that does all of the file creation for the WSDL2Java
	// tool. Using Emitter will allow us to get immediate access to the class
	// names that it
	// generates
	private Emitter emitter;
	private Class locator;
	private Class stub;

	ObjectProxy op;

	// create the java code and get info about that code
	public GeneratedFileInfo createJavaCode(String url, File tmpdir) throws Exception {
		emitter = new Emitter();
		emitter.setOutputDir(tmpdir.toString());
		emitter.run(url);

		return emitter.getGeneratedFileInfo();
	}

	// compile all of the class files in real-time
	public void compileJavaCode(GeneratedFileInfo info, File tmpdir) {
		List<String> fileNames = (ArrayList<String>)info.getFileNames();
		String[] fileNamesArray = fileNames.toArray(new String[]{});
		String[] args = new String[]{};
		// combine file names and other args
		String[] allArgs = new String[args.length + fileNamesArray.length];
		for (int i = 0; i < args.length; i++) {
			allArgs[i] = args[i];
		}
		for (int i = 0; i < fileNamesArray.length; i++) {
			allArgs[args.length+i] = fileNamesArray[i];
		}
		// FIXME: this works in eclipse but not in servlet mode --
		// it seems we need to explicitly supply a classpath which
		// includes axis.jar and jaxrpc.jar?
		com.sun.tools.javac.Main.compile(allArgs);
	}
	
	public void callSite(final Args args, final Token caller) {
		new Thread() {
			public void run() {
				try {
					caller.resume(evaluate(args, caller.getEngine().getTmpdir()));
				} catch (TokenException e) {
					caller.error(e);
				}
			}
		}.start();
	}

	public Value evaluate(Args args, File tmpdir) throws TokenException {
		try {
			// take the passed URL and create java code from it
			GeneratedFileInfo info = createJavaCode(args.stringArg(0), tmpdir);

			URLClassLoader cl = new URLClassLoader(new URL[]{tmpdir.toURL()});
			// compile that java code
			compileJavaCode(info, tmpdir);

			List<Entry> stubs = (ArrayList<Entry>) info.findType("interface");

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

			return new ThreadedObjectProxy(stubObject);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			throw new JavaException(e);
		}
	}
}
