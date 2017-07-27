package com.github.justincranford.jcutils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.github.justincranford.jcutils.FileUtil;
import com.github.justincranford.jcutils.StringUtil;
import com.github.justincranford.jcutils.ThreadPool;

@SuppressWarnings({"hiding","unchecked"})
public class ZipUtil {
	private static final Logger LOG = Logger.getLogger(ZipUtil.class.getName());

	public static final String[] MATCH_ALL					= {".*"};						// NOSONAR Make this member "protected".
	public static final String[] MATCH_ENTRY_JAVA_ZIPS      = {".*\\.jar",".*\\.war",".*\\.ear",".*\\.sar",".*\\.par",".*\\.rar",".*\\.kar"};		// NOSONAR Make this member "protected".
	public static final String[] MATCH_ENTRY_JAR_AND_WAR	= {".*\\.jar$",".*\\.war$"};	// NOSONAR Make this member "protected".
	public static final String[] MATCH_ENTRY_JAR			= {".*\\.jar$"};				// NOSONAR Make this member "protected".
	public static final String[] MATCH_ENTRY_WAR			= {".*\\.war$"};				// NOSONAR Make this member "protected".
	public static final String[] MATCH_ENTRY_EAR 			= {".*\\.ear$"};				// NOSONAR Make this member "protected".
	public static final String[] MATCH_ENTRY_ZIP			= {".*\\.zip$"};				// NOSONAR Make this member "protected".
	public static final String[] MATCH_NULL					= null;							// NOSONAR Make this member "protected".

	private ZipUtil() {
		// prevent instantiation of constructor
	}

	public static List<File> extractFiles(final ExecutorService executorService, final File tempDir, final File[] zipFiles, final String[] includes, final String[] excludes) throws Exception {	// NOSONAR Define and throw a dedicated exception instead of using a generic one.
		return ZipUtil.extractFiles(executorService, tempDir, Arrays.asList(zipFiles), includes, excludes);
	}

	public static List<File> extractFiles(final File tempDir, final File zipFileName, final String[] includes, final String[] excludes) throws Exception {	// NOSONAR Define and throw a dedicated exception instead of using a generic one.
		final String zipFileCanonicalPath = zipFileName.getCanonicalPath();
		if (!zipFileName.exists()) {
			LOG.log(Level.SEVERE, "Zip '" + zipFileCanonicalPath + "' does not exist.");	// NOSONAR Define a constant instead of duplicating this literal "Zip '" 3 times.
			return FileUtil.EMPTY_FILE_LIST;
		} else if (!zipFileName.isFile()) {
			LOG.log(Level.SEVERE, "Zip '" + zipFileCanonicalPath + "' is not a file.");
			return FileUtil.EMPTY_FILE_LIST;
		} else if (!zipFileName.canRead()) {
			LOG.log(Level.SEVERE, "Zip '" + zipFileCanonicalPath + "' cannot be read.");
			return FileUtil.EMPTY_FILE_LIST;
		} else if (LOG.isLoggable(Level.FINE)) {
			LOG.log(Level.FINE, "Reading zip '" + zipFileCanonicalPath + "'.\n");
		}
		final File zipFileExtractDirectory = FileUtil.computeExtractDirectory(tempDir, zipFileName);
		final List<File> extractedFiles = new ArrayList<>();
		try (final ZipFile zipFile = new ZipFile(zipFileName)) {
			final Enumeration<? extends ZipEntry> entries = zipFile.entries();
			while (entries.hasMoreElements()) {
				final ZipEntry entry = entries.nextElement();
				final String entryFilePath = entry.getName();
				if ((entry.isDirectory()) || (!StringUtil.isMatch(entryFilePath, includes, excludes))) {
					continue;
				}
				final File extractEntryPath = new File(zipFileExtractDirectory, entryFilePath);
				final File extractFileDirectory = extractEntryPath.getParentFile();
				if (extractFileDirectory.isDirectory() || extractFileDirectory.mkdirs()) {
					FileUtil.streamToFile(extractEntryPath, zipFile.getInputStream(entry));
					extractedFiles.add(extractEntryPath);
				}
			}
		} catch(Exception e) {
			LOG.log(Level.WARNING, "Error for zip file " + zipFileName);
			throw e;
		}
		return extractedFiles;
	}

	public static Future<List<File>> extractFilesAsync(final ExecutorService executorService, final File tempDir, final File zipFile, final String[] includes, final String[] excludes) throws Exception {	// NOSONAR Define and throw a dedicated exception instead of using a generic one.
		return executorService.submit(new ExtractTask(tempDir, zipFile, includes, excludes));
	}

	public static List<Future<List<File>>> extractFilesAsync(final ExecutorService executorService, final File tempDir, final List<File> zipFiles, final String[] includes, final String[] excludes) throws Exception {	// NOSONAR Define and throw a dedicated exception instead of using a generic one.
		final List<Future<List<File>>> taskRequests = new ArrayList<>(zipFiles.size());
		for (final File zipFile : zipFiles) {
			taskRequests.add(extractFilesAsync(executorService, tempDir, zipFile, includes, excludes));
		}
		return ThreadPool.invokeAll(executorService, taskRequests);
	}

	public static List<File> extractFiles(final ExecutorService executorService, final File tempDir, final List<File> zipFiles, final String[] includes, final String[] excludes) throws Exception {	// NOSONAR Define and throw a dedicated exception instead of using a generic one.
		final List<Callable<List<File>>> taskRequests = new ArrayList<>(zipFiles.size());
		for (final File zipFile : zipFiles) {
			taskRequests.add(new ExtractTask(tempDir, zipFile, includes, excludes));
		}
		final List<List<File>> invokeAll = ThreadPool.invokeAll(executorService, taskRequests);

		// merge lists of files extracted for each zip
		final List<File> allExtractedFiles = new ArrayList<>();
		for (final List<File> extractedFiles : invokeAll) {
			allExtractedFiles.addAll(extractedFiles);
		}

		// if any zips extracted, do recursion on those zips
		if (!allExtractedFiles.isEmpty()) {
			allExtractedFiles.addAll(ZipUtil.extractFiles(executorService, tempDir, allExtractedFiles, includes, excludes));	// recurse
		}
		return allExtractedFiles;
	}

	private static class ExtractTask implements Callable<List<File>> {
		private File tempDir;
		private File zipFile;
		private String[] includes;
		private String[] excludes;
		public ExtractTask(final File tempDir, final File zipFile, final String[] includes, final String[] excludes) {
			this.tempDir = tempDir;
			this.zipFile = zipFile;
			this.includes = includes;
			this.excludes = excludes;
		}
		@Override
		public List<File> call() throws Exception  {
			return ZipUtil.extractFiles(this.tempDir, this.zipFile, this.includes, this.excludes);
		}
	}
}