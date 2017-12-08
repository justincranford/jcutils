package com.github.justincranford.jcutils;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Assert;
import org.junit.Test;

@SuppressWarnings("static-method")
public class TestProviderUtil {
	@Test
	public void testPrivateConstructor() throws Exception {
		ValidationUtil.assertPrivateConstructorNoParameters(ProviderUtil.class, true);
	}

	@Test
	public void testBc() throws Exception {
		ProviderUtil.removeBc();
		ProviderUtil.addBc();
		Assert.assertTrue(ProviderUtil.isSignatureAlgorithmSupported("SHA1withRSA", "BC"));
		Assert.assertTrue(ProviderUtil.isMessageDigestSupported("SHA-1", "BC"));
		ProviderUtil.addBc();
		ProviderUtil.removeBc();
		Assert.assertFalse(ProviderUtil.isSignatureAlgorithmSupported("SHA1withRSA", "BC"));
		Assert.assertFalse(ProviderUtil.isMessageDigestSupported("SHA-1", "BC"));
	}

	@Test
	public void testDefaultSun() throws Exception {
		ProviderUtil.removeBc();
		Assert.assertTrue(ProviderUtil.isSignatureAlgorithmSupported("SHA1withRSA", "SunRsaSign"));
		Assert.assertTrue(ProviderUtil.isMessageDigestSupported("SHA-1", "SUN"));
		Assert.assertTrue(ProviderUtil.isSignatureAlgorithmSupported("SHA1withRSA"));
		Assert.assertTrue(ProviderUtil.isMessageDigestSupported("SHA-1"));
	}

	@Test
	public void testLogging() throws Exception {
		final Logger log = Logger.getLogger(ProviderUtil.class.getName());
		final Level originalLogLevel = log.getLevel();

		log.setLevel(Level.ALL);
		ProviderUtil.printSupportedAlgorithms();
		ProviderUtil.isSignatureAlgorithmSupported("SHA1withRSA");
		ProviderUtil.isMessageDigestSupported("SHA-1");

		log.setLevel(Level.OFF);
		ProviderUtil.printSupportedAlgorithms();
		ProviderUtil.isSignatureAlgorithmSupported("SHA1withRSA");
		ProviderUtil.isMessageDigestSupported("SHA-1");

		log.setLevel(originalLogLevel);
	}

	@Test
	public void testUnsupportedSignatureAlgorithm() throws Exception {
		Assert.assertFalse(ProviderUtil.isSignatureAlgorithmSupported("UnsupportedSignatureAlgorithm"));
	}

	@Test
	public void testUnsupportedMessageDigest() throws Exception {
		Assert.assertFalse(ProviderUtil.isMessageDigestSupported("UnsupportedMessageDigest"));
	}
}