/**
 * Created on February 8 2007
 */
package orc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.List;

import orc.runtime.Token;
import orc.runtime.nodes.Node;

/**
 * Class for processing configuration options. Such options can be provided via command line 
 * arguments or obtained though environment variables, and could also be read from other sources. 
 * 
 * Note that this class does not set up the runtime environment (for example, by instantiating
 * site bindings or defining closures); it only collects the files that will provide those
 * bindings.
 * 
 * TODO Name environment variables from which Orc can extract these options.
 * 
 * @author dkitchin
 *
 */
public class Config {

	Node target;
	Boolean debug = false;
	List<File> bindings;
	List<Object> params;
	Integer maxpub = null;
	InputStream instream;
	
	public Config()
	{
		this.params = new LinkedList<Object>();
		this.bindings = new LinkedList<File>();
	}
	
	public void processArgs(String[] args)
	{
		int i = 0;
		
		instream = System.in;
		File outputfile = null;
		Integer outport = null;
		String outhost = null;

		while (i < args.length){
			if (args[i].equals("-debug")) {
				i++;
				debug = true;
			}
			/*else if (args[i].equals("-rundump")) {
				// initialize the engine from a previously dumped token
				i++;
				dumpfile = new File(args[i++]);
			}*/
			else if (args[i].equals("-pub")) {
				// quit after publishing maxpub values
				i++;
				maxpub = new Integer(args[i++]);
			}
			else if (args[i].equals("-o")) {
				// publish values to outputfile (as serialized Java objects)
				i++;
				outputfile = new File(args[i++]);
			}
			else if (args[i].equals("-os")) {
               // publish values to socket (port, host) (as serialized Java objects)
				i++;
				outport = new Integer(args[i++]);
				outhost = new String(args[i++]);
			}
			else if (args[i].equals("-s")) {
				// add a string to the parameters passed to the program
				i++;
				params.add(new String(args[i++]));
			}
			else if (args[i].equals("-i")) {
				// add an integer to the parameters passed to the program
				i++;
				params.add(new Integer(args[i++]));
			}
			else if (args[i].equals("-l")) {
				// add this file to the list of bindings to load
				i++;
				bindings.add(new File(args[i++]));
			} else
				try {
					instream = new FileInputStream(args[i++]);
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		}

		
		
		
		if (outport != null){
        	Socket sock;
        	ObjectOutputStream oos;
        	try{
	        	sock = new Socket(outhost, outport);
	        	oos = new ObjectOutputStream(sock.getOutputStream());
		        target = new WriteResult(oos);
	        	oos.close();
	        	sock.close();
        	}
        	catch (UnknownHostException e) {
		        System.err.println("Don't know about host: " + outhost +".");
		        System.exit(1);
		    } catch (IOException e) {
		        System.err.println("Couldn't get I/O for the connection to: "
		        		            + outhost + " on port " + outport +".");
		        System.exit(1);
		     
		    } 
		    
		    }
        else if (outputfile != null) {
        	FileOutputStream fos;
        	ObjectOutputStream oos;
			try {
				fos = new FileOutputStream(outputfile);
				oos = new ObjectOutputStream(fos);
				target = new WriteResult(oos);
				oos.close();
				fos.close();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
        else 
			target = new PrintResult();
		
		}
	
	public void processEnvVars()
	{
	}
	
	public Node getTarget()
	{
		return target;
	}
	
	public List<Object> getArgv()
	{
		return params;
	}
	
	
	public Boolean debugMode()
	{
		return debug;
	}
	
	public Integer maxPubs()
	{
		return maxpub;
	}
	
	public InputStream getInstream()
	{
		return instream;
	}
	
	public List<File> getBindings()
	{
		return bindings;
	}
	
}

/**
 * A special node that prints its output.
 * Equivalent to
 * <pre>
 *    P >x> println(x)
 * </pre>
 * @author wcook
 */
class PrintResult extends Node {
	private static final long serialVersionUID = 1L;

	public void process(Token t) {
		Object val = t.getResult();
		t.getEngine().addPub(1); // keep track of how many values have been published
		System.out.println(val.toString());
		System.out.flush();
	}
}

/**
 * A special node that writes its output values on the given ObjectOutputStream.
 * Equivalent to (where F is the output file)
 * <pre>
 *    P >x> F.write(x)
 * </pre>
 * @author mbickford
 */
class WriteResult extends Node {
	private static final long serialVersionUID = 1L;
	ObjectOutputStream out;
	public WriteResult(ObjectOutputStream s){
		this.out = s;
	}
	public void process(Token t) {
		Object val = t.getResult();
		try {
			out.writeObject(val);
			t.getEngine().addPub(1); // keep track of how many values have been published
		} catch (IOException e) {
			System.out.println("Couldn't write a value to the output file.");
			e.printStackTrace();
		}
	}
}
