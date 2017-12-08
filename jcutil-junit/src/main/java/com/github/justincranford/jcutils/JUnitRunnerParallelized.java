package com.github.justincranford.jcutils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.runners.Parameterized;
import org.junit.runners.model.RunnerScheduler;

public class JUnitRunnerParallelized extends Parameterized {
	/*package*/ static final Logger LOG = Logger.getLogger(JUnitRunnerParallelized.class.getName());

	public JUnitRunnerParallelized(Class<?> clazz) throws Throwable {
		super(clazz);
		this.setScheduler(	// INNER CLASS
			new RunnerScheduler() {	// THREAD POOL 1 TO N THREADS (N = JVM AVAILABLE CPUs/vCPUs)
				private final ExecutorService service = new ThreadPool(false).getExecutorService();
				@Override
				public void schedule(Runnable childStatement) {
					this.service.submit(childStatement);
				}
				@Override
				public void finished() {
					try {
						this.service.shutdown();
						this.service.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
					} catch (InterruptedException e) {	// NOSONAR Either re-interrupt this method or rethrow the "InterruptedException".
						LOG.log(Level.WARNING, "Unexpected error during shutdown", e);
						throw new RuntimeException(e);
					}
				}
			}
		);
	}
}