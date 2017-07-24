package com.github.justincranford.jcutils;

import org.junit.Assert;
import org.junit.Test;

@SuppressWarnings("static-method")
public class TestSystemUtil {
	private static final String EMPTY_STRING = "";

	@Test
	public void testPrivateConstructor() throws Exception {
		ValidationUtil.assertPrivateConstructorNoParameters(SystemProperty.class, true);
	}

	@Test
	public void testAssertNonNullValues() throws Exception {
		Assert.assertNotNull(SystemProperty.OS_NAME);
		Assert.assertNotNull(SystemProperty.USER_HOME);
		Assert.assertNotEquals(EMPTY_STRING, SystemProperty.OS_NAME);
		Assert.assertNotEquals(EMPTY_STRING, SystemProperty.USER_HOME);
	}

	@Test
	public void testGetAvailableProcessors() throws Exception {
		ValidationUtil.assertGreaterThan(SystemProperty.getAvailableProcessors(), 0);
	}
}