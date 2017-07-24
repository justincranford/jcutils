package com.github.justincranford.jcutils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main method to recursively file files and directories and recursively unpack them to a TEMP directory.
 * - Recursive depth is unlimited.
 * - All files are unpacked based on file headers, not file name (ex: if something.tar is mislabeled something_tgz, it is unpacked anyway).
 *    - Note: Unrar is missing a file header check, and suffers bad performance from non-rar files, so it is wrapped with a RAR header check (but not RAR5).
 *    - Note: A file name exclusion filter is possible. Change it in code. For example, ignoring *.class from jar files can speed unpacking by 10-25x.
 * - For max performance:
 *    - Set TEMP/TMP to a RamDisk (ex: TEMP=R:/ or TMP=/mount/ramdrive). For example, "Radeon RamDisk" supports up to 4GB on Windows.
 *    - Add the TEMP/TMP "unpack" subdirectory to your Anti-Virus excluded folders, to avoid read-behind scan penalty, then manual scan after if required.
 * - 
 */
@SuppressWarnings({"unused"})
public class Unpack {
	private static final Logger LOG = Logger.getLogger(Unpack.class.getName());

	private static final long WAIT_FOR_PROFILER_MILLIS = 0L;	// <=0 disables it (Recommended for attach: 10000L)

	private static final String DEFAULT_EXTRACT_JAVAZIP_TMP_DIRECTORY = SystemProperty.IS_WINDOWS ? "C:/" : "/tmp/";	// "./unpack" is appended
	private static final boolean DELETE_EXTRACT_JAVAZIP_TMP_DIRECTORY = true;

	private static final boolean IGNORE_CLASS_FILES = true;	// skip extraction & analysis for 10-15x performance improvement

	private Unpack() {
		// declare private constructor to prevent instantiation of this class
	}

	public static void main(final String[] args) throws Exception {
		if ((null == args) || (0 == args.length)) {
			LOG.log(Level.SEVERE, "usage: <directory1> <file1> <file2> <directory2>...\n - Example: Unpack C:/dist1/ D:/archive.tgz\n\nRecommendation: Set 'TEMP' env variable (TEMP=R:/) to a Ram Drive (ex: Radeon RAMDisk) for best performance.\n");
			return;
		}
		final String[] requestedDirectoriesOrFiles = args;

		final String unpackDirectoryName;
		{	// MAX PERFORMANCE => Set TEMP/TMP to RamDisk (ex: TEMP=R:/) and exclude "unpack" subdirectory from Anti-Virus read-behind scanning.
			final String tempEnvironmentVariable = System.getenv(SystemProperty.IS_WINDOWS ? "TEMP" : "TMP");	// EX: C:\Users\%USERNAME%\AppData\Local\Temp
			if (null == tempEnvironmentVariable) {	// Ex: TMP may not be defined on Linux
				unpackDirectoryName = DEFAULT_EXTRACT_JAVAZIP_TMP_DIRECTORY;
			} else {
				unpackDirectoryName = new File(tempEnvironmentVariable, "unpack").getCanonicalPath();
				if (!FileUtil.makeCanonicalDirectories(unpackDirectoryName)) {	// Verify if directory exists, or if we successfully created it.
					LOG.log(Level.SEVERE, "Directory '" + unpackDirectoryName + "' does not exist and could not be created.\n");
					return;
				}
			}
		}

		if (DELETE_EXTRACT_JAVAZIP_TMP_DIRECTORY) {	// Cleanup previous unpacked content
			LOG.log(Level.INFO, "Deleting '" + unpackDirectoryName + "'...");
			try (final Timer deleteExistingUnpackedFiles = new Timer("FileUtil.delete " + unpackDirectoryName)) {
				FileUtil.delete(unpackDirectoryName);
			}	// Timer is auto-closed
		}

		if (Unpack.WAIT_FOR_PROFILER_MILLIS > 0) {
			LOG.log(Level.WARNING, "Waiting");
			Thread.sleep(WAIT_FOR_PROFILER_MILLIS);
			LOG.log(Level.WARNING, "Continuing");
		}
		Timer.setAutoLogInterval(0);	// prevent automatic logging at the close of each internal, so we can print all at the end
		Timer.setLogTotalTimeUnit(TimeUnit.SECONDS);
		Timer.setLogAverageTimeUnit(TimeUnit.SECONDS);
		Timer.start("main");

		final Set<String> filesAlreadyProcessed = new HashSet<>();
		final List<File>  skippedFiles          = new ArrayList<>();
		final List<File>  invalidFiles          = new ArrayList<>();
		final List<File>  validFiles            = new ArrayList<>();
		final List<File>  invalidArchives       = new ArrayList<>();
		final List<File>  validArchives         = new ArrayList<>();
		if (IGNORE_CLASS_FILES) {
			UnpackHelper.unpackDirectoriesAndFiles(new File(unpackDirectoryName), requestedDirectoriesOrFiles, UnpackHelper.MATCH_ALL, UnpackHelper.MATCH_ENTRY_CLASS, skippedFiles, invalidFiles, validFiles, invalidArchives, validArchives, filesAlreadyProcessed, 1);
		} else {
			UnpackHelper.unpackDirectoriesAndFiles(new File(unpackDirectoryName), requestedDirectoriesOrFiles, UnpackHelper.MATCH_ALL, UnpackHelper.MATCH_NULL, skippedFiles, invalidFiles, validFiles, invalidArchives, validArchives, filesAlreadyProcessed, 1);
		}
//		UnpackHelper.unpackDirectoriesAndFiles(new File(tempDirectory), requestedDirectoriesOrFiles, UnpackHelper.MATCH_ALL, UnpackHelper.MATCH_ENTRY_NON_ARCHIVES, skippedFiles, invalidFiles, validFiles, invalidArchives, validArchives, filesAlreadyProcessed, 1);

		try (final Timer getFileNameExtensions = new Timer("Get File Name Extensions")) {
			final Map<Integer,TreeSet<String>> skippedFileNamesExtensions    = getFileNameExtensions(skippedFiles);
			final Map<Integer,TreeSet<String>> invalidFileNamesExtensions    = getFileNameExtensions(invalidFiles);
			final Map<Integer,TreeSet<String>> validFileNamesExtensions      = getFileNameExtensions(validFiles);
			final Map<Integer,TreeSet<String>> invalidArchiveNamesExtensions = getFileNameExtensions(invalidArchives);
			final Map<Integer,TreeSet<String>> validArchiveNamesExtensions   = getFileNameExtensions(validArchives);
			LOG.log(Level.INFO, "Summary:\n" + // NOSONAR
				"Skipped files    " + skippedFiles.size()    + ", types: " + skippedFileNamesExtensions    + "\n" +	// NOSONAR
				"Invalid files    " + invalidFiles.size()    + ", types: " + invalidFileNamesExtensions    + "\n" +	// NOSONAR
				"Valid files      " + validFiles.size()      + ", types: " + validFileNamesExtensions      + "\n" +	// NOSONAR
				"Invalid archives " + invalidArchives.size() + ", types: " + invalidArchiveNamesExtensions + "\n" +	// NOSONAR
				"Valid archives   " + validArchives.size()   + ", types: " + validArchiveNamesExtensions);			// NOSONAR

			if (LOG.isLoggable(Level.FINE)) {	// Disable INFO logging due to verbosity and large sorting performance hit. Only enable log message for debug.
				LOG.log(Level.FINE, "Summary:\n" + // NOSONAR
					"Skipped files    " + skippedFiles.size()    + ", names: \n" + StringUtil.join(FileUtil.getCanonicalPaths(skippedFiles),"\n")    + "\n" +	// NOSONAR
					"Invalid files    " + invalidFiles.size()    + ", names: \n" + StringUtil.join(FileUtil.getCanonicalPaths(invalidFiles),"\n")    + "\n" +	// NOSONAR
					"Valid files      " + validFiles.size()      + ", names: \n" + StringUtil.join(FileUtil.getCanonicalPaths(validFiles),"\n")      + "\n" +	// NOSONAR
					"Invalid archives " + invalidArchives.size() + ", names: \n" + StringUtil.join(FileUtil.getCanonicalPaths(invalidArchives),"\n") + "\n" +	// NOSONAR
					"Valid archives   " + validArchives.size()   + ", names: \n" + StringUtil.join(FileUtil.getCanonicalPaths(validArchives),"\n"));			// NOSONAR
			}
		}	// Timer is auto-closed

		Timer.stop("main");
		Timer.logAllOrderedByStartTime(true);
		Timer.logAllOrderedByTotalTime(false);

		if (Unpack.WAIT_FOR_PROFILER_MILLIS > 0) {
			LOG.log(Level.WARNING, "Take snapshot now");
			Thread.sleep(WAIT_FOR_PROFILER_MILLIS);
		}
	}

	private static Map<Integer,TreeSet<String>> getFileNameExtensions(final List<File> fileObjs) {
		// Compute map of file extension names -> counts
		final Map<String,Integer> fileTypes = new HashMap<>();
		for (final File fileObj : fileObjs) {
			final String  fileName                = fileObj.getName();
			final int     fileNameExtensionOffset = fileName.lastIndexOf('.');
			final String  fileNameExtension       = (-1 == fileNameExtensionOffset ? fileName : fileName.substring(fileNameExtensionOffset+1));
			final Integer fileNameExtensionCount  = fileTypes.get(fileNameExtension);
			if (null == fileNameExtensionCount) {
				fileTypes.put(fileNameExtension, Integer.valueOf(1));
			} else {
				fileTypes.put(fileNameExtension, Integer.valueOf(1+fileNameExtensionCount.intValue()));
			}
		}

		// Compute reverse map of counts -> file extension name lists
		final Map<Integer,TreeSet<String>> counts = new TreeMap<>(Collections.reverseOrder());
		for (final Entry<String,Integer> fileTypesEntry : fileTypes.entrySet()) {
			final String  fileType = fileTypesEntry.getKey();
			final Integer count    = fileTypesEntry.getValue();
			TreeSet<String> fileTypesForThisCount = counts.get(count);
			if (null == fileTypesForThisCount) {
				fileTypesForThisCount = new TreeSet<>();
			}
			fileTypesForThisCount.add(fileType);
			counts.put(count, fileTypesForThisCount);
		}
		return counts;
	}
}