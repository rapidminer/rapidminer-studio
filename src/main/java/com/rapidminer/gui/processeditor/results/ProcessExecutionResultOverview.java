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

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.text.DateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingConstants;

import com.rapidminer.Process;
import com.rapidminer.ProcessLocation;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.actions.ExtendedMouseClickedAdapter;
import com.rapidminer.gui.look.Colors;
import com.rapidminer.gui.look.RapidLookAndFeel;
import com.rapidminer.gui.look.borders.TextFieldBorder;
import com.rapidminer.gui.look.ui.ExtensionButtonUI;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.operator.IOObject;
import com.rapidminer.tools.Tools;


/**
 * Summarizes the result of a single process execution.
 *
 * @author Simon Fischer, Marco Boeck
 *
 */
public class ProcessExecutionResultOverview extends JPanel {

	private static final int MAX_PROCESS_NAME_LENGTH = 60;

	/** arrow icon with an arrow pointing up */
	private static final ImageIcon ICON_ARROW_UP = SwingTools.createIcon("16/" + "navigate_up.png");

	/** arrow icon with an arrow pointing down */
	private static final ImageIcon ICON_ARROW_DOWN = SwingTools.createIcon("16/" + "navigate_down.png");

	private static final long serialVersionUID = 1L;

	private final Action RESTORE_PROCESS = new ResourceAction(true, "resulthistory.restore_process") {

		private static final long serialVersionUID = 1L;

		@Override
		public void loggedActionPerformed(ActionEvent e) {
			try {
				if (RapidMinerGUI.getMainFrame().close()) {
					Process process = new Process(ProcessExecutionResultOverview.this.process);
					process.setProcessLocation(processLocation);
					RapidMinerGUI.getMainFrame().setProcess(process, true);
				}
			} catch (Exception e1) {
				SwingTools.showSimpleErrorMessage("cannot_restore_history_process", e1);
			}
		}
	};

	private final Action REMOVE_FROM_HISTORY = new ResourceAction(true, "resulthistory.remove") {

		private static final long serialVersionUID = 1L;

		@Override
		public void loggedActionPerformed(ActionEvent e) {
			parent.removeProcessOverview(ProcessExecutionResultOverview.this);
		}
	};

	private final JButton removeButton = new JButton(REMOVE_FROM_HISTORY);
	private final JButton restoreButton = new JButton(RESTORE_PROCESS);

	private final String process;

	private final ProcessLocation processLocation;

	private final Date terminationTime;
	private long executionTime;

	private boolean expanded = false;

	private final List<SingleResultOverview> results = new LinkedList<>();

	private final ResultOverview parent;

	private final JLabel labelExp;

	private final JLabel headerLabel;

	private JPanel resultPanel;

	public ProcessExecutionResultOverview(ResultOverview parent, Process process, List<IOObject> results,
			String statusMessage) {
		this.parent = parent;
		this.process = process.getRootOperator().getXML(false);
		this.processLocation = process.getProcessLocation();

		setOpaque(true);
		setBackground(Colors.WHITE);
		setBorder(new TextFieldBorder());
		setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

		setLayout(new BorderLayout());

		JPanel mainPanel = new JPanel(new GridBagLayout());
		mainPanel.setOpaque(true);
		mainPanel.setBackground(Colors.WHITE);
		labelExp = new JLabel(ICON_ARROW_DOWN, SwingConstants.RIGHT);
		labelExp.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
		add(labelExp, BorderLayout.WEST);
		add(mainPanel, BorderLayout.CENTER);

		try {
			executionTime = Long.parseLong(String.valueOf(process.getRootOperator().getValue("execution-time").getValue()));
		} catch (NumberFormatException e) {
			executionTime = System.currentTimeMillis() - process.getRootOperator().getStartTime();
		}
		this.terminationTime = new Date(process.getRootOperator().getStartTime() + executionTime);

		String processName;
		if (process.getProcessLocation() != null) {
			processName = process.getProcessLocation().getShortName();
		} else {
			processName = process.getRootOperator().getName();
		}
		StringBuilder b = new StringBuilder();
		b.append("<html><strong>");
		b.append(SwingTools.getShortenedDisplayName(processName, MAX_PROCESS_NAME_LENGTH));
		b.append("</strong> (");
		b.append(results.size());
		b.append(" results. <small>");
		b.append(statusMessage);
		b.append("</small>)<br/>Completed: ");
		b.append(DateFormat.getDateTimeInstance().format(terminationTime));
		b.append(" (execution time: ").append(Tools.formatDuration(executionTime)).append(")");
		b.append("</html>");
		headerLabel = new JLabel(b.toString());
		headerLabel.setFont(headerLabel.getFont().deriveFont(14f));
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 1.0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		mainPanel.add(headerLabel, gbc);

		restoreButton.setText(null);
		restoreButton.setContentAreaFilled(false);
		restoreButton.setUI(new ExtensionButtonUI());
		gbc.gridx += 1;
		gbc.weightx = 0.0;
		gbc.fill = GridBagConstraints.NONE;
		mainPanel.add(restoreButton, gbc);

		removeButton.setText(null);
		removeButton.setContentAreaFilled(false);
		removeButton.setUI(new ExtensionButtonUI());
		gbc.gridx += 1;
		mainPanel.add(removeButton, gbc);

		resultPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
		resultPanel.setOpaque(true);
		resultPanel.setBackground(Colors.WHITE);
		resultPanel.setCursor(Cursor.getDefaultCursor());
		gbc.gridx = 0;
		gbc.gridy += 1;
		gbc.weightx = 1.0;
		gbc.weighty = 1.0;
		gbc.fill = GridBagConstraints.BOTH;
		gbc.gridwidth = 3;
		mainPanel.add(resultPanel, gbc);

		int i = 0;
		for (IOObject result : results) {
			SingleResultOverview singleOverview = new SingleResultOverview(result, process, i);
			this.results.add(singleOverview);
			i++;
		}

		addMouseListener(new ExtendedMouseClickedAdapter() {

			@Override
			public void click(MouseEvent e) {
				setExpanded(!expanded);
			}

			@Override
			public void showContextMenu(Point point) {
				JPopupMenu menu = new JPopupMenu();
				menu.add(RESTORE_PROCESS);
				menu.add(REMOVE_FROM_HISTORY);
				menu.addSeparator();
				menu.add(ProcessExecutionResultOverview.this.parent.CLEAR_HISTORY_ACTION);
				menu.show(ProcessExecutionResultOverview.this, (int) point.getX(), (int) point.getY());
			}
		});
	}

	@Override
	public void paintComponent(Graphics g) {
		Graphics2D g2 = (Graphics2D) g.create();

		g2.setColor(Colors.WHITE);
		g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, RapidLookAndFeel.CORNER_DEFAULT_RADIUS,
				RapidLookAndFeel.CORNER_DEFAULT_RADIUS);

		g2.dispose();
	}

	/**
	 * Removes and adds the single result blocks according to the current width.
	 */
	private void redoLayout() {
		resultPanel.removeAll();
		if (expanded) {
			int curWidth = parent.getSize().width;
			int relevantWidth = SingleResultOverview.MIN_WIDTH + 25;
			int xCount = curWidth / relevantWidth;
			int yCount = (int) Math.ceil((double) results.size() / xCount);
			resultPanel.setLayout(new GridLayout(yCount, xCount));
			for (SingleResultOverview overview : results) {
				resultPanel.add(overview);
			}
		}
		revalidate();

	}

	/**
	 * Toggle the expansion state.
	 *
	 * @param expanded
	 */
	private void setExpanded(boolean expanded) {
		if (expanded != this.expanded) {
			this.expanded = expanded;
			if (expanded) {
				labelExp.setIcon(ICON_ARROW_UP);
			} else {
				labelExp.setIcon(ICON_ARROW_DOWN);
			}

			redoLayout();
		}
	}
}
