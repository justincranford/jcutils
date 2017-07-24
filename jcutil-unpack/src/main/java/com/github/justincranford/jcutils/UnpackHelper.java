package com.github.justincranford.jcutils;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.zip.UnsupportedZipFeatureException;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.codehaus.plexus.util.SelectorUtils;
import org.redline_rpm.ReadableChannelWrapper;
import org.redline_rpm.Util;
import org.redline_rpm.header.Format;
import org.redline_rpm.header.Header;

import com.github.junrar.Archive;
import com.github.junrar.exception.RarException;
import com.github.junrar.rarfile.FileHeader;

/**
 * Uncompress gzip, bzip2, xz, lzma, Pack200, DEFLATE, Z, arj, dump and Z
 */
public class UnpackHelper {
	private static final Logger LOG = Logger.getLogger(UnpackHelper.class.getName());

	private static final int					FILE_READ_BUFFER_SIZE			= 1024 * 1024;
	private static final int					MAX_FILE_SIZE_IN_MEMORY_BUFFER	= FILE_READ_BUFFER_SIZE * 4;
	private static final int					FILE_WRITE_BUFFER_SIZE			= MAX_FILE_SIZE_IN_MEMORY_BUFFER;
	private static final ThreadLocal<byte[]>	REUSABLE_READ_BUFFER			= new ThreadLocal<byte[]>() {
		@Override
		protected byte[] initialValue() {
			return new byte[FILE_READ_BUFFER_SIZE];
		}
	};
	private static final ThreadLocal<byte[]>	REUSABLE_WRITE_BUFFER			= new ThreadLocal<byte[]>() {
		@Override
		protected byte[] initialValue() {
			return new byte[FILE_WRITE_BUFFER_SIZE];
		}
	};

	// Workaround Junrar limitations for parsing non-rar files. Error logging is too verbose and performance is poor.
	private static final String	PACKAGE_NAME_COM_GITHUB_JUNRAR	= "com.github.junrar";
	private static final int	MAX_LENGTH_FILE_SIGNATURE		= 12;
	private static final byte[]	RAR_FILE_SIGNATURE				= { 0x52, 0x61, 0x72, 0x21, 0x1A, 0x07, 0x00 };			// RAR 1.50+
	@SuppressWarnings("unused")
	private static final byte[]	RAR5_FILE_SIGNATURE				= { 0x52, 0x61, 0x72, 0x21, 0x1A, 0x07, 0x01, 0x00 };	// RAR 5.0+

	public static final String	MATCH_REGEX_PREFIX	= SelectorUtils.REGEX_HANDLER_PREFIX;
	public static final String	MATCH_REGEX_POSTFIX	= SelectorUtils.PATTERN_HANDLER_SUFFIX;

	public static final String[]	MATCH_ALL	= { ".*$" };	// NOSONAR Make this member "protected".
	public static final String[]	MATCH_NULL	= null;			// NOSONAR Make this member "protected".

	public static final String[]	MATCH_ENTRY_ZIPS	= { ".*\\.tar$", ".*\\.zip", ".*\\.jar", ".*\\.war", ".*\\.ear", ".*\\.sar", ".*\\.par", ".*\\.rar", ".*\\.kar" };	// NOSONAR Make this member "protected".
	public static final String[]	MATCH_ENTRY_TAR		= { ".*\\.tar$", };																									// NOSONAR Make this member "protected".
	public static final String[]	MATCH_ENTRY_TARGZ	= { ".*\\.tar.gz$", ".*\\.tgz$" };																					// NOSONAR Make this member "protected".
	public static final String[]	MATCH_ENTRY_TARBZ2	= { ".*\\.bz2$" };																									// NOSONAR Make this member "protected".
	public static final String[]	MATCH_ENTRY_7Z		= { ".*\\.7z$" };																									// NOSONAR Make this member "protected".
	public static final String[]	MATCH_ENTRY_Z		= { ".*\\.Z$" };																									// NOSONAR Make this member "protected".

	public static final String[]	ALL_EXTENSIONS = StringUtil.concatenate(false, false, MATCH_ENTRY_ZIPS, MATCH_ENTRY_TAR, MATCH_ENTRY_TARGZ, MATCH_ENTRY_TARBZ2, MATCH_ENTRY_7Z, MATCH_ENTRY_Z); // NOSONAR

	public static final String[]	MATCH_ENTRY_META_INF	= { ".*META-INF.*$" };			// NOSONAR Make this member "protected".
	public static final String[]	MATCH_ENTRY_CLASS		= { ".*\\.class$" };			// NOSONAR Make this member "protected".
	public static final String[]	MATCH_ENTRY_TXT			= { ".*\\.txt$" };				// NOSONAR Make this member "protected".
	public static final String[]	MATCH_ENTRY_HTML		= { ".*\\.html?$" };			// NOSONAR Make this member "protected".
	public static final String[]	MATCH_ENTRY_JSON		= { ".*\\.json$" };				// NOSONAR Make this member "protected".
	public static final String[]	MATCH_ENTRY_JS			= { ".*\\.js$" };				// NOSONAR Make this member "protected".
	public static final String[]	MATCH_ENTRY_CSS			= { ".*\\.css$" };				// NOSONAR Make this member "protected".
	public static final String[]	MATCH_ENTRY_ICO			= { ".*\\.ico$" };				// NOSONAR Make this member "protected".
	public static final String[]	MATCH_ENTRY_TTF			= { ".*\\.ttf$" };				// NOSONAR Make this member "protected".
	public static final String[]	MATCH_ENTRY_PROPERTIES	= { ".*\\.properties$" };		// NOSONAR Make this member "protected".
	public static final String[]	MATCH_ENTRY_XML			= { ".*\\.xml$" };				// NOSONAR Make this member "protected".
	public static final String[]	MATCH_ENTRY_YML			= { ".*\\.yml$" };				// NOSONAR Make this member "protected".
	public static final String[]	MATCH_ENTRY_PY			= { ".*\\.py$" };				// NOSONAR Make this member "protected".
	public static final String[]	MATCH_ENTRY_XSD			= { ".*\\.xsd$" };				// NOSONAR Make this member "protected".
	public static final String[]	MATCH_ENTRY_XSL			= { ".*\\.xsl$" };				// NOSONAR Make this member "protected".
	public static final String[]	MATCH_ENTRY_SVG			= { ".*\\.svg$" };				// NOSONAR Make this member "protected".
	public static final String[]	MATCH_ENTRY_DOTGIT		= { ".*\\.git.*$" };			// NOSONAR Make this member "protected".
	public static final String[]	MATCH_ENTRY_DTD			= { ".*\\.dtd$" };				// NOSONAR Make this member "protected".
	public static final String[]	MATCH_ENTRY_GIF			= { ".*\\.gif$" };				// NOSONAR Make this member "protected".
	public static final String[]	MATCH_ENTRY_JPG			= { ".*\\.jpe?g$" };			// NOSONAR Make this member "protected".
	public static final String[]	MATCH_ENTRY_PNG			= { ".*\\.png$" };				// NOSONAR Make this member "protected".
	public static final String[]	MATCH_ENTRY_TZ_DATA		= { ".*tz.*data.*$" };			// NOSONAR Make this member "protected".
	public static final String[]	MATCH_ENTRY_SQL			= { ".*\\.sql$" };				// NOSONAR Make this member "protected".
	public static final String[]	MATCH_ENTRY_DDL			= { ".*\\.ddl$" };				// NOSONAR Make this member "protected".
	public static final String[]	MATCH_ENTRY_GROOVY		= { ".*\\.groovy$" };			// NOSONAR Make this member "protected".
	public static final String[]	MATCH_ENTRY_VM			= { ".*\\.vm$" };				// NOSONAR Make this member "protected".
	public static final String[]	MATCH_ENTRY_FTL			= { ".*\\.ftl$" };				// NOSONAR Make this member "protected".
	public static final String[]	MATCH_ENTRY_HANDLERS	= { ".*\\.handler2?s" };		// NOSONAR Make this member "protected".
	public static final String[]	MATCH_ENTRY_RNC			= { ".*\\.rnc$" };				// NOSONAR Make this member "protected".
	public static final String[]	MATCH_ENTRY_G			= { ".*\\.g$" };				// NOSONAR Make this member "protected".
	public static final String[]	MATCH_ENTRY_TYPES		= { ".*\\.types$" };			// NOSONAR Make this member "protected".
	public static final String[]	MATCH_ENTRY_EOT			= { ".*\\.eot$" };				// NOSONAR Make this member "protected".
	public static final String[]	MATCH_ENTRY_WOFF		= { ".*\\.woff$" };				// NOSONAR Make this member "protected".
	public static final String[]	MATCH_ENTRY_CERTSPEC	= { ".*\\.certspec$" };			// NOSONAR Make this member "protected".
	public static final String[]	MATCH_ENTRY_TCL			= { ".*\\.tcl$" };				// NOSONAR Make this member "protected".
	public static final String[]	MATCH_ENTRY_SH			= { ".*\\.sh$" };				// NOSONAR Make this member "protected".
	public static final String[]	MATCH_ENTRY_LICENSE		= { ".*LICENSE.*$" };			// NOSONAR Make this member "protected".
	public static final String[]	MATCH_ENTRY_NOTICE		= { ".*NOTICE.*$" };			// NOSONAR Make this member "protected".
	public static final String[]	MATCH_ENTRY_README		= { ".*README(\\.1st)?.*$" };	// NOSONAR Make this member "protected".
	public static final String[]	MATCH_ENTRY_NON_ARCHIVES = StringUtil.concatenate(false, false, MATCH_ENTRY_META_INF, MATCH_ENTRY_CLASS, MATCH_ENTRY_TXT, MATCH_ENTRY_HTML, MATCH_ENTRY_JS, MATCH_ENTRY_CSS, MATCH_ENTRY_ICO, MATCH_ENTRY_TTF, MATCH_ENTRY_JSON, MATCH_ENTRY_PROPERTIES, MATCH_ENTRY_XML, MATCH_ENTRY_YML, MATCH_ENTRY_PY, MATCH_ENTRY_XSD, MATCH_ENTRY_XSL, MATCH_ENTRY_SVG, MATCH_ENTRY_DOTGIT, MATCH_ENTRY_DTD, MATCH_ENTRY_GIF, MATCH_ENTRY_JPG, MATCH_ENTRY_PNG, MATCH_ENTRY_TZ_DATA, MATCH_ENTRY_SQL, MATCH_ENTRY_DDL, MATCH_ENTRY_GROOVY, MATCH_ENTRY_VM, MATCH_ENTRY_FTL, MATCH_ENTRY_FTL, MATCH_ENTRY_HANDLERS, MATCH_ENTRY_RNC, MATCH_ENTRY_G, MATCH_ENTRY_TYPES, MATCH_ENTRY_EOT, MATCH_ENTRY_WOFF, MATCH_ENTRY_CERTSPEC, MATCH_ENTRY_TCL, MATCH_ENTRY_SH, MATCH_ENTRY_LICENSE, MATCH_ENTRY_NOTICE, MATCH_ENTRY_README); // NOSONAR

	private UnpackHelper() {
		// prevent instantiation of constructor
	}

	public static void unpackDirectoriesAndFiles(final File tempDir, final String[] nonCanonicalFilePaths, final String[] includes, final String[] excludes, final List<File> skippedFiles, final List<File> invalidFiles, final List<File> validFiles, final List<File> invalidArchives, final List<File> validArchives, final Set<String> duplicateFiles, final int level) throws Exception { // NOSONAR
		try (final JulUtil x = new JulUtil(PACKAGE_NAME_COM_GITHUB_JUNRAR, Level.OFF)) {
			for (final String nonCanonicalFilePath : nonCanonicalFilePaths) {
				UnpackHelper.unpackDirectoryOrFile(tempDir, new File(nonCanonicalFilePath).getCanonicalFile(), includes, excludes, skippedFiles, invalidFiles, validFiles, invalidArchives, validArchives, duplicateFiles, level);
			}
			REUSABLE_READ_BUFFER.remove();
			REUSABLE_WRITE_BUFFER.remove();
		}
	}

	private static void unpackDirectoryOrFile(final File tempDir, final File canonicalFileObj, final String[] includes, final String[] excludes, final List<File> skippedFiles, final List<File> invalidFiles, final List<File> validFiles, final List<File> invalidArchives, final List<File> validArchives, final Set<String> duplicateFiles, final int level) throws Exception { // NOSONAR
		final String filePath = canonicalFileObj.getPath();
		LOG.log(Level.FINER, "Level=" + level + ", unpacking '" + filePath + "'..."); // NOSONAR Define a constant instead of duplicating this literal
		if (canonicalFileObj.isDirectory()) {
			LOG.log(Level.FINE, "Level=" + level + ", processing directory '" + filePath + "'."); // NOSONAR Define a constant instead of duplicating this literal
			final String[] wrappedIncludes = StringUtil.wrap(includes, MATCH_REGEX_PREFIX, MATCH_REGEX_POSTFIX, true, true);
			final String[] wrappedExcludes = StringUtil.wrap(excludes, MATCH_REGEX_PREFIX, MATCH_REGEX_POSTFIX, false, false);
			final File[] foundFiles = FileUtil.search(filePath, false, false, wrappedIncludes, wrappedExcludes, duplicateFiles, false); // isCaseSensitive=false, isFollowSymlinks=false, printResults=false
			for (final File foundFileObj : foundFiles) {
				UnpackHelper.unpackDirectoryOrFile(tempDir, foundFileObj, includes, excludes, skippedFiles, invalidFiles, validFiles, invalidArchives, validArchives, duplicateFiles, level);
			}
		} else if (!(canonicalFileObj.isFile())) {
			LOG.log(Level.SEVERE, "Level=" + level + ", file '" + filePath + "' is not a file."); // NOSONAR Define a constant instead of duplicating this literal
			invalidFiles.add(canonicalFileObj);
		} else if (!(StringUtil.isMatch(filePath, includes, excludes))) {
			LOG.log(Level.SEVERE, "Level=" + level + ", skipping file '" + filePath + "'."); // NOSONAR Define a constant instead of duplicating this literal
			skippedFiles.add(canonicalFileObj);
		} else {
			LOG.log(Level.FINE, "Level=" + level + ", processing file '" + filePath + "'."); // NOSONAR Define a constant instead of duplicating this literal
			UnpackHelper.unpackFileFromFileSystem(tempDir, canonicalFileObj, includes, excludes, skippedFiles, invalidFiles, validFiles, invalidArchives, validArchives, duplicateFiles, level);
		}
	}

	private static void unpackFileFromFileSystem(final File tempDir, final File canonicalFileObj, final String[] includes, final String[] excludes, final List<File> skippedFiles, final List<File> invalidFiles, final List<File> validFiles, final List<File> invalidArchives, final List<File> validArchives, final Set<String> duplicateFiles, final int level) { // NOSONAR
		try {
			try (final BufferedInputStream bis = new BufferedInputStream(new FileInputStream(canonicalFileObj))) { // open the file and unpack as archive
				validFiles.add(canonicalFileObj);
				if (UnpackHelper.unpackInputStreamAsArchive(bis, tempDir, canonicalFileObj, includes, excludes, skippedFiles, invalidFiles, validFiles, invalidArchives, validArchives, duplicateFiles, level)) {
					validArchives.add(canonicalFileObj); // valid archive of many files
					return;
				}
			} // auto-close FileInputStream before opening it again
			try (final BufferedInputStream bis = new BufferedInputStream(new FileInputStream(canonicalFileObj))) { // re-open the file and unpack as compressed
				if (UnpackHelper.unpackInputStreamAsCompressed(bis, tempDir, canonicalFileObj, includes, excludes, skippedFiles, invalidFiles, validFiles, invalidArchives, validArchives, duplicateFiles, level)) {
					validArchives.add(canonicalFileObj); // valid archive of many files
					return;
				}
			} // auto-close FileInputStream before opening it again
			try (final BufferedInputStream bis = new BufferedInputStream(new FileInputStream(canonicalFileObj))) { // re-open the file and unpack as compressed
				if (UnpackHelper.unpackInputStreamAsRpm(bis, tempDir, canonicalFileObj, includes, excludes, skippedFiles, invalidFiles, validFiles, invalidArchives, validArchives, duplicateFiles, level)) {
					validArchives.add(canonicalFileObj); // valid archive of many files
					return;
				}
			} // auto-close FileInputStream before opening it again
			try (final BufferedInputStream bis = new BufferedInputStream(new FileInputStream(canonicalFileObj))) { // re-open the file and unpack as compressed
				if (UnpackHelper.unpackInputStreamAsRar(bis, tempDir, canonicalFileObj, includes, excludes, skippedFiles, invalidFiles, validFiles, invalidArchives, validArchives, duplicateFiles, level)) {
					validArchives.add(canonicalFileObj); // valid archive of many files
					return;
				}
			} // auto-close FileInputStream before opening it again
		} catch (Exception e) {
			LOG.log(Level.WARNING, "Level=" + level + ", error opening file " + canonicalFileObj.getPath(), e); // NOSONAR
		}
		invalidArchives.add(canonicalFileObj); // fell through all formats so
												// flag it as invalid
	}

	private static void unpackFileFromByteArray(final byte[] fileContent, final File tempDir, final File canonicalFileObj, final String[] includes, final String[] excludes, final List<File> skippedFiles, final List<File> invalidFiles, final List<File> validFiles, final List<File> invalidArchives, final List<File> validArchives, final Set<String> duplicateFiles, final int level) { // NOSONAR
		try {
			try (final ByteArrayInputStream bais = new ByteArrayInputStream(fileContent)) { // open the file and unpack as archive
				validFiles.add(canonicalFileObj);
				// bais.mark(fileContent.length); // readAheadLimit is ignored and default mark is 0, unlike FileInputStream, so assume this is a NO-OP
				if (UnpackHelper.unpackInputStreamAsArchive(bais, tempDir, canonicalFileObj, includes, excludes, skippedFiles, invalidFiles, validFiles, invalidArchives, validArchives, duplicateFiles, level)) {
					validArchives.add(canonicalFileObj); // valid archive of many files
					return;
				}
				bais.reset(); // rewind input stream to beginning of byte array
				if (UnpackHelper.unpackInputStreamAsCompressed(bais, tempDir, canonicalFileObj, includes, excludes, skippedFiles, invalidFiles, validFiles, invalidArchives, validArchives, duplicateFiles, level)) {
					validArchives.add(canonicalFileObj); // valid archive of 1 file
					return;
				}
				bais.reset(); // rewind input stream to beginning of byte array
				if (UnpackHelper.unpackInputStreamAsRpm(bais, tempDir, canonicalFileObj, includes, excludes, skippedFiles, invalidFiles, validFiles, invalidArchives, validArchives, duplicateFiles, level)) {
					validArchives.add(canonicalFileObj); // valid archive of 1 file
					return;
				}
				bais.reset(); // rewind input stream to beginning of byte array
				if (UnpackHelper.unpackInputStreamAsRar(bais, tempDir, canonicalFileObj, includes, excludes, skippedFiles, invalidFiles, validFiles, invalidArchives, validArchives, duplicateFiles, level)) {
					validArchives.add(canonicalFileObj); // valid archive of many file
					return;
				}
			}
		} catch (Exception e) {
			LOG.log(Level.WARNING, "Level=" + level + ", error opening byte array " + canonicalFileObj.getPath(), e);
		}
		invalidArchives.add(canonicalFileObj); // fell through all formats so flag it as invalid
	}

	private static boolean unpackInputStreamAsArchive(final InputStream is, final File tempDir, final File canonicalFileObj, final String[] includes, final String[] excludes, final List<File> skippedFiles, final List<File> invalidFiles, final List<File> validFiles, final List<File> invalidArchives, final List<File> validArchives, final Set<String> duplicateFiles, final int level) { // NOSONAR
		try (final ArchiveInputStream ais = new ArchiveStreamFactory().createArchiveInputStream(is)) {
			final File outputDirectory = FileUtil.computeExtractDirectory(tempDir, canonicalFileObj);
			String outputFileName;
			ArchiveEntry archiveEntry;
			while (null != (archiveEntry = ais.getNextEntry())) {
				outputFileName = archiveEntry.getName();
				if (archiveEntry.isDirectory()) {
					LOG.log(Level.FINE, "Level=" + level + ", ignoring file '" + canonicalFileObj.getPath() + "' entry '" + outputFileName + "' is a directory."); // NOSONAR
					continue;
				}
				UnpackHelper.writeOutputFileAndRecurse(tempDir, canonicalFileObj, includes, excludes, skippedFiles, invalidFiles, validFiles, invalidArchives, validArchives, duplicateFiles, level, ais, archiveEntry.getSize(), outputFileName, new File(outputDirectory, outputFileName));
			}
			return true;
		} catch (UnsupportedZipFeatureException e) { // NOSONAR Either log or rethrow this exception.
			LOG.log(Level.WARNING, "Level=" + level + ", skipping unsupported zip file '" + canonicalFileObj.getPath() + "', exception=" + e.getMessage() + "."); // NOSONAR
			return true; // Example: file is a valid zip, but it was encrypted, return true to prevent subsequent non-zip parsing
		} catch (ArchiveException e) { // NOSONAR Either log or rethrow this exception.
			LOG.log(Level.FINE, "Level=" + level + ", error for file '" + canonicalFileObj.getPath() + "', exception=" + e.getMessage() + "."); // NOSONAR
		} catch (Exception e) {
			LOG.log(Level.WARNING, "Level=" + level + ", error for file " + canonicalFileObj.getPath(), e); // NOSONAR
		}
		return false;
	}

	private static boolean unpackInputStreamAsCompressed(final InputStream is, final File tempDir, final File canonicalFileObj, final String[] includes, final String[] excludes, final List<File> skippedFiles, final List<File> invalidFiles, final List<File> validFiles, final List<File> invalidArchives, final List<File> validArchives, final Set<String> duplicateFiles, final int level) {
		try (final CompressorInputStream cis = new CompressorStreamFactory().createCompressorInputStream(is)) { // Try uncompress
			final File outputDirectory = FileUtil.computeExtractDirectory(tempDir, canonicalFileObj);
			final String outputFileName = FileUtil.computeUncompressedFileName(canonicalFileObj);
			UnpackHelper.writeOutputFileAndRecurse(tempDir, canonicalFileObj, includes, excludes, skippedFiles, invalidFiles, validFiles, invalidArchives, validArchives, duplicateFiles, level, cis, cis.available(), outputFileName, new File(outputDirectory, outputFileName));
			return true;
		} catch (CompressorException e) { // NOSONAR Either log or rethrow this exception.
			LOG.log(Level.FINE, "Level=" + level + ", error for file '" + canonicalFileObj.getPath() + "', exception=" + e.getMessage() + "."); // NOSONAR
		} catch (Exception e) {
			LOG.log(Level.WARNING, "Level=" + level + ", error for file " + canonicalFileObj.getPath(), e); // NOSONAR
		}
		return false;
	}

	private static boolean unpackInputStreamAsRpm(final InputStream is, final File tempDir, final File canonicalFileObj, final String[] includes, final String[] excludes, final List<File> skippedFiles, final List<File> invalidFiles, final List<File> validFiles, final List<File> invalidArchives, final List<File> validArchives, final Set<String> duplicateFiles, final int level) { // NOSONAR
		try (final ReadableChannelWrapper channelWrapper = new ReadableChannelWrapper(Channels.newChannel(is))) {
			final Format format;
			try { // NOSONAR
				format = new org.redline_rpm.Scanner().run(channelWrapper);
			} catch (Exception e) { // NOSONAR Either log or rethrow this exception.
				LOG.log(Level.FINE, "Level=" + level + ", error for file '" + canonicalFileObj.getPath() + "', exception=" + e.getMessage() + "."); // NOSONAR
				return false;
			}
			final Header header = format.getHeader();
			try (final InputStream inputStream = Util.openPayloadStream(header, is)) {
				final File outputDirectory = FileUtil.computeExtractDirectory(tempDir, canonicalFileObj);
				final String outputFileName = FileUtil.computeCpioFileName(canonicalFileObj);
				UnpackHelper.writeOutputFileAndRecurse(tempDir, canonicalFileObj, includes, excludes, skippedFiles, invalidFiles, validFiles, invalidArchives, validArchives, duplicateFiles, level, inputStream, 10000000, outputFileName, new File(outputDirectory, outputFileName));
			}
			return true;
		} catch (IOException e) { // NOSONAR Either log or rethrow this exception.
			LOG.log(Level.FINE, "Level=" + level + ", error for file '" + canonicalFileObj.getPath() + "', exception=" + e.getMessage() + "."); // NOSONAR
		} catch (Exception e) {
			LOG.log(Level.WARNING, "Level=" + level + ", error for file " + canonicalFileObj.getPath(), e); // NOSONAR
		}
		return false;
	}

	private static boolean unpackInputStreamAsRar(final InputStream is, final File tempDir, final File canonicalFileObj, final String[] includes, final String[] excludes, final List<File> skippedFiles, final List<File> invalidFiles, final List<File> validFiles, final List<File> invalidArchives, final List<File> validArchives, final Set<String> duplicateFiles, final int level) { // NOSONAR
		try {
			is.mark(MAX_LENGTH_FILE_SIGNATURE);
			final byte[] signature = new byte[MAX_LENGTH_FILE_SIGNATURE];
			final int signatureLength = is.read(signature);
			if (UnpackHelper.checkSignature(RAR_FILE_SIGNATURE, signature, signatureLength)) {	// fall through, it is a valid RAR so continue (not RAR5, not fake)
				LOG.log(Level.FINE, "Level=" + level + ", valid rar file " + canonicalFileObj.getPath()); // NOSONAR
			} else {
				LOG.log(Level.FINER, "Level=" + level + ", not a valid rar file " + canonicalFileObj.getPath()); // NOSONAR
				return false;
			}
			is.reset();
		} catch (Exception e) {
			LOG.log(Level.SEVERE, "Level=" + level + ", error for file " + canonicalFileObj.getPath(), e); // NOSONAR
			return false;
		} // auto-close FileInputStream before opening it again
		try (final Archive arc = new Archive(canonicalFileObj)) {
			final File outputDirectory = FileUtil.computeExtractDirectory(tempDir, canonicalFileObj);
			String outputFileName;
			if (arc.isEncrypted()) {
				LOG.log(Level.WARNING, "Level=" + level + ", skipping rar file " + canonicalFileObj.getPath() + " encrypted content."); // NOSONAR
				return true;
			}
			FileHeader fh;
			while (null != (fh = arc.nextFileHeader())) { // NOSONAR
				outputFileName = (fh.isFileHeader() && fh.isUnicode()) ? fh.getFileNameW() : fh.getFileNameString();
				if (fh.isDirectory()) {
					LOG.log(Level.FINE, "Level=" + level + ", ignoring file '" + canonicalFileObj.getPath() + "' entry '" + outputFileName + "' is a directory."); // NOSONAR
					continue;
				} else if (fh.isEncrypted()) {
					LOG.log(Level.INFO, "Level=" + level + ", skipping rar file '" + canonicalFileObj.getPath() + "' encrypted entry '" + outputFileName + "'."); // NOSONAR
					return true;
				}
				UnpackHelper.writeOutputFileAndRecurse(tempDir, canonicalFileObj, includes, excludes, skippedFiles, invalidFiles, validFiles, invalidArchives, validArchives, duplicateFiles, level, arc.getInputStream(fh), fh.getFullUnpackSize(), outputFileName, new File(outputDirectory, outputFileName));
			}
			return true;
		} catch (RarException | NullPointerException e) { // NOSONAR Either log or rethrow this exception.
			LOG.log(Level.FINE, "Level=" + level + ", error for file '" + canonicalFileObj.getPath() + "', exception=" + e.getMessage() + ".");
		} catch (Exception e) {
			LOG.log(Level.WARNING, "Level=" + level + ", error for file " + canonicalFileObj.getPath(), e); // NOSONAR
		}
		return false;
	}

	private static void writeOutputFileAndRecurse(final File tempDir, final File canonicalFileObj, final String[] includes, final String[] excludes, final List<File> skippedFiles, final List<File> invalidFiles, final List<File> validFiles, final List<File> invalidArchives, final List<File> validArchives, final Set<String> duplicateFiles, final int level, final InputStream ais, final long outputFileSizeEstimate, final String outputFileName, final File outputFile) throws Exception, IOException, FileNotFoundException { // NOSONAR
		if (!StringUtil.isMatch(outputFileName, includes, excludes)) {
			LOG.log(Level.FINE, "Level=" + level + ", skipping parent file '" + canonicalFileObj.getPath() + "' entry '" + outputFileName + "'."); // NOSONAR
			skippedFiles.add(outputFile);
		} else {
			final File parentDirectoryObj = outputFile.getParentFile();
			boolean b;
			try (final Timer x = new Timer("Create directory")) {
				b = (!duplicateFiles.add(parentDirectoryObj.getPath())) || (FileUtil.makeCanonicalDirectories(parentDirectoryObj));
			}
			if (b) {
				final byte[] baFileContents;
				try (final FileOutputStream fos = new FileOutputStream(outputFile)) {
					if ((outputFileSizeEstimate > MAX_FILE_SIZE_IN_MEMORY_BUFFER) || (-1L == outputFileSizeEstimate)) {
						FileUtil.copy(ais, fos, REUSABLE_READ_BUFFER.get()); // copy archive entry -> file, content too big to hold in memory
//						FileUtil.copy(ais, fos.getChannel(), REUSABLE_COPY_BUFFER.get()); // copy archive entry -> file, content too big to hold in memory
						baFileContents = null;
					} else { // copy archive contents -> binary array -> file
						// TODO Debug runaway recursion
//						try (final UnsafeByteArrayOutputStream baos = new UnsafeByteArrayOutputStream(REUSABLE_WRITE_BUFFER.get())) {	// ASSUMPTION: outputFileSizeEstimate <= MAX_FILE_SIZE_IN_MEMORY_BUFFER
						try (final ByteArrayOutputStream baos = new ByteArrayOutputStream((int)outputFileSizeEstimate)) {	// ASSUMPTION: outputFileSizeEstimate <= MAX_FILE_SIZE_IN_MEMORY_BUFFER
							FileUtil.copy(ais, baos, REUSABLE_READ_BUFFER.get()); // copy archive entry -> byte array, content small enough to hold in memory
							baFileContents = baos.toByteArray();
//							if (outputFileSizeEstimate != baFileContents.length) {
//								System.out.println("outputFileSizeEstimate=" + outputFileSizeEstimate + ", baFileContents.length=" + baFileContents.length);
//							}
						}
						fos.write(baFileContents); // copy byte array to file, content small enough to hold in memory
					}
				}
				if (null == baFileContents) {
					UnpackHelper.unpackFileFromFileSystem(tempDir, outputFile, includes, excludes, skippedFiles, invalidFiles, validFiles, invalidArchives, validArchives, duplicateFiles, 1 + level);
				} else {
					UnpackHelper.unpackFileFromByteArray(baFileContents, tempDir, outputFile, includes, excludes, skippedFiles, invalidFiles, validFiles, invalidArchives, validArchives, duplicateFiles, 1 + level);
				}
			} else { // directory not created successfully
				LOG.log(Level.SEVERE, "Level=" + level + ", create output directory '" + parentDirectoryObj.getPath() + "' failed."); // NOSONAR
			}
		}
	}

	public static boolean checkSignature(final byte[] expectedSignature, final byte[] actualSignature, final int actualSignatureBytes) {
		if ((null == expectedSignature) || (null == actualSignature) || (actualSignatureBytes < expectedSignature.length) || (actualSignatureBytes > actualSignature.length)) {
			return false;
		}
		for (int i = 0; i < expectedSignature.length; i++) {
			if (expectedSignature[i] != actualSignature[i]) {
				return false;
			}
		}
		return true;
	}
}