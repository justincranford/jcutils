package com.github.justincranford.jcutils;

import java.util.logging.Logger;

import org.junit.Test;

@SuppressWarnings({"static-method","unused"})
public class TestUnpackHelper {
	private static final Logger LOG = Logger.getLogger(TestUnpackHelper.class.getName());

	@Test
	public void testPrivateConstructor() throws Exception {
		ValidationUtil.assertPrivateConstructorNoParameters(UnpackHelper.class, true);
	}
}