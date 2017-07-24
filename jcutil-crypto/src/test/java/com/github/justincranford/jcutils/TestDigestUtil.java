package com.github.justincranford.jcutils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.github.justincranford.jcutils.DigestUtil.ALGORITHM;

@SuppressWarnings({"static-method","unused"})
public class TestDigestUtil {
	private static final File RESOURCE_EMPTY_FILE				= new File("target/test-classes/empty.txt");
	private static final File RESOURCE_HELLO_WORLD_UTF8_FILE	= new File("target/test-classes/helloworld-utf8.txt");
	private static final File RESOURCE_OVER_64KB_FILE			= new File("target/test-classes/over-64KB.txt");
	private static final File RESOURCE_DOES_NOT_EXIST_FILE		= new File("target/test-classes/doesnotexist.txt");

	private static final String EMPTY_STRING		= "";
	private static final String HELLO_WORLD_STRING	= "Hello World!";

	private static final ALGORITHM[] ALGORITHMS_MD5_AND_SHA1 = new ALGORITHM[] {ALGORITHM.MD5,ALGORITHM.SHA_1};

	private static final String EXPECTED_EMPTY_STRING_MD5			= "d41d8cd98f00b204e9800998ecf8427e";			// MD5 of ""
	private static final String EXPECTED_EMPTY_STRING_SHA1			= "da39a3ee5e6b4b0d3255bfef95601890afd80709";	// SHA1 of ""
	private static final String EXPECTED_HELLO_WORLD_STRING_MD5		= "ed076287532e86365e841e92bfc50d8c";			// MD5 of "Hello World!"
	private static final String EXPECTED_HELLO_WORLD_STRING_SHA1	= "2ef7bde608ce5404e97d5f042f95f89f1c232871";	// SHA1 of "Hello World!"
	private static final String EXPECTED_OVER_64KB_MD5				= "90402a01a65be30786a09539012b3051";			// MD5 of "12345678901234567890..."
	private static final String EXPECTED_OVER_64KB_SHA1				= "8db78ac65fe27d116c1cdca6798c66cb4b48fb75";	// SHA1 of "12345678901234567890..."

	private static ExecutorService EXECUTOR_SERVICE = null;

	@BeforeClass
	public static void beforeClass() {
		Assert.assertTrue(RESOURCE_EMPTY_FILE.exists());
		Assert.assertTrue(RESOURCE_HELLO_WORLD_UTF8_FILE.exists());
		Assert.assertTrue(RESOURCE_OVER_64KB_FILE.exists());
		Assert.assertFalse(RESOURCE_DOES_NOT_EXIST_FILE.exists());
		TestDigestUtil.EXECUTOR_SERVICE = new ThreadPool().getExecutorService();
	}

	@AfterClass
	public static void afterClass() {
		if (null != TestDigestUtil.EXECUTOR_SERVICE) {
			TestDigestUtil.EXECUTOR_SERVICE.shutdown();
			TestDigestUtil.EXECUTOR_SERVICE = null;
		}
	}

	@Test
	public void testPrivateConstructor() throws Exception {
		ValidationUtil.assertPrivateConstructorNoParameters(DigestUtil.class, true);
	}

	@Test(expected=FileNotFoundException.class)
	public void testFileHashesFileDoesNotExist() throws Exception {
		DigestUtil.computeHashes(RESOURCE_DOES_NOT_EXIST_FILE, ALGORITHMS_MD5_AND_SHA1);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testFileHashesNullFile() throws Exception {
		DigestUtil.computeHashes((File)null, ALGORITHMS_MD5_AND_SHA1);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testFileHashesNullInputStream() throws Exception {
		Assert.assertNull(DigestUtil.computeHashes((InputStream)null, ALGORITHMS_MD5_AND_SHA1));
	}

	@Test
	public void testFileHashes() throws Exception {
		try (FileInputStream fis = new FileInputStream(RESOURCE_EMPTY_FILE)) {
			TestDigestUtil.assertExpectedMd5Hash( EXPECTED_EMPTY_STRING_MD5,        DigestUtil.computeHash(fis, ALGORITHM.MD5));
		}
		try (FileInputStream fis = new FileInputStream(RESOURCE_EMPTY_FILE)) {
			TestDigestUtil.assertExpectedSha1Hash(EXPECTED_EMPTY_STRING_SHA1,       DigestUtil.computeHash(fis, ALGORITHM.SHA_1));
		}
		try (FileInputStream fis = new FileInputStream(RESOURCE_HELLO_WORLD_UTF8_FILE)) {
			TestDigestUtil.assertExpectedMd5Hash( EXPECTED_HELLO_WORLD_STRING_MD5,  DigestUtil.computeHash(fis, ALGORITHM.MD5));
		}
		try (FileInputStream fis = new FileInputStream(RESOURCE_HELLO_WORLD_UTF8_FILE)) {
			TestDigestUtil.assertExpectedSha1Hash(EXPECTED_HELLO_WORLD_STRING_SHA1, DigestUtil.computeHash(fis, ALGORITHM.SHA_1));
		}
		try (FileInputStream fis = new FileInputStream(RESOURCE_OVER_64KB_FILE)) {
			TestDigestUtil.assertExpectedMd5Hash( EXPECTED_OVER_64KB_MD5,           DigestUtil.computeHash(fis, ALGORITHM.MD5));
		}
		try (FileInputStream fis = new FileInputStream(RESOURCE_OVER_64KB_FILE)) {
			TestDigestUtil.assertExpectedSha1Hash(EXPECTED_OVER_64KB_SHA1,          DigestUtil.computeHash(fis, ALGORITHM.SHA_1));
		}

		TestDigestUtil.assertExpectedMd5Hash( EXPECTED_EMPTY_STRING_MD5,        DigestUtil.computeHash(RESOURCE_EMPTY_FILE,            8192, ALGORITHM.MD5));
		TestDigestUtil.assertExpectedSha1Hash(EXPECTED_EMPTY_STRING_SHA1,       DigestUtil.computeHash(RESOURCE_EMPTY_FILE,            8192, ALGORITHM.SHA_1));
		TestDigestUtil.assertExpectedMd5Hash( EXPECTED_HELLO_WORLD_STRING_MD5,  DigestUtil.computeHash(RESOURCE_HELLO_WORLD_UTF8_FILE, 8192, ALGORITHM.MD5));
		TestDigestUtil.assertExpectedSha1Hash(EXPECTED_HELLO_WORLD_STRING_SHA1, DigestUtil.computeHash(RESOURCE_HELLO_WORLD_UTF8_FILE, 8192, ALGORITHM.SHA_1));
		TestDigestUtil.assertExpectedMd5Hash( EXPECTED_OVER_64KB_MD5,           DigestUtil.computeHash(RESOURCE_OVER_64KB_FILE,        8192, ALGORITHM.MD5));
		TestDigestUtil.assertExpectedSha1Hash(EXPECTED_OVER_64KB_SHA1,          DigestUtil.computeHash(RESOURCE_OVER_64KB_FILE,        8192, ALGORITHM.SHA_1));

		TestDigestUtil.assertExpectedHashes(EXPECTED_EMPTY_STRING_MD5,       EXPECTED_EMPTY_STRING_SHA1,       DigestUtil.computeHashes(RESOURCE_EMPTY_FILE,            ALGORITHMS_MD5_AND_SHA1));
		TestDigestUtil.assertExpectedHashes(EXPECTED_HELLO_WORLD_STRING_MD5, EXPECTED_HELLO_WORLD_STRING_SHA1, DigestUtil.computeHashes(RESOURCE_HELLO_WORLD_UTF8_FILE, ALGORITHMS_MD5_AND_SHA1));
		TestDigestUtil.assertExpectedHashes(EXPECTED_OVER_64KB_MD5,          EXPECTED_OVER_64KB_SHA1,          DigestUtil.computeHashes(RESOURCE_OVER_64KB_FILE,        ALGORITHMS_MD5_AND_SHA1));
	}

	@Test(expected=FileNotFoundException.class)
	public void testFileHashesAsyncFileDoesNotExist() throws Throwable {
		try {
			DigestUtil.computeHashesAsync(TestDigestUtil.EXECUTOR_SERVICE, RESOURCE_DOES_NOT_EXIST_FILE, ALGORITHMS_MD5_AND_SHA1).get();
		} catch(ExecutionException e) {
			throw e.getCause();	// Expect ExecutionException to wrap FileNotFoundException, so throw the cause
		}
	}

	@Test
	public void testFileHashesAsync() throws Exception {
		TestDigestUtil.assertExpectedHashes(EXPECTED_EMPTY_STRING_MD5, EXPECTED_EMPTY_STRING_SHA1, DigestUtil.computeHashesAsync(TestDigestUtil.EXECUTOR_SERVICE, RESOURCE_EMPTY_FILE, ALGORITHMS_MD5_AND_SHA1).get());
		TestDigestUtil.assertExpectedHashes(EXPECTED_HELLO_WORLD_STRING_MD5, EXPECTED_HELLO_WORLD_STRING_SHA1, DigestUtil.computeHashesAsync(TestDigestUtil.EXECUTOR_SERVICE, RESOURCE_HELLO_WORLD_UTF8_FILE, ALGORITHMS_MD5_AND_SHA1).get());
		TestDigestUtil.assertExpectedHashes(EXPECTED_OVER_64KB_MD5, EXPECTED_OVER_64KB_SHA1, DigestUtil.computeHashesAsync(TestDigestUtil.EXECUTOR_SERVICE, RESOURCE_OVER_64KB_FILE, ALGORITHMS_MD5_AND_SHA1).get());

		final String[][] fileMd5AndSha1Hashes = DigestUtil.computeHashes(TestDigestUtil.EXECUTOR_SERVICE, new File[]{RESOURCE_EMPTY_FILE, RESOURCE_HELLO_WORLD_UTF8_FILE, RESOURCE_OVER_64KB_FILE}, ALGORITHMS_MD5_AND_SHA1);
		Assert.assertNotNull(fileMd5AndSha1Hashes);
		Assert.assertEquals(3, fileMd5AndSha1Hashes.length);
		TestDigestUtil.assertExpectedHashes(EXPECTED_EMPTY_STRING_MD5,          EXPECTED_EMPTY_STRING_SHA1,       fileMd5AndSha1Hashes[0]);
		TestDigestUtil.assertExpectedHashes(EXPECTED_HELLO_WORLD_STRING_MD5,    EXPECTED_HELLO_WORLD_STRING_SHA1, fileMd5AndSha1Hashes[1]);
		TestDigestUtil.assertExpectedHashes(EXPECTED_OVER_64KB_MD5,             EXPECTED_OVER_64KB_SHA1,          fileMd5AndSha1Hashes[2]);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testStringHashNull() throws Exception {
		Assert.assertNull(DigestUtil.computeHash((InputStream)null, 8192, ALGORITHM.MD5));
	}

	@Test
	public void testStringHashes() throws Exception {
		TestDigestUtil.assertExpectedMd5Hash( EXPECTED_EMPTY_STRING_MD5,        DigestUtil.computeHash(new ByteArrayInputStream(EMPTY_STRING.getBytes("UTF-8")), 8192, ALGORITHM.MD5));
		TestDigestUtil.assertExpectedSha1Hash(EXPECTED_EMPTY_STRING_SHA1,       DigestUtil.computeHash(new ByteArrayInputStream(EMPTY_STRING.getBytes("UTF-8")), 8192, ALGORITHM.SHA_1));
		TestDigestUtil.assertExpectedMd5Hash( EXPECTED_HELLO_WORLD_STRING_MD5,  DigestUtil.computeHash(new ByteArrayInputStream(HELLO_WORLD_STRING.getBytes("UTF-8")), 8192, ALGORITHM.MD5));
		TestDigestUtil.assertExpectedSha1Hash(EXPECTED_HELLO_WORLD_STRING_SHA1, DigestUtil.computeHash(new ByteArrayInputStream(HELLO_WORLD_STRING.getBytes("UTF-8")), 8192, ALGORITHM.SHA_1));

		TestDigestUtil.assertExpectedMd5Hash( EXPECTED_EMPTY_STRING_MD5,        DigestUtil.computeHash(EMPTY_STRING.getBytes("UTF-8"), ALGORITHM.MD5));
		TestDigestUtil.assertExpectedSha1Hash(EXPECTED_EMPTY_STRING_SHA1,       DigestUtil.computeHash(EMPTY_STRING.getBytes("UTF-8"), ALGORITHM.SHA_1));
		TestDigestUtil.assertExpectedMd5Hash( EXPECTED_HELLO_WORLD_STRING_MD5,  DigestUtil.computeHash(HELLO_WORLD_STRING.getBytes("UTF-8"), ALGORITHM.MD5));
		TestDigestUtil.assertExpectedSha1Hash(EXPECTED_HELLO_WORLD_STRING_SHA1, DigestUtil.computeHash(HELLO_WORLD_STRING.getBytes("UTF-8"), ALGORITHM.SHA_1));

		TestDigestUtil.assertExpectedHashes(EXPECTED_EMPTY_STRING_MD5,       EXPECTED_EMPTY_STRING_SHA1,       new String[]{DigestUtil.computeHash(EMPTY_STRING.getBytes("UTF-8"), ALGORITHM.MD5),       DigestUtil.computeHash(EMPTY_STRING.getBytes("UTF-8"), ALGORITHM.SHA_1)});
		TestDigestUtil.assertExpectedHashes(EXPECTED_HELLO_WORLD_STRING_MD5, EXPECTED_HELLO_WORLD_STRING_SHA1, new String[]{DigestUtil.computeHash(HELLO_WORLD_STRING.getBytes("UTF-8"), ALGORITHM.MD5), DigestUtil.computeHash(HELLO_WORLD_STRING.getBytes("UTF-8"), ALGORITHM.SHA_1)});
	}

	private static void assertExpectedHashes(final String expectedMd5, final String expectedSha1, final String[] actualMd5AndSha1) {
		Assert.assertNotNull(actualMd5AndSha1);
		Assert.assertEquals(2, actualMd5AndSha1.length);
		TestDigestUtil.assertExpectedMd5Hash(expectedMd5, actualMd5AndSha1[0]);
		TestDigestUtil.assertExpectedSha1Hash(expectedSha1, actualMd5AndSha1[1]);
	}

	private static void assertExpectedMd5Hash(final String expectedMd5, final String actualMd5) {
		Assert.assertNotNull(actualMd5);
		Assert.assertNotEquals("", actualMd5);
		Assert.assertEquals(expectedMd5, actualMd5.toLowerCase());
	}

	private static void assertExpectedSha1Hash(final String expectedSha1, final String actualSha1) {
		Assert.assertNotNull(actualSha1);
		Assert.assertNotEquals("", actualSha1);
		Assert.assertEquals(expectedSha1, actualSha1.toLowerCase());
	}

	@Test
	public void testPerformanceReadTextFiles() throws Exception {
		final int[] bufferSizes = new int[] {1024,2048,4096,8192,16384,32768,65536,131072,262144,524288,1048576};
		Timer.setAutoLogInterval(0);
		TestDigestUtil.testPerformanceReadTextFiles(10,bufferSizes);	// warm up
		Timer.resetTimers();
		TestDigestUtil.testPerformanceReadTextFiles(100,bufferSizes);
		Timer.logAllOrderedByStartTime(true);
		Timer.reset();
	}

	@Test
	public void testComputeMd5AndSha1HashesThreadSafetyFast() throws Exception {
		testComputeMd5AndSha1HashesThreadSafety(6000);
	}

//	@Category({JUnitCategories.Slow.class,JUnitCategories.Security.class,JUnitCategories.Performance.class,JUnitCategories.Concurrency.class})
//	@Test
//	public void testComputeMd5AndSha1HashesThreadSafetySlow() throws Exception {
//		testComputeMd5AndSha1HashesThreadSafety(600000); // ThreadLocal<MessageDigest> concurrency issue at >>500000
//	}

	private void testComputeMd5AndSha1HashesThreadSafety(final int numAsyncRequests) throws Exception {
		final File[] seedFiles = new File[]{RESOURCE_EMPTY_FILE, RESOURCE_HELLO_WORLD_UTF8_FILE, RESOURCE_OVER_64KB_FILE};
		final int numSeedFiles = seedFiles.length;
		
		final File[] asyncFileRequests = new File[numAsyncRequests];
		for (int i=0; i<numAsyncRequests; i++) {
			asyncFileRequests[i] = seedFiles[i%numSeedFiles];
		}

		final String[][] md5AndSha1Hashes;
		try (Timer x = new Timer("DigestUtil.computeMd5AndSha1Hashes " + numAsyncRequests)) {
			md5AndSha1Hashes = DigestUtil.computeHashes(TestDigestUtil.EXECUTOR_SERVICE, asyncFileRequests, ALGORITHMS_MD5_AND_SHA1);
		}
		for (int i=0; i<numAsyncRequests; i++) {
			Assert.assertNotNull(md5AndSha1Hashes[i][0]);
			Assert.assertNotNull(md5AndSha1Hashes[i][1]);
			Assert.assertNotEquals("", md5AndSha1Hashes[i][0]);
			Assert.assertNotEquals("", md5AndSha1Hashes[i][1]);
		}
	}

	private static void testPerformanceReadTextFiles(final int iterations, final int[] bufferSizes) throws IOException, Exception {
		for (int iteration=0; iteration < iterations; iteration++) {
			for (final int bufferSize : bufferSizes) {
				try (Timer doesNotExist = new Timer("TestDigestUtil-BufferSizePerfTest-doesnotexist-" + bufferSize)) {
					try {
						DigestUtil.computeHashes(RESOURCE_DOES_NOT_EXIST_FILE, bufferSize, ALGORITHMS_MD5_AND_SHA1);
					} catch(Exception e) {
						// ignore exception
					}
				}
			}
		}
		for (int iteration=0; iteration < iterations; iteration++) {
			for (final int bufferSize : bufferSizes) {
				try (Timer readEmpty = new Timer("TestDigestUtil-BufferSizePerfTest-over64KB-" + bufferSize)) {
					try {
						DigestUtil.computeHashes(RESOURCE_OVER_64KB_FILE, bufferSize, ALGORITHMS_MD5_AND_SHA1);
					} catch(Exception e) {
						// ignore exception
					}
				}
			}
		}
		for (int iteration=0; iteration < iterations; iteration++) {
			for (final int bufferSize : bufferSizes) {
				try (final Timer helloWorld = new Timer("TestDigestUtil-BufferSizePerfTest-helloWorld-" + bufferSize)) {
					try {
						DigestUtil.computeHashes(RESOURCE_HELLO_WORLD_UTF8_FILE, bufferSize, ALGORITHMS_MD5_AND_SHA1);
					} catch(Exception e) {
						// ignore exception
					}
				}
			}
		}
		for (int iteration=0; iteration < iterations; iteration++) {
			for (final int bufferSize : bufferSizes) {
				try (final Timer over64KB = new Timer("TestDigestUtil-BufferSizePerfTest-readEmtpy-" + bufferSize)) {
					try {
						DigestUtil.computeHashes(RESOURCE_EMPTY_FILE, bufferSize, ALGORITHMS_MD5_AND_SHA1);
					} catch(Exception e) {
						// ignore exception
					}
				}
			}
		}
	}
}