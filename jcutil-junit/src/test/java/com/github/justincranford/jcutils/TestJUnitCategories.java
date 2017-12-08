package com.github.justincranford.jcutils;

import org.junit.Test;

@SuppressWarnings("static-method")
public class TestJUnitCategories {
	@Test
	public void testPrivateConstructor() throws Exception {
		ValidationUtil.assertPrivateConstructorNoParameters(JUnitCategories.class, true);
	}
}