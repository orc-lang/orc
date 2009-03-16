package orc.qos;

import java.io.IOException;

import orc.Config;
import orc.qos.error.QoSOrcException;
import orc.qos.trace.QosTracer;
import orc.runtime.OrcEngine;
import orc.runtime.nodes.Node;

/**
 * Main class for the QoS plugin. 
 * 
 * 
 * @author srosario
 */
public class QOrc {

	public static void process(Config cfg,Node n) {
		//try{
			
			// Configure the runtime engine
			cfg.setTracer(new QosTracer());
			OrcEngine engine = new OrcEngine(cfg);
		
			// Run the Orc program
			engine.run(n);
			
//		} catch (QoSOrcException e) {
//			System.err.println(e);
//		} catch (IOException e) {
//			System.err.println(e);
//		} 
	}
	
}
