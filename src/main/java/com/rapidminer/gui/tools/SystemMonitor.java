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

import com.rapidminer.gui.MainFrame;
import com.rapidminer.tools.Tools;
import com.vlsolutions.swing.docking.DockKey;
import com.vlsolutions.swing.docking.Dockable;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.GeneralPath;

import javax.swing.JPanel;
import javax.swing.Timer;


/**
 * The default system monitor which graphically displays the amount of used memory. Please note that
 * the upper line of the system monitor shows the amount of currently reserved memory (total).
 * Unfortunately it is (currently) not possible to ask for the processor usage with methods from
 * Java.
 * 
 * @author Ingo Mierswa, Simon Fischer
 */
public class SystemMonitor extends JPanel implements Dockable {

	private static final long serialVersionUID = -6860624220979494451L;

	private static final Color BACKGROUND = Color.WHITE;

	private static final Color TEXT_COLOR = SwingTools.VERY_DARK_BLUE;

	private static final Color MEMORY_COLOR = SwingTools.LIGHTEST_BLUE;

	private static final Color GRID_COLOR = Color.LIGHT_GRAY;

	private static final Color LINE_COLOR = SwingTools.VERY_DARK_BLUE;

	private static final int NUMBER_OF_MEASUREMENTS = 20;

	private static final int GRID_X = 10;

	private static final int GRID_Y = 10;

	private static final int MARGIN = 10;

	private static final int DELAY = 1000;

	private final long[] memory = new long[NUMBER_OF_MEASUREMENTS];

	private int currentMeasurement = 0;

	private double currentlyUsed = 0;

	private Color backgroundColor = BACKGROUND;
	private Color textColor = TEXT_COLOR;
	private Color memoryColor = MEMORY_COLOR;
	private Color gridColor = GRID_COLOR;
	private Color lineColor = LINE_COLOR;

	public SystemMonitor() {
		setBackground(backgroundColor);
	}

	public void startMonitorThread() {
		new Timer(DELAY, new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// memory
				SystemMonitor.this.currentlyUsed = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
				SystemMonitor.this.memory[SystemMonitor.this.currentMeasurement] = (long) currentlyUsed;
				SystemMonitor.this.currentMeasurement = (SystemMonitor.this.currentMeasurement + 1)
						% SystemMonitor.this.memory.length;
				SystemMonitor.this.repaint();
			}
		}).start();
	}

	protected final void setBackgroundColor(Color color) {
		this.backgroundColor = color;
		setBackground(backgroundColor);
	}

	protected final void setTextColor(Color color) {
		this.textColor = color;
	}

	protected final void setMemoryColor(Color color) {
		this.memoryColor = color;
	}

	protected final void setGridColor(Color color) {
		this.gridColor = color;
	}

	protected final void setLineColor(Color color) {
		this.lineColor = color;
	}

	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);

		GeneralPath path = new GeneralPath();

		Dimension d = getSize();
		int monitorWidth = (int) d.getWidth() - 2 * MARGIN;
		int monitorHeight = (int) d.getHeight() - 2 * MARGIN;

		long total = Runtime.getRuntime().totalMemory();

		path.moveTo(MARGIN, MARGIN + monitorHeight);
		for (int i = 0; i < memory.length; i++) {
			int index = (currentMeasurement + i) % memory.length;
			path.lineTo(MARGIN + i * monitorWidth / (memory.length - 1), MARGIN + monitorHeight - monitorHeight
					* memory[index] / total);
		}
		path.lineTo(MARGIN + monitorWidth, MARGIN + monitorHeight);
		path.closePath();

		Graphics2D g2d = (Graphics2D) g;

		g2d.setColor(gridColor);
		for (int x = 0; x < GRID_X + 1; x++) {
			g2d.drawLine(MARGIN + x * monitorWidth / GRID_X, MARGIN, MARGIN + x * monitorWidth / GRID_X, MARGIN
					+ monitorHeight);
		}
		for (int y = 0; y < GRID_Y + 1; y++) {
			g2d.drawLine(MARGIN, MARGIN + y * monitorHeight / GRID_Y, MARGIN + monitorWidth, MARGIN + y * monitorHeight
					/ GRID_Y);
		}

		Color currentMemoryColor = memoryColor;
		if (currentlyUsed > 0.2d * Runtime.getRuntime().maxMemory()) {
			double more = currentlyUsed - (0.2d * Runtime.getRuntime().maxMemory());
			double factor = more / (0.6d * Runtime.getRuntime().maxMemory());
			currentMemoryColor = getMemoryColor(Math.max(Math.min(1.0d, factor), 0.0d));
		}
		g2d.setColor(currentMemoryColor);
		g2d.fill(path);
		g2d.setColor(lineColor);
		g2d.draw(path);

		// text
		g2d.setColor(textColor);
		Font font = new Font("Courier", Font.PLAIN, 11);
		g2d.setFont(font);
		// String totalString = " Total: " + Tools.formatSizeInBytes(total);
		String maxString = Tools.formatSizeInBytes(total) + " used. Will use up to "
				+ Tools.formatSizeInBytes(Runtime.getRuntime().maxMemory()) + ".";
		// Rectangle2D totalBounds = g2d.getFontMetrics().getStringBounds(totalString, g2d);
		// Rectangle2D maxBounds = g2d.getFontMetrics().getStringBounds(totalString, g2d);
		int totalHeight = 2 * font.getSize() + 2 * MARGIN;
		// int totalWidth = (int)Math.max(totalBounds.getWidth(), maxBounds.getWidth()) + 3 *
		// MARGIN;
		if (totalHeight < getHeight()) {
			// g2d.drawString(totalString, MARGIN, MARGIN + (monitorHeight - font.getSize()));
			g2d.drawString(maxString, 2 * MARGIN, 2 * MARGIN + font.getSize());
		}
	}

	private Color getMemoryColor(double value) {
		if (Double.isNaN(value)) {
			return SwingTools.LIGHTEST_BLUE;
		}
		float[] minCol = Color.RGBtoHSB(SwingTools.LIGHTEST_BLUE.getRed(), SwingTools.LIGHTEST_BLUE.getGreen(),
				SwingTools.LIGHTEST_BLUE.getBlue(), null);
		float[] maxCol = Color.RGBtoHSB(SwingTools.LIGHTEST_RED.getRed(), SwingTools.LIGHTEST_RED.getGreen(),
				SwingTools.LIGHTEST_RED.getBlue(), null);
		double hColorDiff = maxCol[0] - minCol[0];
		double sColorDiff = maxCol[1] - minCol[1];
		double bColorDiff = maxCol[2] - minCol[2];
		return new Color(Color.HSBtoRGB((float) (minCol[0] + hColorDiff * value), (float) (minCol[1] + value * sColorDiff),
				(float) (minCol[2] + value * bColorDiff)));
	}

	public static final String SYSTEM_MONITOR_DOCK_KEY = "system_monitor";
	private final DockKey DOCK_KEY = new ResourceDockKey(SYSTEM_MONITOR_DOCK_KEY);
	{
		DOCK_KEY.setDockGroup(MainFrame.DOCK_GROUP_ROOT);
	}

	@Override
	public Component getComponent() {
		return this;
	}

	@Override
	public DockKey getDockKey() {
		return DOCK_KEY;
	}
}
