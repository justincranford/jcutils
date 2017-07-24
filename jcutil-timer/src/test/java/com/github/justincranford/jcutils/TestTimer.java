package com.github.justincranford.jcutils;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * This test class does not do much other than populate as much as possible in a code coverage report for Timer.
 */
@SuppressWarnings("static-method")
public class TestTimer {
	private static final float floatCompareErrorTolerance = 0.98437498F;	// found this by trial and error

	@BeforeClass
	public static void beforeClass() throws Exception {
		Timer.setLogLevel(Level.FINEST);
		Timer.setLogTotalTimeUnit(TimeUnit.SECONDS);
		Timer.setAutoLogInterval(1);
		Timer.setAutoResetInterval(2);
	}

	@AfterClass
	public static void afterClass() throws Exception {
		Timer.resetLogLevel();
		Timer.resetLogTotalTimeUnit();
		Timer.resetAutoLogInterval();
		Timer.resetAutoResetInterval();
		Timer.reset();
	}

	@Before
	public void setUp() {
		Timer.reset();
	}

	@Test
	public void testCodeCoverage() throws Exception {
		try (Timer test = new Timer("testInstanceStartStop")) {
			// do nothing, auto-close will trigger print
		}
		Timer.stop("doesnotexist");
		Timer.start("testStaticStartStop");
		Timer.stop("testStaticStartStop");
		Timer.start("testStaticStartStop");
		Timer.stop("testStaticStartStop");
		Timer.start("testStaticStartStop");
		Timer.stop("testStaticStartStop");
		Timer.startStop("testStaticStartStop2");
		Timer.setLogTotalTimeUnit(TimeUnit.DAYS);
		Timer.logAllOrderedByStartTime(true);
		Timer.setLogTotalTimeUnit(TimeUnit.HOURS);
		Timer.logAllOrderedByStartTime(true);
		Timer.setLogTotalTimeUnit(TimeUnit.MINUTES);
		Timer.logAllOrderedByStartTime(true);
		Timer.setLogTotalTimeUnit(TimeUnit.SECONDS);
		Timer.logAllOrderedByStartTime(true);
		Timer.setLogTotalTimeUnit(TimeUnit.MILLISECONDS);
		Timer.logAllOrderedByStartTime(true);
		Timer.setLogTotalTimeUnit(TimeUnit.MICROSECONDS);
		Timer.logAllOrderedByStartTime(true);
		Timer.setLogTotalTimeUnit(TimeUnit.NANOSECONDS);
		Timer.logAllOrderedByStartTime(true);
		Timer.resetTimer("testStaticStartStop");
		Timer.resetTimer("testStaticStartStop4");
		Timer.resetTimers();

		Timer.getTotalTime("testStaticStartStop");
		Timer.getIterations("testStaticStartStop");
		Timer.getAverageTime("testStaticStartStop");
		Timer.getTotalTime("testStaticStartStop");
		Timer.getAverageTime("testStaticStartStop");
	}

	@Test
	public void testNormalizedNanoTimeToTimeUnits() throws Exception {
		final Long LONG_MAX_OBJ = new Long(Long.MAX_VALUE);
		Assert.assertEquals(TimeUnit.NANOSECONDS.toNanos(Long.MAX_VALUE),	Timer.normalizeNanoTimeToTimeUnits(LONG_MAX_OBJ, TimeUnit.NANOSECONDS),		floatCompareErrorTolerance);
		Assert.assertEquals(TimeUnit.NANOSECONDS.toMicros(Long.MAX_VALUE),	Timer.normalizeNanoTimeToTimeUnits(LONG_MAX_OBJ, TimeUnit.MICROSECONDS),	floatCompareErrorTolerance);
		Assert.assertEquals(TimeUnit.NANOSECONDS.toMillis(Long.MAX_VALUE),	Timer.normalizeNanoTimeToTimeUnits(LONG_MAX_OBJ, TimeUnit.MILLISECONDS),	floatCompareErrorTolerance);
		Assert.assertEquals(TimeUnit.NANOSECONDS.toSeconds(Long.MAX_VALUE),	Timer.normalizeNanoTimeToTimeUnits(LONG_MAX_OBJ, TimeUnit.SECONDS),			floatCompareErrorTolerance);
		Assert.assertEquals(TimeUnit.NANOSECONDS.toMinutes(Long.MAX_VALUE),	Timer.normalizeNanoTimeToTimeUnits(LONG_MAX_OBJ, TimeUnit.MINUTES),			floatCompareErrorTolerance);
		Assert.assertEquals(TimeUnit.NANOSECONDS.toHours(Long.MAX_VALUE),	Timer.normalizeNanoTimeToTimeUnits(LONG_MAX_OBJ, TimeUnit.HOURS),			floatCompareErrorTolerance);
		Assert.assertEquals(TimeUnit.NANOSECONDS.toDays(Long.MAX_VALUE),	Timer.normalizeNanoTimeToTimeUnits(LONG_MAX_OBJ, TimeUnit.DAYS),			floatCompareErrorTolerance);
		Assert.assertEquals(Float.MIN_VALUE,								Timer.normalizeNanoTimeToTimeUnits(null,         TimeUnit.NANOSECONDS),		floatCompareErrorTolerance);
	}

	@Test
	public void testConvertTimeUnitsToStringAbbreviation() throws Exception {
		Assert.assertEquals(null,  Timer.convertTimeUnitsToStringAbbreviation(null));
		Assert.assertEquals("nsec", Timer.convertTimeUnitsToStringAbbreviation(TimeUnit.NANOSECONDS));
		Assert.assertEquals("usec", Timer.convertTimeUnitsToStringAbbreviation(TimeUnit.MICROSECONDS));
		Assert.assertEquals("msec", Timer.convertTimeUnitsToStringAbbreviation(TimeUnit.MILLISECONDS));
		Assert.assertEquals("sec",  Timer.convertTimeUnitsToStringAbbreviation(TimeUnit.SECONDS));
		Assert.assertEquals("min",  Timer.convertTimeUnitsToStringAbbreviation(TimeUnit.MINUTES));
		Assert.assertEquals("hour", Timer.convertTimeUnitsToStringAbbreviation(TimeUnit.HOURS));
		Assert.assertEquals("day",  Timer.convertTimeUnitsToStringAbbreviation(TimeUnit.DAYS));
	}

	@Test
	public void testGetAverageTime() throws Exception {
		Assert.assertEquals(Float.MIN_VALUE, Timer.getAverageTime(null, TimeUnit.NANOSECONDS),	floatCompareErrorTolerance);
		Timer.start("zeroCompletedIterations");
		Assert.assertEquals(Float.MIN_VALUE, Timer.getAverageTime("zeroCompletedIterations", TimeUnit.NANOSECONDS),	floatCompareErrorTolerance);
		Timer.logAllOrderedByAverageTime(false);
		Timer.resetTimer("zeroCompletedIterations");
	}

	@Test
	public void testLogAllOrderedByTotalTime() throws Exception {
	}
}