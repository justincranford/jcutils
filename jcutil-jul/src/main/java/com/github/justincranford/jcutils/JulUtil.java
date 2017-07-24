package com.github.justincranford.jcutils;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility methods to temporarily set a log level and then restore it.
 * Static, or constructor with auto-close, options are available.
 * 
 * 
 * Examples static method calls.
 * 
 * 1) Set new log level to ALL to log all log levels.
 * 
 *   JulUtil.setLogLevel(MyClass.class, Level.ALL);
 *   // invoke MyClass directly or indirectly
 * 
 * 2) Set new log level to null to inherit log level from an ancestor class.
 * 
 *   JulUtil.setLogLevel(MyClass.class, null);
 *   // invoke MyClass directly or indirectly
 * 
 * 3) Set new log level to ALL, but use method chaining to reset to the original value right away. This is effectively a NO-OP.
 * 
 *   JulUtil.setLogLevel(MyClass.class, JulUtil.setLogLevel(MyClass.class, Level.ALL));
 * 
 * 4) Set new log level to NULL, but use method chaining to reset to the original value right away. This is effectively a NO-OP.
 * 
 *   JulUtil.setLogLevel(MyClass.class, JulUtil.setLogLevel(MyClass.class, NULL));
 * 
 * 5) Set new log level to ALL, use method chaining to invoke your method, and use method chaining again to reset to the original value.
 * 
 *   JulUtil.setLogLevel(MyClass.class, MyClass.myMethodWithLogging(JulUtil.setLogLevel(MyClass.class, Level.ALL)));
 * 
 * 6) Set new log method to ALL and save the old log level, do something, then set the old log level again.
 * 
 *   final Level origLogLevel = JulUtil.setLogLevel(MyClass.class, Level.ALL);
 *     // invoke MyClass directly or indirectly
 *   final Level tempLogLevel = JulUtil.setLogLevel(MyClass.class, origLogLevel);
 * 
 * 
 * Examples of auto-closed (Java 7+) calls for try-with-resources scoping.
 * 
 * 1) Turn off package level logging, enable all log levels for a single class. This can help eliminate noise from logs for diagnostics or debugging.
 * 
 *   try (final JulUtil pkg = new JulUtil(com.example.httpclient.class, Level.OFF)) {
 *     try (final JulUtil child = new JulUtil(com.example.httpclient.httpsend.class, Level.ALL)) {
 *     // invoke childClass directly or indirectly
 *     } // Triggers restore inner class logger to original log level
 *   } // Triggers restore outer class logger to original log level
 * 
 * 2) Turn on package level logging, force child clas to inherit from parent package.
 * 
 * try (final JulUtil pkg = new JulUtil(com.example.httpclient.class, Level.ALL)) {
 *   try (final JulUtil child = new JulUtil(com.example.httpclient.httpsend.class, null)) {
 *     // invoke childClass directly or indirectly
 *   } // Triggers restore inner class logger to original log level
 * } // Triggers restore outer class logger to original log level
 * 
 */
public class JulUtil implements AutoCloseable {	// NOPMD

	// PARAMETERS Used by constructor & close, not by static methods.

	private final Logger logger;			// NOPMD Saved logger object.
	private final Level  restoreLogLevel;	// NOPMD Saved log level. Null values are valid.

	// OBJECT METHODS

	/**
	 * Save the logger object and current level, and set the new log level.
	 * @param clazz
	 * @param tempLogLevel
	 * @throws IllegalArgumentException
	 */
	public JulUtil(final Class<?> clazz, final Level tempLogLevel) throws IllegalArgumentException {
		if (null == clazz) {
			throw new IllegalArgumentException("Null class not allowed");
		}
		this.logger = Logger.getLogger(clazz.getCanonicalName());	// ASSUMPTION: Never returns null
		this.restoreLogLevel = this.logger.getLevel();	// save initial log level
		this.logger.setLevel(tempLogLevel);		// change log level to temporary setting
	}

	/**
	 * Save the logger object and current level, and set the new log level.
	 * @param canonicalName
	 * @param tempLogLevel
	 * @throws IllegalArgumentException
	 */
	public JulUtil(final String canonicalName, final Level tempLogLevel) throws IllegalArgumentException {
		if (null == canonicalName) {
			throw new IllegalArgumentException("Null canonical name not allowed");
		}
		this.logger = Logger.getLogger(canonicalName);	// ASSUMPTION: Never returns null
		this.restoreLogLevel = this.logger.getLevel();	// save initial log level
		this.logger.setLevel(tempLogLevel);		// change log level to temporary setting
	}

	/**
	 * Use the saved logger object and log level to restore it.
	 */
	@Override
	public void close() {
		if (null != this.logger) {
			this.logger.setLevel(this.restoreLogLevel);		// change log level to original setting
		}
	}

	// CLASS METHODS

	/**
	 * Save current log level, set new log level, and return old log level.
	 * @param clazz
	 * @param newLogLevel
	 * @return origLogLevel
	 * @throws IllegalArgumentException
	 */
	public static Level setLogLevel(final Class<?> clazz, final Level newLogLevel) throws IllegalArgumentException {
		if (null == clazz) {
			throw new IllegalArgumentException("Null class not allowed");
		}
		final Logger logger = Logger.getLogger(clazz.getCanonicalName());	// ASSUMPTION: Never returns null
		final Level origLogLevel = logger.getLevel();	// NOPMD
		logger.setLevel(newLogLevel);					// NOPMD
		return origLogLevel;							// NOPMD
	}

	/**
	 * Save current log level, set new log level, and return old log level.
	 * @param canonicalName
	 * @param newLogLevel
	 * @return origLogLevel
	 * @throws IllegalArgumentException
	 */
	public static Level setLogLevel(final String canonicalName, final Level newLogLevel) throws IllegalArgumentException {
		if (null == canonicalName) {
			throw new IllegalArgumentException("Null canonical name not allowed");
		}
		final Logger logger = Logger.getLogger(canonicalName);	// ASSUMPTION: Never returns null
		final Level origLogLevel = logger.getLevel();	// NOPMD
		logger.setLevel(newLogLevel);					// NOPMD
		return origLogLevel;							// NOPMD
	}
}