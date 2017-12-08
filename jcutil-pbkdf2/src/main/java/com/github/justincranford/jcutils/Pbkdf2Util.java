package com.github.justincranford.jcutils;

import java.math.BigInteger;
import java.security.InvalidParameterException;
import java.security.SecureRandom;

import javax.crypto.Mac;
import javax.crypto.SecretKeyFactory;
import javax.crypto.interfaces.PBEKey;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Implementation of PBKDF2 from RFC 2898 with Test Vectors from RFC 6070:
 * 
 *   PKCS #5: Password-Based Cryptography Specification Version 2.0
 *   https://tools.ietf.org/html/rfc2898#section-5.2
 * 
 *   PKCS #5: Password-Based Key Derivation Function 2 (PBKDF2) Test Vectors
 *   https://tools.ietf.org/html/rfc6070
 * 
 * Examples:
 * 
 *   Pbkdf2Util.deriveKey(SecureRandom.getInstanceStrong(), "SunJCE", "PBKDF2WithHmacSHA256", "cleartextpassword", 10000,  256);
 *   Pbkdf2Util.deriveKey(SecureRandom.getInstanceStrong(), "BC",     "PBKDF2WithHmacSHA512", "cleartextpassword", 100000, 512);
 * @author cranfoj
 */
public class Pbkdf2Util {
//	private static final ExecutorService SERVICE                   = new ThreadPoolExecutor(0, Runtime.getRuntime().availableProcessors(), 10L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
	public  static final BigInteger      MAX_PBKDF2_PRF_ITERATIONS = BigInteger.valueOf(4294967295L);	// RFC 2898 Section 5.2 says 2^32-1 iterations * hash length = dkLen max
	public  static final int             MAX_PBKDF2_DKLEN          = 8192;

	private Pbkdf2Util() {
		// declare private constructor to prevent instantiation of this class
	}

	public static byte[] deriveKeyFromPassword(final String provider, final String algorithm, final byte[] password, final byte[] salt, final int xorIterations, final int dkLen) throws Exception {	// NOSONAR Refactor this method to throw at most one checked exception
		if ((null == password) || (0 == password.length)) {
			throw new InvalidParameterException("Password must be non-null and non-empty.");
		} else if ((null == salt) || (0 == salt.length)) {
			throw new InvalidParameterException("Salt must be non-null and non-empty.");
		} else if (xorIterations < 1) {
			throw new InvalidParameterException("Min count is 1. Invalid valid " + xorIterations + ".");
		} else if (dkLen < 1) {
			throw new InvalidParameterException("Min length is 1. Invalid valid " + dkLen + ".");
		}
		final Mac mac = Mac.getInstance(algorithm, provider);
		final int hashLength = mac.getMacLength();	// SHA1 => 20 bytes, SHA-256 => 32 bytes, SHA-512 => 64 bytes
		if (dkLen > MAX_PBKDF2_DKLEN) {
			final BigInteger maxDkLength = MAX_PBKDF2_PRF_ITERATIONS.multiply(BigInteger.valueOf(hashLength));	// iterations * hashLength
			if (BigInteger.valueOf(dkLen).compareTo(maxDkLength) > 0) {	// concatenated byte values are 0x00000000=1 to 0xFFFFFFFF=4294967295, so expand is limited to 4294967295 iterations of hashLength
				throw new InvalidParameterException("Max length is " + maxDkLength + " (=4294967295*HashLen=4294967295*" + hashLength + "). Invalid value " + dkLen + ".");	// NOSONAR
			}
			throw new InvalidParameterException("Max length is " + maxDkLength + " (=4294967295*HashLen=4294967295*" + hashLength + "), but this implementations only supports max Integer.MAX_VALUE=2^31-1=" + MAX_PBKDF2_DKLEN + ".");
		}
		mac.init(new SecretKeySpec(password, algorithm));

		final boolean isLastBlockTruncated = (dkLen % hashLength > 0);
		final long    dkBlocks             = (isLastBlockTruncated?1:0) + dkLen/hashLength;
		final byte[]  DK                   = new byte[(int)(hashLength*dkBlocks)];
		final int     saltLength           = salt.length;
		final byte[]  U0                   = new byte[saltLength+4];	// Note: U_0 size is saltLength+4, U_1/U_2/U_3/... sizes are hashLength
		System.arraycopy(salt, 0, U0, 0, saltLength);	// U_0 => S || INT(i), set S outside the loop vs INT(i) inside the loop to avoid repeating S initialization for each hash block
		byte[] Ui;
		int xorIteration, uiOffset;	// NOSONAR Declare "dkBlockIndex" on a separate line.
		for (int dkBlock=1, dkOffset=0; dkBlock<=dkBlocks; dkBlock++, dkOffset+=hashLength) {
			U0[saltLength    ] = (byte) (dkBlock >>> 24);	// Initialize suffix of reusable U_0 byte array with dkBlock=1..n
			U0[saltLength + 1] = (byte) (dkBlock >>> 16);
			U0[saltLength + 2] = (byte) (dkBlock >>> 8);
			U0[saltLength + 3] = (byte) (dkBlock);
			Ui = U0;	// reusable U_i for computing U_0=>U_1, U_1=>U_2, U_2=>U_3, ..., U_{c-1}, U_c
			for (xorIteration=1; xorIteration<=xorIterations; xorIteration++) {
				Ui = mac.doFinal(Ui);	// doFinal() implicitly resets mac back to init(), no need to re-init each loop iteration
				for (uiOffset = 0; uiOffset < hashLength; uiOffset++) {	// U_1 \xor U_2 \xor ... \xor U_c
					DK[uiOffset+dkOffset] ^= Ui[uiOffset];	// Instead of Tn blocks copied to DK, work directly with DK to avoid unnecessary Tn zero fills and copy to DK for each block
				}
			}
		}

		// Multi-threaded (Overhead from Mac constructor+init and thread pool slows overall throughput below single threads, so stick to single threaded) 
//		final DeriveKeyBlockFromPassword deriveKeyBlockFromPassword = new DeriveKeyBlockFromPassword(mac, salt, xorIterations, DK);	// first block, use existing initialized Mac object
//		if (1 == dkBlocks) {
//			deriveKeyBlockFromPassword.call();	// synchronous call, first block, use existing Mac object
//		} else {
//			final ArrayList<Future<Object>> futureResults = new ArrayList<>((int)dkBlocks);
//			futureResults.add(SERVICE.submit(deriveKeyBlockFromPassword));	// asynchronous call, first block, use existing Mac object
//			for (int dkBlock=2; dkBlock<=dkBlocks; dkBlock++) {
//				DeriveKeyBlockFromPassword task = new DeriveKeyBlockFromPassword(provider, algorithm, password, salt, xorIterations, DK, dkBlock);
//				futureResults.add(SERVICE.submit(task));	// asynchronous call, subsequent blocks, construct and initialize new Mac objects
//			}
//			for (final Future<Object> futureResult : futureResults) {
//				futureResult.get();
//			}
//		}

		if (isLastBlockTruncated) {	// no need to truncate last T(n) block
	        final byte[] truncatedDK = new byte[dkLen];		// last T(n) block needs to be truncated
	        System.arraycopy(DK, 0, truncatedDK, 0, dkLen);	// Equivalent to length/hashLength + numBytesLastIteration
	        return truncatedDK;
		}
		return DK;
	}

	public static byte[] deriveKeyFromPasswordBuiltIn(final String provider, final String algorithm, final char[] password, final byte[] salt, final int xorIterations, int dkLen) throws Exception {	// NOSONAR Refactor this method to throw at most one checked exception
		final PBEKeySpec pwKey = new PBEKeySpec(password, salt, xorIterations, dkLen);
		final SecretKeyFactory factory = SecretKeyFactory.getInstance(algorithm, provider);
		final PBEKey pbeKey = (PBEKey) factory.generateSecret(pwKey);
		return pbeKey.getEncoded();
	}

//	protected static class DeriveKeyBlockFromPassword implements Callable<Object> {
//		private       Mac    mac;
//		private final String provider;
//		private final String algorithm;
//		private final byte[] password;
//		private final byte[] salt;
//		private final int    xorIterations;
//		private final byte[] DK;
//		private final int    dkBlock;
//
//		public DeriveKeyBlockFromPassword(final String provider, final String algorithm, final byte[] password, final byte[] salt, final int xorIterations, final byte[] DK, int dkBlock) {
//			this.mac           = null;
//			this.provider      = provider;
//			this.algorithm     = algorithm;
//			this.password      = password;
//			this.salt          = salt;
//			this.xorIterations = xorIterations;
//			this.DK            = DK;
//			this.dkBlock       = dkBlock;
//		}
//
//		public DeriveKeyBlockFromPassword(final Mac mac, final byte[] salt, final int xorIterations, final byte[] DK) {
//			this.mac           = mac;
//			this.provider      = null;
//			this.algorithm     = null;
//			this.password      = null;
//			this.salt          = salt;
//			this.xorIterations = xorIterations;
//			this.DK            = DK;
//			this.dkBlock       = 1;
//		}
//
//		@Override
//		public Object call() throws Exception  {
//			if (null == this.mac) {
//				this.mac = Mac.getInstance(this.algorithm, this.provider);
//				this.mac.init(new SecretKeySpec(this.password, this.algorithm));
//			}
//			final int hashLength = this.mac.getMacLength();	// SHA1 => 20 bytes, SHA-256 => 32 bytes, SHA-512 => 64 bytes
//			final int dkOffset   = (this.dkBlock-1) * hashLength;
//			final int saltLength = this.salt.length;
//
//			byte[] Ui = new byte[saltLength+4];	// Note: U_0 size is saltLength+4, U_1/U_2/U_3/... sizes are hashLength
//			System.arraycopy(this.salt, 0, Ui, 0, saltLength);	// U_0 => S || INT(i), set S outside the loop vs INT(i) inside the loop to avoid repeating S initialization for each hash block
//			arrayEncodeInt(this.dkBlock, Ui, saltLength);	// Initialize INT(i) part of reusable U_0 byte array
//			for (int xorIteration=1; xorIteration<=this.xorIterations; xorIteration++) {
//				Ui = this.mac.doFinal(Ui);	// doFinal() implicitly resets mac back to init(), no need to re-init each loop iteration
//				for (int x = 0; x < hashLength; x++) {	// U_1 \xor U_2 \xor ... \xor U_c
//					this.DK[x+dkOffset] ^= Ui[x];	// Instead of Tn blocks copied to DK, work directly with DK to avoid unnecessary Tn zero fills and copy to DK for each block
//				}
//			}
//			return null;
//		}
//	}
}