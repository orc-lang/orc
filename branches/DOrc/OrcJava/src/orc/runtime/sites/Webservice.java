package orc.runtime.sites;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import orc.runtime.Args;
import orc.runtime.Token;
import orc.runtime.sites.java.ObjectProxy;
import orc.runtime.values.Value;

import org.apache.axis.wsdl.toJava.Emitter;
import org.apache.axis.wsdl.toJava.GeneratedFileInfo;
import org.apache.axis.wsdl.toJava.GeneratedFileInfo.Entry;

public class Webservice extends EvalSite {
    
    private static final String OUTPUT_DIR = "webservices";
    private static final String CLASSPATH = 
        String.format("webservices%slib%saxis.jar%slib%sjaxrpc.jar", File.pathSeparator, File.separator, File.pathSeparator, File.separator);

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
        emitter.setOutputDir(OUTPUT_DIR);
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
                            CLASSPATH,
                            file });
        }
        
        
    }

    @Override
    public void callSite(Args args, Token caller) {

        try {
            // take the passed URL and create java code from it
            GeneratedFileInfo info = createJavaCode(args.stringArg(0));
            
            // compile that java code
            compileJavaCode(info);
            

            List<Entry> stubs = (ArrayList<Entry>) info.findType("interface");

            
            for(Entry e : stubs){
                Class c = Class.forName(e.className);
                if(c.getName().endsWith("Port") || c.getName().endsWith("PortType")) {
                    stub = Class.forName(e.className);
                    break;
                }
                for (Class iface : c.getInterfaces()) {
                    if (iface.getName().equals("java.rmi.Remote")) {
                        stub = Class.forName(e.className);
                        break;
                    }
                }
            }
            
            if (stub == null) {
                throw new Error("Unable to find stub among port interfaces");
            }           

            List<Entry> services = (ArrayList<Entry>) info.findType("service");
            
            for(Entry e : services){
                if(e.className.endsWith("Locator")){
                    locator = Class.forName(e.className);
                }
            }
            
            if (locator == null) {
                throw new Error("Unable to find Locator among services");
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

            if (getStub == null) {
                throw new Error("Unable to find getStub method within Locator");
            }

            Object arglist[] = new Object[0];
            Object locatorObject = locator.newInstance();
            Object stubObject = getStub.invoke(locatorObject, arglist);

            op = new ObjectProxy(stubObject);
            
            caller.resume(evaluate(args));
            
        } catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    @Override
    public Value evaluate(Args args) {
        return op;
    }

}
