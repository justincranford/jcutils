package com.github.justincranford.jcutils;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

@SuppressWarnings("static-method")
public class TestStringUtil {
	private static final String[]     STRING_ARRAY_NULL            = null;
	private static final String[]     STRING_ARRAY_LENGTH_0        = new String[] {};
	private static final String[]     STRING_ARRAY_LENGTH_1        = new String[] {"test"};
	private static final String[]     STRING_ARRAY_LENGTH_2        = new String[] {"test","test"};
	private static final List<String> STRING_LIST_NULL             = null;
	private static final List<String> STRING_LIST_LENGTH_0         = Arrays.asList(STRING_ARRAY_LENGTH_0);
	private static final List<String> STRING_LIST_LENGTH_1         = Arrays.asList(STRING_ARRAY_LENGTH_1);
	private static final List<String> STRING_LIST_LENGTH_2         = Arrays.asList(STRING_ARRAY_LENGTH_2);
	private static final String       EXPECTED_JOIN_VALUE_LENGTH_0 = "";
	private static final String       EXPECTED_JOIN_VALUE_LENGTH_1 = "test";
	private static final String       EXPECTED_JOIN_VALUE_LENGTH_2 = "test,test";

	@BeforeClass
	public static void beforeClass() {
		Assert.assertEquals(STRING_ARRAY_LENGTH_0.length, STRING_LIST_LENGTH_0.size());
		Assert.assertEquals(STRING_ARRAY_LENGTH_1.length, STRING_LIST_LENGTH_1.size());
		Assert.assertEquals(STRING_ARRAY_LENGTH_2.length, STRING_LIST_LENGTH_2.size());
	}

	@Test
	public void testPrivateConstructor() throws Exception {
		ValidationUtil.assertPrivateConstructorNoParameters(StringUtil.class, true);
	}

	@Test
	public void testBase64EncodeDecode() throws Exception {
		final String originalString = "Hello World!";
		final String base64EncodeBytes = StringUtil.base64Encode(originalString.getBytes("UTF8"));
		final byte[] base64DecodeBytes = StringUtil.base64Decode(base64EncodeBytes);
		final String base64DecodeString = new String(base64DecodeBytes, "UTF8");
		if (!originalString.equals(base64DecodeString)) {
			throw new Exception("Not equals");
		}
	}

	@Test
	public void testHexEncodeDecode() throws Exception {
		final String originalString = "Hello World!";
		final String hexEncodeBytes = StringUtil.hexEncode(originalString.getBytes("UTF8"));
		final byte[] hexDecodeBytes = StringUtil.hexDecode(hexEncodeBytes);
		final String hexDecodeString = new String(hexDecodeBytes, "UTF8");
		if (!originalString.equals(hexDecodeString)) {
			throw new Exception("Not equals");
		}
	}

	@Test
	public void testBinaryEncodeDecode() throws Exception {
		final String originalString = "Hello World!";
		final byte[] binaryEncodeBytes = StringUtil.binaryEncode(originalString, "UTF8");
		final String binaryDecodeBytes = StringUtil.binaryDecode(binaryEncodeBytes, "UTF8");
		if (!originalString.equals(binaryDecodeBytes)) {
			throw new Exception("Not equals");
		}
	}

	@Test(expected=NullPointerException.class)
	public void testJoinArrayNullList() throws Exception {
		StringUtil.join(STRING_ARRAY_NULL, ",");
	}

	@Test(expected=NullPointerException.class)
	public void testJoinArrayNullDelimiter() throws Exception {
		StringUtil.join(STRING_ARRAY_LENGTH_2, null);
	}

	@Test
	public void testJoinArrayNullListOk() throws Exception {
		StringUtil.join(STRING_ARRAY_NULL, ",", false, true);
	}

	@Test(expected=NullPointerException.class)
	public void testJoinArrayNullDelimiterNotOk() throws Exception {
		StringUtil.join(STRING_ARRAY_LENGTH_2, null, true, true);
	}

	@Test(expected=NullPointerException.class)
	public void testJoinListNullList() throws Exception {
		StringUtil.join(STRING_LIST_NULL, ",");
	}

	@Test(expected=NullPointerException.class)
	public void testJoinListNullDelimiter() throws Exception {
		StringUtil.join(STRING_LIST_LENGTH_2, null);
	}

	@Test
	public void testJoinListNullListOk() throws Exception {
		StringUtil.join(STRING_LIST_NULL, ",", false, true);
	}

	@Test(expected=NullPointerException.class)
	public void testJoinListNullDelimiterNotOk() throws Exception {
		StringUtil.join(STRING_LIST_LENGTH_2, null, true, true);
	}

	@Test(expected=NullPointerException.class)
	public void testAppendJoinNullStringBuilderNotOk() throws Exception {
		StringUtil.appendJoin(null, STRING_ARRAY_LENGTH_2, ",");
	}

	@Test(expected=NullPointerException.class)
	public void testAppendJoinNullArrayNotOk() throws Exception {
		StringUtil.appendJoin(new StringBuilder(0), (String[])null, ",");
	}

	@Test(expected=NullPointerException.class)
	public void testAppendJoinNullDelimiterNotOk() throws Exception {
		StringUtil.appendJoin(new StringBuilder(0), STRING_ARRAY_LENGTH_2, null);
	}

	@Test
	public void testAppendJoinNullArrayOk() throws Exception {
		StringUtil.appendJoin(new StringBuilder(0), (String[])null, ",", false, true);
	}

	@Test
	public void testAppendJoinArrayNullElementOk() throws Exception {
		StringUtil.appendJoin(new StringBuilder(0), new String[] {null}, ",", true, false);
	}

	@Test(expected=NullPointerException.class)
	public void testAppendJoinArrayNullElementNotOk() throws Exception {
		StringUtil.appendJoin(new StringBuilder(0), new String[] {null}, ",", true, true);
	}

	@Test
	public void testJoinArraysAndLists() throws Exception {
		Assert.assertEquals(EXPECTED_JOIN_VALUE_LENGTH_0, StringUtil.join(STRING_ARRAY_LENGTH_0, ","));
		Assert.assertEquals(EXPECTED_JOIN_VALUE_LENGTH_1, StringUtil.join(STRING_ARRAY_LENGTH_1, ","));
		Assert.assertEquals(EXPECTED_JOIN_VALUE_LENGTH_2, StringUtil.join(STRING_ARRAY_LENGTH_2, ","));

		Assert.assertEquals(EXPECTED_JOIN_VALUE_LENGTH_0, StringUtil.join(STRING_LIST_LENGTH_0, ","));
		Assert.assertEquals(EXPECTED_JOIN_VALUE_LENGTH_1, StringUtil.join(STRING_LIST_LENGTH_1, ","));
		Assert.assertEquals(EXPECTED_JOIN_VALUE_LENGTH_2, StringUtil.join(STRING_LIST_LENGTH_2, ","));
	}

	@Test(expected=NullPointerException.class)
	public void testAppendJoinListNullStringBuilder() throws Exception {
		StringUtil.appendJoin((StringBuilder)null, STRING_LIST_LENGTH_2, ",");
	}

	@Test(expected=NullPointerException.class)
	public void testAppendJoinListNullList() throws Exception {
		StringUtil.appendJoin(new StringBuilder(0), STRING_LIST_NULL, ",");
	}

	@Test(expected=NullPointerException.class)
	public void testAppendJoinListNullDelimiter() throws Exception {
		StringUtil.appendJoin(new StringBuilder(0), STRING_LIST_LENGTH_2, null);
	}

	@Test
	public void testAppendJoinNullListOk() throws Exception {
		StringUtil.appendJoin(new StringBuilder(0), (List<String>)null, ",", false, true);
	}

	@Test
	public void testAppendJoinListNullElementOk() throws Exception {
		StringUtil.appendJoin(new StringBuilder(0), Arrays.asList((String)null), ",", true, false);
	}

	@Test(expected=NullPointerException.class)
	public void testAppendJoinListNullElementNotOk() throws Exception {
		StringUtil.appendJoin(new StringBuilder(0), Arrays.asList((String)null), ",", true, true);
	}

	@Test(expected=NullPointerException.class)
	public void testAppendJoinLists() throws Exception {
		Assert.assertEquals(EXPECTED_JOIN_VALUE_LENGTH_2, StringUtil.appendJoin(new StringBuilder(), STRING_LIST_LENGTH_2, null).toString());
	}

	@Test
	public void testSafeTrim() {
		Assert.assertEquals(null,                               StringUtil.safeTrim(null));
		Assert.assertEquals(EXPECTED_JOIN_VALUE_LENGTH_2, StringUtil.safeTrim(" "  + EXPECTED_JOIN_VALUE_LENGTH_2 + " "));
		Assert.assertEquals(EXPECTED_JOIN_VALUE_LENGTH_2, StringUtil.safeTrim("\t" + EXPECTED_JOIN_VALUE_LENGTH_2 + "\t"));
	}

	@Test
	public void testSafeSubString() {
		Assert.assertEquals(null,                         StringUtil.safeSubstring(null, 0, 0));
		Assert.assertEquals(EXPECTED_JOIN_VALUE_LENGTH_2, StringUtil.safeSubstring(EXPECTED_JOIN_VALUE_LENGTH_2, 0, EXPECTED_JOIN_VALUE_LENGTH_2.length()));
		Assert.assertEquals("",                           StringUtil.safeSubstring("", 0, 1));
		Assert.assertEquals("12345",                      StringUtil.safeSubstring("12345", 0, 5));
		Assert.assertEquals("123",                        StringUtil.safeSubstring("12345", 0, 3));
		Assert.assertEquals("23",                         StringUtil.safeSubstring("12345", 1, 3));
		Assert.assertEquals("5",                          StringUtil.safeSubstring("12345", 4, 5));
		Assert.assertEquals("",                           StringUtil.safeSubstring("12345", 5, 5));
		Assert.assertEquals("12345",                      StringUtil.safeSubstring("12345", 0, 100));
	}

	@Test(expected=NullPointerException.class)
	public void testIsMatchNullIncludePattern() throws Exception {
		StringUtil.isMatch("C:\\helloworld", new String[]{"nomatch",null}, null);
	}

	@Test(expected=NullPointerException.class)
	public void testIsMatchNullExcludePattern() throws Exception {
		StringUtil.isMatch("C:\\helloworld", null, new String[]{"nomatch",null});
	}

	@Test
	public void testIsMatch() throws Exception {
		Assert.assertFalse(StringUtil.isMatch(null            , new String[]{"nomatch", "^.*allo.*$"}, null));
		Assert.assertTrue( StringUtil.isMatch("C:\\helloworld", new String[]{"nomatch", "^.*ello.*$"}, null));
		Assert.assertFalse(StringUtil.isMatch("C:\\helloworld", new String[]{"nomatch", "^.*allo.*$"}, null));
		Assert.assertFalse(StringUtil.isMatch("C:\\helloworld", null,                                  new String[]{"nomatch", "^.*ello.*$"}));
		Assert.assertTrue( StringUtil.isMatch("C:\\helloworld", null,                                  new String[]{"nomatch", "^.*allo.*$"}));
	}

	@Test(expected=NullPointerException.class)
	public void testIsMatchAnyPatternNullPatterns() throws Exception {
		StringUtil.isMatchAnyPattern("", null);
	}

	@Test(expected=NullPointerException.class)
	public void testIsMatchAnyPatternNullPattern() throws Exception {
		StringUtil.isMatchAnyPattern("", new Pattern[] {null});
	}

	@Test
	public void testConcatenateSkipNullElementsSkipDuplicates() throws Exception {
		Assert.assertEquals(1,StringUtil.concatenate(true, true,   STRING_ARRAY_LENGTH_2, STRING_ARRAY_LENGTH_2).length);	// isSkipNullElements=true isSkipDuplicates=true
		Assert.assertEquals(1,StringUtil.concatenate(true, true,   STRING_ARRAY_LENGTH_2, new String[] {null}  ).length);	// isSkipNullElements=true isSkipDuplicates=true
		Assert.assertEquals(2,StringUtil.concatenate(true, false,  STRING_ARRAY_LENGTH_2, new String[] {null}  ).length);	// isSkipNullElements=true isSkipDuplicates=false
		Assert.assertEquals(4,StringUtil.concatenate(true, false,  STRING_ARRAY_LENGTH_2, STRING_ARRAY_LENGTH_2).length);	// isSkipNullElements=true isSkipDuplicates=false
		Assert.assertEquals(1,StringUtil.concatenate(false, true,  STRING_ARRAY_LENGTH_2, STRING_ARRAY_LENGTH_2).length);	// isSkipNullElements=false isSkipDuplicates=true
		Assert.assertEquals(2,StringUtil.concatenate(false, true,  STRING_ARRAY_LENGTH_2, new String[] {null}  ).length);	// isSkipNullElements=false isSkipDuplicates=true
		Assert.assertEquals(3,StringUtil.concatenate(false, false, STRING_ARRAY_LENGTH_2, new String[] {null}  ).length);	// isSkipNullElements=false isSkipDuplicates=false
		Assert.assertEquals(4,StringUtil.concatenate(false, false, STRING_ARRAY_LENGTH_2, STRING_ARRAY_LENGTH_2).length);	// isSkipNullElements=false isSkipDuplicates=false
	}

	@Test
	public void testWrappedList() throws Exception {
		final List<String> actualWrappedElements = StringUtil.wrap(STRING_LIST_LENGTH_2, "PREFIX", "POSTFIX");
		Assert.assertEquals(STRING_LIST_LENGTH_2.size(), actualWrappedElements.size());
		for (int i=0; i< actualWrappedElements.size(); i++) {
			final String actualWrappedElement = actualWrappedElements.get(i);
			Assert.assertNotNull(actualWrappedElement);
			Assert.assertEquals(actualWrappedElement, "PREFIX" + STRING_LIST_LENGTH_2.get(i) + "POSTFIX");
		}
	}

	@Test
	public void testWrappedArray() throws Exception {
		final String[] actualWrappedElements = StringUtil.wrap(STRING_ARRAY_LENGTH_2, "PREFIX", "POSTFIX");
		Assert.assertEquals(STRING_ARRAY_LENGTH_2.length, actualWrappedElements.length);
		for (int i=0; i< actualWrappedElements.length; i++) {
			final String actualWrappedElement = actualWrappedElements[i];
			Assert.assertNotNull(actualWrappedElement);
			Assert.assertEquals(actualWrappedElement, "PREFIX" + STRING_ARRAY_LENGTH_2[i] + "POSTFIX");
		}
	}

	@Test(expected=NullPointerException.class)
	public void testWrappedArrayNullPrefixNotOk() throws Exception {
		StringUtil.wrap(STRING_ARRAY_LENGTH_2, null, "POSTFIX");
	}

	@Test(expected=NullPointerException.class)
	public void testWrappedListNullPrefixNotOk() throws Exception {
		StringUtil.wrap(STRING_LIST_LENGTH_2, null, "POSTFIX");
	}

	@Test(expected=NullPointerException.class)
	public void testWrappedArrayNullPostfixNotOk() throws Exception {
		StringUtil.wrap(STRING_ARRAY_LENGTH_2, "PREFIX", null);
	}

	@Test(expected=NullPointerException.class)
	public void testWrappedListNullPostfixNotOk() throws Exception {
		StringUtil.wrap(STRING_LIST_LENGTH_2, "PREFIX", null);
	}

	@Test
	public void testWrappedNullArrayOk() throws Exception {
		StringUtil.wrap((String[])null, "PREFIX", "POSTFIX", false, true);	// isNullListAnError, isNullElementAnError
	}

	@Test
	public void testWrappedNullListOk() throws Exception {
		StringUtil.wrap((List<String>)null, "PREFIX", "POSTFIX", false, true);	// isNullListAnError, isNullElementAnError
	}

	@Test
	public void testArrayToString() throws Exception {
		Assert.assertNull(StringUtil.toString(STRING_ARRAY_NULL));
		Assert.assertArrayEquals(STRING_ARRAY_LENGTH_0, StringUtil.toString(STRING_ARRAY_LENGTH_0));
		Assert.assertArrayEquals(STRING_ARRAY_LENGTH_1, StringUtil.toString(STRING_ARRAY_LENGTH_1));
		Assert.assertArrayEquals(STRING_ARRAY_LENGTH_2, StringUtil.toString(STRING_ARRAY_LENGTH_2));
		Assert.assertArrayEquals(new String[] {"null", "null"}, StringUtil.toString(new Object[] {null, null}));
		Assert.assertArrayEquals(new String[] {"null", "null"}, StringUtil.toString(new Object[] {null, "null"}));
		Assert.assertArrayEquals(new String[] {"null", "good"}, StringUtil.toString(new Object[] {null, "good"}));
	}

	@Test
	public void testListToString() throws Exception {
		Assert.assertNull(StringUtil.toString(STRING_LIST_NULL));
		Assert.assertEquals(STRING_LIST_LENGTH_0, StringUtil.toString(STRING_LIST_LENGTH_0));
		Assert.assertEquals(STRING_LIST_LENGTH_1, StringUtil.toString(STRING_LIST_LENGTH_1));
		Assert.assertEquals(STRING_LIST_LENGTH_2, StringUtil.toString(STRING_LIST_LENGTH_2));
		Assert.assertEquals(Arrays.asList("null", "null"), StringUtil.toString(Arrays.asList(null, null)));
		Assert.assertEquals(Arrays.asList("null", "null"), StringUtil.toString(Arrays.asList(null, "null")));
		Assert.assertEquals(Arrays.asList("null", "good"), StringUtil.toString(Arrays.asList(null, "good")));
	}
}