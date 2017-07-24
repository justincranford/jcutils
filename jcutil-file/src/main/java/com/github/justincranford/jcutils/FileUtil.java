package com.github.justincranford.jcutils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FilenameUtils;
import org.codehaus.plexus.util.DirectoryScanner;

import com.github.justincranford.jcutils.DigestUtil.ALGORITHM;

@SuppressWarnings({"hiding","unchecked"})
public class FileUtil {
	private static final Logger LOG = Logger.getLogger(FileUtil.class.getName());

	public static final File[]     NULL_FILE_ARRAY  = null;
	public static final File[]     EMPTY_FILE_ARRAY = {};
	public static final List<File> EMPTY_FILE_LIST  = new ArrayList<>(0);

	public static final String[] MATCH_FILE_JUNK                     = {"**/.git/**","**/target/**","**/.DS_Store","**/_*_","**/_*_/**"};				// NOSONAR Make this member "protected".
	public static final String[] MATCH_FILE_ALL                      = {"**/*"};			// NOSONAR Make this member "protected".
	public static final String[] MATCH_FILE_POMXML                   = {"**/pom.xml"};		// NOSONAR Make this member "protected".
	public static final String[] MATCH_FILE_JAVA_ZIPS                = {"**/*.jar","**/*.war","**/*.ear","**/*.sar","**/*.par","**/*.rar","**/*.kar"};		// NOSONAR Make this member "protected".
	public static final String[] MATCH_FILE_JAR                      = {"**/*.jar"};		// NOSONAR Make this member "protected".
	public static final String[] MATCH_FILE_WAR                      = {"**/*.war"};		// NOSONAR Make this member "protected".
	public static final String[] MATCH_FILE_EAR                      = {"**/*.ear"};		// NOSONAR Make this member "protected".
	public static final String[] MATCH_FILE_SAR                      = {"**/*.sar"};		// NOSONAR Make this member "protected".
	public static final String[] MATCH_FILE_PAR                      = {"**/*.par"};		// NOSONAR Make this member "protected".
	public static final String[] MATCH_FILE_RAR                      = {"**/*.rar"};		// NOSONAR Make this member "protected".
	public static final String[] MATCH_FILE_KAR                      = {"**/*.kar"};		// NOSONAR Make this member "protected".
	public static final String[] MATCH_FILE_JAVA                     = {"**/*.java"};		// NOSONAR Make this member "protected".
	public static final String[] MATCH_FILE_DOTCLASSPATH             = {"**/.classpath"};	// NOSONAR Make this member "protected".
	public static final String[] MATCH_FILE_DOTFACTORYPATH           = {"**/.factorypath"};	// NOSONAR Make this member "protected".
	public static final String[] MATCH_FILE_DOTPROJECT               = {"**/.project"};		// NOSONAR Make this member "protected".
	public static final String[] MATCH_FILE_DOTGITIGNORE             = {"**/.gitignore"};	// NOSONAR Make this member "protected".
	public static final String[] MATCH_DIR_DOTSETTINGS               = {"**/.settings"};	// NOSONAR Make this member "protected".
	public static final String[] MATCH_DIR_DOTDSSTORE                = {"**/.DS_Store"};	// NOSONAR Make this member "protected".
	public static final String[] MATCH_DIR_EXTRACTED                 = {"**/_*_"};			// NOSONAR Make this member "protected".
	public static final String[] MATCH_PARENTDIR_EXTRACTED_CONTENTS  = {"**/_*_/**"};		// NOSONAR Make this member "protected".
	public static final String[] MATCH_PARENTDIR_DOTGIT              = {"**/.git/**"};		// NOSONAR Make this member "protected".
	public static final String[] MATCH_PARENTDIR_TARGET              = {"**/target/**"};	// NOSONAR Make this member "protected".
	public static final String[] MATCH_NULL                          = null;				// NOSONAR Make this member "protected".

	private FileUtil() {
		// prevent instantiation of constructor
	}

	public static String getCurrentCanonicalDirectory() throws IOException {
		return new File(".").getCanonicalPath();
	}

	public static void printCurrentDirectory() throws IOException {
		LOG.log(Level.INFO, "Current Directory=" + FileUtil.getCurrentCanonicalDirectory());
	}

	/**
	 * Recursively delete a directory, or delete a file.
	 */
	public static boolean delete(final String path) throws Exception {
		final File fileOrDir = new File(path);
		final boolean isDirectory = fileOrDir.isDirectory();
		if (isDirectory) {
			final File[] filesAndSubdirs = fileOrDir.listFiles();
			if (null == filesAndSubdirs) {
				LOG.log(Level.FINER, fileOrDir + " (null)");
				return false;
			} else if (0 != filesAndSubdirs.length) {
				LOG.log(Level.FINER, fileOrDir + " (" + filesAndSubdirs.length + " entries)");
				for (final File fileOrSubdir : filesAndSubdirs) {
					if (!delete(fileOrSubdir.getCanonicalPath())) {	// NOSONAR Refactor this code to not nest more than 3 if/for/while/switch/try statements.
						return false;
					}
				}
			} else {
				LOG.log(Level.FINER, fileOrDir + " (empty)");
			}
		}
		try {
			Files.delete(fileOrDir.toPath());
			LOG.log(Level.FINER, fileOrDir + " was deleted");
			return true;
		} catch(DirectoryNotEmptyException e) {	// NOSONAR Either log or rethrow this exception.
			// Could be race condition between Java DirectoryNotEmptyException and ImDisk RamDrive null/empty directory list, so retry.
			final File[] entries = fileOrDir.listFiles();
			LOG.log(Level.WARNING, fileOrDir + " was not deleted. Exception: " + e.getMessage());
			if ((null == entries) || (0 == entries.length)) {
				if (fileOrDir.delete()) {	// File.delete() does not throw exception, unlike Files.delete(Path)
					LOG.log(Level.WARNING, fileOrDir + " delete failed because File.listFiles() was " + ((null == entries)?"null":"empty") + ". Retry succeeded.");
					return true;
				}
				LOG.log(Level.WARNING, fileOrDir + " delete failed because File.listFiles() was " + ((null == entries)?"null":"empty") + ". Retry failed.");
			} else {
				LOG.log(Level.WARNING, "File.listFiles["+entries.length+"]:\n'"+StringUtil.join(StringUtil.toString(entries), "'\n")+"'\n");	// not empty, something list of 1 empty entry is returned
			}
		} catch(NoSuchFileException e) {	// NOSONAR Either log or rethrow this exception.
			LOG.log(Level.WARNING, fileOrDir + " was not deleted because it does not exist. Exception: " + e.getMessage());
		} catch(FileSystemException e) {	// NOSONAR Either log or rethrow this exception.
			LOG.log(Level.WARNING, fileOrDir + " was not deleted because of a file system exception.", e);
		} catch(Exception e) {	// NOSONAR Either log or rethrow this exception.
			LOG.log(Level.WARNING, fileOrDir + " was not deleted. Exception: " + e.getMessage());
		}
		return false;
	}

	public static String[] parseDirNameAndFileName(final String filePath) {
		return new String[]{FilenameUtils.getFullPathNoEndSeparator(filePath), FilenameUtils.getName(filePath)};
	}

	public static String removeFileNameExtension(final String fileName) {
		return FilenameUtils.removeExtension(fileName);
	}

	public static final File computeExtractDirectory(final File tempDir, final File zipFile) throws IOException {
		final String zipFileParentDirCanonicalPath = zipFile.getParentFile().getCanonicalPath();
		if (zipFile.getCanonicalPath().startsWith(tempDir.getCanonicalPath())) {
			// Zip file is in the extract directory, no need to modify the root directory/drive in the path
			return new File(zipFileParentDirCanonicalPath, "_" + zipFile.getName() + "_");
		}
		// Zip file is not in the extract directory, modify the root directory/drive to point under extract directory
		final String zipFileCanonicalPathAbsolutePathPrefix = FilenameUtils.getPrefix(zipFileParentDirCanonicalPath);
		final File extractDirectory1 = new File(tempDir, zipFileParentDirCanonicalPath.substring(zipFileCanonicalPathAbsolutePathPrefix.length()));
		return new File(extractDirectory1, "_" + zipFile.getName() + "_");
	}

	public static boolean makeCanonicalDirectories(final String canonicalDirectory) throws Exception {
		return FileUtil.makeCanonicalDirectories(new File(canonicalDirectory).getCanonicalFile());
	}

	/*
	 * Similar logic as File.mkdirs() except for a couple of performance improvements:
	 * - Skip expensive JNI calls to File.getCanonicalFile() by assuming the parameter already wraps a canonical path string.
	 * - Call isDirectory() instead of exists(), and return true. This is more efficient that File.mkdirs() which returns false, forcing a File.isDirectory() call.
	 * - Use optimistic NPE catch for more efficient performance instead of explicit null parent checking.
	 */
	public static boolean makeCanonicalDirectories(final File canonicalDirectoryObj) {
		if (canonicalDirectoryObj.isDirectory()) {
//			System.out.println("canonicalDirectoryObj=" + canonicalDirectoryObj + " is an existing directory");
			return true;
		}
		if (canonicalDirectoryObj.mkdir()) {
//			System.out.println("canonicalDirectoryObj=" + canonicalDirectoryObj + " was successfully created");
			return true;
		}
//		System.out.println("canonicalDirectoryObj=" + canonicalDirectoryObj + " was not created on first attempt");
		try {
			final File canonicalParentDirectoryObj = canonicalDirectoryObj.getParentFile();	// attempt recursion on parent
			boolean x = FileUtil.makeCanonicalDirectories(canonicalParentDirectoryObj);
			if (!x) {
				boolean y = canonicalParentDirectoryObj.exists();
				x = y;
//				System.out.println("canonicalDirectoryObj=" + canonicalDirectoryObj + " was exists on retry");
			}
			if (x) {
				boolean z = canonicalDirectoryObj.mkdir();
				x = z;
//				System.out.println("canonicalDirectoryObj=" + canonicalDirectoryObj + " was successfully created on retry");
			}
			return x;
//			return (FileUtil.makeCanonicalDirectories(canonicalParentDirectoryObj) || (canonicalParentDirectoryObj.exists()) && (canonicalDirectoryObj.mkdir()));
		} catch(NullPointerException e) {	// NOSONAR Either log or rethrow this exception.
//			System.out.println("canonicalDirectoryObj=" + canonicalDirectoryObj + " is null");
			return false;	// no ancestors left, we have arrived at the root directory
		}
	}

	public static boolean makeCanonicalDirectories2(final File canonicalDirectoryObj) {
		if (canonicalDirectoryObj.mkdir()) {	// initial attempt to create a single directory
			return true;
		}
		final File canonicalParentDirectoryObj = canonicalDirectoryObj.getParentFile();	// attempt recursion on parent
		if (null == canonicalParentDirectoryObj) {	// no ancestors left, we have arrived at the root directory
			return canonicalDirectoryObj.exists();	// return if the root directory exists
		}
		if (makeCanonicalDirectories(canonicalParentDirectoryObj)) {
			return canonicalDirectoryObj.mkdir();	// retry attempt to create a single directory after recursion succeeded
		}
		return false;	// recursion failed, return false without retrying to create a single directory
	}

	public static byte[] readBinaryFile(final String filePath) throws IOException {
		return FileUtil.readBinaryFile(new File(filePath));
	}

	public static byte[] readBinaryFile(final File file) throws IOException {
		if (file.exists()) {
			try (final InputStream is = new FileInputStream(file)) {
				return FileUtil.readBinaryStream(is);
			}
		}
		return null;	// NOSONAR Return an empty array instead of null.
	}

	public static byte[] readBinaryStream(final InputStream is) throws IOException {
		return FileUtil.readBinaryStream(new BufferedInputStream(is));
	}

	public static byte[] readBinaryStream(final BufferedInputStream bis) throws IOException {
		return FileUtil.readBinaryStream(bis, 8192);
	}

	public static byte[] readBinaryStream(final BufferedInputStream bis, final int bufferSize) throws IOException {
		try (final ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			final byte[] buffer = new byte[bufferSize];
			int bytesRead;
			while (-1 != (bytesRead=bis.read(buffer))) {
				baos.write(buffer, 0, bytesRead);
			}
			return baos.toByteArray();
		}
	}

	public static int writeBinaryFile(final String filePath, final byte[] bytes) throws IOException {
		return FileUtil.streamToFile(filePath, new ByteArrayInputStream(bytes));
	}

	public static int writeBinaryFile(final File file, final byte[] bytes) throws IOException {
		return FileUtil.streamToFile(file, new ByteArrayInputStream(bytes));
	}

	public static int streamToFile(final String filePath, final InputStream is) throws IOException {
		return FileUtil.streamToFile(new File(filePath), is);
	}

	public static int streamToFile(final File file, final InputStream is) throws IOException {
		try (final OutputStream os = new FileOutputStream(file)) {
			return FileUtil.streamToStream(os, is);
		}
	}

	public static int streamToStream(final OutputStream os, final InputStream is) throws IOException {
		return FileUtil.streamToStream(new BufferedOutputStream(os), new BufferedInputStream(is));
	}

	public static int streamToStream(final BufferedOutputStream bos, final InputStream is) throws IOException {
		return FileUtil.streamToStream(bos, new BufferedInputStream(is));
	}

	public static int copyBinaryStream(final OutputStream os, final BufferedInputStream bis) throws IOException {
		return FileUtil.streamToStream(new BufferedOutputStream(os), bis);
	}

	public static int streamToStream(final BufferedOutputStream bos, final BufferedInputStream bis) throws IOException {
		return FileUtil.streamToStream(bos, bis, 8192);
	}

	public static int streamToStream(final BufferedOutputStream bos, final BufferedInputStream bis, final int bufferSize) throws IOException {
		int totalBytesWritten = 0;
		final byte[] buffer = new byte[bufferSize];
		int bytesRead;
		while (-1 != (bytesRead=bis.read(buffer))) {
			bos.write(buffer, 0, bytesRead);
			totalBytesWritten += bytesRead;
		}
		bos.flush();
		return totalBytesWritten;
	}

	// Read text files

	public static String readFirstLineOfTextFile(final String filePath, final String charset) throws IOException {
		return FileUtil.readFirstLineOfTextFile(filePath, charset, 8192);
	}

	/*package*/ static String readFirstLineOfTextFile(final String filePath, final String charset, final int bufferSize) throws IOException {
		if (new File(filePath).exists()) {
			try (final BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(filePath), charset), bufferSize)) {	// ASSUMPTION: 8K default buffer
				return br.readLine();
			}
		}
		return null;
	}

	// File checksums (ex: MD5, SHA1, SHA256)

	public static String[] readChecksums(File file, final ALGORITHM[] algorithms) throws FileNotFoundException, IOException {	// NOSONAR Refactor this method to throw at most one checked exception
		final String[] checksums = new String[algorithms.length];
		final String   filePath  = file.getCanonicalPath();
		for (int i=0; i<algorithms.length; i++) {
			final ALGORITHM algorithm         = algorithms[i];
			final String    fileNameExtension = algorithm.getFileNameExtension();
			ValidationUtil.assertNonNullObject(fileNameExtension, algorithm.getAlgorithmName() + " checksum file not supported.");
			final String fileContents = FileUtil.readFirstLineOfTextFile(filePath + "." + fileNameExtension,  "UTF-8");
			final int    numberOfbytes = algorithm.getNumberOfBytes();
			if (null != fileContents) {
				ValidationUtil.assertGreaterThanOrEqual(fileContents.length(), numberOfbytes, "Checksum file '" + filePath + "." + fileNameExtension + "' is truncated.");
			}
			checksums[i] = StringUtil.safeSubstring(fileContents,0,numberOfbytes);
		}
		return checksums;
	}

	public static Future<String[]> readChecksumsAsync(final ExecutorService executorService, final File file, final ALGORITHM[] algorithms) throws Exception {	// NOSONAR Define and throw a dedicated exception instead of using a generic one.
		return executorService.submit(new ReadChecksumsTask(file, algorithms));
	}

	public static String[][] readChecksums(final ExecutorService executorService, final File[] files, final ALGORITHM[] algorithms) throws Exception {	// NOSONAR Define and throw a dedicated exception instead of using a generic one.
		final List<Callable<String[]>> taskRequests = new ArrayList<>(files.length);
		for (final File file : files) {
			taskRequests.add(new ReadChecksumsTask(file, algorithms));
		}
		final List<String[]> invokeAll = ThreadPool.invokeAll(executorService, taskRequests);
		return invokeAll.toArray(new String[taskRequests.size()][algorithms.length]);
	}

	private static class ReadChecksumsTask implements Callable<String[]> {
		private final File file;
		private final ALGORITHM[] algorithms;
		public ReadChecksumsTask(final File file, final ALGORITHM[] algorithms) {
			this.file = file;
			this.algorithms = algorithms;
		}
		@Override
		public String[] call() throws NoSuchAlgorithmException, IOException  {
			return FileUtil.readChecksums(this.file, this.algorithms);
		}
	}

	// File lengths

	public static List<Future<Long>> getFileLengthsAsync(final ExecutorService executorService, final File[] files) throws Exception {	// NOSONAR Define and throw a dedicated exception instead of using a generic one.
		final List<Future<Long>> taskRequests = new ArrayList<>(files.length);
		for (final File file : files) {
			taskRequests.add(getFileLengthAsync(executorService, file));
		}
		return taskRequests;
	}

	public static Future<Long> getFileLengthAsync(final ExecutorService executorService, final File file) throws Exception {	// NOSONAR Define and throw a dedicated exception instead of using a generic one.
		return executorService.submit(new GetFileLengthTask(file));
	}

	public static Long[] getFileLengths(final ExecutorService executorService, final File[] files) throws Exception {	// NOSONAR Define and throw a dedicated exception instead of using a generic one.
		final List<Callable<Long>> taskRequests = new ArrayList<>(files.length);
		for (final File file : files) {
			taskRequests.add(new GetFileLengthTask(file));
		}
		final List<Long[]> invokeAll = ThreadPool.invokeAll(executorService, taskRequests);
		return invokeAll.toArray(new Long[taskRequests.size()]);
	}

	private static class GetFileLengthTask implements Callable<Long> {
		private File file;
		public GetFileLengthTask(final File file) {
			this.file = file;
		}
		@Override
		public Long call() throws NoSuchAlgorithmException, IOException  {
			return Long.valueOf(this.file.length());
		}
	}

	// File exists

	public static List<Future<Boolean>> getFileExistsAsync(final ExecutorService executorService, final File[] files) throws Exception {	// NOSONAR Define and throw a dedicated exception instead of using a generic one.
		final List<Future<Boolean>> taskRequests = new ArrayList<>(files.length);
		for (final File file : files) {
			taskRequests.add(getFileExistAsync(executorService, file));
		}
		return taskRequests;
	}

	public static Future<Boolean> getFileExistAsync(final ExecutorService executorService, final File file) throws Exception {	// NOSONAR Define and throw a dedicated exception instead of using a generic one.
		return executorService.submit(new GetFileExistsTask(file));
	}

	public static Boolean[] getFileExists(final ExecutorService executorService, final File[] files) throws Exception {	// NOSONAR Define and throw a dedicated exception instead of using a generic one.
		final List<Callable<Boolean>> taskRequests = new ArrayList<>(files.length);
		for (final File file : files) {
			taskRequests.add(new GetFileExistsTask(file));
		}
		final List<Boolean[]> invokeAll = ThreadPool.invokeAll(executorService, taskRequests);
		return invokeAll.toArray(new Boolean[taskRequests.size()]);
	}

	private static class GetFileExistsTask implements Callable<Boolean> {
		private File file;
		public GetFileExistsTask(final File file) {
			this.file = file;
		}
		@Override
		public Boolean call() throws NoSuchAlgorithmException, IOException  {
			return Boolean.valueOf(this.file.exists());
		}
	}

	// File search

	public static Future<File[]> searchAsync(final ExecutorService executorService, final String requestedDirectory, final boolean isCaseSensitive, final boolean isFollowSymlinks, final String[] includes, final String[] excludes, final Set<String> duplicateFiles, final boolean printMatches) throws Exception {	// NOSONAR Define and throw a dedicated exception instead of using a generic one.
		return executorService.submit(new SearchTask(requestedDirectory, isCaseSensitive, isFollowSymlinks, includes, excludes, duplicateFiles, printMatches));
	}

	private static class SearchTask implements Callable<File[]> {
		private String requestedDirectory;
		private boolean isCaseSensitive;
		private boolean isFollowSymlinks;
		private String[] includes;
		private String[] excludes;
		private Set<String> duplicateFiles;
		private boolean printMatches;
		public SearchTask(final String requestedDirectory, final boolean isCaseSensitive, final boolean isFollowSymlinks, final String[] includes, final String[] excludes, final Set<String> duplicateFiles, final boolean printMatches) {
			this.requestedDirectory = requestedDirectory;
			this.isCaseSensitive = isCaseSensitive;
			this.isFollowSymlinks = isFollowSymlinks;
			this.includes = includes;
			this.excludes = excludes;
			this.duplicateFiles = duplicateFiles;
			this.printMatches = printMatches;
		}
		@Override
		public File[] call() throws Exception  {
			return FileUtil.search(this.requestedDirectory, this.isCaseSensitive, this.isFollowSymlinks, this.includes, this.excludes, this.duplicateFiles, this.printMatches);
		}
	}

	public static File[] search(final String requestedDirectory, final boolean isCaseSensitive, final boolean isFollowSymlinks, final String[] includes, final String[] excludes, final Set<String> duplicateFiles, final boolean printMatches) throws Exception {	// NOSONAR Define and throw a dedicated exception instead of using a generic one.
		final File requestedDirectoryObj = new File(requestedDirectory);
		final String canonicalPath = requestedDirectoryObj.getCanonicalPath();
		if (!requestedDirectoryObj.exists()) {
			LOG.log(Level.SEVERE, "Directory '" + canonicalPath + "' does not exist.");
			return EMPTY_FILE_ARRAY;
		} else if (!requestedDirectoryObj.isDirectory()) {
			LOG.log(Level.SEVERE, "Path '" + canonicalPath + "' is not a directory.");
			return EMPTY_FILE_ARRAY;
		} else if (!requestedDirectoryObj.canRead()) {
			LOG.log(Level.SEVERE, "Directory '" + canonicalPath + "' cannot be read.");
			return EMPTY_FILE_ARRAY;
		} else if (LOG.isLoggable(Level.FINE)) {
			LOG.log(Level.FINE, "Scanning directory '" + canonicalPath + "'.\n");	// NOSONAR Define a constant instead of duplicating this literal "Scanning directory '" 3 times.
		}
		try {
			final File canonicalDirectory = new File(canonicalPath);	// new File object with canonical path, otherwise printed paths include absolute and relative path

			final DirectoryScanner directoryScanner = new DirectoryScanner();
			directoryScanner.setBasedir(canonicalDirectory);
			directoryScanner.setCaseSensitive(isCaseSensitive);
			directoryScanner.setFollowSymlinks(isFollowSymlinks);
			directoryScanner.setIncludes(includes);
			directoryScanner.setExcludes(excludes);
			directoryScanner.scan();

			final String[] includedFiles = directoryScanner.getIncludedFiles();
			if ((null == includedFiles) || (0 == includedFiles.length)) {
				LOG.log(Level.INFO, "Scanned directory '" + canonicalPath + "' for [" + StringUtil.join(includes,",") + "]. No files found.\n");	// NOSONAR Define a constant instead of duplicating this literal "Scanned directory '" 3 times.
				return EMPTY_FILE_ARRAY;
			}
			final ArrayList<File> files = new ArrayList<>(includedFiles.length);
			final StringBuilder sb = new StringBuilder(1024);
			for (final String fileName : includedFiles) {
				final File file = new File(canonicalDirectory, fileName);
				final String fileCanonicalPath = file.getCanonicalPath();	// absolute path
				if (duplicateFiles.add(fileCanonicalPath)) {
					sb.append(file.getAbsolutePath()).append('\n');
					files.add(file);
				} else if (LOG.isLoggable(Level.FINE)) {
					LOG.log(Level.FINE, "Ignoring duplicate file '" + fileCanonicalPath + "'.");
				}
			}
			if (printMatches) {
				LOG.log(Level.INFO, "Scanned directory '" + canonicalPath + "' for [" + StringUtil.join(includes,",") + "]. Found " + includedFiles.length + " file(s).\n" + sb.toString());
			} else {
				LOG.log(Level.INFO, "Scanned directory '" + canonicalPath + "' for [" + StringUtil.join(includes,",") + "]. Found " + includedFiles.length + " file(s).\n");
			}
			return files.toArray(new File[files.size()]);
		} catch(Exception ex) {
			LOG.log(Level.SEVERE, "Failed to scan directory '" + canonicalPath + "'.", ex);
			throw ex;
		}
	}

	public static void close(final Closeable closeable) {
        if (null != closeable) {
            try {
                closeable.close();
            } catch (IOException e) {
    			LOG.log(Level.WARNING, "Error for file " + closeable.toString(), e);
            }
        }
    }

	public static final String[] getCanonicalPaths(final File[] files) throws IOException {
		if (null == files) {
			return StringUtil.NULL_STRING_ARRAY;
		}

		final int fileArrayLength = files.length;
		if (0 == fileArrayLength) {
			return StringUtil.EMPTY_STRING_ARRAY;
		}
		final String[] fileNames = new String[fileArrayLength];
		for (int i=0; i<fileArrayLength; i++) {
			fileNames[i] = files[i].getCanonicalPath();
		}
		return fileNames;
	}

	public static final String[] getFileNames(final File[] files) {
		if (null == files) {
			return StringUtil.NULL_STRING_ARRAY;
		}
		final int fileArrayLength = files.length;
		if (0 == fileArrayLength) {
			return StringUtil.EMPTY_STRING_ARRAY;
		}
		final String[] fileNames = new String[fileArrayLength];
		for (int i=0; i<fileArrayLength; i++) {
			fileNames[i] = files[i].getName();
		}
		return fileNames;
	}

	public static final List<String> getCanonicalPaths(final List<File> files) throws IOException {
		if (null == files) {
			return StringUtil.NULL_STRING_LIST;
		} else if (files.isEmpty()) {
			return StringUtil.EMPTY_STRING_LIST;
		}
		final List<String> fileNames = new ArrayList<>(files.size());
		for (final File file : files) {
			if (null == file) {
				fileNames.add(null);
			} else {
				fileNames.add(file.getCanonicalPath());
			}
		}
		return fileNames;
	}

	public static final List<String> getFileNames(final List<File> files) {
		if (null == files) {
			return StringUtil.NULL_STRING_LIST;
		} else if (files.isEmpty()) {
			return StringUtil.EMPTY_STRING_LIST;
		}
		final List<String> fileNames = new ArrayList<>(files.size());
		for (final File file : files) {
			if (null == file) {
				fileNames.add(null);
			} else {
				fileNames.add(file.getName());
			}
		}
		return fileNames;
	}

	public static String computeUncompressedFileName(final File canonicalFileObj) {
		final String fileNameWithExtensionRemoved = FileUtil.removeFileNameExtension(canonicalFileObj.getName());
		if (-1 == fileNameWithExtensionRemoved.indexOf('.')) {
			return fileNameWithExtensionRemoved + ".unc"; // a.tgz=>a.unc, a_tar.gz=>a_tar.unc
		}
		return fileNameWithExtensionRemoved; // a.tar.gz=>a.tar
	}

	public static String computeCpioFileName(final File canonicalFileObj) {
		return FileUtil.removeFileNameExtension(canonicalFileObj.getName()) + ".cpio"; // a.rpm=>a.cpio, a_rpm=>a_rpm.cpio
	}

	public static void copy(final InputStream input, final OutputStream output, final byte[] buffer) throws IOException {
		int n = 0;
		while (-1 != (n = input.read(buffer))) {
			output.write(buffer, 0, n);
		}
	}

	public static void copy(final InputStream is, final FileChannel outChannel, final byte[] buffer) throws IOException {
		final ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);
		long offset = 0;
		int n = 0;
		while (-1 != (n = is.read(buffer))) {
			if (n < buffer.length) {
				byteBuffer.limit(n);
			}
			outChannel.write(byteBuffer, offset);
			while (byteBuffer.hasRemaining()) {
				outChannel.write(byteBuffer, offset);
			}
			offset += n;
			byteBuffer.flip();
		}
	}
}