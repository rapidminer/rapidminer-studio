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
package com.rapidminer.gui.properties.celleditors.value;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.AbstractCellEditor;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.ScrollPaneConstants;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;

import com.rapidminer.Process;
import com.rapidminer.gui.properties.ExpressionPropertyDialog;
import com.rapidminer.gui.tools.ExtendedJScrollPane;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.operator.Operator;
import com.rapidminer.parameter.ParameterTypeExpression;


/**
 * Cell editor that contains an expression value
 *
 * @author Ingo Mierswa, Nils Woehler, Sabrina Kirstein
 *
 */
public class ExpressionValueCellEditor extends AbstractCellEditor implements PropertyValueCellEditor {

	private static final long serialVersionUID = 2355429695124754211L;

	/** defines whether the overflow indicator should be shown */
	private boolean showOverflow = false;

	/** button which opens the {@link ExpressionPropertyDialog} */
	private JButton button;

	/** the controlling process */
	private Process controllingProcess;

	/** used to get the line count of the current text */
	private RSyntaxTextArea currentExpression = new RSyntaxTextArea();

	/** margin of the expression */
	private Insets margin = new Insets(8, 5, 8, 5);

	/** background color of the overflow indicator */
	private static final Color LIGHTER_GRAY = new Color(237, 237, 237);

	/** name of the calculator icon */
	private static final String CALCULATOR_NAME = "calculator.png";

	/** icon showing the calculator */
	private static Icon CALCULATOR_ICON = null;

	static {
		CALCULATOR_ICON = SwingTools.createIcon("16/" + CALCULATOR_NAME);
	}

	/** panel which is returned */
	private final JPanel panel = new JPanel();

	/** {@link JEditorPane}, which contains the expression */
	private final JEditorPane editorPane = new JEditorPane();

	/** parameter type */
	private final ParameterTypeExpression type;

	/** layout of the panel */
	private final GridBagLayout gridBagLayout = new GridBagLayout();

	public ExpressionValueCellEditor(ParameterTypeExpression type) {

		this.type = type;
		panel.setLayout(gridBagLayout);
		panel.setToolTipText(type.getDescription());

		editorPane.setMargin(margin);
		editorPane.setAlignmentX(JEditorPane.LEFT_ALIGNMENT);
		editorPane.setToolTipText(type.getDescription());
		editorPane.addFocusListener(new FocusListener() {

			@Override
			public void focusLost(FocusEvent e) {
				// fire only if the focus didn't move to the button. If this check
				// would not be included, fireEditingStopped() would prevent the button's
				// ActionEvent from being fired. The user would have to click a second time to
				// trigger the button action.
				// Additionally, the event is only fired if the focus loss is permamently,
				// i.e. it is not fired if the user e.g. just switched to another window.
				// Otherwise any changes made after switching back to rapidminer would
				// not be saved for the same reasons as stated above.
				Component oppositeComponent = e.getOppositeComponent();
				if (oppositeComponent != button && !e.isTemporary()) {
					fireEditingStopped();
				}
				resetEditorPanePosition();
				updateOverflowIndicator();
			}

			@Override
			public void focusGained(FocusEvent e) {
				resetEditorPanePosition();
				// don't show the overflow indicator if the user is editing the field
				showOverflow = false;
				panel.repaint();
			}
		});
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.weighty = 1;
		c.weightx = 1;
		ExtendedJScrollPane scrollPane = new ExtendedJScrollPane(editorPane) {

			private static final long serialVersionUID = 1L;

			@Override
			public void paint(Graphics g) {
				super.paint(g);
				if (showOverflow) {
					drawOverflowIndicator(g, getPreferredSize().width);
				}
			};
		};
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
		scrollPane.setBorder(null);
		panel.add(scrollPane, c);

		button = new JButton(CALCULATOR_ICON);
		button.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				buttonPressed();
				resetEditorPanePosition();
				updateOverflowIndicator();
			}

		});
		button.addFocusListener(new FocusListener() {

			@Override
			public void focusLost(FocusEvent e) {
				if (e.getOppositeComponent() != editorPane && !e.isTemporary()) {
					fireEditingStopped();
				}
				resetEditorPanePosition();
				updateOverflowIndicator();
			}

			@Override
			public void focusGained(FocusEvent e) {

				resetEditorPanePosition();
				updateOverflowIndicator();
			}
		});
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.weightx = 0;
		panel.add(button, c);

	}

	/** Does nothing. */
	@Override
	public void setOperator(Operator operator) {
		this.controllingProcess = operator.getProcess();
	}

	@Override
	public boolean rendersLabel() {
		return false;
	}

	@Override
	public Object getCellEditorValue() {
		return editorPane.getText().trim().length() == 0 ? null : editorPane.getText().trim();
	}

	@Override
	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int col) {
		editorPane.setText(value == null ? "" : value.toString());
		resetEditorPanePosition();
		updateOverflowIndicator();
		return panel;
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
			int row, int column) {
		return getTableCellEditorComponent(table, value, isSelected, row, column);
	}

	@Override
	public boolean useEditorAsRenderer() {
		return false;
	}

	@Override
	public void activate() {
		button.doClick();
	}

	/** set the text of the expression and update the panel */
	protected void setText(String text) {
		if (text == null) {
			editorPane.setText("");
		} else {
			editorPane.setText(text);
		}
		resetEditorPanePosition();
		updateOverflowIndicator();
	}

	/** Reset the position of the caret, when the expression lost its focus */
	private void resetEditorPanePosition() {
		editorPane.setCaretPosition(0);
		editorPane.setMargin(margin);
	}

	/**
	 * Open the {@link ExpressionPropertyDialog} when the button was pressed
	 */
	private void buttonPressed() {
		Object value = getCellEditorValue();
		String initial = value == null ? null : value.toString();

		ExpressionPropertyDialog dialog = new ExpressionPropertyDialog(type, controllingProcess, initial);
		dialog.setVisible(true);
		if (dialog.isOk()) {
			setText(dialog.getExpression());
		}
		fireEditingStopped();
		resetEditorPanePosition();
		updateOverflowIndicator();
	}

	/**
	 * Draws indicator in case the expression text overflows on the y axis.
	 *
	 * @param g
	 *            the graphics context to draw upon
	 * @param maxX
	 *            maximal width
	 */
	private void drawOverflowIndicator(final Graphics g, int maxX) {

		int width = 25;
		int height = 10;
		int xOffset = 10;
		int stepSize = width / 5;
		int dotSize = 3;
		int x = maxX - width - xOffset;
		int y = button.getSize().height - height;
		g.setColor(LIGHTER_GRAY);

		g.fillRect(x, y, width, width);

		g.setColor(Color.GRAY);
		g.drawRoundRect(x, y, width, width, 5, 5);

		g.setColor(Color.BLACK);
		g.fillOval(x + stepSize, y + 4, dotSize, dotSize);
		g.fillOval(x + stepSize * 2, y + 4, dotSize, dotSize);
		g.fillOval(x + stepSize * 3, y + 4, dotSize, dotSize);

		g.dispose();
	}

	/**
	 * @return the line count of the current text
	 */
	private int getLineCount() {
		currentExpression.setText(editorPane.getText());
		return currentExpression.getLineCount();
	}

	/**
	 * Check whether the overflow indicator should be shown (if the expression has more than one
	 * line) and update the indicator
	 */
	private void updateOverflowIndicator() {
		if (getLineCount() > 1) {
			showOverflow = true;
			panel.repaint();
		} else {
			showOverflow = false;
			panel.repaint();
		}
	}

}
