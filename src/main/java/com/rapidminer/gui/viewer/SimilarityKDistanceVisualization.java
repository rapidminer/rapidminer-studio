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
package com.rapidminer.gui.viewer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.gui.look.Colors;
import com.rapidminer.gui.plotter.PlotterAdapter;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.math.similarity.DistanceMeasure;


/**
 * A k-distance visualization for similarities.
 *
 * WARNING: Although extending PlotterAdapter, this is no real plotter! It does not support
 * PlotterSettings or anything else needed for a "real" plotter.
 *
 * @author Peter B. Volk, Michael Wurst, Ingo Mierswa
 */
public class SimilarityKDistanceVisualization extends PlotterAdapter implements ActionListener {

	private static final long serialVersionUID = -3774235976141625821L;

	private static final int DEFAULT_K_NUMBER = 3;

	private static final int LABEL_MARGIN_X = 50;

	private static final int LABEL_MARGIN_Y = 15;

	private static final Font SCALED_LABEL_FONT = LABEL_FONT.deriveFont(AffineTransform.getScaleInstance(1, -1));

	private double minX, maxX, minY, maxY, xTicSize, yTicSize;

	private DistanceMeasure measure = null;

	private ExampleSet exampleSet = null;

	private JTextField k_distance_jtext;

	private int updatePanelHeight;

	private LinkedList<Double> kDistanceValues;

	private int k = DEFAULT_K_NUMBER;

	public SimilarityKDistanceVisualization(DistanceMeasure measure, ExampleSet exampleSet) {
		super(null);
		this.measure = measure;
		this.exampleSet = exampleSet;
		setBackground(Color.white);

		setLayout(new BorderLayout());
		JLabel label = null;
		label = new JLabel("k : ");

		JButton updateButton = new JButton("Update");
		updateButton.addActionListener(this);

		k_distance_jtext = new JTextField();
		k_distance_jtext.setText(Integer.toString(this.k));
		k_distance_jtext.setColumns(5);

		JPanel updatePanel = new JPanel(new FlowLayout());
		updatePanel.setOpaque(true);
		updatePanel.setBackground(Colors.WHITE);
		updatePanel.add(label);
		updatePanel.add(k_distance_jtext);
		updatePanel.add(updateButton);

		JPanel updatePanelAligned = new JPanel(new BorderLayout());
		updatePanelAligned.setOpaque(true);
		updatePanelAligned.setBackground(Colors.WHITE);
		updatePanelAligned.add(updatePanel, BorderLayout.CENTER);
		updatePanelAligned.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

		add(updatePanelAligned, BorderLayout.NORTH);
		this.updatePanelHeight = updatePanelAligned.getHeight();
	}

	@Override
	public JComponent getOptionsComponent(int index) {
		JLabel label = new JLabel("K:");
		label.setToolTipText("Set the k which should be displayed.");
		return label;

	}

	/** Indicates how many bins should be used for the distribution plot. */
	public void setK(int k) {
		this.k = k;
		repaint();
	}

	protected void prepareData() {
		this.minX = Double.POSITIVE_INFINITY;
		this.maxX = Double.NEGATIVE_INFINITY;
		this.minY = Double.POSITIVE_INFINITY;
		this.maxY = Double.NEGATIVE_INFINITY;
		// expection handling. The k is larger than the number of points in the map

		int numberOfExamples = exampleSet.size();
		if (this.k >= numberOfExamples) {
			// LogService.getGlobal().log("KDistanceVisualization: k is larger than the number of examples",
			// LogService.WARNING);
			LogService.getRoot().log(Level.WARNING,
					"com.rapidminer.gui.viewer.SimilarityKDistanceVisualization.k_is_larger_than_the_numer_of_examples");
			k = numberOfExamples;
		}

		this.minX = 0;
		this.maxX = numberOfExamples;

		this.kDistanceValues = new LinkedList<Double>();
		for (Example example : exampleSet) {
			List<Double> sortList = new ArrayList<Double>();
			for (Example compExample : exampleSet) {
				sortList.add(measure.calculateDistance(example, compExample));
			}
			// sort list
			Collections.sort(sortList);
			double currentValue = sortList.get(this.k - 1);
			this.minY = Math.min(minY, currentValue);
			this.maxY = Math.max(maxY, currentValue);
			this.kDistanceValues.add(currentValue);
		}
		Collections.sort(this.kDistanceValues);
		Collections.reverse(this.kDistanceValues);

		xTicSize = getNumericalTicSize(minX, maxX);
		yTicSize = getNumericalTicSize(minY, maxY);
		minX = Math.floor(minX / xTicSize) * xTicSize;
		maxX = Math.ceil(maxX / xTicSize) * xTicSize;
		minY = Math.floor(minY / yTicSize) * yTicSize;
		maxY = Math.ceil(maxY / yTicSize) * yTicSize;
	}

	protected void drawPoints(Graphics2D g, double dx, double dy, double sx, double sy) {
		if (this.kDistanceValues != null && this.kDistanceValues.size() <= 2) {
			// LogService.getGlobal().log("KDistanceVisualization: No values in value map",
			// LogService.WARNING);
			LogService.getRoot().log(Level.WARNING,
					"com.rapidminer.gui.viewer.SimilarityKDistanceVisualization.no_values_in_value_map");

			return;
		}
		if (this.kDistanceValues != null) {
			double offset = 0;
			for (double dist : kDistanceValues) {
				drawPoint(g, offset + dx, (dist + dy) * sy, Color.RED, Color.BLACK);
				offset += sx;
			}
		}
	}

	private void drawGrid(Graphics2D g, double dx, double dy, double sx, double sy) {
		DecimalFormat format = new DecimalFormat("0.00E0");
		g.setFont(SCALED_LABEL_FONT);
		int numberOfXTics = (int) Math.ceil((maxX - minX) / xTicSize) + 1;
		for (int i = 0; i < numberOfXTics; i++) {
			drawVerticalTic(g, i, format, dx, dy, sx, sy);
		}

		int numberOfYTics = (int) Math.ceil((maxY - minY) / yTicSize) + 1;
		for (int i = 0; i < numberOfYTics; i++) {
			drawHorizontalTic(g, i, format, dx, dy, sx, sy);
		}
	}

	private void drawVerticalTic(Graphics2D g, int ticNumber, DecimalFormat format, double dx, double dy, double sx,
			double sy) {
		double x = ticNumber * xTicSize + minX;
		g.setColor(GRID_COLOR);
		g.draw(new Line2D.Double((x + dx) * sx, (minY + dy) * sy, (x + dx) * sx, (maxY + dy) * sy));
		g.setColor(Color.black);
	}

	private void drawHorizontalTic(Graphics2D g, int ticNumber, DecimalFormat format, double dx, double dy, double sx,
			double sy) {
		double y = ticNumber * yTicSize + minY;
		g.setColor(GRID_COLOR);
		g.draw(new Line2D.Double((minX + dx) * sx, (y + dy) * sy, (maxX + dx) * sx, (y + dy) * sy));
		g.setColor(Color.black);
		String label = format.format(y) + " ";
		Rectangle2D stringBounds = SCALED_LABEL_FONT.getStringBounds(label, g.getFontRenderContext());
		g.drawString(label, (float) ((minX + dx) * sx - stringBounds.getWidth()),
				(float) ((y + dy) * sy - stringBounds.getHeight() / 2 - stringBounds.getY()));
	}

	private void drawPoints(Graphics2D g, int pixWidth, int pixHeight) {
		double sx = 0.0d;
		double sy = 0.0d;
		sx = ((double) pixWidth - LABEL_MARGIN_X) / (maxX - minX);
		sy = ((double) pixHeight - LABEL_MARGIN_Y) / (maxY - minY);

		Graphics2D coordinateSpace = (Graphics2D) g.create();
		coordinateSpace.translate(LABEL_MARGIN_X, LABEL_MARGIN_Y);
		drawGrid(coordinateSpace, -minX, -minY, sx, sy);
		drawPoints(coordinateSpace, -minX, -minY, sx, sy);
		coordinateSpace.dispose();
	}

	@Override
	public void paintComponent(Graphics graphics) {
		super.paintComponent(graphics);

		int pixWidth = getWidth() - 2 * MARGIN;
		int pixHeight = getHeight() - 2 * MARGIN - this.updatePanelHeight - 50;
		Graphics2D translated = (Graphics2D) graphics.create();
		translated.translate(MARGIN, MARGIN);
		paintGraph(translated, pixWidth, pixHeight);

	}

	public void paintGraph(Graphics graphics, int pixWidth, int pixHeight) {
		Graphics2D g = (Graphics2D) graphics;
		Graphics2D scaled = (Graphics2D) g.create();

		scaled.translate(0, pixHeight + 1 + this.updatePanelHeight + 50);
		// prepare data
		prepareData();

		scaled.scale(1, -1);
		g.setColor(Color.black);
		drawPoints(scaled, pixWidth, pixHeight);

		scaled.dispose();

		// x-axis label
		String xAxisLabel = "sorted k-distances";
		Rectangle2D stringBounds = SCALED_LABEL_FONT.getStringBounds(xAxisLabel, g.getFontRenderContext());
		g.drawString(xAxisLabel, MARGIN + (float) (pixWidth / 2.0d - stringBounds.getWidth() / 2.0d), MARGIN
				+ (float) (pixHeight - 2.0d * stringBounds.getHeight()) + 3);

		// y-axis label
		String yAxisLabel = "k-distance value";
		stringBounds = LABEL_FONT.getStringBounds(yAxisLabel, g.getFontRenderContext());
		g.drawString(yAxisLabel, MARGIN, (int) (MARGIN + stringBounds.getHeight() + 6));
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		try {
			Integer.parseInt(k_distance_jtext.getText());
		} catch (NumberFormatException e) {
			SwingTools.showVerySimpleErrorMessage("enter_k_value");
			return;
		}

		this.k = Integer.parseInt(k_distance_jtext.getText());
		this.kDistanceValues = null;
		repaint();
	}

	@Override
	public String getPlotterName() {
		return "K Distance Visualization";
	}
}
