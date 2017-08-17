/**
 * Copyright (C) 2001-2017 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 * http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Affero General Public License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see http://www.gnu.org/licenses/.
 */
package com.rapidminer.tools;

import java.lang.ref.WeakReference;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.WeakHashMap;

import com.rapidminer.Process;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.ProcessRootOperator;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.parameter.conditions.BooleanParameterCondition;


/**
 * The global random number generator. This should be used for all random purposes of RapidMiner to
 * ensure that two runs of the same process setup provide the same results.
 *
 * @author Ralf Klinkenberg, Ingo Mierswa, Jan Czogalla
 */
public class RandomGenerator extends Random {

	private static final long serialVersionUID = 7562534107359981433L;

	public static final String PARAMETER_USE_LOCAL_RANDOM_SEED = "use_local_random_seed";

	public static final String PARAMETER_LOCAL_RANDOM_SEED = "local_random_seed";

	/** The default seed (used by the ProcessRootOperator) */
	public static final int DEFAULT_SEED = 2001;

	/** Class name of the BackgroundExecutionProcess */
	private static final String BACKGROUND_EXECUTION_PROCESS_CLASS_NAME = "com.rapidminer.extension.concurrency.execution.BackgroundExecutionProcess";

	/** Magic seed of the ProcessRootOperator to use the current system time as seed */
	private static final int USE_SYSTEM_TIME = -1;

	/** Use this alphabet for random String creation. */
	private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

	/**
	 * Process-global random number generator using the random number generator seed specified for
	 * the root operator ({@link ProcessRootOperator}). Only present for processes that are not
	 * background execution processes.
	 */
	private static final ThreadLocal<RandomGenerator> GLOBAL_RANDOM_GENERATOR = new ThreadLocal<>();

	/**
	 * Map of threads to their respective {@link Process Processes}. Used to determine the correct
	 * random generator.
	 */
	private static final ThreadLocal<WeakReference<Process>> THREAD_TO_PROCESS = new ThreadLocal<>();

	/**
	 * Map of processes to their respective random generators. Mainly used for background execution
	 * processes to manage parallel executed iterations to provide independent random generators.
	 * <strong>Attention: Synchronized get and put methods to prevent race conditions!</strong>
	 */
	private static final Map<Process, RandomGenerator> GLOBAL_RANDOM_GENERATOR_MAP = new WeakHashMap<Process, RandomGenerator>() {

		@Override
		public synchronized RandomGenerator get(Object key) {
			return super.get(key);
		}

		@Override
		public synchronized RandomGenerator put(Process key, RandomGenerator value) {
			return super.put(key, value);
		}
	};
	static {
		// default RNG in cases where the GUI needs one outside of running a process (e.g. process
		// validation)
		GLOBAL_RANDOM_GENERATOR_MAP.put(null, new RandomGenerator(DEFAULT_SEED));
	}

	/** Initializes the random number generator without a seed. */
	private RandomGenerator() {
		super();
	}

	/** Initializes the random number generator with the given <code>seed</code> */
	public RandomGenerator(long seed) {
		super(seed);
	}

	// ================================================================================

	/** Returns the global random number generator for the given context/thread. */
	public static RandomGenerator getGlobalRandomGenerator() {
		RandomGenerator rg = GLOBAL_RANDOM_GENERATOR.get();
		if (rg != null) {
			return rg;
		}
		WeakReference<Process> wrp = THREAD_TO_PROCESS.get();
		if (wrp != null) {
			Process process = wrp.get();
			if (process != null) {
				RandomGenerator rng = GLOBAL_RANDOM_GENERATOR_MAP.get(process);
				if (rng != null) {
					return rng;
				}
			}
		}
		return GLOBAL_RANDOM_GENERATOR_MAP.get(null);
	}

	/**
	 * Returns the global random number generator if useLocalGenerator is false and a new
	 * RandomGenerator with the given seed if the seed is positive or zero. This way is is possible
	 * to allow for local random seeds. Operators like learners or validation operators should
	 * definitely make use of such a local random generator.
	 */
	public static RandomGenerator getRandomGenerator(boolean useLocalGenerator, int localSeed) {
		return useLocalGenerator ? getRandomGenerator(null, localSeed) : getGlobalRandomGenerator();
	}

	/**
	 * Returns the global random number generator if the seed is negative and a new RandomGenerator
	 * with the given seed if the seed is positive or zero. This way is is possible to allow for
	 * local random seeds. Operators like learners or validation operators should definitely make
	 * use of such a local random generator.
	 *
	 * @param process
	 *            Used to get the corresponding random generator
	 * @param seed
	 *            The seed to use in the RandomGenerator
	 * @return new RandomGenerator if seed >=0, globalRandomGenerator otherwise
	 */
	public static RandomGenerator getRandomGenerator(Process process, int seed) {
		if (seed < 0) {
			if (process == null) {
				return getGlobalRandomGenerator();
			}
			RandomGenerator rg = GLOBAL_RANDOM_GENERATOR_MAP.get(process);
			return rg != null ? rg : GLOBAL_RANDOM_GENERATOR_MAP.get(null);
		} else {
			return new RandomGenerator(seed);
		}
	}

	/**
	 * Instantiates the global random number generator for the given process and initializes it with
	 * the random number generator seed specified in the <code>global</code> section of the
	 * configuration file. Should be invoked before the process starts.
	 */
	public static void init(Process process) {
		long seed = DEFAULT_SEED;
		if (process != null) {
			try {
				seed = process.getRootOperator().getParameterAsInt(ProcessRootOperator.PARAMETER_RANDOM_SEED);
			} catch (UndefinedParameterError e) {
				// tries to read the general random seed
				// if no seed was specified (cannot happen) use default seed
				seed = DEFAULT_SEED;
			}
		}
		RandomGenerator rg;
		if (seed == USE_SYSTEM_TIME) {
			rg = new RandomGenerator();
		} else {
			rg = new RandomGenerator(seed);
		}
		GLOBAL_RANDOM_GENERATOR_MAP.put(process, rg);

		// don't have access to the class here, so reference by qualified name
		if (process != null && process.getClass().getName().equals(BACKGROUND_EXECUTION_PROCESS_CLASS_NAME)) {
			THREAD_TO_PROCESS.set(new WeakReference<>(process));
		} else {
			GLOBAL_RANDOM_GENERATOR.set(rg);
		}
	}

	/**
	 * This method returns a list of parameters usable to conveniently provide parameters for random
	 * generator use within operators
	 *
	 * @param operator
	 *            the operator
	 */
	public static List<ParameterType> getRandomGeneratorParameters(Operator operator) {
		List<ParameterType> types = new LinkedList<>();

		types.add(new ParameterTypeBoolean(PARAMETER_USE_LOCAL_RANDOM_SEED,
				"Indicates if a local random seed should be used.", false));

		ParameterType type = new ParameterTypeInt(PARAMETER_LOCAL_RANDOM_SEED, "Specifies the local random seed", 1,
				Integer.MAX_VALUE, 1992);
		type.registerDependencyCondition(new BooleanParameterCondition(operator, PARAMETER_USE_LOCAL_RANDOM_SEED, false,
				true));
		types.add(type);

		return types;
	}

	/**
	 * This method returns the appropriate RandomGenerator for the user chosen parameter
	 * combination. If the operator does not use a local random generator, use the appropriate
	 * process related random generator.
	 *
	 * @param operator
	 * @throws UndefinedParameterError
	 */
	public static RandomGenerator getRandomGenerator(Operator operator) throws UndefinedParameterError {
		if (operator.getParameterAsBoolean(PARAMETER_USE_LOCAL_RANDOM_SEED)) {
			return new RandomGenerator(operator.getParameterAsInt(PARAMETER_LOCAL_RANDOM_SEED));
		} else {
			return getRandomGenerator(operator.getProcess(), -1);
		}
	}

	// ================================================================================
	/**
	 * Returns the next pseudorandom, uniformly distributed <code>double</code> value between
	 * <code>lowerBound</code> and <code>upperBound</code> from this random number generator's
	 * sequence (exclusive of the interval endpoint values).
	 *
	 * @throws IllegalArgumentException
	 *             if upperBound < lowerBound
	 */
	public double nextDoubleInRange(double lowerBound, double upperBound) throws IllegalArgumentException {
		if (upperBound < lowerBound) {
			throw new IllegalArgumentException("RandomGenerator.nextDoubleInRange : the upper bound of the "
					+ "random number range should be greater than the lower bound.");
		}
		return nextDouble() * (upperBound - lowerBound) + lowerBound;
	}

	/**
	 * returns the next pseudorandom, uniformly distributed <code>long</code> value between
	 * <code>lowerBound</code> and <code>upperBound</code> from this random number generator's
	 * sequence (exclusive of the interval endpoint values).
	 */
	public long nextLongInRange(long lowerBound, long upperBound) {
		if (upperBound <= lowerBound) {
			throw new IllegalArgumentException("RandomGenerator.nextLongInRange : the upper bound of the "
					+ "random number range should be greater than the lower bound.");
		}
		return (long) (nextDouble() * (upperBound - lowerBound + 1)) + lowerBound;
	}

	/**
	 * Returns the next pseudorandom, uniformly distributed <code>int</code> value between
	 * <code>lowerBound</code> and <code>upperBound</code> from this random number generator's
	 * sequence (lower bound inclusive, upper bound exclusive).
	 */
	public int nextIntInRange(int lowerBound, int upperBound) {
		if (upperBound <= lowerBound) {
			throw new IllegalArgumentException("RandomGenerator.nextIntInRange : the upper bound of the "
					+ "random number range should be greater than the lower bound.");
		}
		return nextInt(upperBound - lowerBound) + lowerBound;
	}

	/** Returns a random String of the given length. */
	public String nextString(int length) {
		char[] chars = new char[length];
		for (int i = 0; i < chars.length; i++) {
			chars[i] = ALPHABET.charAt(nextInt(ALPHABET.length()));
		}
		return new String(chars);
	}

	/**
	 * Returns a randomly selected integer between 0 and the length of the given array. Uses the
	 * given probabilities to determine the index, all values in this array must sum up to 1.
	 */
	public int randomIndex(double[] probs) {
		double r = nextDouble();
		double sum = 0.0d;
		for (int i = 0; i < probs.length; i++) {
			sum += probs[i];
			if (r < sum) {
				return i;
			}
		}
		return probs.length - 1;
	}

	/**
	 * This method returns a randomly filled array of given length
	 *
	 * @param length
	 *            the length of the returned array
	 * @return the filled array
	 */
	public double[] nextDoubleArray(int length) {
		double[] values = new double[length];
		for (int i = 0; i < length; i++) {
			values[i] = nextDouble();
		}
		return values;
	}

	/** Returns a random date between the given ones. */
	public Date nextDateInRange(Date start, Date end) {
		return new Date(nextLongInRange(start.getTime(), end.getTime()));
	}

	/** Returns a set of integer within the given range and given size */
	public Set<Integer> nextIntSetWithRange(int lowerBound, int upperBound, int size) {
		if (upperBound <= lowerBound) {
			throw new IllegalArgumentException("RandomGenerator.nextIntInRange : the upper bound of the "
					+ "random number range should be greater than the lower bound.");
		}
		if (upperBound - lowerBound < size) {
			throw new IllegalArgumentException(
					"RandomGenerator.nextIntInRange : impossible to deliver the desired set of integeres --> range is too small.");
		}
		Set<Integer> set = new HashSet<>();
		while (set.size() < size) {
			set.add(nextIntInRange(lowerBound, upperBound));
		}
		return set;
	}
}
