package com.github.justincranford.jcutils;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

@SuppressWarnings({"static-method","unused"})
public class TestFileUtil {
	private static final Logger LOG = Logger.getLogger(TestFileUtil.class.getName());
	private static final File RESOURCE_EMPTY_FILE				= new File("target/test-classes/empty.txt");
	private static final File RESOURCE_HELLO_WORLD_UTF8_FILE	= new File("target/test-classes/helloworld-utf8.txt");
	private static final File RESOURCE_OVER_64KB_FILE			= new File("target/test-classes/over-64KB.txt");
	private static final File RESOURCE_DOES_NOT_EXIST_FILE		= new File("target/test-classes/doesnotexist.txt");

	@BeforeClass
	public static void beforeClass() {
		Assert.assertTrue(RESOURCE_EMPTY_FILE.exists());
		Assert.assertTrue(RESOURCE_HELLO_WORLD_UTF8_FILE.exists());
		Assert.assertTrue(RESOURCE_OVER_64KB_FILE.exists());
		Assert.assertFalse(RESOURCE_DOES_NOT_EXIST_FILE.exists());
	}

	@Test
	public void testPrivateConstructor() throws Exception {
		ValidationUtil.assertPrivateConstructorNoParameters(FileUtil.class, true);
	}

	@Test
	public void testGetCurrentDirectoryNoException() throws IOException {
		FileUtil.getCurrentCanonicalDirectory();
	}

	@Test
	public void testPrintCurrentDirectoryNoException() throws IOException {
		FileUtil.printCurrentDirectory();
	}

	@Test
	public void testParseDirNameAndFileName() throws IOException {
		Assert.assertArrayEquals(new String[]{"", ""}, FileUtil.parseDirNameAndFileName(""));
		Assert.assertArrayEquals(new String[]{"", "file.txt"}, FileUtil.parseDirNameAndFileName("file.txt"));
		Assert.assertArrayEquals(new String[]{"C:\\hello\\world", "file.txt"}, FileUtil.parseDirNameAndFileName("C:\\hello\\world\\file.txt"));
		Assert.assertArrayEquals(new String[]{"/hello/world", "file.txt"}, FileUtil.parseDirNameAndFileName("/hello/world/file.txt"));
	}

	@Test
	public void testRemoveFileNameExtension() throws IOException {
		Assert.assertEquals("", FileUtil.removeFileNameExtension(""));
		Assert.assertEquals("", FileUtil.removeFileNameExtension("."));
		Assert.assertEquals("", FileUtil.removeFileNameExtension(".txt"));
		Assert.assertEquals("file", FileUtil.removeFileNameExtension("file"));
		Assert.assertEquals("file", FileUtil.removeFileNameExtension("file.txt"));
	}

	@Test
	public void testComputeExtractDirectory() throws IOException {
		Assert.assertEquals("R:\\tmp\\_something.jar_", FileUtil.computeExtractDirectory(new File("R:\\tmp"), new File("C:\\something.jar")).getCanonicalPath());
		Assert.assertEquals("C:\\_something.jar_", FileUtil.computeExtractDirectory(new File(""), new File("C:\\something.jar")).getCanonicalPath());
		Assert.assertEquals("C:\\_something_", FileUtil.computeExtractDirectory(new File(""), new File("C:\\something")).getCanonicalPath());
		Assert.assertEquals("R:\\doesnotexist\\_something_", FileUtil.computeExtractDirectory(new File("R:\\doesnotexist"), new File("C:\\something")).getCanonicalPath());
		Assert.assertEquals("C:\\something\\_else_", FileUtil.computeExtractDirectory(new File("C:\\something"), new File("C:\\something\\else")).getCanonicalPath());
	}

	@Test
	public void testReadWriteDelete() throws Exception {
		final String currentCanonicalDirectory = FileUtil.getCurrentCanonicalDirectory();
		Assert.assertFalse(FileUtil.delete(currentCanonicalDirectory + "/thisdirectorydoesnotexist"));
		final byte[] helloWorldBytes = "Hello World!".getBytes("UTF-8");
		FileUtil.delete(currentCanonicalDirectory + "/thisdirectorydoesexist");	// Ensure directory does not exist before testing creation of the directory.
		Assert.assertTrue(FileUtil.makeCanonicalDirectories(currentCanonicalDirectory + "/thisdirectorydoesexist"));
		try {
			// different writes
			Assert.assertEquals(helloWorldBytes.length, FileUtil.writeBinaryFile(currentCanonicalDirectory + "/thisdirectorydoesexist/thisfileexists.txt", helloWorldBytes));
			Assert.assertEquals(helloWorldBytes.length, FileUtil.writeBinaryFile(new File(currentCanonicalDirectory + "/thisdirectorydoesexist/thisfileexists2.txt"), helloWorldBytes));
			Assert.assertEquals(helloWorldBytes.length, FileUtil.streamToFile(currentCanonicalDirectory + "/thisdirectorydoesexist/thisfileexists3.txt", new ByteArrayInputStream(helloWorldBytes)));
			Assert.assertEquals(helloWorldBytes.length, FileUtil.streamToFile(new File(currentCanonicalDirectory + "/thisdirectorydoesexist/thisfileexists4.txt"), new ByteArrayInputStream(helloWorldBytes)));

			// different successful reads
			Assert.assertArrayEquals(helloWorldBytes, FileUtil.readBinaryFile(currentCanonicalDirectory + "/thisdirectorydoesexist/thisfileexists.txt"));
			Assert.assertArrayEquals(helloWorldBytes, FileUtil.readBinaryFile(new File(currentCanonicalDirectory + "/thisdirectorydoesexist/thisfileexists.txt")));
			try (final FileInputStream fis = new FileInputStream(new File(currentCanonicalDirectory + "/thisdirectorydoesexist/thisfileexists.txt"))) {
				Assert.assertArrayEquals(helloWorldBytes, FileUtil.readBinaryStream(fis));
			}
			try (final BufferedInputStream bis = new BufferedInputStream(new FileInputStream(new File(currentCanonicalDirectory + "/thisdirectorydoesexist/thisfileexists.txt")))) {
				Assert.assertArrayEquals(helloWorldBytes, FileUtil.readBinaryStream(bis));
			}
			try (final BufferedInputStream bis = new BufferedInputStream(new FileInputStream(new File(currentCanonicalDirectory + "/thisdirectorydoesexist/thisfileexists.txt")))) {
				Assert.assertArrayEquals(helloWorldBytes, FileUtil.readBinaryStream(bis, 8192));
			}

			// different failed reads
			Assert.assertArrayEquals(null, FileUtil.readBinaryFile(currentCanonicalDirectory + "/thisdirectorydoesexist/thisfiledoesnotexist.txt"));
			Assert.assertArrayEquals(null, FileUtil.readBinaryFile(new File(currentCanonicalDirectory + "/thisdirectorydoesexist/thisfiledoesnotexist.txt")));

			// failed delete (in use)
			try (final FileInputStream fis = new FileInputStream(new File(currentCanonicalDirectory + "/thisdirectorydoesexist/thisfileexists.txt"))) {
				Assert.assertFalse(FileUtil.delete(currentCanonicalDirectory + "/thisdirectorydoesexist/thisfileexists.txt"));	// In use
			}

			Assert.assertFalse(FileUtil.delete(currentCanonicalDirectory + "/thisdirectorydoesexist/thisfiledoesnotexist.txt"));	// does not exist
			Assert.assertTrue(FileUtil.delete(currentCanonicalDirectory + "/thisdirectorydoesexist/thisfileexists.txt"));		// file deleted OK
		} finally {
			Assert.assertTrue(FileUtil.delete(currentCanonicalDirectory + "/thisdirectorydoesexist"));							// directory deleted OK (in finally to do cleanup)
		}
	}

	@Test
	public void testPerformanceReadTextFiles() throws Exception {
		final int[] bufferSizes = new int[] {1024,2048,4096,8192,16384,32768,65536,131072,262144,524288,1048576};
		Timer.setAutoLogInterval(0);
		Timer.setLogTotalTimeUnit(TimeUnit.MILLISECONDS);
		Timer.setLogAverageTimeUnit(TimeUnit.MICROSECONDS);
		TestFileUtil.testPerformanceReadTextFiles(5,bufferSizes);	// warm up
		Timer.resetTimers();
		TestFileUtil.testPerformanceReadTextFiles(100,bufferSizes);
		Timer.logAllOrderedByStartTime(true);
		Timer.logAllOrderedByStopTime(true);
		Timer.logAllOrderedByName(false);
		Timer.logAllOrderedByTotalTime(false);
		Timer.logAllOrderedByAverageTime(false);
		Timer.reset();
	}

	private static void testPerformanceReadTextFiles(final int iterations, final int[] bufferSizes) throws IOException, Exception {
		for (int iteration=0; iteration < iterations; iteration++) {
			for (final int bufferSize : bufferSizes) {
				try (Timer doesNotExist = new Timer("UTFileUtil-BufferSizePerfTest-doesnotexist-" + bufferSize)) {
					try {
						FileUtil.readFirstLineOfTextFile(RESOURCE_DOES_NOT_EXIST_FILE.getCanonicalPath(), "UTF-8", bufferSize);
					} catch(Exception e) {
						// ignore exception
					}
				}
			}
		}
		for (int iteration=0; iteration < iterations; iteration++) {
			for (final int bufferSize : bufferSizes) {
				try (Timer readEmpty = new Timer("UTFileUtil-BufferSizePerfTest-over64KB-" + bufferSize)) {
					try {
						FileUtil.readFirstLineOfTextFile(RESOURCE_OVER_64KB_FILE.getCanonicalPath(), "UTF-8", bufferSize);
					} catch(Exception e) {
						// ignore exception
					}
				}
			}
		}
		for (int iteration=0; iteration < iterations; iteration++) {
			for (final int bufferSize : bufferSizes) {
				try (final Timer helloWorld = new Timer("UTFileUtil-BufferSizePerfTest-helloWorld-" + bufferSize)) {
					try {
						FileUtil.readFirstLineOfTextFile(RESOURCE_HELLO_WORLD_UTF8_FILE.getCanonicalPath(), "UTF-8", bufferSize);
					} catch(Exception e) {
						// ignore exception
					}
				}
			}
		}
		for (int iteration=0; iteration < iterations; iteration++) {
			for (final int bufferSize : bufferSizes) {
				try (final Timer over64KB = new Timer("UTFileUtil-BufferSizePerfTest-readEmtpy-" + bufferSize)) {
					try {
						FileUtil.readFirstLineOfTextFile(RESOURCE_EMPTY_FILE.getCanonicalPath(), "UTF-8", bufferSize);
					} catch(Exception e) {
						// ignore exception
					}
				}
			}
		}
	}
}