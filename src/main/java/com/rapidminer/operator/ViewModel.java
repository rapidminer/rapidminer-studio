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

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.set.ModelViewExampleSet;


/**
 * The view model is typically used for preprocessing models. With help of the
 * {@link ModelViewExampleSet} one can create a new view of the data by applying the necessary
 * transformations defined by this view model.
 * 
 * @author Sebastian Land
 */
public interface ViewModel extends Model {

	/**
	 * This method has to return a legal Attributes object containing every Attribute, the view
	 * should contain
	 * 
	 * @return The attribute object
	 */
	public abstract Attributes getTargetAttributes(ExampleSet viewParent);

	/**
	 * This method has to provide the attribute value mapping for the view. The views attributes
	 * will ask this method for their value.
	 * 
	 * @param targetAttribute
	 *            the attribute, which asks for his value
	 * @param value
	 *            the value the source attribute had in original data
	 * @return the value the attribute should have in target view
	 */
	public abstract double getValue(Attribute targetAttribute, double value);

}
