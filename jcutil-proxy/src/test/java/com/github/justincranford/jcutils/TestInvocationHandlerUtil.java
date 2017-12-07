package com.github.justincranford.jcutils;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Assert;
import org.junit.Test;

@SuppressWarnings("static-method")
public class TestInvocationHandlerUtil {
	private static final Logger LOG = Logger.getLogger(TestInvocationHandlerUtil.class.getCanonicalName());
	private static interface EmptyInterface {/*empty*/}

	@Test
	public void testMethodInterceptAndBypass() throws Exception {
		final Object objectWithoutInterface = new Object();
		// Apparently the object does not need to implement the specified interface, methods can be intercepted anyway
		final Object proxiedWithInterface = InvocationHandlerUtil.create(
			InvocationHandlerUtil.class.getClassLoader(),
			objectWithoutInterface,
			new Class<?>[]{EmptyInterface.class}
		);
		Integer hashCodeNotProxied               = Integer.valueOf(objectWithoutInterface.hashCode());
		String  toStringNotProxied               = objectWithoutInterface.toString();
		Boolean equalsNotProxied                 = Boolean.valueOf(objectWithoutInterface.equals(null));

		Integer hashCodeProxiedAndIntercepted    = Integer.valueOf(proxiedWithInterface.hashCode());
		String  toStringProxiedButNotIntercepted = proxiedWithInterface.toString();
		Boolean equalsProxiedAndIntercepted      = Boolean.valueOf(proxiedWithInterface.equals(null));

		Assert.assertNotEquals(hashCodeNotProxied, hashCodeProxiedAndIntercepted);
		Assert.assertEquals(toStringNotProxied,    toStringProxiedButNotIntercepted);
		Assert.assertNotEquals(equalsNotProxied,   equalsProxiedAndIntercepted);

		LOG.log(Level.INFO, "========================================================================");
		LOG.log(Level.INFO, "Original hashCode:  {0}", new Object[] {hashCodeNotProxied});					// not proxied
		LOG.log(Level.INFO, "Proxied  hashCode:  {0}", new Object[] {hashCodeProxiedAndIntercepted});		// proxied, and method is intercepted
		LOG.log(Level.INFO, "========================================================================");
		LOG.log(Level.INFO, "Original toString   {0}", new Object[] {toStringNotProxied});					// not proxied
		LOG.log(Level.INFO, "Proxied  toString:  {0}", new Object[] {toStringProxiedButNotIntercepted});	// proxied, but method is not intercepted
		LOG.log(Level.INFO, "========================================================================");
		LOG.log(Level.INFO, "Original equals:    {0}", new Object[] {equalsNotProxied});					// not proxied
		LOG.log(Level.INFO, "Proxied  equals:    {0}", new Object[] {equalsProxiedAndIntercepted});			// proxied, and method is intercepted
		LOG.log(Level.INFO, "========================================================================");
	}
}