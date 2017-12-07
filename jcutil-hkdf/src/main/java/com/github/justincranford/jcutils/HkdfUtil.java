package com.github.justincranford.jcutils;

import java.security.InvalidKeyException;
import java.security.InvalidParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

import javax.crypto.Mac;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.SecretKeySpec;

/**
 * Implementation of RFC 5869:
 * 
 *   HMAC-based Extract-and-Expand Key Derivation Function (HKDF)
 *   https://tools.ietf.org/html/rfc5869
 */
public class HkdfUtil {
	private HkdfUtil() {
		// declare private constructor to prevent instantiation of this class
	}

	public static byte[] hkdfExtractAndExpand(final String provider, final String algorithm, final int length, final byte[] ikm, final byte[] salt, final byte[] info) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeyException, ShortBufferException, IllegalStateException {	// NOSONAR
		final byte[] prk = HkdfUtil.hkdfExtract(provider, algorithm, ikm, salt);
		return hkdfExpand(provider, algorithm, length, info, prk);
	}

	public static byte[] hkdfExtract(final String provider, final String algorithm, final byte[] ikm, final byte[] salt) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeyException {	// NOSONAR
		if ((null == ikm) || (0 == ikm.length)) {
			throw new InvalidParameterException("IKM must be non-null and non-empty");
		}
		final Mac           mac           = Mac.getInstance(algorithm, provider);
		final byte[]        nonEmptySalt  = ((null == salt) || (0 == salt.length)) ? new byte[mac.getMacLength()] : salt;	// salt is recommended by RFC but not required, default to zeroed-out salt
		final SecretKeySpec secretKeySpec = new SecretKeySpec(nonEmptySalt, algorithm);
		mac.init(secretKeySpec);
		return mac.doFinal(ikm);
	}

	public static byte[] hkdfExpand(final String provider, final String algorithm, final int length, final byte[] info, final byte[] prk) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeyException, ShortBufferException, IllegalStateException {	// NOSONAR
		if ((null == prk) || (0 == prk.length)) {
			throw new InvalidParameterException("PRK must be non-null and non-empty");
		}
		final Mac mac        = Mac.getInstance(algorithm, provider);
		final int hashLength = mac.getMacLength();
		if (length < 1) {
			throw new InvalidParameterException("Min length is 1. Invalid valid " + length + ".");
		} else if (length > 255 * hashLength) {	// byte value 0x01=1 to 0xFF=255 is concatenated to each hashed block, so max HKDF expand is 255 * hashLength
			throw new InvalidParameterException("Max length is " + (255*hashLength) + " (=255*HashLen=255*" + hashLength + ". Invalid valid " + length + ".");	// Ex: HmacSHA512 => 64*255 => 16,320 bytes max
		}
		mac.init(new SecretKeySpec(prk, algorithm));
		final boolean isLastBlockTruncated = (length % hashLength > 0);
		final int     iterations           = (isLastBlockTruncated?1:0) + length/hashLength;	// round up to a multiple of the hash length
		final byte[]  T                    = new byte[hashLength*iterations];	// all blocks T(1), T(2), T(3), ..., T(n) concatenated
		final boolean doInfoUpdate         = (null != info) && (0 != info.length);
		for (int iteration=1, Toffset=0; iteration<=iterations; iteration++, Toffset+=hashLength) {
			// compute T(n) = HMAC-Hash(PRK, T(n-1) | info | 0x01) for T(1), T(2), T(3), ..., T(n)
			if (1 != iteration) {	// skip T(0) = empty string (zero length)
				mac.update(T, Toffset-hashLength, hashLength);	// previous T block section used to compute current T block section
			}
			if (doInfoUpdate) {	// only update non-empty info values, skip if null or empty
				mac.update(info);
			}
			mac.update((byte)iteration);	// always update with hash block number 1-255 inclusive
			mac.doFinal(T, Toffset);		// implicit reset, no need to re-init each loop iteration, update hash block directly at T offset (i.e. concatenation)
		}
		if (isLastBlockTruncated) {
	        final byte[] truncatedT = new byte[length];		// last T(n) block needs to be truncated
	        System.arraycopy(T, 0, truncatedT, 0, length);	// Equivalent to length/hashLength + numBytesLastIteration
	        return truncatedT;
		}
		return T;	// last block not truncated, return T as is
	}
}