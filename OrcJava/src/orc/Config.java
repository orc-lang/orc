/**
 * Created on February 8 2007
 */
package orc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.Reader;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.List;

import orc.runtime.nodes.Node;
import orc.runtime.nodes.Pub;
import orc.runtime.nodes.result.PrintResult;
import orc.runtime.nodes.result.WriteResult;

/**
 * Class for processing configuration options. Such options could be provided via command line 
 * arguments or obtained though environment variables, and could also be read from other sources. 
 * 
 * Note that this class does not set up the runtime environment (for example, by instantiating
 * site bindings or defining closures); it only collects the files that will provide those
 * bindings.
 * 
 * @author dkitchin
 *
 */
public class Config {

	Node target;
	Boolean debug = false;
	List<String> includes = new LinkedList<String>();
	Integer maxpub = null;
	Reader instream;
	
	public void processArgs(String[] args)
	{
		int i = 0;
		
		instream = new InputStreamReader(System.in);
		File outputfile = null;
		Integer outport = null;
		String outhost = null;

		while (i < args.length){
			if (args[i].equals("-debug")) {
				i++;
				debug = true;
			}
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
			else if (args[i].equals("-i")) {
				// add this file to the list of bindings to load
				i++;
				includes.add(args[i++]);
			} 
			else {
				// This is the name of the source file
				try {
					instream = new FileReader(args[i++]);
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
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
			target = new Pub();
		
		}
	
	public void processEnvVars() 
	{ 
		// TODO: implement environment variable processing of configuration options
	}
	
	public Node getTarget()
	{
		return target;
	}
	
	public Boolean debugMode()
	{
		return debug;
	}
	
	public Integer maxPubs()
	{
		return maxpub;
	}
	
	public Reader getInstream()
	{
		return instream;
	}
	
	public List<String> getIncludes()
	{
		return includes;
	}
	
}