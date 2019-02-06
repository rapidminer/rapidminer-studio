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
package com.rapidminer.gui.operatortree;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.tree.DefaultTreeCellRenderer;

import com.rapidminer.BreakpointListener;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.operator.ExecutionUnit;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.ProcessSetupError;
import com.rapidminer.tools.OperatorService;


/**
 * A renderer for operator tree cells that displays the operator's icon, name, class, breakpoints,
 * droplines and error hints.
 *
 * @author Ingo Mierswa, Helge Homburg ingomierswa Exp $
 */
public class OperatorTreeCellRenderer extends DefaultTreeCellRenderer {

	/** The panel which will be used for the actual rendering. */
	private static class OperatorPanel extends JPanel {

		private static final String BREAKPOINT_BEFORE = "16/breakpoint_left.png";

		private static final String BREAKPOINT_AFTER = "16/breakpoint_right.png";

		private static final String BREAKPOINTS = "16/breakpoints.png";

		private static final String WARNINGS = "16/sign_warning.png";

		private static final String BLACKLISTED = "16/lock.png";

		private static final long serialVersionUID = -7680223153786362865L;

		private static final Color SELECTED_COLOR = UIManager.getColor("Tree.selectionBackground");

		private static final Color BORDER_SELECTED_COLOR = UIManager.getColor("Tree.selectionBorderColor");

		private static final Color TEXT_SELECTED_COLOR = UIManager.getColor("Tree.selectionForeground");

		private static final Color TEXT_NON_SELECTED_COLOR = UIManager.getColor("Tree.textForeground");

		private static Icon breakpointBeforeIcon = SwingTools.createIcon(BREAKPOINT_BEFORE);

		private static Icon breakpointAfterIcon = SwingTools.createIcon(BREAKPOINT_AFTER);

		private static Icon breakpointsIcon = SwingTools.createIcon(BREAKPOINTS);

		private static Icon warningsIcon = SwingTools.createIcon(WARNINGS);

		private static Icon blacklistedIcon = SwingTools.createIcon(BLACKLISTED);

		private final JLabel iconLabel = new JLabel("");

		private final JLabel nameLabel = new JLabel("");

		private final JLabel classLabel = new JLabel("");

		private final JLabel breakpoint = new JLabel("");

		private final JLabel error = new JLabel("");

		private boolean isSelected = false;

		private boolean hasFocus = false;

		public OperatorPanel() {
			setBackground(new java.awt.Color(0, 0, 0, 0));
			setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

			GridBagLayout layout = new GridBagLayout();
			setLayout(layout);
			GridBagConstraints c = new GridBagConstraints();
			c.anchor = GridBagConstraints.LINE_START;
			c.fill = GridBagConstraints.NONE;
			c.weightx = 0;

			layout.setConstraints(iconLabel, c);
			add(iconLabel);

			// name panel
			JPanel namePanel = new JPanel();
			namePanel.setBackground(new java.awt.Color(0, 0, 0, 0));
			GridBagLayout nameLayout = new GridBagLayout();
			GridBagConstraints nameC = new GridBagConstraints();
			nameC.fill = GridBagConstraints.BOTH;
			nameC.insets = new Insets(0, 5, 0, 5);
			namePanel.setLayout(nameLayout);

			nameLabel.setHorizontalAlignment(SwingConstants.LEFT);
			nameLabel.setFont(getFont().deriveFont(Font.PLAIN, 12));
			nameC.gridwidth = GridBagConstraints.REMAINDER;
			nameLayout.setConstraints(nameLabel, nameC);
			namePanel.add(nameLabel);

			classLabel.setHorizontalAlignment(SwingConstants.LEFT);
			classLabel.setFont(getFont().deriveFont(Font.PLAIN, 10));
			nameLayout.setConstraints(classLabel, nameC);
			namePanel.add(classLabel);

			c.weightx = 1;
			add(namePanel, c);

			c.gridwidth = GridBagConstraints.RELATIVE;
			c.weightx = 0;

			layout.setConstraints(breakpoint, c);
			add(breakpoint);

			c.gridwidth = GridBagConstraints.REMAINDER;

			layout.setConstraints(error, c);
			add(error);
		}

		public void updateOperator(JTree tree, Operator operator, boolean selected, boolean focus) {
			this.isSelected = selected;
			this.hasFocus = focus;

			if (selected) {
				nameLabel.setForeground(TEXT_SELECTED_COLOR);
				classLabel.setForeground(TEXT_SELECTED_COLOR);
			} else {
				nameLabel.setForeground(TEXT_NON_SELECTED_COLOR);
				classLabel.setForeground(TEXT_NON_SELECTED_COLOR);
			}

			OperatorDescription descr = operator.getOperatorDescription();
			Icon icon = descr.getSmallIcon();
			if (icon != null) {
				iconLabel.setIcon(icon);
			} else {
				iconLabel.setIcon(null);
			}
			iconLabel.setEnabled(operator.isEnabled());

			nameLabel.setText(operator.getName());
			nameLabel.setEnabled(operator.isEnabled());
			classLabel.setText(descr.getName());
			classLabel.setEnabled(operator.isEnabled());

			// ICONS
			// breakpoints
			if (operator.hasBreakpoint(BreakpointListener.BREAKPOINT_BEFORE)) {
				breakpoint.setIcon(breakpointBeforeIcon);
			} else if (operator.hasBreakpoint(BreakpointListener.BREAKPOINT_AFTER)) {
				breakpoint.setIcon(breakpointAfterIcon);
			} else {
				breakpoint.setIcon(null);
			}

			if (operator.hasBreakpoint(BreakpointListener.BREAKPOINT_BEFORE)
					&& operator.hasBreakpoint(BreakpointListener.BREAKPOINT_AFTER)) {
				breakpoint.setIcon(breakpointsIcon);
			}
			breakpoint.setEnabled(operator.isEnabled());

			// errors
			List<ProcessSetupError> errors = operator.getErrorList();
			if (errors.size() > 0) {
				error.setIcon(warningsIcon);
			} else {
				error.setIcon(null);

				String descriptionText = descr.getLongDescriptionHTML();
				if (descriptionText == null) {
					descriptionText = descr.getShortDescription();
				}
			}

			if (OperatorService.isOperatorBlacklisted(descr.getKey())) {
				error.setIcon(blacklistedIcon);
			}

			error.setEnabled(operator.isEnabled());

			setEnabled(operator.isEnabled());
			setPreferredSize(new Dimension((int) (Math.max(nameLabel.getPreferredSize().getWidth(), classLabel
					.getPreferredSize().getWidth()) + 3 * 22), (int) (nameLabel.getPreferredSize().getHeight()
							+ classLabel.getPreferredSize().getHeight() + 4)));
		}

		@Override
		public void paint(Graphics g) {
			if (isSelected) {
				g.setColor(SELECTED_COLOR);
				g.fillRect(0, 0, getWidth(), getHeight());
			}

			if (hasFocus) {
				g.setColor(BORDER_SELECTED_COLOR);
				g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
			}

			super.paint(g);
		}

	}

	private static final long serialVersionUID = 1L;

	private static final Icon SUBPROCESS_ICON = SwingTools.createIcon("16/element_selection.png");

	private static final Border SUBPROCESS_BORDER = BorderFactory.createEmptyBorder(2, 2, 2, 2);

	private final OperatorPanel operatorPanel = new OperatorPanel();

	public OperatorTreeCellRenderer() {}

	@Override
	public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded,
			boolean leaf, int row, boolean hasFocus) {
		if (value instanceof Operator) {
			operatorPanel.updateOperator(tree, (Operator) value, selected, hasFocus);
			SwingTools.setEnabledRecursive(operatorPanel, operatorPanel.isEnabled());
			return operatorPanel;
		} else if (value instanceof ExecutionUnit) {
			Component component = super.getTreeCellRendererComponent(tree, ((ExecutionUnit) value).getName(), selected,
					expanded, leaf, row, hasFocus);
			((JComponent) component).setBorder(SUBPROCESS_BORDER);
			((JLabel) component).setIcon(SUBPROCESS_ICON);
			SwingTools.setEnabledRecursive(component, tree.isEnabled());
			return component;
		} else {
			return super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
		}
	}
}
