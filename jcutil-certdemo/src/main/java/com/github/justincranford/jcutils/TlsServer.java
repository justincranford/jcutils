package com.github.justincranford.jcutils;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.security.KeyStore;
import java.security.Provider;
import java.security.SecureRandom;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.KeyStoreBuilderParameters;
import javax.net.ssl.ManagerFactoryParameters;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;

@SuppressWarnings({ "unused", "hiding" })
public class TlsServer extends Thread {
	private static final Logger LOG = Logger.getLogger(TlsServer.class.getName());

	public static void main(final String[] args) throws Exception {
		ProviderUtil.addBc();
		final SecureRandom secureRandom = SecureRandom.getInstanceStrong();
		final String ksFilePath = CertChainDemo.TEMPDIR + "ksSubEntity1BySubCa1." + CertChainDemo.KSTYPE;
		final char[] ksPassword = new char[0];
		final String ksAlias = CertChainDemo.SERVER;
		final char[] ksAliasPassword = new char[0];
		final InetAddress hostAddress = InetAddress.getByName(CertChainDemo.HOSTNAME_LOCALHOST);
		final int port = 8080;
		final int soTimeout = 0;
		new TlsServer(secureRandom, hostAddress, port, soTimeout, ksFilePath, ksPassword, ksAlias, ksAliasPassword).run(); // NOSONAR Call the method Thread.start() to execute the content of the run() method in a dedicated thread.
	}

	private final SecureRandom	secureRandom;
	private final InetAddress	hostAddress;
	private final int			port;
	private final int			soTimeout;
	private final String		ksFilePath;
	private final char[]		ksPassword;
	private final String		ksAlias;
	private final char[]		ksAliasPassword;

	public TlsServer(final SecureRandom secureRandom, final InetAddress hostAddress, final int port, final int soTimeout, final String ksFilePath, final char[] ksPassword, final String ksAlias, final char[] ksAliasPassword) {
		this.secureRandom = secureRandom;
		this.hostAddress = hostAddress;
		this.port = port;
		this.soTimeout = soTimeout;
		this.ksFilePath = ksFilePath;
		this.ksPassword = ksPassword;
		this.ksAlias = ksAlias;
		this.ksAliasPassword = ksAliasPassword;
	}

	@Override
	public void run() {
		try {
			final KeyStore ks = KeyStore.getInstance(CertChainDemo.KSTYPE, CertChainDemo.KSPROVIDER);
			try (final FileInputStream fis = new FileInputStream(this.ksFilePath)) {
				ks.load(fis, this.ksPassword);
			}
			final KeyManagerFactory kmf = KeyManagerFactory.getInstance(CertChainDemo.TRUSTMANAGER, CertChainDemo.SUNJSSE);
			kmf.init(ks, this.ksPassword);
//	        KeyStore.Builder clientCrtBuilder = KeyStore.Builder.newInstance(CertChainDemo.KSTYPE, (Provider)null, new File(this.ksFilePath), new KeyStore.PasswordProtection(this.ksPassword));
//			KeyStore.Builder clientCrtBuilder = KeyStore.Builder.newInstance(ks, new KeyStore.PasswordProtection(this.ksPassword));
//			kmf.init(new KeyStoreBuilderParameters(clientCrtBuilder));
			final KeyManager[] keyManagers = kmf.getKeyManagers();
//			keyManagers[0] = new X509KeyManagerAppendTrustManager((X509KeyManager)keyManagers[0], (X509TrustManager)trustManagers[0], null);	// Wrap first KeyManager with a delegator object to append secondary trust chains from truststore
			final SSLContext sslContext = SSLContext.getInstance(CertChainDemo.TLS_V1_2_PROTOCOL);
			sslContext.init(keyManagers, null, this.secureRandom);
			final SSLServerSocketFactory sslServerSocketFactory = sslContext.getServerSocketFactory();
			try (final SSLServerSocket sslServerSocket = (SSLServerSocket) sslServerSocketFactory.createServerSocket(this.port, 1, this.hostAddress)) {
				sslServerSocket.setNeedClientAuth(false);
				sslServerSocket.setWantClientAuth(false);
				sslServerSocket.setEnabledCipherSuites(CertChainDemo.TLS_CIPHER_SUITES);
				sslServerSocket.setEnabledProtocols(CertChainDemo.TLS_PROTOCOLS);
				sslServerSocket.setSoTimeout(this.soTimeout);
				LOG.log(Level.INFO, "Waiting for connection");
				try (final SSLSocket sslSocket = (SSLSocket) sslServerSocket.accept()) {
					System.out.println("Server socket opened."); // NOSONAR
					try (final OutputStream out = sslSocket.getOutputStream()) {
						try (final InputStream in = sslSocket.getInputStream()) {
							out.write(("Hello ".getBytes()));
							int ch = 0;
							while ((ch = in.read()) != '!') {
								out.write(ch);
							}
							out.write('!');
						} finally {
							System.out.println("Server input stream closed."); // NOSONAR
						}
					} finally {
						System.out.println("Server output stream closed."); // NOSONAR
					}
					//							sslSocket.setEnabledCipherSuites(sslSocket.getSupportedCipherSuites());
					//							sslSocket.startHandshake();
					//							final SSLSession sslSession = sslSocket.getSession();
					//							final InputStream inputStream = sslSocket.getInputStream();
					//							final OutputStream outputStream = sslSocket.getOutputStream();
					//							try (final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {
					//								try (final PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(outputStream))) {
					//									String line = null;
					//									while((line = bufferedReader.readLine()) != null){
					//										LOG.log(Level.INFO, "Input : " + line);	// NOSONAR Use the built-in formatting to construct this argument.
					//										if (line.trim().isEmpty()) {
					//											break;
					//										}
					//									}
					//									printWriter.print("HTTP/1.1 200\r\n");
					//									printWriter.flush();
					//									sslSocket.close();
					//								}
					//							}
				} finally {
					System.out.println("Server socket closed."); // NOSONAR
				}
			}
		} catch (Exception e) {
			LOG.log(Level.INFO, "Server Exception: ", e);
		}
	}
}