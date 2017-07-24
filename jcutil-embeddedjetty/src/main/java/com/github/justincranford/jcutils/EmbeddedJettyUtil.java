package com.github.justincranford.jcutils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.util.log.JavaUtilLog;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.StdErrLog;

@SuppressWarnings({"hiding","unused"})
public class EmbeddedJettyUtil {
	private static final Logger LOG = Logger.getLogger(EmbeddedJettyUtil.class.getName());

	public static final String                       DEFAULT_HOSTNAME      = "[::1]";	// IPv6 alternatives are 127.0.0.1, localhost, physical NIC address, or hostname
	public static final int                          DEFAULT_PORT          = 80;
	public static final long                         DEFAULT_STOP_TIMEOUT  = 30000L;
	public static final Class<? extends HttpServlet> DEFAULT_SERVLET_CLASS = DefaultServletHelloWorld.class;
	public static final String                       DEFAULT_SERVLET_PATH  = "/*";
	public static final Class<? extends Filter>      DEFAULT_FILTER_CLASS  = DefaultFilterSetAttribute.class;
	public static final String                       DEFAULT_FILTER_PATH   = "/*";

	private Server                       jettyServerInstance;
	private String                       hostname;
	private int                          port;
	private long                         stopTimeoutMillis;
	private Class<? extends HttpServlet> servletClass;
	private String                       servletPath;
	private Class<? extends Filter>      filterClass;
	private String                       filterPath;
	private InetSocketAddress            inetSocketAddress;	// resolved hostname+port

	/**
	 * Create object to hold embedded jetty server settings and instance, enabling reusable and/or concurrent server instances.
	 * @param hostname HOSTNAME (localhost, myhostname, myhostname.mydomain), IPv6 (::1), IPv4 (0.0.0.0, 127.0.0.1, 192.168.10.200)
	 * @param port (1..65535)
	 * @param stopTimeoutMillis (1..)
	 * @param servletClass Non-null HttpServlet implementation class (Default DEFAULT_SERVLET_CLASS is available for demo or example purposes)
	 * @param servletPath Non-null servlet request URI pattern
	 * @param filterClass Nullable Filter implementation class (Default DEFAULT_FILTER_CLASS is available for demo or example purposes)
	 * @param filterPath Nullable filter request URI pattern
	 * @throws Exception
	 */
	public EmbeddedJettyUtil(final String hostname, final int port, final long stopTimeoutMillis, final Class<? extends HttpServlet> servletClass, final String servletPath, final Class<? extends Filter> filterClass, final String filterPath) throws Exception {
		if (null == hostname) {
			throw new Exception("Hostname is null.");
		} else if (hostname.isEmpty()) {
			throw new Exception("Hostname is empty.");
		} else if ((port < 1) || (port > 65535)) {
			throw new Exception("Port must be between 1 to 65535 inclusive.");
		}
		this.inetSocketAddress = new InetSocketAddress(hostname, port);
		if (this.inetSocketAddress.isUnresolved()) {
			throw new Exception("Hostname is unresolved.");
		} else if (stopTimeoutMillis < 1L) {
			throw new Exception("Stop timeout must be greater than 0.");
		} else if (null == servletClass) {
			throw new Exception("Servlet class is null.");
		} else if (null == servletPath) {
			throw new Exception("Servlet path is null.");
		} else if (servletPath.isEmpty()) {
			throw new Exception("Servlet path is empty.");
		} else if ((null != filterClass) && (null == filterPath)) {
			throw new Exception("Servlet class is specified, but filter path is null.");
		} else if ((null != filterClass) && (filterPath.isEmpty())) {
			throw new Exception("Servlet class is specified, but filter path is empty.");
		} else if ((null != filterPath) && (null == filterClass)) {
			throw new Exception("Servlet path is specified, but filter class is null.");
		}
		this.jettyServerInstance = null;
		this.hostname            = hostname;
		this.port                = port;
		this.stopTimeoutMillis   = stopTimeoutMillis;
		this.servletClass        = servletClass;
		this.servletPath         = servletPath;
		this.filterClass         = filterClass;
		this.filterPath          = filterPath;
	}

	public EmbeddedJettyUtil(final String hostname, final int port, final long stopTimeoutMillis, final Class<? extends HttpServlet> servletClass, final String servletPath) throws Exception {
		this(hostname, port, stopTimeoutMillis, servletClass, servletPath, DEFAULT_FILTER_CLASS, DEFAULT_FILTER_PATH);
	}

	public EmbeddedJettyUtil(final String hostname, final int port, final Class<? extends HttpServlet> servletClass, final String servletPath) throws Exception {
		this(hostname, port, DEFAULT_STOP_TIMEOUT, servletClass, servletPath, DEFAULT_FILTER_CLASS, DEFAULT_FILTER_PATH);
	}

	public EmbeddedJettyUtil(final Class<? extends HttpServlet> servletClass, final String servletPath) throws Exception {
		this(DEFAULT_HOSTNAME, DEFAULT_PORT, DEFAULT_STOP_TIMEOUT, servletClass, servletPath, DEFAULT_FILTER_CLASS, DEFAULT_FILTER_PATH);
	}

	public EmbeddedJettyUtil() throws Exception {
		this(DEFAULT_HOSTNAME, DEFAULT_PORT, DEFAULT_STOP_TIMEOUT, DEFAULT_SERVLET_CLASS, DEFAULT_SERVLET_PATH, DEFAULT_FILTER_CLASS, DEFAULT_FILTER_PATH);
	}

	public void start() throws Exception {
		if (null != this.jettyServerInstance) {
			throw new Exception("Server already started");
		}
		LOG.log(Level.INFO, "Starting Jetty");
		final ServletHandler servletHandler = new ServletHandler();
		servletHandler.addServletWithMapping(this.servletClass, this.servletPath);
		if ((null != this.filterClass) || (!this.filterPath.isEmpty())) {
			servletHandler.addFilterWithMapping(this.filterClass, this.filterPath, 1);
		}

		this.jettyServerInstance = new Server(this.inetSocketAddress);
		this.jettyServerInstance.setHandler(servletHandler);
		this.jettyServerInstance.setStopTimeout(this.stopTimeoutMillis);
		this.jettyServerInstance.start();
		do {
			LOG.log(Level.INFO, "Waiting Jetty start");
		} while (!this.jettyServerInstance.isStarted());
		LOG.log(Level.INFO, "Finished beforeClass()");
	}

	public void stop() throws Exception {
		if (null == this.jettyServerInstance) {
			throw new Exception("Server not running");
		}
		LOG.log(Level.INFO, "Stopping Jetty");
		this.jettyServerInstance.stop();
		do {
			LOG.log(Level.INFO, "Waiting Jetty stop");
		} while (!this.jettyServerInstance.isStopped());
		this.jettyServerInstance = null;
		LOG.log(Level.INFO, "Stopped Jetty");
	}

	public static class DefaultServletHelloWorld extends HttpServlet {
		private static final Logger LOG = Logger.getLogger(DefaultServletHelloWorld.class.getName());
		private static final long serialVersionUID = 1L;
		@Override
		protected void doGet(final HttpServletRequest httpServletRequest, final HttpServletResponse httpServletResponse) throws ServletException, IOException {
			LOG.log(Level.INFO, "DefaultFilterSetAttribute=" + httpServletRequest.getAttribute("DefaultFilterSetAttribute"));
		}
	}

	public static class DefaultFilterSetAttribute implements Filter {
		private static final Logger LOG = Logger.getLogger(DefaultFilterSetAttribute.class.getName());
		@Override
		public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
			final HttpServletRequest  httpServletRequest  = (HttpServletRequest)  servletRequest;
			final HttpServletResponse httpServletResponse = (HttpServletResponse) servletResponse;
			httpServletRequest.setAttribute("DefaultFilterSetAttribute", "yes");
		}
		@Override
		public void destroy() {
			// do nothing
		}
		@Override
		public void init(FilterConfig arg0) throws ServletException {
			// do nothing
		}
	}
}