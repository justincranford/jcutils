package com.github.justincranford.jcutils;

import org.junit.Assert;
import org.junit.Test;

@SuppressWarnings("static-method")
public class TestDaemonThreadFactoryUtil {
	@Test
	public void testDaemonThreadMinPriority() throws Exception {
		testDaemonThreadAnyPriority(true, Thread.MIN_PRIORITY);
	}

	@Test
	public void testNonDaemonThreadNormPriority() throws Exception {
		testDaemonThreadAnyPriority(false, Thread.NORM_PRIORITY);
	}

	private static void testDaemonThreadAnyPriority(final boolean isDaemon, final int priority) {
		final Runnable runnable = null;
		final DaemonThreadFactory nonDaemonThreadFactoryUtil = new DaemonThreadFactory(isDaemon, priority);
		final Thread newThread = nonDaemonThreadFactoryUtil.newThread(runnable);
		Assert.assertEquals(Boolean.valueOf(isDaemon), Boolean.valueOf(newThread.isDaemon()));
		Assert.assertEquals(priority, newThread.getPriority());
	}
}