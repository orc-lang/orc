package orc.orchard;

import java.net.URI;
import java.util.Date;
import java.util.EventListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import orc.ast.oil.Expr;
import orc.orchard.errors.InvalidJobStateException;
import orc.orchard.errors.InvalidOilException;
import orc.orchard.errors.QuotaException;
import orc.orchard.errors.UnsupportedFeatureException;
import orc.orchard.java.CompilerService;
import orc.orchard.oil.Oil;
import orc.runtime.OrcEngine;
import orc.runtime.nodes.Node;
import orc.runtime.nodes.result.Result;
import orc.runtime.values.Constant;
import orc.runtime.values.Value;

public final class Executor {
	private Map<String, Job> jobs = new HashMap<String, Job>(); 
	
	public Job createJob(Oil program, JobConfiguration configuration)
			throws QuotaException, InvalidOilException,
			UnsupportedFeatureException
	{
		Job out = new Job(createJobID(), program.unmarshal(), configuration);
		out.onFinish(new Job.FinishListener() {
			public void finished(Job job) {
				jobs.remove(job.getID());
			}
		});
		jobs.put(out.getID(), out);
		return out;
	}
	
	/**
	 * Generate a unique unguessable identifier for a job.
	 * @return
	 */
	private String createJobID() {
		// This generates a type 4 random UUID having 124 bits of
		// cryptographically secure randomness, which is unguessable
		// and unique enough for our purposes.
		return UUID.randomUUID().toString();
	}
	
	public Map<String, Job> jobs() {
		return jobs;
	}
}