package com.github.justincranford.jcutils;

import org.junit.Assert;
import org.junit.Test;

@SuppressWarnings("static-method")
public class TestXmlUtil {
	@Test
	public void testPrivateConstructor() throws Exception {
		ValidationUtil.assertPrivateConstructorNoParameters(XmlUtil.class, true);
	}

	@Test
	public void testInitialized() throws Exception {
		Assert.assertNotNull(XmlUtil.DOC_DOM_BUILDER);
		Assert.assertNotNull(XmlUtil.DOC_DOM_BUILDER.get());
		Assert.assertNotNull(XmlUtil.get());
		XmlUtil.remove();
	}
}