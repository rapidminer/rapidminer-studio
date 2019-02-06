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
package com.rapidminer.gui.processeditor.results;

import java.util.function.Supplier;
import javax.swing.JComponent;

import com.rapidminer.example.ExampleSet;


/**
 * To be implemented to provide GUI components for the ResultTab action
 *
 * @author Andreas Timm
 * @since 9.1
 */
public interface ResultActionGuiProvider {

	/**
	 * Create a swing {@link JComponent} like a {@link javax.swing.AbstractButton} that executes further actions based on the given exampleSet.
	 *
	 * @param exampleSetSupplier
	 * 		the exampleSetSupplier to get() the potentially filtered {@link ExampleSet} from when executing this action
	 * @return an individual JComponent for user interaction
	 */
	JComponent createComponent(Supplier<ExampleSet> exampleSetSupplier);
}
