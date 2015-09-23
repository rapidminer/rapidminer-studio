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
package com.rapidminer.gui.operatortree;

import com.rapidminer.BreakpointListener;
import com.rapidminer.gui.dnd.OperatorTreeTransferHandler;
import com.rapidminer.gui.dnd.OperatorTreeTransferHandler.Position;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.operator.ExecutionUnit;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.ProcessSetupError;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Polygon;
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


/**
 * A renderer for operator tree cells that displays the operator's icon, name, class, breakpoints,
 * droplines and error hints.
 * 
 * @author Ingo Mierswa, Helge Homburg ingomierswa Exp $
 */
public class OperatorTreeCellRenderer extends DefaultTreeCellRenderer {

	/** The panel which will be used for the actual rendering. */
	private static class OperatorPanel extends JPanel {

		private static final String BREAKPOINT_BEFORE = "16/breakpoint_up.png";

		private static final String BREAKPOINT_AFTER = "16/breakpoint_down.png";

		private static final String BREAKPOINTS = "16/breakpoints.png";

		private static final String WARNINGS = "16/sign_warning.png";

		private static final long serialVersionUID = -7680223153786362865L;

		private static final Color SELECTED_COLOR = UIManager.getColor("Tree.selectionBackground");

		private static final Color BORDER_SELECTED_COLOR = UIManager.getColor("Tree.selectionBorderColor");

		private static final Color TEXT_SELECTED_COLOR = UIManager.getColor("Tree.selectionForeground");

		private static final Color TEXT_NON_SELECTED_COLOR = UIManager.getColor("Tree.textForeground");

		private static Icon breakpointBeforeIcon = null;

		private static Icon breakpointAfterIcon = null;

		private static Icon breakpointsIcon = null;

		private static Icon warningsIcon = null;

		static {
			// init breakpoint icons
			breakpointBeforeIcon = SwingTools.createIcon(BREAKPOINT_BEFORE);
			breakpointAfterIcon = SwingTools.createIcon(BREAKPOINT_AFTER);
			breakpointsIcon = SwingTools.createIcon(BREAKPOINTS);

			// init warnings icon
			warningsIcon = SwingTools.createIcon(WARNINGS);
		}

		private final JLabel iconLabel = new JLabel("");

		private final JLabel nameLabel = new JLabel("");

		private final JLabel classLabel = new JLabel("");

		private final JLabel breakpoint = new JLabel("");

		private final JLabel error = new JLabel("");

		private boolean isSelected = false;

		private boolean hasFocus = false;

		private OperatorTreeTransferHandler.Position dndMarker;

		private final int[] downArrowXPoints = { 4, 4, 6, 3, 0, 2, 2 };

		private final int[] downArrowYPoints = { 0, 4, 4, 7, 4, 4, 0 };

		private final int[] upArrowXPoints = { 3, 6, 4, 4, 2, 2, 0 };

		private final int[] upArrowYPoints = { 0, 3, 3, 7, 7, 3, 3 };

		private final Polygon upArrow = new Polygon(upArrowXPoints, upArrowYPoints, 7);

		private final Polygon downArrow = new Polygon(downArrowXPoints, downArrowYPoints, 7);

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
			nameC.insets = new Insets(0, 5, 0, 5); // new Insets(1, 1, 1, 1);
			namePanel.setLayout(nameLayout);

			nameLabel.setHorizontalAlignment(SwingConstants.LEFT);
			nameLabel.setFont(getFont().deriveFont(Font.PLAIN, 12));
			// nameLabel.setBorder(BorderFactory.createEmptyBorder(0, 3, 0, 3));
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

			if (tree instanceof OperatorTree) {
				dndMarker = ((OperatorTree) tree).getOperatorTreeTransferHandler().getMarkerPosition(operator);
			} else {
				dndMarker = Position.UNMARKED;
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
			error.setEnabled(operator.isEnabled());

			setEnabled(operator.isEnabled());
			setPreferredSize(new Dimension((int) (Math.max(nameLabel.getPreferredSize().getWidth(), classLabel
					.getPreferredSize().getWidth()) + 3 * 22), (int) (nameLabel.getPreferredSize().getHeight()
					+ classLabel.getPreferredSize().getHeight() + 4)));
		}

		private void paintUpperDropline(Graphics graphics) {
			Graphics g = graphics.create();
			g.setColor(SwingTools.LIGHT_BLUE);
			g.fillRect(0, 0, getWidth() - 1, 2);
			g.setColor(SwingTools.DARK_BLUE);
			g.drawRect(0, 0, getWidth() - 1, 2);

			g.translate(1, 3);
			g.setColor(SwingTools.LIGHT_BLUE);
			g.fillPolygon(upArrow);
			g.setColor(SwingTools.DARK_BLUE);
			g.drawPolygon(upArrow);

			g.translate(getWidth() - 10, 0);
			g.setColor(SwingTools.LIGHT_BLUE);
			g.fillPolygon(upArrow);
			g.setColor(SwingTools.DARK_BLUE);
			g.drawPolygon(upArrow);

			g.dispose();
		}

		private void paintLowerDropline(Graphics graphics) {
			Graphics g = graphics.create();
			g.setColor(SwingTools.LIGHT_BLUE);
			g.fillRect(0, getHeight() - 3, getWidth() - 1, 2);
			g.setColor(SwingTools.DARK_BLUE);
			g.drawRect(0, getHeight() - 3, getWidth() - 1, 2);

			g.translate(1, getHeight() - 11);
			g.setColor(SwingTools.LIGHT_BLUE);
			g.fillPolygon(downArrow);
			g.setColor(SwingTools.DARK_BLUE);
			g.drawPolygon(downArrow);

			g.translate(getWidth() - 10, 0);
			g.setColor(SwingTools.LIGHT_BLUE);
			g.fillPolygon(downArrow);
			g.setColor(SwingTools.DARK_BLUE);
			g.drawPolygon(downArrow);

			g.dispose();
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

			switch (dndMarker) {
				case ABOVE:
					paintUpperDropline(g);
					break;
				case BELOW:
					paintLowerDropline(g);
					break;
				case INTO:
					g.setColor(BORDER_SELECTED_COLOR);
					g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
					break;
			}
			super.paint(g);
		}

		/**
		 * This is a workaround to fix a Swing bug with causes the drag cursor to flicker. See
		 * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6700748 TODO: Occasionally check
		 * whether this bug was resolved by Sun.
		 */
		@Override
		public boolean isVisible() {
			return false;
		}
	}

	private static final long serialVersionUID = -8256080174651447518L;

	private static final Icon SUBPROCESS_ICON = SwingTools.createIcon("16/element_selection.png");

	private static final Border SUBPROCESS_BORDER = BorderFactory.createEmptyBorder(2, 2, 2, 2);

	private static final Border SUBPROCESS_MARKED_BORDER = BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(SwingTools.LIGHT_BLUE), BorderFactory.createEmptyBorder(2, 2, 2, 2));

	private final OperatorPanel operatorPanel = new OperatorPanel();

	public OperatorTreeCellRenderer() {}

	@Override
	public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded,
			boolean leaf, int row, boolean hasFocus) {
		// operatorPanel = new OperatorPanel();
		if (value instanceof Operator) {
			// operatorPanel = new OperatorPanel();
			operatorPanel.updateOperator(tree, (Operator) value, selected, hasFocus);
			SwingTools.setEnabledRecursive(operatorPanel, operatorPanel.isEnabled());
			return operatorPanel;
		} else if (value instanceof ExecutionUnit) {
			Component component = super.getTreeCellRendererComponent(tree, ((ExecutionUnit) value).getName(), selected,
					expanded, leaf, row, hasFocus);
			if (tree instanceof OperatorTree) {
				OperatorTreeTransferHandler.Position dndMarker = ((OperatorTree) tree).getOperatorTreeTransferHandler()
						.getMarkerPosition((ExecutionUnit) value);
				if (dndMarker != OperatorTreeTransferHandler.Position.UNMARKED) {
					((JComponent) component).setBorder(SUBPROCESS_MARKED_BORDER);
				} else {
					((JComponent) component).setBorder(SUBPROCESS_BORDER);
				}
			} else {
				((JComponent) component).setBorder(SUBPROCESS_BORDER);
			}
			((JLabel) component).setIcon(SUBPROCESS_ICON);
			SwingTools.setEnabledRecursive(component, tree.isEnabled());
			return component;
		} else {
			return super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
		}
	}
}
