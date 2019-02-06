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
package com.rapidminer.gui.properties.tablepanel.model;

import com.rapidminer.gui.properties.tablepanel.TablePanel;
import com.rapidminer.gui.properties.tablepanel.cells.interfaces.CellType;
import com.rapidminer.gui.properties.tablepanel.cells.interfaces.CellTypeCheckBox;
import com.rapidminer.gui.properties.tablepanel.cells.interfaces.CellTypeComboBox;
import com.rapidminer.gui.properties.tablepanel.cells.interfaces.CellTypeDate;
import com.rapidminer.gui.properties.tablepanel.cells.interfaces.CellTypeDateTime;
import com.rapidminer.gui.properties.tablepanel.cells.interfaces.CellTypeTextFieldDefault;
import com.rapidminer.gui.properties.tablepanel.cells.interfaces.CellTypeTextFieldNumerical;
import com.rapidminer.gui.properties.tablepanel.cells.interfaces.CellTypeTextFieldTime;

import java.util.Collection;
import java.util.List;

import javax.swing.event.TableModelEvent;
import javax.swing.table.TableModel;


/**
 * The model for a {@link TablePanel}.
 * 
 * @author Marco Boeck
 * 
 */
public interface TablePanelModel extends TableModel {

	/**
	 * Appends a new row to the model. Has to fire a {@link TableModelEvent}.
	 */
	public void appendRow();

	/**
	 * Removes a new row from the model. Will do nothing if there are no rows left. Has to fire a
	 * {@link TableModelEvent}.
	 * 
	 * @param rowIndex
	 */
	public void removeRow(int rowIndex);

	/**
	 * Returns the column class for a specific cell. Returned classes can include all interfaces
	 * from the cells subpackage, e.g.:
	 * <ul>
	 * <li>{@link CellTypeComboBox}</li>
	 * <li>{@link CellTypeTextFieldDefault}</li>
	 * <li>{@link CellTypeTextFieldNumerical}</li>
	 * <li>{@link CellTypeDate}</li>
	 * <li>{@link CellTypeTextFieldTime}</li>
	 * <li>{@link CellTypeDateTime}</li>
	 * <li>{@link CellTypeCheckBox}</li>
	 * </ul>
	 * 
	 * @param rowIndex
	 * @param columnIndex
	 * @return
	 */
	public Class<? extends CellType> getColumnClass(int rowIndex, int columnIndex);

	/**
	 * Returns a help text containing information about the selected cell. If no helptext is
	 * available, returns <code>null</code>.
	 * 
	 * @param rowIndex
	 * @param columnIndex
	 * @return
	 */
	public String getHelptextAt(int rowIndex, int columnIndex);

	/**
	 * Returns a {@link String} which exemplarily displays the syntax required for this cell. If no
	 * syntax help is available, returns <code>null</code>.
	 * 
	 * @param rowIndex
	 * @param columnIndex
	 * @return
	 */
	public String getSyntaxHelpAt(int rowIndex, int columnIndex);

	/**
	 * Returns a {@link List} of the possible values for a given cell. If the selected cells' class
	 * is not a {@link Collection} or {@link #isContentAssistPossibleForCell(int, int)} is
	 * <code>false</code>, returns <code>null</code>.
	 * 
	 * @param rowIndex
	 * @param columnIndex
	 * @return
	 * @throws IllegalArgumentException
	 *             if the row/column index is out of range
	 */
	public List<String> getPossibleValuesForCellOrNull(int rowIndex, int columnIndex) throws IllegalArgumentException;

	/**
	 * Returns <code>true</code> if there is content assist available for the specified cell (see
	 * {@link #getPossibleValuesForCellOrNull(int, int)} to get content assist values);
	 * <code>false</code> otherwise.
	 * 
	 * @param rowIndex
	 * @param columnIndex
	 * @return
	 */
	public boolean isContentAssistPossibleForCell(int rowIndex, int columnIndex);

	/**
	 * Returns <code>true</code> if multiple values can be entered into one cell. Can only return
	 * <code>true</code> when {@link #isContentAssistPossibleForCell(int, int)} also returns
	 * <code>true</code>, however this may return <code>false</code> anyway.
	 * 
	 * @param rowIndex
	 * @param columnIndex
	 * @return
	 */
	public boolean canCellHaveMultipleValues(int rowIndex, int columnIndex);

	/**
	 * Converts a given encoded {@link String} (for value cells where multiple values can be
	 * entered; see {@link #canCellHaveMultipleValues(int, int)} ) to a {@link List} of Strings
	 * which contains all separated values.
	 * 
	 * @param encodedValue
	 * @return
	 */
	public List<String> convertEncodedStringValueToList(String encodedValue);

	/**
	 * Converts a {@link List} of Strings to an encoded {@link String} (for value cells where
	 * multiple values can be entered; see {@link #canCellHaveMultipleValues(int, int)}).
	 * 
	 * @param listOfStrings
	 * @return
	 */
	public String encodeListOfStringsToValue(List<String> listOfStrings);

	/**
	 * Returns a {@link List} of {@link String} arrays where each list entry represents a row in
	 * this model.
	 * 
	 * @return
	 */
	public List<String[]> getRowTupels();

	/**
	 * Sets the rows of this model (Overrides any existing rows). Each list entry should represent a
	 * row in this model.
	 * 
	 * @return
	 */
	public void setRowTupels(List<String[]> tupelList);

}
