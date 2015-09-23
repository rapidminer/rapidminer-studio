/**
 * Copyright (C) 2001-2015 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 *      http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 */
package com.rapidminer.gui.properties;

import com.rapidminer.Process;
import com.rapidminer.gui.MainFrame;
import com.rapidminer.gui.processeditor.ProcessEditor;
import com.rapidminer.gui.tools.ResourceDockKey;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.operator.Operator;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.UndefinedParameterError;
import com.vlsolutions.swing.docking.DockKey;
import com.vlsolutions.swing.docking.Dockable;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;


/**
 * This is the most important implementation of PropertyTable. It is used to edit the parameters of
 * a single operator. Data changes in the table are immediately reflected in the operator. They
 * cannot be canceled. Depending on the parameter types the correct implementation of editors /
 * renderers is automatically used.
 * 
 * @author Ingo Mierswa, Simon Fischer
 */
public class OperatorPropertyTable extends DefaultPropertyTable implements Dockable, ProcessEditor {

	private static final long serialVersionUID = -4129852766426437419L;

	private static final Font MESSAGE_FONT = new Font("SansSerif", Font.PLAIN, 11);

	private static final Icon WARNING_ICON;

	static {
		WARNING_ICON = SwingTools.createIcon("16/sign_warning.png");
	}

	private transient Operator operator;

	private transient ParameterType[] parameterTypes;

	private boolean expertMode = false;

	private JLabel propertyMessageLabel = new JLabel();

	@Deprecated
	public OperatorPropertyTable(final MainFrame mainFrame) {
		super();
		this.propertyMessageLabel.setMinimumSize(new Dimension(0, 0));
		this.propertyMessageLabel.setFont(MESSAGE_FONT);
		this.propertyMessageLabel.setForeground(new Color(150, 150, 150));
		this.propertyMessageLabel.setOpaque(false);
		this.propertyMessageLabel.setBorder(BorderFactory.createEmptyBorder(3, 5, 3, 5));
		this.propertyMessageLabel.addMouseListener(new MouseListener() {

			@Override
			public void mouseReleased(MouseEvent e) {
				mainFrame.TOGGLE_EXPERT_MODE_ACTION.actionPerformed(null);
			}

			@Override
			public void mouseClicked(MouseEvent e) {}

			@Override
			public void mouseEntered(MouseEvent e) {}

			@Override
			public void mouseExited(MouseEvent e) {}

			@Override
			public void mousePressed(MouseEvent e) {}
		});

		setSelection(Collections.<Operator> emptyList());
	}

	public void toggleExpertMode() {
		expertMode = !expertMode;
		setSelection(operator);
	}

	public void setExpertMode(boolean expertMode) {
		this.expertMode = expertMode;
	}

	public boolean isExpertMode() {
		return expertMode;
	}

	public void refresh() {
		setSelection(this.operator);
	}

	public void stopCurrentEditing() {
		// this is necessary for stopping the current editing process and save the changes
		if (isEditing()) {
			cellEditor.stopCellEditing();
		}
	}

	@Override
	public void setSelection(List<Operator> selection) {
		setSelection(selection.isEmpty() ? null : selection.get(0));
	}

	public void setSelection(Operator operator) {
		// this is necessary for stopping the current editing process and save the changes
		stopCurrentEditing();

		int hidden = 0;
		this.operator = operator;
		if (operator != null) {
			Collection<ParameterType> parameters = operator.getParameters().getParameterTypes();
			List<ParameterType> viewableParameters = new LinkedList<ParameterType>();
			Iterator i = parameters.iterator();
			while (i.hasNext()) {
				ParameterType type = (ParameterType) i.next();
				if (!type.isHidden()) {
					if (expertMode || !type.isExpert()) {
						viewableParameters.add(type);
					} else {
						hidden++;
					}
				}
			}
			parameterTypes = new ParameterType[viewableParameters.size()];
			viewableParameters.toArray(parameterTypes);
		} else {
			parameterTypes = new ParameterType[0];
		}

		updateTableData(parameterTypes.length);

		for (int i = 0; i < parameterTypes.length; i++) {
			getModel().setValueAt(parameterTypes[i].getKey(), i, 0);
			Object value = parameterTypes[i].getDefaultValue();
			try {
				value = operator.getParameters().getParameter(parameterTypes[i].getKey());
			} catch (UndefinedParameterError e) {
				// tries to get non-default parameter (empty exception handling is ok)
			}
			getModel().setValueAt(value, i, 1);
		}

		updateEditorsAndRenderers();

		getModel().addTableModelListener(new TableModelListener() {

			@Override
			public void tableChanged(TableModelEvent e) {
				setValue(e.getFirstRow(), getModel().getValueAt(e.getFirstRow(), 1));
				getModel().removeTableModelListener(this);
				refresh();
			}
		});

		if (hidden == 1) {
			this.propertyMessageLabel.setText("There is 1 hidden parameter shown in expert mode only.");
			if (WARNING_ICON != null) {
				this.propertyMessageLabel.setIcon(WARNING_ICON);
			}
			this.propertyMessageLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
		} else if (hidden > 1) {
			this.propertyMessageLabel.setText("There are " + hidden + " hidden parameters shown in expert mode only.");
			if (WARNING_ICON != null) {
				this.propertyMessageLabel.setIcon(WARNING_ICON);
			}
			this.propertyMessageLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
		} else {
			this.propertyMessageLabel.setText(null);
			this.propertyMessageLabel.setIcon(null);
			this.propertyMessageLabel.setCursor(Cursor.getDefaultCursor());
		}
	}

	@Override
	public ParameterType getParameterType(int row) {
		return parameterTypes[row];
	}

	@Override
	public Operator getOperator(int row) {
		return operator;
	}

	private final DockKey DOCK_KEY = new ResourceDockKey("property_editor");

	private JPanel component;

	@Override
	public Component getComponent() {
		if (component == null) {
			component = new JPanel(new BorderLayout());
			component.add(new JScrollPane(this), BorderLayout.CENTER);
			component.add(propertyMessageLabel, BorderLayout.SOUTH);
		}
		return component;
	}

	@Override
	public DockKey getDockKey() {
		return DOCK_KEY;
	}

	@Override
	public void processChanged(Process process) {}

	@Override
	public void processUpdated(Process process) {}

}
