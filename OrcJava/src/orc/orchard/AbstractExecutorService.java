package orc.orchard;

import java.net.URI;
import java.rmi.RemoteException;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import orc.ast.oil.Expr;
import orc.orchard.java.CompilerService;
import orc.orchard.oil.Oil;
import orc.runtime.OrcEngine;
import orc.runtime.nodes.Node;
import orc.runtime.nodes.result.Result;
import orc.runtime.values.Constant;
import orc.runtime.values.Value;


/**
 * Standard implementation of an ExecutorService. Extenders should implement:
 * <ul>
 * <li>subclass of AbstractJobService
 * <li>constructor
 * <li>getDefaultJobConfiguration
 * <li>createJob
 * </ul>
 * 
 * @author quark
 * 
 */
public abstract class AbstractExecutorService implements ExecutorServiceInterface {
	/**
	 * Standard implementation of a JobService. Extenders should only need to
	 * provide a constructor and override onFinish() to clean up any external
	 * resources.
	 * 
	 * @author quark
	 * 
	 */
	public abstract class AbstractJobService implements JobServiceInterface {
		/** Has start() been called yet? */
		private boolean isNew = true;
		/**
		 * This is stored here in case the job implementation or client needs to
		 * refer to it.
		 */
		private JobConfiguration configuration;
		/** The engine will handle all the interesting work of the job. */
		private OrcEngine engine = new OrcEngine();
		/**
		 * Queue publications for retrieval by listen() and publications(). Values
		 * are published directly from the engine to this buffer. listen() will wait
		 * for this buffer to become non-empty, then drain it to pubs and return the
		 * list of drained publications. publications() will return pubs with
		 * pubsBuff concatenated. This object also serves as the lock/monitor for
		 * all modifications to pubsBuff and pubs.
		 * 
		 * Initially this was a BlockingQueue but that did not provide sufficient
		 * control over locking.
		 */
		private List<Publication> pubsBuff = new LinkedList<Publication>();
		/**
		 * History of values published via listen().
		 */
		private List<Publication> pubs = new LinkedList<Publication>();
		private URI uri;

		protected AbstractJobService(URI uri, JobConfiguration configuration, Expr expression) {
			this.uri = uri;
			this.configuration = configuration;
			Node node = expression.compile(new Result() {
				int sequence = 1;
				public void emit(Value v) {
					logger.info("received publication " + sequence);
					synchronized (pubsBuff) {
						pubsBuff.add(new Publication(
								sequence++, new Date(), ((Constant)v).getValue()));
						pubsBuff.notify();
					}
				}
			});
			engine.debugMode = true;
			engine.start(node);
		}
		
		/**
		 * Override this to handle any cleanup which needs to occur when the job is
		 * successfully finished (via finish() or abort()).
		 * 
		 * @throws RemoteException
		 */
		protected abstract void onFinish() throws RemoteException;
		
		private void _onFinish() throws RemoteException {
			jobs.remove(uri);
			onFinish();
		}

		public synchronized void start() throws InvalidJobStateException {
			logger.info("start");
			if (!isNew) throw new InvalidJobStateException(state());
			isNew = false;
			new Thread(engine).start();
		}
		
		public synchronized void finish() throws InvalidJobStateException, RemoteException {
			logger.info("finish");
			if (!engine.isDead()) throw new InvalidJobStateException(state());
			_onFinish();
		}

		public synchronized void abort() throws RemoteException {
			logger.info("abort");
			engine.terminate();
			_onFinish();
		}

		public JobConfiguration configuration() {
			return configuration;
		}

		public List<Publication> listen() throws InvalidJobStateException, UnsupportedFeatureException {
			synchronized (pubsBuff) {
				// wait for the buffer to fill up
				while (pubsBuff.isEmpty()) {
					try {
						pubsBuff.wait();
						break;
					} catch (InterruptedException e) {
						// keep waiting
					}
				}
				// remember these publications for later
				pubs.addAll(pubsBuff);
				// drain the buffer
				List<Publication> out = new LinkedList<Publication>(pubsBuff);
				pubsBuff.clear();
				return out;
			}
		}

		public List<Publication> publications() throws InvalidJobStateException {
			synchronized (pubsBuff) {
				logger.info("publications");
				List<Publication> out = new LinkedList<Publication>(pubs);
				out.addAll(pubsBuff);
				return out;
			}
		}

		public List<Publication> publicationsAfter(int sequence) throws InvalidJobStateException {
			logger.info("publications(" + sequence + ")");
			List<Publication> out = new LinkedList<Publication>();
			try {
				// sequence numbers are guaranteed to correspond to indices in the
				// list, so we can skip directly to the next sequence number
				ListIterator<Publication> it = publications().listIterator(sequence);
				// copy all of the subsequent publications into the output list
				while (it.hasNext()) out.add(it.next());
			} catch (IndexOutOfBoundsException e) {
				// ignore this error
			}
			return out;
		}

		public synchronized String state() {
			logger.info("state");
			if (isNew) return "NEW";
			else if (engine.isDead()) return "DONE";
			else if (engine.isBlocked()) return "WAITING";
			else return "RUNNING";
		}
		
		protected URI getURI() { return uri; }
	}

	protected Logger logger;
	/**
	 * I don't know how to represent local Java objects as URIs, so I'll just
	 * treat them as keys which can be used with lookupJob. That's not so
	 * strange, since to make use of any URI you have to apply some
	 * protocol-specific method to it to get a Java proxy object.
	 */
	private Set<URI> jobs = new HashSet<URI>();

	protected AbstractExecutorService(Logger logger) {
		this.logger = logger;
	}

	protected AbstractExecutorService() {
		this(getDefaultLogger());
	}
	
	protected abstract JobConfiguration getDefaultJobConfiguration();
	
	public URI submit(Oil program) throws QuotaException, InvalidOilException, RemoteException {
		logger.info("submit");
		try {
			return submitConfigured(program, getDefaultJobConfiguration());
		} catch (UnsupportedFeatureException e) {
			// impossible by construction
			throw new AssertionError(e);
		}
	}
	
	public URI submitConfigured(Oil program, JobConfiguration configuration)
		throws QuotaException, InvalidOilException,	UnsupportedFeatureException, RemoteException
	{
		if (configuration.getDebuggable()) {
			throw new UnsupportedFeatureException("Debuggable jobs not supported yet.");
		}
		URI out = createJob(configuration, program.unmarshal());
		jobs.add(out);
		return out;
	}
	
	protected abstract URI createJob(JobConfiguration configuration, Expr expression)
		throws UnsupportedFeatureException, RemoteException;
	
	public Set<URI> jobs() {
		return new HashSet(jobs);
	}
	
	public URI compileAndSubmit(String program) throws QuotaException, InvalidProgramException, InvalidOilException, RemoteException {
		CompilerService compiler = new CompilerService(logger);
		return submit(compiler.compile(program));
	}

	public URI compileAndSubmitConfigured(String program, JobConfiguration configuration) throws QuotaException, InvalidProgramException, InvalidOilException, UnsupportedFeatureException, RemoteException {
		CompilerService compiler = new CompilerService(logger);
		return submitConfigured(compiler.compile(program), configuration);
	}
	
	/**
	 * Generate a unique unguessable identifier for a job.
	 * @return
	 */
	protected String jobID() {
		// This generates a type 4 random UUID having 124 bits of
		// cryptographically secure randomness, which is unguessable
		// and unique enough for our purposes.
		return UUID.randomUUID().toString();
	}

	protected static Logger getDefaultLogger() {
		Logger out = Logger.getLogger(AbstractExecutorService.class.toString());
		return out;
	}
}
