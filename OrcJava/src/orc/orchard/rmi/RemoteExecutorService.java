package orc.orchard.rmi;

import orc.orchard.JobConfiguration;
import orc.orchard.Oil;
import orc.orchard.interfaces.ExecutorService;

/**
 * This class exists solely to force the type checker to verify the concrete
 * types on the arguments.
 * 
 * @author quark
 */
public interface RemoteExecutorService extends ExecutorService<Oil, JobConfiguration> {
	
}
