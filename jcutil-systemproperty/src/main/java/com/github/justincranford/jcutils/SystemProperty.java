package com.github.justincranford.jcutils;

public class SystemProperty {
	public static final String  OS_NAME			= System.getProperty("os.name");
	public static final String  LINE_SEPARATOR	= System.getProperty("line.separator");
	public static final String  USER_HOME		= System.getProperty("user.home");
	public static final boolean IS_WINDOWS		= OS_NAME.toLowerCase().contains("win");

	private SystemProperty() {
		// prevent class instantiation by making constructor private
	}

	/**
	 * See Runtime.getRuntime().availableProcessors().
	 */
	public static final int getAvailableProcessors() {
		return Runtime.getRuntime().availableProcessors();
	}
}