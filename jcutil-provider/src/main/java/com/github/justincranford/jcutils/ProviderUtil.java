package com.github.justincranford.jcutils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.Provider.Service;
import java.security.Security;
import java.security.Signature;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Dynamic provider registration and removal to avoid repeat modification
 * of java.security for frequent JRE CPUs (Critical Patch Update).
 */
public class ProviderUtil {
	private static final Logger LOG = Logger.getLogger(ProviderUtil.class.getName());

	private ProviderUtil() {
		// prevent class instantiation by making constructor private
	}

	// Agnostic helper methods

	public static int add(final String canonicalClassName) throws Exception {
		return Security.addProvider((Provider)Class.forName(canonicalClassName).newInstance());
	}

	public static int insertAt(final String canonicalClassName, final int position) throws Exception {
		return Security.insertProviderAt((Provider)Class.forName(canonicalClassName).newInstance(), position);
	}

    public static void remove(final String provider) {
    	Security.removeProvider(provider);
    }

    // BC specific helper methods

	public static int addBc() throws Exception {
		return ProviderUtil.add("org.bouncycastle.jce.provider.BouncyCastleProvider");
	}

	public static int insertBcAt(final int position) throws Exception {
		return ProviderUtil.insertAt("org.bouncycastle.jce.provider.BouncyCastleProvider", position);
	}

    public static void removeBc() {
    	ProviderUtil.remove("BC");
    }

    // Helper methods

    public static boolean isSignatureAlgorithmSupported(final String signatureAlgorithm, final String provider) {
    	try {
    		Signature.getInstance(signatureAlgorithm, provider);
			return true;
		} catch (@SuppressWarnings("unused") NoSuchAlgorithmException | NoSuchProviderException e) {	// NOSONAR Either log or rethrow this exception.
			return false;
		}
    }

    public static boolean isSignatureAlgorithmSupported(final String signatureAlgorithm) {
    	try {
    		final Signature signatureAlgorithmInstance = Signature.getInstance(signatureAlgorithm);
    		if (LOG.isLoggable(Level.FINER)) {
    			LOG.log(Level.FINER, "Provider '" + signatureAlgorithmInstance.getProvider().getName() + "' found for signature algorithm '" + signatureAlgorithm + "'.");
    		}
			return true;
		} catch (@SuppressWarnings("unused") NoSuchAlgorithmException e) {	// NOSONAR Either log or rethrow this exception.
			return false;
		}
    }

    public static boolean isMessageDigestSupported(final String messageDigestAlgorithm, final String provider) {
    	try {
    		MessageDigest.getInstance(messageDigestAlgorithm, provider);
			return true;
		} catch (@SuppressWarnings("unused") NoSuchAlgorithmException | NoSuchProviderException e) {	// NOSONAR Either log or rethrow this exception.
			return false;
		}
    }

    public static boolean isMessageDigestSupported(final String messageDigestAlgorithm) {
    	try {
    		final MessageDigest messageDigestInstance = MessageDigest.getInstance(messageDigestAlgorithm);
    		if (LOG.isLoggable(Level.FINER)) {
    			LOG.log(Level.FINER, "Provider '" + messageDigestInstance.getProvider().getName() + "' found for signature algorithm '" + messageDigestAlgorithm + "'.");
    		}
			return true;
		} catch (@SuppressWarnings("unused") NoSuchAlgorithmException e) {	// NOSONAR Either log or rethrow this exception.
			return false;
		}
    }

    public static void printSupportedAlgorithms() {	// NOSONAR The Cyclomatic Complexity of this method "printSupportedAlgorithms" is 15 which is greater than 10 authorized.
		final StringBuilder sb = new StringBuilder("Supported Signature algorithms\n");
		final String[] signatureAlgorithms = {"SHA512withRSA", "SHA512withRSAEncryption", "SHA256withRSA", "SHA256withRSAEncryption", "SHA1withRSA", "SHA1withRSAEncryption", "MD5withRSAwithRSA", "MD5withRSAwithRSAEncryption", "rsaEncryption"};
		for (String signatureAlgorithm : signatureAlgorithms) {
			sb.append(" - Signature=").append(signatureAlgorithm);
			boolean wasFound = false;
			for (Provider provider : Security.getProviders()) {
				for (Service service : provider.getServices()) {
					if ("Signature".equals(service.getType()) && signatureAlgorithm.equalsIgnoreCase(service.getAlgorithm())) {
						if (wasFound) {
							sb.append("   - Provider=").append(provider.getName()).append(" (IGNORED)\n");	// NOSONAR Define a constant instead of duplicating this literal
						} else {
							sb.append("   - Provider=").append(provider.getName()).append("\n");
							wasFound = true;
						}
					}
				}
			}
			if (!wasFound) {
				sb.append("   - Not Found!\n");
			}
		}
		sb.append("Supported MessageDigest algorithms\n");
		final String[] digestAlgorithms = {"SHA512", "SHA-512", "SHA256", "SHA-256", "SHA1", "SHA-1", "MD-5"};
		for (String digestAlgorithm : digestAlgorithms) {
			sb.append(" - MessageDigest=").append(digestAlgorithm).append("\n");
			boolean wasFound = false;
			for (Provider provider : Security.getProviders()) {
				for (Service service : provider.getServices()) {
					if ("MessageDigest".equals(service.getType()) && digestAlgorithm.equalsIgnoreCase(service.getAlgorithm())) {
						if (wasFound) {
							sb.append("   - Provider=").append(provider.getName()).append(" (IGNORED)\n");
						} else {
							sb.append("   - Provider=").append(provider.getName()).append("\n");
							wasFound = true;
						}
					}
				}
			}
			if (!wasFound) {
				sb.append("   - Not Found!\n");
			}
		}
		LOG.log(Level.INFO, sb.toString());	// NOSONAR Invoke method(s) only conditionally.
	}
}