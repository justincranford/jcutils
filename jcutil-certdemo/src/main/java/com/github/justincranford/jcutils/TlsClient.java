package com.github.justincranford.jcutils;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

@SuppressWarnings({"hiding"})
public class TlsClient extends Thread {
	private static final Logger LOG = Logger.getLogger(TlsClient.class.getName());

	public static void main(final String[] args) throws Exception {
		ProviderUtil.addBc();
		final SecureRandom secureRandom    = SecureRandom.getInstanceStrong();
		final InetAddress  hostAddress     = InetAddress.getByName(CertChainDemo.HOSTNAME_LOCALHOST);
		final int          port            = 8080;
		final int          soTimeout       = 10000;
		final String       tsFilePath      = CertChainDemo.TEMPDIR+"tsSubCa1." +CertChainDemo.TSTYPE;
		final char[]       tsPassword      = new char[0];
		final String       ksFilePath      = CertChainDemo.TEMPDIR + "ksSubEntity1BySubCa1." + CertChainDemo.KSTYPE;
		final char[]       ksPassword      = new char[0];
		final String       ksAlias         = CertChainDemo.KSALIASCLIENT;
		final char[]       ksAliasPassword = new char[0];
		new TlsClient(secureRandom, hostAddress, port, soTimeout, tsFilePath, tsPassword, ksFilePath, ksPassword, ksAlias, ksAliasPassword).run();	// NOSONAR Call the method Thread.start() to execute the content of the run() method in a dedicated thread.
	}

	private final SecureRandom	secureRandom;
	private final InetAddress	hostAddress;
	private final int			port;
	private final int			soTimeout;
	private final String		tsFilePath;
	private final char[]		tsPassword;
	private final String		ksFilePath;
	private final char[]		ksPassword;
	private final String		ksAlias;
	private final char[]		ksAliasPassword;
	public TlsClient(final SecureRandom secureRandom, final InetAddress hostAddress, final int port, final int soTimeout, final String tsFilePath, final char[] tsPassword, final String ksFilePath, final char[] ksPassword, final String ksAlias, final char[] ksAliasPassword) {
		this.secureRandom    = secureRandom;
		this.hostAddress     = hostAddress;
		this.port            = port;
		this.soTimeout       = soTimeout;
		this.tsFilePath      = tsFilePath;
		this.tsPassword      = tsPassword;
		this.ksFilePath      = ksFilePath;
		this.ksPassword      = ksPassword;
		this.ksAlias         = ksAlias;
		this.ksAliasPassword = ksAliasPassword;
	}

	@Override
	public void run() {
		try {
			boolean needClientAuth = true;

			// TRUST STORE => TRUST MANAGER
			final KeyStore ts = KeyStore.getInstance(CertChainDemo.TSTYPE, CertChainDemo.TSPROVIDER);
			try (final FileInputStream fis = new FileInputStream(this.tsFilePath)) {
				ts.load(fis, this.tsPassword);
			}
			TrustManagerFactory tmf = TrustManagerFactory.getInstance(CertChainDemo.TRUSTMANAGER, CertChainDemo.SUNJSSE);
	        tmf.init(ts);
			final TrustManager[] trustManagers = tmf.getTrustManagers();

			// KEY STORE => KEY MANAGER
			final KeyStore ks = KeyStore.getInstance(CertChainDemo.KSTYPE, CertChainDemo.KSPROVIDER);
			try (final FileInputStream fis = new FileInputStream(this.ksFilePath)) {
				ks.load(fis, this.ksPassword);
			}
			final KeyManagerFactory kmf = KeyManagerFactory.getInstance(CertChainDemo.KEYMANAGER, CertChainDemo.SUNJSSE);
			kmf.init(ks, this.ksPassword);
			final KeyManager[] keyManagers = kmf.getKeyManagers();

//			trustManagers[0] = new X509TrustManagerAssertEKUs((X509TrustManager)trustManagers[0], null);	// Wrap first TrustManager with a delegator object to enforce mandatory EKUs
	        final SSLContext sslContext = SSLContext.getInstance(CertChainDemo.TLS_V1_2_PROTOCOL, CertChainDemo.SUNJSSE);
			sslContext.init((needClientAuth?keyManagers:null), trustManagers, this.secureRandom);	// Use trustAllCerts for debug only to disable validation
	        final SSLSocketFactory fact = sslContext.getSocketFactory();
	        try (final SSLSocket cSock = (SSLSocket)fact.createSocket(this.hostAddress, this.port)) {	// NOSONAR
		        System.out.println("Client socket opened.");	// NOSONAR
	        	cSock.setNeedClientAuth(needClientAuth);
	        	cSock.setEnabledCipherSuites(CertChainDemo.TLS_CIPHER_SUITES);
				cSock.setEnabledProtocols(CertChainDemo.TLS_PROTOCOLS);
				cSock.setSoTimeout(this.soTimeout);
		        try (final OutputStream out = cSock.getOutputStream()) {
			        try (final InputStream in = cSock.getInputStream()) {
				        out.write("World!".getBytes());	// Exception: Received fatal alert: handshake_failure
				        int ch = 0;
				        while ((ch = in.read()) != '!') {
				            System.out.print((char)ch);	// NOSONAR
				        }
				        System.out.println((char)ch);	// NOSONAR
					} finally {
				        System.out.println("Client input stream closed.");	// NOSONAR
			        }
				} finally {
			        System.out.println("Client output stream closed.");	// NOSONAR
		        }
			} finally {
		        System.out.println("Client socket closed.");	// NOSONAR
	        }
		} catch(Exception e) {
			LOG.log(Level.INFO, "Client Exception: ", e);
		}
	}
}