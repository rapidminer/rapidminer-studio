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
package com.rapidminer.operator.visualization;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.DataRow;
import com.rapidminer.example.table.DoubleArrayDataRow;
import com.rapidminer.example.utils.ExampleSetBuilder;
import com.rapidminer.example.utils.ExampleSets;
import com.rapidminer.gui.plotter.PlotterConfigurationModel;
import com.rapidminer.gui.plotter.som.SOMClassColorizer;
import com.rapidminer.gui.plotter.som.SOMMatrixColorizer;
import com.rapidminer.gui.plotter.som.SOMPlotter;
import com.rapidminer.operator.Model;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.tools.LogService;


/**
 * This class uses the SOM Plotter for displaying the classification behavior of every model. It
 * overlays the SOM with the classification area transparently, so that missclassifications may be
 * recognized immediately.
 *
 * @author Sebastian Land
 */
public class SOMModelPlotter extends SOMPlotter {

	private static final long serialVersionUID = 1L;

	private Model model;

	private ExampleSet exampleSet;

	private double[][] classificationMatrix;

	private float alphaLevel = 0.5f;

	private transient BufferedImage classImage;

	private transient SOMMatrixColorizer colorizer;

	private int lastPlotterComponentIndex;

	public SOMModelPlotter(PlotterConfigurationModel settings) {
		super(settings);
	}

	public SOMModelPlotter(PlotterConfigurationModel settings, ExampleSet exampleSet, Model model) {
		super(settings);
		this.model = model;
		this.exampleSet = exampleSet;
		this.colorizer = new SOMClassColorizer(exampleSet.getAttributes().getLabel().getMapping().size());
	}

	public void setExampleSet(ExampleSet exampleSet) {
		this.exampleSet = exampleSet;
		this.colorizer = new SOMClassColorizer(exampleSet.getAttributes().getLabel().getMapping().size());
	}

	public void setModel(Model model) {
		this.model = model;
	}

	@Override
	public void paintComponent(Graphics graphics) {
		super.paintComponent(graphics);
		// painting only if approved
		if (show) {
			// init graphics
			Graphics2D g = (Graphics2D) graphics;

			int pixWidth = getWidth() - 2 * MARGIN;
			int pixHeight = getHeight() - 2 * MARGIN;

			// painting background
			g.drawImage(this.image, MARGIN, MARGIN, pixWidth, pixHeight, Color.WHITE, null);

			// painting transparent class overlay
			g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alphaLevel));
			g.drawImage(this.classImage, MARGIN, MARGIN, pixWidth, pixHeight, Color.WHITE, null);
			g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1));
			// painting points
			drawPoints(g);

			// painting Legend
			drawLegend(graphics, this.dataTable, colorColumn);

			// paint Tooltip
			drawToolTip((Graphics2D) graphics);
		}
	}

	@Override
	protected void createMatrices() {
		List<Attribute> attributes = new ArrayList<Attribute>(exampleSet.getAttributes().size());
		for (Attribute attribute : exampleSet.getAttributes()) {
			attributes.add((Attribute) attribute.clone());
		}
		ExampleSetBuilder builder = ExampleSets.from(attributes);
		for (int x = 0; x < dimensions[0]; x++) {
			for (int y = 0; y < dimensions[1]; y++) {
				DataRow row = new DoubleArrayDataRow(net.getNodeWeights(new int[] { x, y }));
				builder.addDataRow(row);
			}
		}
		ExampleSet set = builder.build();
		this.classificationMatrix = new double[dimensions[0]][dimensions[1]];
		try {
			set = model.apply(set);
			Iterator<Example> exampleIterator = set.iterator();
			for (int x = 0; x < dimensions[0]; x++) {
				for (int y = 0; y < dimensions[1]; y++) {
					Example example = exampleIterator.next();
					classificationMatrix[x][y] = example.getValue(example.getAttributes().getPredictedLabel());
				}
			}
		} catch (OperatorException e) {
			// LogService.getGlobal().log("Cannot use Model for prediction of node label: " +
			// e.getMessage(), LogService.WARNING);
			LogService.getRoot().log(
					Level.WARNING,
					"com.rapidminer.operator.visualization.SOMModelPlotter.using_model_for_prediction_error"
							+ e.getMessage());
		}
		super.createMatrices();
	}

	@Override
	protected void recalculateBackgroundImage() {
		super.recalculateBackgroundImage();
		this.classImage = new BufferedImage(IMAGE_WIDTH, IMAGE_HEIGHT, BufferedImage.TYPE_INT_ARGB);
		int width = IMAGE_WIDTH / dimensions[0];
		int height = IMAGE_HEIGHT / dimensions[1];
		for (int i = 0; i < dimensions[0]; i++) {
			for (int j = 0; j < dimensions[1]; j++) {
				// using 1 as scale factor, because SOMClasscolorzier needs original classvalues!
				interpolateRect(classImage, width * i, height * j, width, height, classificationMatrix, i, j, 1, colorizer);
			}
		}
	}

	@Override
	public JComponent getOptionsComponent(int index) {
		JComponent comp = super.getOptionsComponent(index);
		if (comp != null) {
			lastPlotterComponentIndex = index;
			return comp;
		} else {
			if (index == lastPlotterComponentIndex + 1) {
				JLabel label = new JLabel("Transparency");
				label.setToolTipText("Select level of transparency");
				return label;
			} else if (index == lastPlotterComponentIndex + 2) {
				String toolTip = "Select level of transparency";
				final JSlider alphaSlider = new JSlider(0, 100, 50);
				alphaSlider.setToolTipText(toolTip);
				alphaSlider.addChangeListener(new ChangeListener() {

					@Override
					public void stateChanged(ChangeEvent e) {
						setAlphaLevel(((double) alphaSlider.getValue()) / 100);
					}
				});
				return alphaSlider;
			}
		}
		return null;
	}

	public void setAlphaLevel(double alphaLevel) {
		this.alphaLevel = (float) alphaLevel;
		if (show) {
			this.repaint();
		}
	}
}
