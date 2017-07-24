package com.github.justincranford.jcutils;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import javax.xml.bind.DatatypeConverter;

@SuppressWarnings({"hiding","unused"})
public class DigestUtil {
	private static final Logger LOG = Logger.getLogger(DigestUtil.class.getName());	// NOSONAR Remove this unused "LOG" private field.

	private static final int DEFAULT_READ_BUFFER_SIZE = 8192;

	public enum ALGORITHM {
		// MD5 Algorithm & Variants (1992)
		MD5			("MD5",			40,					"md5"),		//  40 bites =  5 bytes
		// SHA-0 Algorithm & Variants (1993)
		SHA_0		("SHA-0",		160,				"sha0"),	// 160 bites = 20 bytes
		// SHA-1 Algorithm & Variants (1995)
		SHA_1		("SHA-1",		160,				"sha1"),	// 160 bites = 20 bytes
		// SHA-2 Algorithm & Variants (2001)
		SHA_224		("SHA-224", 	224,				"sha224"),	// 224 bites = 28 bytes
		SHA_256		("SHA-256", 	256,				"sha256"),	// 256 bites = 32 bytes
		SHA_384		("SHA-384", 	384,				"sha384"),	// 384 bites = 48 bytes
		SHA_512		("SHA-512", 	512,				"sha512"),	// 512 bites = 64 bytes
		SHA_512_224	("SHA-512/224", 224,				null),		// 224 bites = 28 bytes	(x64 CPUs compute SHA-512 faster than SHA-224)
		SHA_512_256	("SHA-512/256", 256,				null),		// 256 bites = 32 bytes	(x64 CPUs compute SHA-512 faster than SHA-256)
		// SHA-3 Algorithm & Variants (2015)
		SHA3_224	("SHA3-224",	224,				null),		// 224 bites = 28 bytes
		SHA3_256	("SHA3-256",	256,				null),		// 256 bites = 32 bytes
		SHA3_384	("SHA3-384",	384,				null),		// 384 bites = 48 bytes
		SHA3_512	("SHA3-512",	512,				null),		// 512 bites = 64 bytes
		SHAKE128	("SHAKE128",	Integer.MAX_VALUE,	null),		// arbitrary
		SHAKE256	("SHAKE256",	Integer.MAX_VALUE,	null);		// arbitrary
		private final String algorithmName;
		private final String fileNameExtension;
		private final int    numberOfBits;
		ALGORITHM(final String algorithmName, final int numberOfBits, final String fileNameExtension) {
			this.algorithmName = algorithmName;
			this.fileNameExtension = fileNameExtension;
			this.numberOfBits = numberOfBits;
		}
		public String getAlgorithmName() {
			return this.algorithmName;
		}
		public String getFileNameExtension() {
			return this.fileNameExtension;
		}
		public int getNumberOfBits() {
			return this.numberOfBits;
		}
		public int getNumberOfBytes() {
			return this.numberOfBits / 8;
		}
	}

	private DigestUtil() {
		// prevent class instantiation by making constructor private
	}

	public static String computeHash(final String string, final ALGORITHM algorithm) throws NoSuchAlgorithmException, UnsupportedEncodingException {	// NOSONAR Refactor this method to throw at most one checked exception 
		ValidationUtil.assertNonNullObject(string, "String must not be null");	// NOSONAR Define a constant instead of duplicating this literal
		return DigestUtil.computeHash(string.getBytes("UTF-8"), algorithm);	// ASSUMPTION: Algorithm will be null checked in here.
	}

	public static String computeHash(final byte[] byteArray, final ALGORITHM algorithm) throws NoSuchAlgorithmException {
		ValidationUtil.assertNonNullObject(byteArray, "Byte array must not be null");	// NOSONAR Define a constant instead of duplicating this literal
		ValidationUtil.assertNonNullObject(algorithm, "Algorithm must not be null");	// NOSONAR Define a constant instead of duplicating this literal
	    final MessageDigest messageDigest = MessageDigest.getInstance(algorithm.getAlgorithmName());
		messageDigest.update(byteArray, 0, byteArray.length);
		return DatatypeConverter.printHexBinary(messageDigest.digest());
	}

	public static String[] computeHashes(final byte[] value, final ALGORITHM[] algorithms) throws NoSuchAlgorithmException {
		ValidationUtil.assertNonNullObject(value, "Byte array must not be null");	// NOSONAR Define a constant instead of duplicating this literal
		ValidationUtil.assertNonNullArrayAndElements(algorithms, "Algorithms array and elements must not be null.");	// NOSONAR Define a constant instead of duplicating this literal
		final int numAlgorithms = algorithms.length;
	    final String[] computedHashes = new String[numAlgorithms];
		for (int i=0; i< numAlgorithms; i++) {
			computedHashes[i] = DigestUtil.computeHash(value, algorithms[i]);
		}
		return computedHashes;
	}

	public static String computeHash(final File file, final ALGORITHM algorithm) throws NoSuchAlgorithmException, IOException {	// NOSONAR Refactor this method to throw at most one checked exception 
		return DigestUtil.computeHashes(file, new ALGORITHM[]{algorithm})[0];
	}

	public static String computeHash(final File file, final int readBufferSize, final ALGORITHM algorithm) throws NoSuchAlgorithmException, IOException {	// NOSONAR Refactor this method to throw at most one checked exception 
		return DigestUtil.computeHashes(file, readBufferSize, new ALGORITHM[]{algorithm})[0];
	}

	public static String computeHash(final InputStream is, final ALGORITHM algorithm) throws NoSuchAlgorithmException, IOException {	// NOSONAR Refactor this method to throw at most one checked exception 
		return DigestUtil.computeHashes(is, DEFAULT_READ_BUFFER_SIZE, new ALGORITHM[]{algorithm})[0];
	}

	public static String computeHash(final InputStream is, final int readBufferSize, final ALGORITHM algorithm) throws NoSuchAlgorithmException, IOException {	// NOSONAR Refactor this method to throw at most one checked exception 
		return DigestUtil.computeHashes(is, readBufferSize, new ALGORITHM[]{algorithm})[0];
	}

	public static String[] computeHashes(final File file, final ALGORITHM[] algorithms) throws NoSuchAlgorithmException, IOException {	// NOSONAR Refactor this method to throw at most one checked exception
		return DigestUtil.computeHashes(file, DEFAULT_READ_BUFFER_SIZE, algorithms);
	}

	public static String[] computeHashes(final File file, final int readBufferSize, final ALGORITHM[] algorithms) throws NoSuchAlgorithmException, IOException {	// NOSONAR Refactor this method to throw at most one checked exception
		ValidationUtil.assertNonNullObject(file, "File must not be null");
		try (final BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file))) {
			return DigestUtil.computeHashes(bis, readBufferSize, algorithms);
		}
	}

	public static String[] computeHashes(final InputStream is, final ALGORITHM[] algorithms) throws NoSuchAlgorithmException, IOException {	// NOSONAR Refactor this method to throw at most one checked exception
		return DigestUtil.computeHashes(is, DEFAULT_READ_BUFFER_SIZE, algorithms);
	}

	public static String[] computeHashes(final InputStream is, final int readBufferSize, final ALGORITHM[] algorithms) throws NoSuchAlgorithmException, IOException {	// NOSONAR Refactor this method to throw at most one checked exception
		ValidationUtil.assertNonNullObject(is, "Input stream must not be null");
		ValidationUtil.assertNonNullArrayAndElements(algorithms, "Algorithms array and elements must not be null.");
		final int numAlgorithms = algorithms.length;
		final MessageDigest[] messageDigests = new MessageDigest[numAlgorithms];
		for (int i=0; i< numAlgorithms; i++) {
			messageDigests[i] = MessageDigest.getInstance(algorithms[i].getAlgorithmName());
		}
	    final byte[] buffer = new byte[readBufferSize];
	    int bytesRead;
	    while (-1 != (bytesRead = is.read(buffer))) {
			for (int i=0; i< numAlgorithms; i++) {
				messageDigests[i].update(buffer, 0, bytesRead);
			}
	    }
	    final String[] computedHashes = new String[numAlgorithms];
		for (int i=0; i< numAlgorithms; i++) {
			computedHashes[i] = DatatypeConverter.printHexBinary(messageDigests[i].digest());
		}
		return computedHashes;
	}

    public static Future<String[]> computeHashesAsync(final ExecutorService executorService, final File file, final ALGORITHM[] algorithms) throws Exception {	// NOSONAR Define and throw a dedicated exception instead of using a generic one.
    	return executorService.submit(new ComputeFileHashesTask(file, algorithms));
    }

	public static String[][] computeHashes(final ExecutorService executorService, final File[] files, final ALGORITHM[] algorithms) throws Exception {	// NOSONAR Define and throw a dedicated exception instead of using a generic one.
		ValidationUtil.assertNonNullArrayAndElements(files, "File array and elements must not be null.");
		ValidationUtil.assertNonNullArrayAndElements(algorithms, "Algorithms array and elements must not be null.");
    	final int numFiles = files.length;
        @SuppressWarnings("unchecked")
    	final Future<String[]>[] taskRequests = new Future[numFiles];
		for (int i=0; i<numFiles; i++) {
			taskRequests[i] = executorService.submit(new ComputeFileHashesTask(files[i], algorithms));
		}
		final String[][] computedHashes = new String[numFiles][algorithms.length];
		for (int i=0; i<numFiles; i++) {
			computedHashes[i] = taskRequests[i].get();
		}
		return computedHashes;
    }

	private static class ComputeFileHashesTask implements Callable<String[]> {
		private File file;
		private ALGORITHM[] algorithms;
		public ComputeFileHashesTask(final File file, final ALGORITHM[] algorithms) {
			this.file = file;
			this.algorithms = algorithms;
		}
		@Override
		public String[] call() throws NoSuchAlgorithmException, IOException  {
			return DigestUtil.computeHashes(this.file, this.algorithms);
		}
	}

	private static class MessageDigestUpdateTask implements Callable<Object> {
		private MessageDigest messageDigest;
		private byte[] data;
		public MessageDigestUpdateTask(final MessageDigest messageDigest, final byte[] data) {
			this.messageDigest = messageDigest;
			this.data = data;
		}
		@Override
		public Object call() throws NoSuchAlgorithmException, IOException  {
			this.messageDigest.update(this.data);
			return null;
		}
	}
}