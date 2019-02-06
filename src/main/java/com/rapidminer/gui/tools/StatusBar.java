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

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSeparator;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import com.rapidminer.BreakpointListener;
import com.rapidminer.Process;
import com.rapidminer.ProcessListener;
import com.rapidminer.gui.look.RapidLookTools;
import com.rapidminer.gui.processeditor.ProcessEditor;
import com.rapidminer.operator.IOContainer;
import com.rapidminer.operator.Operator;
import com.rapidminer.tools.Tools;


/**
 * The status bar shows the currently applied operator and the time it needed so far. In addition,
 * the number of times the operator was already applied is also displayed.
 *
 * @author Ingo Mierswa, Simon Fischer
 */
public class StatusBar extends JPanel implements ProcessEditor {

	private final ProcessListener processListener = new ProcessListener() {

		@Override
		public void processStarts(com.rapidminer.Process process) {
			rootOperator = null;
			clearSpecialText();
		}

		@Override
		public void processStartedOperator(final com.rapidminer.Process process, final Operator op) {
			if (rootOperator == null) {
				rootOperator = new OperatorEntry(op);
			} else {
				rootOperator.addOperator(op);
			}
			setText();
		}

		@Override
		public void processFinishedOperator(com.rapidminer.Process process, final Operator op) {
			if (rootOperator != null) {
				rootOperator.removeOperator(op);
			}
		}

		@Override
		public void processEnded(Process process) {
			rootOperator = null;
			clearSpecialText();
		}
	};

	private final BreakpointListener breakpointListener = new BreakpointListener() {

		@Override
		public void resume() {
			breakpoint = -1;
			if (rootOperator != null) {
				setText();
			} else {
				operatorLabel.setText(" ");
			}
		}

		@Override
		public void breakpointReached(Process process, Operator op, IOContainer io, int location) {
			breakpoint = location;
			operatorLabel.setText("[" + op.getApplyCount() + "] " + op.getName() + ": breakpoint reached "
					+ BreakpointListener.BREAKPOINT_POS_NAME[breakpoint] + " operator, press resume...");
		}
	};

	private static class OperatorEntry {

		private final Collection<OperatorEntry> children = new LinkedList<>();

		private final Operator operator;

		public OperatorEntry(Operator operator) {
			this.operator = operator;
		}

		public void addOperator(Operator operator) {
			synchronized (children) {
				if (this.operator == operator.getParent()) {
					children.add(new OperatorEntry(operator));
				} else {
					for (OperatorEntry childEntry : children) {
						childEntry.addOperator(operator);
					}
				}
			}
		}

		public void removeOperator(Operator operator) {
			synchronized (children) {
				Iterator<OperatorEntry> iterator = children.iterator();
				while (iterator.hasNext()) {
					OperatorEntry childEntry = iterator.next();
					if (childEntry.getOperator() == operator) {
						iterator.remove();
					} else {
						childEntry.removeOperator(operator);
					}
				}
			}
		}

		public String toString(OperatorEntry entry, long time) {
			synchronized (children) {
				StringBuffer buffer = new StringBuffer();
				Operator currentOperator = entry.getOperator();
				buffer.append("[" + currentOperator.getApplyCount() + "] " + currentOperator.getName() + "  "
						+ Tools.formatDuration(time - currentOperator.getStartTime()));
				Iterator<OperatorEntry> iterator = children.iterator();
				if (iterator.hasNext()) {
					buffer.append(" \u21B3 ");
				}
				while (iterator.hasNext()) {
					OperatorEntry childEntry = iterator.next();
					if (children.size() > 1) {
						buffer.append(" ( ");
					}
					buffer.append(childEntry.toString(childEntry, time));
					if (children.size() > 1) {
						buffer.append(" ) ");
					}
					if (iterator.hasNext()) {
						buffer.append(" | ");
					}
				}
				return buffer.toString();
			}
		}

		public Operator getOperator() {
			return operator;
		}
	}

	private static final long serialVersionUID = 1L;

	private final JLabel operatorLabel = createLabel("                         ");

	private OperatorEntry rootOperator = null;

	private final JProgressBar progressBar;

	private int breakpoint = -1;

	private String specialText = null;

	/** Only needed to keep track where we added ourselves as listeners. */
	private Process process;

	public StatusBar() {
		this(true);
	}

	public StatusBar(boolean showProgressBar) {
		GridBagLayout layout = new GridBagLayout();
		setLayout(layout);
		GridBagConstraints constraints = new GridBagConstraints();

		constraints.fill = GridBagConstraints.HORIZONTAL;

		constraints.weightx = 0;
		constraints.gridwidth = 1;
		constraints.insets = new Insets(0, 5, 0, 0);
		BetaFeaturesIndicator indicator = new BetaFeaturesIndicator();
		JLabel modeLabel = indicator.getModeLabel();
		layout.setConstraints(modeLabel, constraints);
		add(modeLabel);

		constraints.weighty = 1;
		constraints.fill = GridBagConstraints.VERTICAL;
		JSeparator separator = indicator.getModeSeparator();
		layout.setConstraints(separator, constraints);
		add(separator);

		constraints.weightx = 1;
		constraints.weighty = 0;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		layout.setConstraints(operatorLabel, constraints);
		add(operatorLabel);

		constraints.weightx = 0;
		constraints.fill = GridBagConstraints.NONE;
		constraints.gridwidth = GridBagConstraints.RELATIVE;
		constraints.insets = new Insets(0, 0, 1, 3);

		progressBar = new JProgressBar() {

			private static final long serialVersionUID = 1L;

			@Override
			public Dimension getPreferredSize() {
				Dimension size = super.getPreferredSize();
				return new Dimension(500, size.height);
			}

			@Override
			public boolean isStringPainted() {
				// the status bar progress bar needs to be of the same height regardless so we
				// always pretend we paint the label because that affects its height
				return true;
			}
		};
		progressBar.putClientProperty(RapidLookTools.PROPERTY_PROGRESSBAR_COMPRESSED, true);
		if (showProgressBar) {
			progressBar.setStringPainted(false);
			progressBar.setEnabled(false);
			progressBar.setOpaque(false);
			progressBar.addMouseListener(new MouseAdapter() {

				@Override
				public void mouseClicked(MouseEvent e) {
					if (e.getClickCount() == 2) {
						if (!ProgressThreadDialog.getInstance().isVisible()) {
							ProgressThreadDialog.getInstance().setVisible(true, true);
						}
					}
				}
			});

			layout.setConstraints(progressBar, constraints);
			add(progressBar);
		}
	}

	public void setSpecialText(String specialText) {
		this.specialText = specialText;
		setText(this.specialText);
	}

	public void clearSpecialText() {
		this.specialText = null;
		setText();
	}

	public void startClockThread() {
		new Timer(1000, new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (specialText != null && !specialText.isEmpty()) {
					setText(specialText);
				} else {
					setText();
				}
			}
		}).start();
	}

	/** Sets the progress in the status bar. Executed on EDT. */
	public void setProgress(final String label, final int completed, final int total) {
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				if (label == null || label.isEmpty()) {
					progressBar.setStringPainted(false);
				} else {
					progressBar.setStringPainted(true);
				}

				progressBar.setIndeterminate(false);
				if (completed < total) {
					if (!progressBar.isEnabled()) {
						progressBar.setEnabled(true);
					}
					progressBar.setIndeterminate(false);
					progressBar.setString(label);
					progressBar.setMaximum(total);
					progressBar.setValue(completed);
				} else {
					progressBar.setString("");
					progressBar.setValue(0);
					progressBar.setEnabled(false);
				}
				progressBar.repaint();
			}
		});
	}

	/**
	 * Sets the progress in the status bar using intermediate mode. Executed on the EDT.
	 *
	 * @param label
	 * @param completed
	 * @param total
	 */
	public void setIndeterminateProgress(final String label, final int completed, final int total) {
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				if (completed < total) {
					if (!progressBar.isEnabled()) {
						progressBar.setEnabled(true);
					}
					progressBar.setIndeterminate(true);
					progressBar.setString(label);
					progressBar.setMaximum(total);
					progressBar.setValue(completed);
				} else {
					progressBar.setString("");
					progressBar.setValue(0);
					progressBar.setEnabled(false);
					progressBar.setIndeterminate(false);
				}
				progressBar.repaint();
			}
		});
	}

	@Override
	public void processChanged(Process process) {
		if (this.process != process) {
			if (this.process != null) {
				this.process.removeBreakpointListener(breakpointListener);
				this.process.getRootOperator().removeProcessListener(processListener);
			}
			this.process = process;
			if (this.process != null) {
				this.process.addBreakpointListener(breakpointListener);
				this.process.getRootOperator().addProcessListener(processListener);
			}
		}
	}

	@Override
	public void processUpdated(Process process) {}

	@Override
	public void setSelection(List<Operator> selection) {}

	private synchronized void setText(final String text) {
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				operatorLabel.setText(text);
			}
		});
	}

	private void setText() {
		if (rootOperator != null) {
			setText(rootOperator.toString(rootOperator, System.currentTimeMillis()));
		} else {
			setText("");
		}
	}

	private static JLabel createLabel(String text) {
		JLabel label = new JLabel(text);
		label.setFont(label.getFont().deriveFont(Font.PLAIN));
		return label;
	}
}
