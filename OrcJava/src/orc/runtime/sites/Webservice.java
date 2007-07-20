package orc.runtime.sites;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.wsdl.Definition;
import javax.wsdl.Service;
import javax.xml.namespace.QName;

import orc.runtime.OrcEngine;
import orc.runtime.Token;
import orc.runtime.sites.java.MethodProxy;
import orc.runtime.sites.java.ObjectProxy;
import orc.runtime.values.GroupCell;
import orc.runtime.values.Tuple;
import orc.runtime.values.Value;

import org.apache.axis.description.ServiceDesc;
import org.apache.axis.encoding.TypeMapping;
import org.apache.axis.wsdl.toJava.Emitter;
import org.apache.axis.wsdl.toJava.GeneratedFileInfo;
import org.apache.axis.wsdl.toJava.GeneratedFileInfo.Entry;

public class Webservice extends EvalSite {

	// Emitter is the class that does all of the file creation for the WSDL2Java
	// tool.  Using Emitter will allow us to get immediate access to the class names that it
	// generates
	
	private Emitter emitter;

	private Class locator;

	private Class stub;

	ObjectProxy op;

	// create the java code and get info about that code
	public GeneratedFileInfo createJavaCode(String url) throws Exception {
		emitter = new Emitter();
		emitter.setOutputDir("./webservices/");
		emitter.run(url);
		
		return emitter.getGeneratedFileInfo();
	}

	// compile all of the class files in real-time
	public void compileJavaCode(GeneratedFileInfo info) {
		List<String> fileNames = (ArrayList<String>) info.getFileNames();
		
		for (String file : fileNames) {
			com.sun.tools.javac.Main
					.compile(new String[] {
							"-cp",
							"./webservices:./lib/axis.jar:./lib/jaxrpc.jar",
							file });
		}
		
		
	}

	@Override
	public void callSite(Tuple args, Token returnToken, GroupCell caller,
			OrcEngine engine) {

		try {
			// take the passed URL and create java code from it
			GeneratedFileInfo info = createJavaCode(args.stringArg(0));

			// compile that java code
			compileJavaCode(info);
			

			List<Entry> stubs = (ArrayList<Entry>) info.findType("interface");

			for(Entry e : stubs){
				if(e.className.endsWith("PortType")){
					stub = Class.forName(e.className);
				}
			}
			

			List<Entry> services = (ArrayList<Entry>) info.findType("service");
			
			for(Entry e : services){
				if(e.className.endsWith("Locator")){
					locator = Class.forName(e.className);
				}
			}

			
			Method[] locatorMethods = locator.getMethods();
			Method getStub = null;

			//use the stub with the default no-arg constructor
			for (int i = 0; i < locatorMethods.length; i++) {
				if (locatorMethods[i].getReturnType().equals(stub)
						&& locatorMethods[i].getParameterTypes().length == 0) {
					getStub = locatorMethods[i];
				}
			}

			

			Object arglist[] = new Object[0];
			Object locatorObject = locator.newInstance();
			Object stubObject = getStub.invoke(locatorObject, arglist);

			op = new ObjectProxy(stubObject);
			
			
			returnToken.setResult(evaluate(args));
			engine.activate(returnToken);

		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@Override
	public Value evaluate(Tuple args) {
		return op;
	}

}
