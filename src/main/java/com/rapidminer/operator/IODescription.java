/**
 * Copyright (C) 2001-2019 by RapidMiner and the contributors
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
package com.rapidminer.operator;

/**
 * This interface describes the (expected) input and (guaranteed) output classes of operators.
 * 
 * @author Ingo Mierswa
 */
public interface IODescription {

	/** Returns the classes that are expected as input. */
	public abstract Class<?>[] getInputClasses();

	/**
	 * Returns the output classes dependent on the outputBehaviour
	 * <ul>
	 * <li><tt>PASS_UNUSED_INPUT_TO_OUTPUT:</tt>output classes are the classes used in the
	 * constructor plus those classes in <tt>input[]</tt> that were not consumed. Classes are
	 * supposed to be consumed by the operator if they find a matching class in the input classes
	 * used in the constructor (which can be a superclass or interface)
	 * <li><tt>DELETE_UNUSED_INPUT:</tt>output classes are exactly those classes used in the
	 * constructor.
	 * </ul>
	 * In either case the output classes precede the unused input classes. Their order is conserved.
	 */
	public abstract Class<?>[] getOutputClasses(Class<?>[] input, Operator operator) throws IllegalInputException;

}
