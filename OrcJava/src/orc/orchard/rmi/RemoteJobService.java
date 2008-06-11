package orc.orchard.rmi;

import orc.orchard.JobConfiguration;
import orc.orchard.Publication;
import orc.orchard.interfaces.JobService;

/**
 * This class exists solely to force the type checker to verify the concrete
 * types on the arguments.
 * 
 * @author quark
 */
public interface RemoteJobService extends JobService<JobConfiguration, Publication> {

}
