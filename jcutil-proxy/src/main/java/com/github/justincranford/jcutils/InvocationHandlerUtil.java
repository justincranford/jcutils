package com.github.justincranford.jcutils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.security.InvalidParameterException;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Demonstrate that any object can be proxied, no matter if the object implements any interfaces. A totally unrelated
 * interface can be used for the object, no need for it to match the object or declare any methods.
 * @author justi
 */
@SuppressWarnings({"unused","hiding"})
public class InvocationHandlerUtil implements InvocationHandler {
	private static final Logger LOG = Logger.getLogger(InvocationHandlerUtil.class.getCanonicalName());

	// apparently this works for the object even though it does not implement the interface
	private final ClassLoader classLoader;				// NOSONAR Remove this unused "classLoader" private field.
	private final Object      proxiedObject;			// the class loader to define the proxy class
	private final Class<?>[]  proxiedInterfaceClasses;	// the list of interfaces for the proxy class to implement	// NOSONAR

	/**
	 * Called by applications to construct this invocation handler and return it wrapped in a proxy object. 
	 */
	public static Object create(ClassLoader classLoader, Object objectWithoutInterface, Class<?>[] interfaceClasses) throws InvalidParameterException {
		if (null == classLoader) {
			throw new InvalidParameterException("Class loader must be non-null");
		} else if (null == objectWithoutInterface) {
			throw new InvalidParameterException("Object must be non-null");
		} else if (null == interfaceClasses) {
			throw new InvalidParameterException("Interface classes array must be non-null");
		} else if (0 == interfaceClasses.length) {
			throw new InvalidParameterException("Interface classes array must be non-empty");
		}
		for (final Class<?> interfaceClass : interfaceClasses) {
			if (!interfaceClass.isInterface()) {
				throw new InvalidParameterException("Class must be an interface");
			}
		}
		final InvocationHandlerUtil invocationHandler = new InvocationHandlerUtil(classLoader, objectWithoutInterface, interfaceClasses);
		return Proxy.newProxyInstance(classLoader, interfaceClasses, invocationHandler
		);
	}

	/**
	 * Called by create() method.
	 * @param proxiedObject
	 * @param proxiedInterfaceClass
	 */
	private InvocationHandlerUtil(ClassLoader classLoader, Object proxiedObject, Class<?>[] proxiedInterfaceClasses) {
		this.classLoader             = classLoader;
		this.proxiedObject           = proxiedObject;
		this.proxiedInterfaceClasses = proxiedInterfaceClasses;
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		final String methodName = method.getName();
		LOG.log(Level.INFO, "*** Intercepted method {0} ***", new Object[]{methodName});
		if (null == this.proxiedObject) {
			throw new NullPointerException();
		} else if ("hashCode".equals(methodName)) {
			LOG.log(Level.INFO, "*** Overriding method {0} ***", new Object[]{methodName});
			return Integer.valueOf(0);	// override proxiedObject.hashCode()
		} else if ("equals".equals(methodName)) {
			LOG.log(Level.INFO, "*** Overriding method {0} ***", new Object[]{methodName});
			return Boolean.TRUE;		// override proxiedObject.equals()
		}
		LOG.log(Level.INFO, "*** Not overriding method {0} ***", new Object[]{methodName});
		return method.invoke(this.proxiedObject, args);	// pass-through other proxiedObject methods like toString()
	}
}