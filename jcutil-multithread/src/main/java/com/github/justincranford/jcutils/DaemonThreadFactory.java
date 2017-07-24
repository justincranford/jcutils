package com.github.justincranford.jcutils;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Same as Executors$DefaultThreadFactory except namePrefix="daemonpool-", and configurable Thread.isDaemon and Thread.priority.
 */
@SuppressWarnings("hiding")
public final class DaemonThreadFactory implements ThreadFactory {
    private static final AtomicInteger POOL_NUMBER = new AtomicInteger(1);

	private final AtomicInteger	threadNumber = new AtomicInteger(1);
	private final ThreadGroup	group;
	private final String		namePrefix;
	private final boolean       isDaemon;
	private final int			priority;

	public DaemonThreadFactory(final boolean isDaemon, final int priority) {
		this(((isDaemon?"daemonpool":"nondaemonpool") + POOL_NUMBER.getAndIncrement() + "-thread-"), isDaemon, priority);
	}

	public DaemonThreadFactory(final String namePrefix, final boolean isDaemon, final int priority) {
        final SecurityManager s = System.getSecurityManager();
        this.group      = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
        this.namePrefix = namePrefix;
        this.isDaemon   = isDaemon;
		this.priority   = priority;
	}

	@Override
	public Thread newThread(final Runnable runnable) {
		final Thread thread = new Thread(this.group, runnable, this.namePrefix + this.threadNumber.getAndIncrement(), 0);
		thread.setDaemon(this.isDaemon);
		thread.setPriority(this.priority);
		return thread;
	}
}