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
package com.rapidminer.gui.processeditor.results;

import com.rapidminer.Process;
import com.rapidminer.ProcessLocation;
import com.rapidminer.gui.RapidMinerGUI;
import com.rapidminer.gui.look.ui.ExtensionButtonUI;
import com.rapidminer.gui.tools.ResourceAction;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.operator.IOObject;
import com.rapidminer.tools.Tools;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;


/**
 * Summarizes the result of a single process execution.
 * 
 * @author Simon Fischer
 * 
 */
public class ProcessExecutionResultOverview extends JPanel {

	private static final int MAX_PROCESS_NAME_LENGTH = 60;

	private static final Color[][] COLORS = { { SwingTools.LIGHTEST_BLUE, SwingTools.LIGHT_BLUE },
			{ SwingTools.LIGHTEST_YELLOW, SwingTools.LIGHT_YELLOW } };
	private static final Map<String, Color[]> processLocationToColor = new HashMap<>();
	private static int lastUsedProcessColor = 0;

	private final Action RESTORE_PROCESS = new ResourceAction(true, "resulthistory.restore_process") {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(ActionEvent e) {
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
		public void actionPerformed(ActionEvent e) {
			parent.removeProcessOverview(ProcessExecutionResultOverview.this);
		}
	};

	private final JButton removeButton = new JButton(REMOVE_FROM_HISTORY);
	private final JButton restoreButton = new JButton(RESTORE_PROCESS);

	private static final long serialVersionUID = 1L;

	private final String process;

	private final ProcessLocation processLocation;

	private final Date terminationTime;
	private final long executionTime;

	private boolean expanded = false;

	private final List<SingleResultOverview> results = new LinkedList<>();

	private final ResultOverview parent;

	private final JLabel headerLabel;

	private int columns;
	private int rows;

	public ProcessExecutionResultOverview(ResultOverview parent, Process process, List<IOObject> results,
			String statusMessage) {
		setOpaque(false);
		setLayout(null);
		setBackground(Color.WHITE);

		restoreButton.setUI(new ExtensionButtonUI());
		restoreButton.setMargin(new Insets(0, 0, 0, 0));
		restoreButton.setText(null);
		restoreButton.setOpaque(false);
		add(restoreButton);

		removeButton.setUI(new ExtensionButtonUI());
		removeButton.setMargin(new Insets(0, 0, 0, 0));
		removeButton.setText(null);
		removeButton.setOpaque(false);
		add(removeButton);

		Color[] colors = getColorFor(process);
		RapidBorder border = new RapidBorder(colors[1], 10, 18);
		setBackground(colors[0]);
		setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(5, 5, 10, 5, Color.WHITE), border));

		this.parent = parent;
		this.process = process.getRootOperator().getXML(false);
		this.processLocation = process.getProcessLocation();
		this.terminationTime = new Date();
		executionTime = System.currentTimeMillis() - process.getRootOperator().getStartTime();

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
		add(headerLabel);
		headerLabel.setBounds(10, 2, 800, 40);

		int i = 0;
		for (IOObject result : results) {
			SingleResultOverview singleOverview = new SingleResultOverview(result, process, i);
			this.results.add(singleOverview);
			i++;
		}

		addMouseListener(new MouseAdapter() {

			@Override
			public void mouseReleased(MouseEvent e) {
				if (e.isPopupTrigger()) {
					showContextMenu(e.getPoint());
				}
			}

			@Override
			public void mousePressed(MouseEvent e) {
				if (e.isPopupTrigger()) {
					showContextMenu(e.getPoint());
				}
			}

			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.isPopupTrigger()) {
					showContextMenu(e.getPoint());
				} else {
					setExpanded(!expanded);
				}
			}
		});
	}

	private void setExpanded(boolean expanded) {
		if (expanded != this.expanded) {
			this.expanded = expanded;
			for (SingleResultOverview overview : results) {
				if (expanded) {
					add(overview);
				} else {
					remove(overview);
				}
			}

			getParent().doLayout();
			revalidate();
		}
	}

	void recomputeLayout() {
		int parentWidth = parent.getWidth();
		Dimension totalSize;
		if (!expanded) {
			totalSize = new Dimension(parentWidth - 30, 50);
		} else {
			int freeWidth = parentWidth - 30;
			this.columns = (int) Math.floor((double) freeWidth / SingleResultOverview.MIN_WIDTH);
			this.rows = (int) Math.ceil((double) results.size() / columns);
			int individualWidth = SingleResultOverview.MIN_WIDTH - 10;

			int i = 0;
			for (SingleResultOverview overview : results) {
				int row = i / columns;
				int col = i % columns;
				overview.setBounds(10 + col * individualWidth, 45 + row * SingleResultOverview.MIN_HEIGHT,
						individualWidth - 5, SingleResultOverview.MIN_HEIGHT - 10);
				i++;
			}
			totalSize = new Dimension(parentWidth - 30, rows * SingleResultOverview.MIN_HEIGHT + 50);
		}

		restoreButton.setBounds(new Rectangle((int) totalSize.getWidth() - 48 - 5, 6, (int) restoreButton.getPreferredSize()
				.getWidth(), (int) restoreButton.getPreferredSize().getHeight()));

		removeButton.setBounds(new Rectangle((int) totalSize.getWidth() - 24 - 5, 6, (int) restoreButton.getPreferredSize()
				.getWidth(), (int) restoreButton.getPreferredSize().getHeight()));

		setSize(totalSize);
		setPreferredSize(totalSize);
		setMinimumSize(totalSize);
		setMaximumSize(totalSize);
	}

	private void showContextMenu(Point point) {
		JPopupMenu menu = new JPopupMenu();
		menu.add(RESTORE_PROCESS);
		menu.add(REMOVE_FROM_HISTORY);
		menu.add(parent.CLEAR_HISTORY_ACTION);
		menu.show(this, (int) point.getX(), (int) point.getY());
	}

	private Color[] getColorFor(Process process) {
		String loc = process.getProcessLocation() == null ? null : process.getProcessLocation().toString();

		Color[] colors = processLocationToColor.get(loc);
		if (colors == null) {
			colors = COLORS[((lastUsedProcessColor++) % COLORS.length)];
			processLocationToColor.put(loc, colors);
		}
		return colors;
	}
}
