package com.github.justincranford.jcutils;

import org.junit.Assert;
import org.junit.Test;

@SuppressWarnings("static-method")
public class UTBitMaskUtil {
	@Test
	public void testPrivateConstructorEmptyParameters() throws Exception {
		ValidationUtil.assertPrivateConstructorNoParameters(BitMaskUtil.class, false);
		ValidationUtil.assertPrivateConstructorNoParameters(BitMaskUtil.class, true);
	}

	@Test(expected=Exception.class)
	public void testMinInt() throws Exception {
		BitMaskUtil.toBooleanArray(Integer.MIN_VALUE);
	}

	@Test(expected=Exception.class)
	public void testMinLong() throws Exception {
		BitMaskUtil.toBooleanArray(Long.MIN_VALUE);
	}

	@Test(expected=Exception.class)
	public void testNegativeInt() throws Exception {
		BitMaskUtil.toBooleanArray(-1);
	}

	@Test(expected=Exception.class)
	public void testNegativeLong() throws Exception {
		BitMaskUtil.toBooleanArray(-1L);
	}

	@Test
	public void testZeroLong() throws Exception {
		final boolean[] false63        = generateBooleanArray(BitMaskUtil.NUMBITSSIGNEDINT, false);
		final boolean[] maxLongBitMask = BitMaskUtil.toBooleanArray(0);
		Assert.assertArrayEquals(false63, maxLongBitMask);
	}

	@Test
	public void testZeroInt() throws Exception {
		final boolean[] false31       = generateBooleanArray(BitMaskUtil.NUMBITSSIGNEDINT, false);
		final boolean[] maxIntBitMask = BitMaskUtil.toBooleanArray(0);
		Assert.assertArrayEquals(false31, maxIntBitMask);
	}

	@Test
	public void testMaxInt() throws Exception {
		final boolean[] true31        = generateBooleanArray(BitMaskUtil.NUMBITSSIGNEDINT, true);
		final boolean[] maxIntBitMask = BitMaskUtil.toBooleanArray(Integer.MAX_VALUE);
		Assert.assertArrayEquals(true31, maxIntBitMask);
	}

	@Test
	public void testMaxLong() throws Exception {
		final boolean[] true63         = generateBooleanArray(BitMaskUtil.NUMBITSSIGNEDLONG, true);
		final boolean[] maxLongBitMask = BitMaskUtil.toBooleanArray(Long.MAX_VALUE);
		Assert.assertArrayEquals(true63, maxLongBitMask);
	}

	@Test
	public void testLong() throws Exception {
		long bitMaskLong = 1;
		for (int bitMaskArrayTrueOffset = 0; bitMaskArrayTrueOffset < BitMaskUtil.NUMBITSSIGNEDLONG; bitMaskArrayTrueOffset++) {
			final boolean[] oneTrueBitArray = generateBooleanArray(BitMaskUtil.NUMBITSSIGNEDLONG, bitMaskArrayTrueOffset);
			final boolean[] bitMaskArray    = BitMaskUtil.toBooleanArray(bitMaskLong);
			Assert.assertArrayEquals(bitMaskArray, oneTrueBitArray);
			bitMaskLong <<= 1;
		}
	}

	@Test
	public void testInt() throws Exception {
		int bitMaskInt = 1;
		for (int bitMaskArrayTrueOffset = 0; bitMaskArrayTrueOffset < BitMaskUtil.NUMBITSSIGNEDINT; bitMaskArrayTrueOffset++) {
			final boolean[] oneTrueBitArray = generateBooleanArray(BitMaskUtil.NUMBITSSIGNEDINT, bitMaskArrayTrueOffset);
			final boolean[] bitMaskArray    = BitMaskUtil.toBooleanArray(bitMaskInt);
			Assert.assertArrayEquals(bitMaskArray, oneTrueBitArray);
			bitMaskInt <<= 1;
		}
	}

	private static final boolean[] generateBooleanArray(final int length, final int trueOffset) {
		final boolean[] oneTrueBitArray = generateBooleanArray(length, false);
		oneTrueBitArray[trueOffset] = true;
		return oneTrueBitArray;
	}

	private static final boolean[] generateBooleanArray(final int length, final boolean value) {
		final boolean[] trueBooleansArray = new boolean[length];
		for (int i=0; i<length; i++) {
			trueBooleansArray[i] = value;
		}
		return trueBooleansArray;
	}
}