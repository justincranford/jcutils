package com.github.justincranford.jcutils;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

@SuppressWarnings("static-method")
public class TestHttpUtil {
	private static final Logger LOG = Logger.getLogger(TestHttpUtil.class.getName());

	@BeforeClass
	public static void beforeClass() throws Exception {
        LOG.log(Level.INFO, "beforeClass()");
	}

	@AfterClass
	public static void afterClass() throws Exception {
        LOG.log(Level.INFO, "afterClass()");
	}

	@Test
	public void testPrivateConstructor() throws Exception {
		ValidationUtil.assertPrivateConstructorNoParameters(HttpUtil.class, true);
	}

	@Test
	public void testParseCharacterEncoding() throws Exception {
		Assert.assertNull(HttpUtil.parseCharacterEncoding(null));
		Assert.assertEquals(HttpUtil.DEFAULT_CHARACTER_ENCODING, HttpUtil.parseCharacterEncoding("nonsense"));
		Assert.assertNull(HttpUtil.parseCharacterEncoding("text/xml; charset="));
		Assert.assertNull(HttpUtil.parseCharacterEncoding("text/xml; charset=nonsense"));
		Assert.assertEquals("UTF-8",      HttpUtil.parseCharacterEncoding("text/xml; charset=UTF-8"));
		Assert.assertEquals("UTF-16",     HttpUtil.parseCharacterEncoding("text/xml; charset=UTF-16"));
		Assert.assertEquals("UTF-32",     HttpUtil.parseCharacterEncoding("text/xml; charset=UTF-32"));
		Assert.assertEquals("ISO-8859-1", HttpUtil.parseCharacterEncoding("text/xml; charset=ISO-8859-1"));
		Assert.assertEquals("UTF-8",      HttpUtil.parseCharacterEncoding("text/xml; charset=UTF-8; nonsense"));
		Assert.assertEquals("UTF-16",     HttpUtil.parseCharacterEncoding("text/xml; charset=UTF-16; nonsense"));
		Assert.assertEquals("UTF-32",     HttpUtil.parseCharacterEncoding("text/xml; charset=UTF-32; nonsense"));
		Assert.assertEquals("ISO-8859-1", HttpUtil.parseCharacterEncoding("text/xml; charset=ISO-8859-1; nonsense"));
	}

	@Test
	public void testTrustAllCertificates() throws Exception {
		HttpUtil.trustAllCertificates();
	}

	@Test
	public void testAcceptAllHostnames() throws Exception {
		HttpUtil.acceptAllHostnames();
	}
}