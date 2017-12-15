package com.github.justincranford.jcutils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.CertPathValidatorException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.PKIXCertPathValidatorResult;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509ExtendedTrustManager;
import javax.security.auth.x500.X500Principal;

@SuppressWarnings("unused")
public class CertUtil {
	private static final Logger     LOG                = Logger.getLogger(CertUtil.class.getName());
	private static final int        INT_ONE_BILLION    = 1000000000;
	private static final BigInteger BIGINT_TEN_BILLION = BigInteger.valueOf(10000000000L);

	private CertUtil() {
		// declare private constructor to prevent instantiation of this class
	}

	public static KeyStore loadKeyStore(final String ksProvider, final String ksType, final String ksFile, final char[] ksPassword) throws Exception {
		final KeyStore ks = KeyStore.getInstance(ksType, ksProvider);
		try (final FileInputStream fis = new FileInputStream(ksFile)) {
			ks.load(fis, ksPassword);
		}
		return ks;
	}

	public static KeyStore saveKeyStore(final String ksProvider, final String ksType, final String ksFile, final char[] ksPassword, final X509Certificate[] ksChain, final String ksAlias, final PrivateKey privateKey, final char[] ksAliasPassword) throws Exception {
		final KeyStore ks = KeyStore.getInstance(ksType, ksProvider);
		ks.load(null, ksPassword);	// Create in-memory
		ks.setKeyEntry(ksAlias, privateKey, ksAliasPassword, ksChain);	// unique alias (ex: "server", "client", etc)
		try (FileOutputStream fos = new FileOutputStream(ksFile)) {
			ks.store(fos, ksPassword);	// Save to disk
		}
		return ks;
	}

	public static KeyStore saveTrustStore(final String tsProvider, final String tsType, final String tsFile, final char[] tsPassword, final X509Certificate[] tsCerts) throws Exception {	// NOSONAR
		final KeyStore ts = KeyStore.getInstance(tsType, tsProvider);
		ts.load(null, tsPassword);	// Create in-memory
		for (int i=0; i<tsCerts.length; i++) {
			ts.setCertificateEntry("ts"+i, tsCerts[0]);	// unique alias (ex: "ts0", "ts1", "ts2", etc)
		}
		try (FileOutputStream fos = new FileOutputStream(tsFile)) {
			ts.store(fos, tsPassword);	// Save to disk
		}
		return ts;
	}

	public static X509Certificate[] getChainForAlias(final KeyStore ks, final String alias) throws KeyStoreException {
		final Certificate[]     rawChain = ks.getCertificateChain(alias);
		final X509Certificate[] chain    = new X509Certificate[rawChain.length];
		for (int i=0; i<rawChain.length; i++) {
			chain[i] = (X509Certificate) rawChain[i];
		}
		return chain;
	}

	public static X509Certificate[] getTrustedCerts(final KeyStore ts) throws KeyStoreException {
		final List<String>      aliases = Collections.list(ts.aliases());	// order does not matter
		final X509Certificate[] certs   = new X509Certificate[aliases.size()];
		for (int i=0; i<aliases.size(); i++) {
			certs[i] = (X509Certificate) ts.getCertificate(aliases.get(i));
		}
		return certs;
	}

	public static X509Certificate[] buildTrustedChain(final X509Certificate[] untrustedChain, final X509Certificate[] trustedCerts) throws Exception {
		final List<X509Certificate> trustedChain = new ArrayList<>(untrustedChain.length+1);	// max length is entire chain with a trusted issuer appended to the end
		for (final X509Certificate cert : untrustedChain) {
			for (final X509Certificate trustedCert : trustedCerts) {
				if (isMatch(cert, trustedCert)) {
					trustedChain.add(trustedCert);	// certs match or subjects+issuers+publickeys match
					return trustedChain.toArray(new X509Certificate[trustedChain.size()]);	// truncate chain at first found trusted cert
				}
			}
			trustedChain.add(cert);	// cert from chain is not trusted, append and continue search
		}
		final X509Certificate lastCert       = untrustedChain[untrustedChain.length-1];	// check if any trusted cert matches the last cert issuer 
		final X500Principal   lastCertIssuer = lastCert.getIssuerX500Principal();
		for (final X509Certificate trustedCert : trustedCerts) {
			if (lastCertIssuer.equals(trustedCert.getSubjectX500Principal())) {	// compare last cert issuer to trusted cert subject
				try {
					lastCert.verify(trustedCert.getPublicKey());// check if last cert signature can be verified by trusted cert public key
					trustedChain.add(trustedCert);	// if we get here then we found a match for the last cert issuer
					return trustedChain.toArray(new X509Certificate[trustedChain.size()]);	// append trusted cert to end of chain
				} catch(Exception e) {	// NOSONAR
					// swallow exception
				}
			}
		}
		throw new Exception("No trusted certs found in or after the chain");
	}

	// SunX509 => SimpleValidator.java. Sun-specific and compatibility use only. No logging. KeyStore only.
	// PKIX    => PKIXValidator.java. Default and recommended. Logging. KeyStore or ManagerFactoryParameters.
	// Note: PKIX is more rigorous with keystore protection, logging, and even AKI=>SKI chain validation.
	public static void validateTrustedChain(final X509Certificate[] chain) throws Exception {
		final Date now = new Date();
		for (int i=0; i<chain.length; i++) {
			try {
				chain[i].checkValidity(now);	// check date validity range
			} catch (GeneralSecurityException e) {
				throw new Exception("For CERT["+(i)+"].SUBJECT='"+chain[i].getSubjectX500Principal()+"', date check failed", e);	// NOSONAR
			}
		}
		for (int i=0; i<chain.length-1; i++) {
			final X509Certificate childCert  = chain[i];
			final X509Certificate parentCert = chain[i+1];
			final X500Principal childSubject = childCert.getSubjectX500Principal();
			final X500Principal childIssuer = childCert.getIssuerX500Principal();
			final X500Principal parentSubject = parentCert.getSubjectX500Principal();
			if (!childIssuer.equals(parentSubject)) { // check name chaining of child to parent
				throw new Exception("For CERT["+(i)+"].SUBJECT='"+childSubject+"', the issuer DNs chain is broken. Mismatch between CERT["+(i)+"].ISSUER='"+childIssuer+"' vs CERT["+(i+1)+"].SUBJECT='"+parentSubject+"'");
			}
			try {
				childCert.verify(parentCert.getPublicKey());	// check signature of child by parent
			} catch (GeneralSecurityException e) {
				throw new Exception("For CERT["+(i)+"].SUBJECT='"+childSubject+"', signature verification failed for using public key from CERT["+(i+1)+"].SUBJECT='"+parentSubject+"'", e);
			}
		}
		final int i = chain.length-1;
		final X509Certificate lastCert = chain[i];
		if (lastCert.getSubjectX500Principal().equals(lastCert.getIssuerX500Principal())) {
			try {
				lastCert.verify(lastCert.getPublicKey());	// check self signature
			} catch (GeneralSecurityException e) {
				throw new Exception("For CERT["+(i)+"].SUBJECT='"+lastCert.getSubjectX500Principal()+"', signature verification failed for self", e);
			}
		}
	}

	private static boolean isMatch(final X509Certificate cert, final X509Certificate trustedCert) {
		if (trustedCert.equals(cert)) {
			return true;
		}
		return (cert.getSubjectX500Principal().equals(trustedCert.getSubjectX500Principal()))
			&& (cert.getIssuerX500Principal().equals(trustedCert.getIssuerX500Principal()))
			&& (cert.getPublicKey().equals(trustedCert.getPublicKey()));
	}

	public static BigInteger generateSerialNumber(final SecureRandom secureRandom) {
		BigInteger x = BIGINT_TEN_BILLION.add(BigInteger.valueOf(secureRandom.nextInt(INT_ONE_BILLION)));
		for (int round=1, rounds=5; round<rounds; round++) {
			x = x.multiply(BIGINT_TEN_BILLION).add(BigInteger.valueOf(secureRandom.nextInt(INT_ONE_BILLION)));
		}
		return x;	// Example: 10 787329641 0 487540051 0 830229501 0 840285735 0 736587789
	}

	public static final TrustManager[] TRUSTALLCERTS = new TrustManager[] { new X509ExtendedTrustManager() {
		@Override
		public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
			// do nothing
		}

		@Override
		public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
			// do nothing
		}

		@Override
		public X509Certificate[] getAcceptedIssuers() {
			return null;
		}

		@Override
		public void checkClientTrusted(X509Certificate[] x509Certificates, String s, Socket socket) throws CertificateException {
			// do nothing
		}

		@Override
		public void checkServerTrusted(X509Certificate[] x509Certificates, String s, Socket socket) throws CertificateException {
			// do nothing
		}

		@Override
		public void checkClientTrusted(X509Certificate[] x509Certificates, String s, SSLEngine sslEngine) throws CertificateException {
			// do nothing
		}

		@Override
		public void checkServerTrusted(X509Certificate[] x509Certificates, String s, SSLEngine sslEngine) throws CertificateException {
			// do nothing
		}
	} };
}