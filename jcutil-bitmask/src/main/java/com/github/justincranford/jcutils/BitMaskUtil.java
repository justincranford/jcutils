package com.github.justincranford.jcutils;

public class BitMaskUtil {
	public static final int NUMBITSSIGNEDINT  = 31;
	public static final int NUMBITSSIGNEDLONG = 63;

	private BitMaskUtil() {
		// prevent class instantiation by making constructor private
	}

	public static boolean[] toBooleanArray(final int bitMask) throws Exception {
		ValidationUtil.assertGreaterThanOrEqual(bitMask, 0);
		final boolean[] b = new boolean[NUMBITSSIGNEDINT];
		for (int i=0, remainingBitMask = bitMask; i<NUMBITSSIGNEDINT; i++, remainingBitMask >>= 1) {
			b[i] = ((remainingBitMask & 1) != 0);
		}
		return b;
	}

	public static boolean[] toBooleanArray(final long bitMask) throws Exception {
		ValidationUtil.assertGreaterThanOrEqual(bitMask, 0);
		final boolean[] b = new boolean[NUMBITSSIGNEDLONG];
		long remainingBitMask = bitMask;
		for (int i=0; i<NUMBITSSIGNEDLONG; i++, remainingBitMask >>= 1) {
			b[i] = ((remainingBitMask & 1) != 0);
		}
		return b;
	}
}