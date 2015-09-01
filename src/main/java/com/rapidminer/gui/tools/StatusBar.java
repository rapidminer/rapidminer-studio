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
package com.rapidminer.gui.tools;

import com.rapidminer.BreakpointListener;
import com.rapidminer.Process;
import com.rapidminer.ProcessListener;
import com.rapidminer.gui.processeditor.ProcessEditor;
import com.rapidminer.operator.IOContainer;
import com.rapidminer.operator.Operator;
import com.rapidminer.tools.Tools;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
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

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.Border;


/**
 * The status bar shows the currently applied operator and the time it needed so far. In addition,
 * the number of times the operator was already applied is also displayed. On the right side a clock
 * is shown which shows the system time. On the left side a colored bullet shows the current running
 * state similar to a traffic light. Please note that the clock thread must be manually started by
 * invoking {@link #startClockThread()} after construction.
 * 
 * @author Ingo Mierswa, Simon Fischer
 */
public class StatusBar extends JPanel implements ProcessEditor {

	private final ProcessListener processListener = new ProcessListener() {

		@Override
		public void processStarts(com.rapidminer.Process process) {
			rootOperator = null;
			specialText = null;
			SwingUtilities.invokeLater(new Runnable() {

				@Override
				public void run() {
					operatorLabel.setText("");
					trafficLightLabel.setIcon(RUNNING_ICON);
				}
			});
		}

		@Override
		public void processStartedOperator(final com.rapidminer.Process process, final Operator op) {
			if (rootOperator == null) {
				rootOperator = new OperatorEntry(op);
			} else {
				rootOperator.addOperator(op);
			}
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
			specialText = null;
			SwingUtilities.invokeLater(new Runnable() {

				@Override
				public void run() {
					operatorLabel.setText("");
					trafficLightLabel.setIcon(INACTIVE_ICON);
				}
			});
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
			trafficLightLabel.setIcon(RUNNING_ICON);
		}

		@Override
		public void breakpointReached(Process process, Operator op, IOContainer io, int location) {
			breakpoint = location;
			operatorLabel.setText("[" + op.getApplyCount() + "] " + op.getName() + ": breakpoint reached "
					+ BreakpointListener.BREAKPOINT_POS_NAME[breakpoint] + " operator, press resume...");
			trafficLightLabel.setIcon(STOPPED_ICON);
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

	private static final String INACTIVE_ICON_NAME = "24/bullet_ball_glass_grey.png";
	private static final String RUNNING_ICON_NAME = "24/bullet_ball_glass_green.png";
	private static final String STOPPED_ICON_NAME = "24/bullet_ball_glass_red.png";
	private static final String PENDING_ICON_NAME = "24/bullet_ball_glass_yellow.png";

	private static final Icon INACTIVE_ICON = SwingTools.createIcon(INACTIVE_ICON_NAME);
	private static final Icon RUNNING_ICON = SwingTools.createIcon(RUNNING_ICON_NAME);
	private static final Icon STOPPED_ICON = SwingTools.createIcon(STOPPED_ICON_NAME);
	private static final Icon PENDING_ICON = SwingTools.createIcon(PENDING_ICON_NAME);

	public static final int TRAFFIC_LIGHT_INACTIVE = 0;
	public static final int TRAFFIC_LIGHT_RUNNING = 1;
	public static final int TRAFFIC_LIGHT_STOPPED = 2;
	public static final int TRAFFIC_LIGHT_PENDING = 3;

	private static final long serialVersionUID = 1189363377612273467L;

	private final JLabel clockLabel = createLabel(getTime(), true);

	private final JLabel operatorLabel = createLabel("                         ", false);

	private OperatorEntry rootOperator = null;

	private final JLabel trafficLightLabel = new JLabel();

	private final JProgressBar progressBar = new JProgressBar();

	private int breakpoint = -1;

	private String specialText = null;

	public StatusBar() {
		this(true, true, true);
	}

	public StatusBar(boolean showClock, boolean showTrafficLight, boolean showProgressBar) {
		GridBagLayout layout = new GridBagLayout();
		setLayout(layout);
		GridBagConstraints constraints = new GridBagConstraints();

		constraints.fill = GridBagConstraints.HORIZONTAL;

		if (showTrafficLight) {
			trafficLightLabel.setIcon(INACTIVE_ICON);
			trafficLightLabel.setToolTipText("Indicates the current running state.");
			constraints.weightx = 0;
			layout.setConstraints(trafficLightLabel, constraints);
			add(trafficLightLabel);
		}

		constraints.weightx = 1;
		constraints.gridwidth = 1;
		layout.setConstraints(operatorLabel, constraints);
		add(operatorLabel);

		constraints.weightx = 0;
		constraints.fill = GridBagConstraints.NONE;
		constraints.gridwidth = GridBagConstraints.RELATIVE;
		if (showProgressBar) {
			progressBar.setStringPainted(true);
			progressBar.setEnabled(false);
			progressBar.setFont(progressBar.getFont().deriveFont(8.5f));
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

		if (showClock) {
			clockLabel.setToolTipText("The current system time.");
			constraints.weightx = 0;
			constraints.gridwidth = GridBagConstraints.REMAINDER;
			layout.setConstraints(clockLabel, constraints);
			add(clockLabel);
		}
	}

	public void startClockThread() {
		new Timer(1000, new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				clockLabel.setText(getTime());
				if ((specialText != null) && (specialText.trim().length() > 0)) {
					setText(specialText);
				} else {
					setText();
				}
			}
		}).start();
	}

	private static Border createBorder() {
		return new Border() {

			@Override
			public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
				Color highlight = c.getBackground().brighter().brighter();
				Color shadow = c.getBackground().darker().darker();
				Color oldColor = g.getColor();
				g.translate(x, y);
				g.setColor(shadow);
				g.drawLine(3, 0, 3, h - 2);
				g.drawLine(3, 0, w - 3, 0);
				g.setColor(highlight);
				g.drawLine(3, h - 2, w - 3, h - 2);
				g.drawLine(w - 3, 1, w - 3, h - 2);
				g.translate(-x, -y);
				g.setColor(oldColor);
			}

			@Override
			public Insets getBorderInsets(Component c) {
				return new Insets(1, 4, 2, 3);
			}

			@Override
			public boolean isBorderOpaque() {
				return false;
			}
		};
	}

	private static JLabel createLabel(String text, boolean border) {
		JLabel label = new JLabel(text);
		if (border) {
			label.setBorder(createBorder());
		}
		label.setFont(label.getFont().deriveFont(Font.PLAIN));
		return label;
	}

	private static String getTime() {
		return java.text.DateFormat.getTimeInstance().format(new java.util.Date());
	}

	public void setTrafficLight(int trafficLightState) {
		switch (trafficLightState) {
			case TRAFFIC_LIGHT_RUNNING:
				trafficLightLabel.setIcon(RUNNING_ICON);
				break;
			case TRAFFIC_LIGHT_STOPPED:
				trafficLightLabel.setIcon(STOPPED_ICON);
				break;
			case TRAFFIC_LIGHT_PENDING:
				trafficLightLabel.setIcon(PENDING_ICON);
				break;
			case TRAFFIC_LIGHT_INACTIVE:
			default:
				trafficLightLabel.setIcon(INACTIVE_ICON);
				break;
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

	private synchronized void setText(final String text) {
		operatorLabel.setText(text);
	}

	private void setText() {
		if (rootOperator != null) {
			setText(rootOperator.toString(rootOperator, System.currentTimeMillis()));
		} else {
			setText("");
		}
	}

	/** Sets the progress in the status bar. Executed on EDT. */
	public void setProgress(final String label, final int completed, final int total) {
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
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

	/** Only needed to keep track where we added ourselves as listeners. */
	private Process process;

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
}
