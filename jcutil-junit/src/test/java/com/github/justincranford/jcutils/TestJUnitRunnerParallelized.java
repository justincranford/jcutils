package com.github.justincranford.jcutils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameters;

/**
 * Assumption: Thread pool size is >1 so thread execution is not serialized.
 */
@SuppressWarnings("hiding")
@RunWith(JUnitRunnerParallelized.class)
public class TestJUnitRunnerParallelized {
	private static final int                            NUM_RANDOM_INPUT_VALUES = 50;
	private static final List<Integer>                  INPUT                   = new ArrayList<Integer>(NUM_RANDOM_INPUT_VALUES);
	private static final ConcurrentLinkedQueue<Integer> OUTPUT                  = new ConcurrentLinkedQueue<Integer>();

	private final Integer randomInputValue;

	public TestJUnitRunnerParallelized(final Integer randomInputValue) {
		this.randomInputValue = randomInputValue;
	}

	@Parameters(name="{index}: {0}")	// Add parameter {0} to JUnit result label. EX: "[0: 987,654]" and "[1: -123,456,789]"
	public static Collection<Integer> data() {
		final Random random = new Random();	// secure randomness not important for disposable data, Random is OK instead of SecureRandom
		for (int i=0; i<NUM_RANDOM_INPUT_VALUES; i++) {
			INPUT.add(Integer.valueOf(random.nextInt()));
		}
		return INPUT;
	}

	@Test
	public void testConcurrent() {
		OUTPUT.add(this.randomInputValue);	// OUTPUT has same order as INPUT if execution is serialized, concurrent execution will lead to different order
	}

	@AfterClass
	public static void afterClass() {
		Assert.assertEquals(INPUT.size(), OUTPUT.size());
		final Iterator<Integer> iterOrderedInput     = INPUT.iterator();
		final Iterator<Integer> iterConcurrentOutput = OUTPUT.iterator();
		boolean allElementsMatch = true;
		while (iterConcurrentOutput.hasNext()) {
			final Integer concurrentOutput = iterConcurrentOutput.next();
			Assert.assertTrue(INPUT.contains(concurrentOutput));					// validate each OUTPUT element is somewhere in the INPUT array
			allElementsMatch &= (concurrentOutput.equals(iterOrderedInput.next()));	// validate some OUTPUT elements in different order from INPUT to confirm asynchronous execution
		}
		Assert.assertFalse("Expected output elements to be in different order from input, but they are same order implying serialization.", allElementsMatch);
	}
}