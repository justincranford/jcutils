package com.github.justincranford.jcutils;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import javax.xml.bind.DatatypeConverter;

public class StringUtil {
	public static final String        NULL_STRING  = null;														// NOSONAR Make this member "protected".
	public static final String[]      NULL_STRING_ARRAY = null;													// NOSONAR Make this member "protected".
	public static final List<String>  NULL_STRING_LIST = null;													// NOSONAR Make this member "protected".
	public static final String        EMPTY_STRING = "";														// NOSONAR Make this member "protected".
	public static final String[]      EMPTY_STRING_ARRAY = {};													// NOSONAR Make this member "protected".
	public static final List<String>  EMPTY_STRING_LIST = new ArrayList<>(0);									// NOSONAR Make this member "protected".
	public static final Pattern[]     NULL_PATTERN_ARRAY = null;												// NOSONAR Make this member "protected".
	public static final Pattern[]     EMPTY_PATTERN_ARRAY = new Pattern[0];										// NOSONAR Make this member "protected".
	public static final Pattern[]     DEFAULT_INCLUDE_PATTERN_ARRAY = new Pattern[] {Pattern.compile(".*")};	// NOSONAR Make this member "protected".
	public static final Pattern[]     DEFAULT_EXCLUDE_PATTERN_ARRAY = EMPTY_PATTERN_ARRAY;						// NOSONAR Make this member "protected".

	private StringUtil() {
		// prevent instantiation of this class
	}

	public static String base64Encode(final byte[] unencodedBytes) {
		return DatatypeConverter.printBase64Binary(unencodedBytes);
	}

	public static byte[] base64Decode(final String encodedCharacters) {
		return DatatypeConverter.parseBase64Binary(encodedCharacters);
	}

	public static String hexEncode(final byte[] unencodedBytes) {
		return DatatypeConverter.printHexBinary(unencodedBytes);
	}

	public static byte[] hexDecode(final String encodedCharacters) {
		return DatatypeConverter.parseHexBinary(encodedCharacters);
	}

	public static byte[] binaryEncode(final String unencodedCharacters, final String charset) throws UnsupportedEncodingException {
		return unencodedCharacters.getBytes(charset);
	}

	public static String binaryDecode(final byte[] encodedCharacters, final String charset) throws UnsupportedEncodingException {
		return new String(encodedCharacters, charset);
	}

	public static String join(final String[] array, final String delimiter) throws Exception {
		return StringUtil.appendJoin(new StringBuilder(1024), array, delimiter, true, true).toString();
	}

	public static String join(final String[] array, final String delimiter, final boolean isNullListAnError, final boolean isNullElementAnError) throws Exception {
		return StringUtil.appendJoin(new StringBuilder(1024), array, delimiter, isNullListAnError, isNullElementAnError).toString();
	}

//	public static String join(final Collection<String> set, final String delimiter) throws Exception {
//		return StringUtil.appendJoin(new StringBuilder(1024), new ArrayList<String>(set), delimiter, true, true).toString();
//	}
//
//	public static String join(final Collection<String> set, final String delimiter, final boolean isNullListAnError, final boolean isNullElementAnError) throws Exception {
//		return StringUtil.appendJoin(new StringBuilder(1024), new ArrayList<String>(set), delimiter, isNullListAnError, isNullElementAnError).toString();
//	}

	public static String join(final List<String> list, final String delimiter) throws Exception {
		return StringUtil.appendJoin(new StringBuilder(1024), list, delimiter, true, true).toString();
	}

	public static String join(final List<String> list, final String delimiter, final boolean isNullListAnError, final boolean isNullElementAnError) throws Exception {
		return StringUtil.appendJoin(new StringBuilder(1024), list, delimiter, isNullListAnError, isNullElementAnError).toString();
	}

	public static StringBuilder appendJoin(final StringBuilder sb, final String[] array, final String delimiter) throws Exception {
		return StringUtil.appendJoin(sb, array, delimiter, true, true);
	}

	public static StringBuilder appendJoin(final StringBuilder sb, final List<String> list, final String delimiter) throws Exception {
		return StringUtil.appendJoin(sb, list, delimiter, true, true);
	}

	public static StringBuilder appendJoin(final StringBuilder sb, final String[] array, final String delimiter, final boolean isNullListAnError, final boolean isNullElementAnError) throws Exception {
		if (null == sb) {
			throw new NullPointerException("StringBuilder is null");
		} else if (null == delimiter) {
			throw new NullPointerException("Delimiter is null");
		} else if ((null == array) && (!isNullListAnError)) {
			return sb;
		} else if (null == array) {
			throw new NullPointerException("Array is null");
		}
		for (int size=array.length, i=0; i<size; i++) {
			final String element = array[i];
			if ((null == element) && (isNullElementAnError)) {
				throw new NullPointerException("Element is null");
			}
			if (0 != i) {
				sb.append(delimiter);
			}
			sb.append(element);	// ASSUMPTION: null references are printed as "null" string value (without quotes)
		}
		return sb;
	}

	public static StringBuilder appendJoin(final StringBuilder sb, final List<String> list, final String delimiter, final boolean isNullListAnError, final boolean isNullElementAnError) throws Exception {
		if (null == sb) {
			throw new NullPointerException("StringBuilder is null");
		} else if (null == delimiter) {
			throw new NullPointerException("Delimiter is null");
		} else if ((null == list) && (!isNullListAnError)) {
			return sb;
		} else if (null == list) {
			throw new NullPointerException("List is null");
		}
		for (int size=list.size(), i=0; i<size; i++) {
			final String element = list.get(i);
			if ((null == element) && (isNullElementAnError)) {
				throw new NullPointerException("Element is null");
			}
			if (0 != i) {
				sb.append(delimiter);
			}
			sb.append(element);	// ASSUMPTION: null references are printed as "null" string value (without quotes)
		}
		return sb;
	}

	public static String safeTrim(final String value) {
		if (null != value) {
			return value.trim();
		}
		return value;
	}

	public static String safeSubstring(final String value, final int beginIndex, final int endIndex) {
		if (null == value) {
			return null;
		} else if (value.length() < endIndex) {
			return value;
		}
		return value.substring(beginIndex, endIndex);
	}

	public static boolean isMatch(final String entryFilePath, String[] includes, String[] excludes) throws Exception {
		if (null == entryFilePath) {
			return false;
		}
		
		final Pattern[] includePatterns = StringUtil.compilePatterns(includes, DEFAULT_INCLUDE_PATTERN_ARRAY);
		if (!(StringUtil.isMatchAnyPattern(entryFilePath, includePatterns))) {
			return false;	// not included
		}	// included

		final Pattern[] excludePatterns = StringUtil.compilePatterns(excludes, DEFAULT_EXCLUDE_PATTERN_ARRAY);
		return(!(StringUtil.isMatchAnyPattern(entryFilePath, excludePatterns)));	// returns not excluded
	}

	/*package*/ static Pattern[] compilePatterns(final String[] strings, final Pattern[] nullDefault) throws Exception {	// NOSONAR Define and throw a dedicated exception instead of using a generic one.
		if (null == strings) {
			return nullDefault;
		}
		final int numStrings = strings.length;
		final Pattern[] patterns = new Pattern[numStrings];
		for (int i=0; i<numStrings; i++) {
			final String include = strings[i];
			if (null == include) {
				throw new NullPointerException("Null pattern is not allowed");
			}
			patterns[i] = Pattern.compile(include);
		}
		return patterns;
	}

	/*package*/ static boolean isMatchAnyPattern(final String candidate, final Pattern[] patterns) throws Exception {	// NOSONAR Define and throw a dedicated exception instead of using a generic one.
		if (null == patterns) {
			throw new NullPointerException("Null patterns array not allowed");
		}
		for (final Pattern pattern : patterns) {
			if (null == pattern) {
				throw new NullPointerException("Null pattern is not allowed");
			} else if (pattern.matcher(candidate).matches()) {
				return true;
			}
		}
		return false;
	}

	public static String[] concatenate(final boolean isSkipNullElements, final boolean isSkipDuplicates, final String[]... arrays) {
		final Set<String>  uniqueElements    = new HashSet<>();
		final List<String> concatenatedArray = new ArrayList<>();
		for (final String[] array : arrays) {
			for (final String element : array) {
				if (	
					((!isSkipNullElements) || (null != element)) &&
					((!isSkipDuplicates)   || (uniqueElements.add(element)))
				) {
					concatenatedArray.add(element);
				}
			}
		}
		return concatenatedArray.toArray(new String[concatenatedArray.size()]);
	}

	public static String[] wrap(final String[] array, final String prefix, final String postfix) throws Exception {
		return StringUtil.wrap(array, prefix, postfix, true, true);
	}

	public static List<String> wrap(final List<String> list, final String prefix, final String postfix) throws Exception {
		return StringUtil.wrap(list, prefix, postfix, true, true);
	}

	public static String[] wrap(final String[] array, final String prefix, final String postfix, final boolean isNullListAnError, final boolean isNullElementAnError) throws Exception {
		if (null == prefix) {
			throw new NullPointerException("Prefix is null");
		} else if (null == postfix) {
			throw new NullPointerException("Postfix is null");
		} else if ((null == array) && (!isNullListAnError)) {
			return new String[0];
		} else if (null == array) {
			throw new NullPointerException("Array is null");
		}
		final String[] newArray = new String[array.length];
		for (int length=array.length, i=0; i<length; i++) {
			final String element = array[i];
			if ((null == element) && (isNullElementAnError)) {
				throw new NullPointerException("Element is null");
			}
			newArray[i] = prefix + element + postfix;
		}
		return newArray;
	}

	public static List<String> wrap(final List<String> list, final String prefix, final String postfix, final boolean isNullListAnError, final boolean isNullElementAnError) throws Exception {
		if (null == prefix) {
			throw new NullPointerException("Prefix is null");
		} else if (null == postfix) {
			throw new NullPointerException("Postfix is null");
		} else if ((null == list) && (!isNullListAnError)) {
			return new ArrayList<>(0);
		} else if (null == list) {
			throw new NullPointerException("List is null");
		}
		final List<String> newList = new ArrayList<>(list.size());
		for (final String element : list) {
			if ((null == element) && (isNullElementAnError)) {
				throw new NullPointerException("Element is null");
			}
			newList.add(prefix + element + postfix);
		}
		return newList;
	}

	public static final String[] toString(final Object[] array) {
		if (null == array) {
			return StringUtil.NULL_STRING_ARRAY;
		}
		final int lengthLength = array.length;
		if (0 == lengthLength) {
			return StringUtil.EMPTY_STRING_ARRAY;
		}
		final String[] strings = new String[lengthLength];
		for (int i=0; i<lengthLength; i++) {
			final Object object = array[i];
			if (null == object) {
				strings[i] = "null";
			} else {
				strings[i] = object.toString();
			}
		}
		return strings;
	}

	public static final List<String> toString(final List<?> list) {
		if (null == list) {
			return StringUtil.NULL_STRING_LIST;
		} else if (list.isEmpty()) {
			return StringUtil.EMPTY_STRING_LIST;
		}
		final List<String> strings = new ArrayList<>(list.size());
		for (final Object object : list) {
			if (null == object) {
				strings.add("null");
			} else {
				strings.add(object.toString());
			}
		}
		return strings;
	}
}