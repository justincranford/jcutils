package com.github.justincranford.jcutils;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Assert;
import org.junit.Test;

@SuppressWarnings("static-method")
public class TestJulUtil {
	@Test(expected=IllegalArgumentException.class)
	public void testStaticNullByClassInstance() throws Exception {
		JulUtil.setLogLevel((Class<?>)null, Level.ALL);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testStaticNullByCanonicalName() throws Exception {
		JulUtil.setLogLevel((String)null, Level.ALL);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testInstanceNullByClassInstance() throws Exception {
		try (final JulUtil autoClose = new JulUtil((Class<?>)null, Level.ALL)) {
			// do nothing
		}
	}

	@Test(expected=IllegalArgumentException.class)
	public void testInstanceNullByCanonicalName() throws Exception {
		try (final JulUtil autoClose = new JulUtil((String)null, Level.ALL)) {
			// do nothing
		}
	}

	@Test
	public void testStaticRestoreNullByClassInstance() throws Exception {
		TestJulUtil.testStaticRestoreByClassInstance(null);
	}

	@Test
	public void testStaticRestoreNullByCanonicalName() throws Exception {
		TestJulUtil.testStaticRestoreByCanonicalName(null);
	}

	@Test
	public void testStaticRestoreNonNullByClassInstance() throws Exception {
		TestJulUtil.testStaticRestoreByClassInstance(Level.ALL);
	}

	@Test
	public void testStaticRestoreNonNullByCanonicalName() throws Exception {
		TestJulUtil.testStaticRestoreByCanonicalName(Level.ALL);
	}

	@Test
	public void testInstanceRestoreNullByClassInstance() throws Exception {
		TestJulUtil.testInstanceRestoreByClassInstance(null);
	}

	@Test
	public void testInstanceRestoreNullByCanonicalName() throws Exception {
		TestJulUtil.testInstanceRestoreByCanonicalName(null);
	}

	@Test
	public void testInstanceRestoreNonNullByClassInstance() throws Exception {
		TestJulUtil.testInstanceRestoreByClassInstance(Level.ALL);
	}

	@Test
	public void testInstanceRestoreNonNullByCanonicalName() throws Exception {
		TestJulUtil.testInstanceRestoreByCanonicalName(Level.ALL);
	}

	private static void testStaticRestoreByClassInstance(final Level nullOrNonNullTemporaryLevel) throws Exception {
		final Logger preTestLogger   = Logger.getLogger(JulUtil.class.getName());
		final Level  preTestLogLevel = preTestLogger.getLevel();

		final Level originalLogLevel = JulUtil.setLogLevel(JulUtil.class, nullOrNonNullTemporaryLevel);
		Assert.assertEquals(preTestLogLevel, originalLogLevel);

		final Level temporaryLogLevel = JulUtil.setLogLevel(JulUtil.class, originalLogLevel);
		Assert.assertEquals(temporaryLogLevel, nullOrNonNullTemporaryLevel);
	}

	private static void testInstanceRestoreByClassInstance(final Level nullOrNonNullTemporaryLevel) throws Exception {
		final Logger preTestLogger   = Logger.getLogger(JulUtil.class.getName());
		final Level  preTestLogLevel = preTestLogger.getLevel();

		try (final JulUtil autoClose = new JulUtil(JulUtil.class, nullOrNonNullTemporaryLevel)) {
			final Level temporaryLogLevel = preTestLogger.getLevel();
			Assert.assertEquals(temporaryLogLevel, nullOrNonNullTemporaryLevel);
		}
		final Level postTestLogLevel = preTestLogger.getLevel();
		Assert.assertEquals(preTestLogLevel, postTestLogLevel);
	}

	private static void testStaticRestoreByCanonicalName(final Level nullOrNonNullTemporaryLevel) throws Exception {
		final Logger preTestLogger   = Logger.getLogger(JulUtil.class.getName());
		final Level  preTestLogLevel = preTestLogger.getLevel();

		final Level originalLogLevel = JulUtil.setLogLevel(JulUtil.class.getName(), nullOrNonNullTemporaryLevel);
		Assert.assertEquals(preTestLogLevel, originalLogLevel);

		final Level temporaryLogLevel = JulUtil.setLogLevel(JulUtil.class.getName(), originalLogLevel);
		Assert.assertEquals(temporaryLogLevel, nullOrNonNullTemporaryLevel);
	}

	private static void testInstanceRestoreByCanonicalName(final Level nullOrNonNullTemporaryLevel) throws Exception {
		final Logger preTestLogger   = Logger.getLogger(JulUtil.class.getName());
		final Level  preTestLogLevel = preTestLogger.getLevel();

		try (final JulUtil autoClose = new JulUtil(JulUtil.class.getName(), nullOrNonNullTemporaryLevel)) {
			final Level temporaryLogLevel = preTestLogger.getLevel();
			Assert.assertEquals(temporaryLogLevel, nullOrNonNullTemporaryLevel);
		}
		final Level postTestLogLevel = preTestLogger.getLevel();
		Assert.assertEquals(preTestLogLevel, postTestLogLevel);
	}
}