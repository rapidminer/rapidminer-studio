/**
 * Copyright (C) 2001-2020 by RapidMiner and the contributors
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
package com.rapidminer.gui.dnd;

import java.nio.file.Path;
import java.util.List;

import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorCreationException;


/**
 * Callback interface when a file from the file system is dropped into the process. Register in the {@link
 * DropFileIntoProcessActionRegistry}.
 *
 * @author Marco Boeck
 * @since 9.7
 */
public interface DropFileIntoProcessCallback {

	/**
	 * Only if {@link #willReturnOperator(Path)} returns {@code true} is this method called.
	 * <p>
	 * This method is triggered by the user UI action (e.g. dropping a .py file into the process canvas). This method
	 * must return quickly, as this happens in the EDT! Otherwise the entire Studio UI will freeze for the duration of
	 * this call.
	 * </p>
	 * <p>
	 * You must <strong>never</strong> read a large file here or do other time-consuming operations!
	 * </p>
	 *
	 * @param filePath the path to the file, never {@code null}
	 * @return the list of operators to add into the process, must not be {@code null}
	 */
	List<Operator> createAndConfigureOperatorsForDroppedFile(Path filePath) throws OperatorCreationException;

	/**
	 * Only if {@link #willReturnOperator(Path)} returns {@code false} is this method called.
	 * <p>
	 * Contrary to the {@link #createAndConfigureOperatorsForDroppedFile(Path) other method}, this one is started in a
	 * {@link com.rapidminer.gui.tools.ProgressThread}, so the UI does not block during this call. Note that this means
	 * that you must use {@link javax.swing.SwingUtilities#invokeLater(Runnable)} if you want to manipulate UI elements
	 * here.
	 *
	 * @param filePath the path to the file, never {@code null}
	 */
	void triggerAction(Path filePath);

	/**
	 * Defines whether for the given file, an operator instance will be returned or a custom action should be
	 * triggered.
	 * <p>
	 * When a callback is expected, this method is always triggered first, followed by either {@link
	 * #createAndConfigureOperatorsForDroppedFile(Path)} (if this returns {@code true}), or {@link #triggerAction(Path)}
	 * (if this returns {@code false}).
	 * </p>
	 *
	 * @param filePath the path to the file, never {@code null}
	 * @return {@code true} if a subsequent call to {@link #createAndConfigureOperatorsForDroppedFile(Path)} will return
	 * an operator instance, or if {@link #triggerAction(Path)} should be called instead
	 */
	boolean willReturnOperator(Path filePath);
}
