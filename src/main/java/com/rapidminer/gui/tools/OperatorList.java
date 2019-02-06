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
package com.rapidminer.gui.tools;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

import javax.swing.Action;
import javax.swing.JPopupMenu;

import com.rapidminer.gui.dnd.OperatorTransferHandler;
import com.rapidminer.gui.operatortree.actions.InfoOperatorAction;
import com.rapidminer.gui.properties.PropertyPanel;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorCreationException;
import com.rapidminer.operator.OperatorDescription;


/**
 * This class specifies a special JList which is capable of showing all available kinds of
 * RapidMiner operators, allowing the user to drag a copy of them into his own process tree. The
 * list elements must be of type {@link OperatorDescription}.
 *
 * @author Helge Homburg, Ingo Mierswa
 */
public class OperatorList extends MenuShortcutJList<OperatorDescription> implements MouseListener {

	private static final long serialVersionUID = -2719941529572427942L;

	// ======================================================================
	// Operator Menu Actions and Items
	// ======================================================================

	private transient final Action INFO_OPERATOR_ACTION = new InfoOperatorAction() {

		private static final long serialVersionUID = 1L;

		@Override
		protected Operator getOperator() {
			return selectedOperator;
		}

	};

	/** Creates a special CellRenderer for this class */
	private final OperatorListCellRenderer operatorDialogCellRenderer;

	/* The drag source of the NewOperatorDialog */
	// private DragSource dragSource;

	private transient Operator selectedOperator;

	/** Creates a new instance of OperatorList */
	public OperatorList() {
		this(false, true);
	}

	/** Creates a new instance of OperatorList */
	public OperatorList(boolean horizontalWrap, boolean coloredCellBackgrounds) {
		super(false);
		operatorDialogCellRenderer = new OperatorListCellRenderer(coloredCellBackgrounds);
		if (horizontalWrap) {
			setLayoutOrientation(HORIZONTAL_WRAP);
			setVisibleRowCount(-1);
		}
		setFixedCellHeight(PropertyPanel.VALUE_CELL_EDITOR_HEIGHT);
		setCellRenderer(operatorDialogCellRenderer);
		addMouseListener(this);

		setDragEnabled(true);
		setTransferHandler(new OperatorTransferHandler() {

			private static final long serialVersionUID = 1L;

			@Override
			protected List<Operator> getDraggedOperators() {
				return Collections.singletonList(OperatorList.this.getSelectedOperator());
			}
		});
	}

	public void setOperatorDescriptions(Vector<OperatorDescription> descriptions) {
		setListData(descriptions);
	}

	/** Returns the currently selected operator. */
	private Operator getSelectedOperator() {

		Point clickOrigin = getMousePosition();
		if (clickOrigin == null) {
			return null;
		}
		int selectedIndex = locationToIndex(clickOrigin);
		if (selectedIndex != -1) {
			setSelectedIndex(selectedIndex);
		}
		OperatorDescription selectedListElement = getSelectedValue();
		Operator selectedOperator = null;
		if (selectedListElement != null) {
			try {
				selectedOperator = selectedListElement.createOperatorInstance();
			} catch (OperatorCreationException ocE) {
				ocE.printStackTrace();
			}
		}
		return selectedOperator;
	}

	@Override
	public void mouseEntered(MouseEvent e) {}

	@Override
	public void mouseExited(MouseEvent e) {}

	@Override
	public void mouseClicked(MouseEvent e) {}

	@Override
	public void mousePressed(MouseEvent e) {}

	@Override
	public void mouseReleased(MouseEvent e) {
		selectedOperator = getSelectedOperator();
		evaluatePopup(e);
	}

	/**
	 * Checks if the given mouse event is a popup trigger and creates a new popup menu if necessary.
	 */
	private void evaluatePopup(MouseEvent e) {
		if (e.isPopupTrigger()) {
			createOperatorPopupMenu().show(this, e.getX(), e.getY());
		}
	}

	/** Creates a new popup menu for the selected operator. */
	private JPopupMenu createOperatorPopupMenu() {
		JPopupMenu menu = new JPopupMenu();
		menu.add(this.INFO_OPERATOR_ACTION);
		return menu;
	}
}
