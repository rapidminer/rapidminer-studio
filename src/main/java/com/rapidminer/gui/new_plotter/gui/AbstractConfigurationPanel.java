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
package com.rapidminer.gui.new_plotter.gui;

import com.rapidminer.gui.new_plotter.configuration.PlotConfiguration;
import com.rapidminer.gui.new_plotter.data.PlotInstance;
import com.rapidminer.gui.new_plotter.listener.PlotConfigurationListener;
import com.rapidminer.gui.new_plotter.listener.PlotInstanceChangedListener;
import com.rapidminer.gui.new_plotter.listener.events.PlotConfigurationChangeEvent;
import com.rapidminer.gui.tools.SwingTools;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;


/**
 * @author Nils Woehler
 * @deprecated since 9.2.0
 */
@Deprecated
public abstract class AbstractConfigurationPanel extends JPanel implements PlotConfigurationListener,
		PlotInstanceChangedListener {

	public enum DatasetTransformationType {
		ORIGINAL, DE_PIVOTED
	}

	private transient List<PlotInstanceChangedListener> plotInstanceChangeListener = new LinkedList<>();

	private static final long serialVersionUID = 1L;

	private boolean enabled = true;

	private final Insets standardInset = new Insets(0, 5, 5, 5);

	private final Map<DatasetTransformationType, PlotInstance> typeToInstanceMap = new HashMap<>();

	private DatasetTransformationType currentType = DatasetTransformationType.ORIGINAL;

	public AbstractConfigurationPanel(PlotInstance plotInstance) {
		this.setLayout(new GridBagLayout());
		typeToInstanceMap.put(DatasetTransformationType.ORIGINAL, plotInstance);
	}

	protected void registerAsPlotConfigurationListener() {
		getCurrentPlotInstance().getMasterPlotConfiguration().addPlotConfigurationListener(this);
	}

	protected void unregisterAsPlotConfigurationListener() {
		getCurrentPlotInstance().getMasterPlotConfiguration().removePlotConfigurationListener(this);
	}

	protected PlotConfiguration getPlotConfiguration() {
		return getPlotInstance(currentType).getMasterPlotConfiguration();
	}

	protected PlotInstance getCurrentPlotInstance() {
		return typeToInstanceMap.get(currentType);
	}

	protected void setPlotInstance(PlotInstance instance, DatasetTransformationType type) {
		PlotInstance oldPlotInstance = getCurrentPlotInstance();
		this.typeToInstanceMap.put(type, instance);
		this.currentType = type;
		if (instance != null && oldPlotInstance != null) {
			informPlotInstanceChangeListener(oldPlotInstance, instance, type);
		}
	}

	protected DatasetTransformationType getCurrentTranformationType() {
		return currentType;
	}

	protected void addPlotInstanceChangeListener(PlotInstanceChangedListener l) {
		plotInstanceChangeListener.add(l);
	}

	protected void removePlotInstanceChangeListener(PlotInstanceChangedListener l) {
		plotInstanceChangeListener.remove(l);
	}

	protected void informPlotInstanceChangeListener(PlotInstance oldPlotInstance, PlotInstance newPlotInstance,
			DatasetTransformationType newType) {
		for (PlotInstanceChangedListener l : plotInstanceChangeListener) {
			l.plotInstanceChanged(oldPlotInstance, newPlotInstance, newType);
		}
	}

	@Override
	public void plotInstanceChanged(PlotInstance oldPlotInstance, PlotInstance newPlotInstance,
			DatasetTransformationType newType) {
		oldPlotInstance.getMasterPlotConfiguration().removePlotConfigurationListener(this);
		setPlotInstance(newPlotInstance, newType);
		registerAsPlotConfigurationListener();
		adaptGUI();
	}

	protected PlotInstance getPlotInstance(DatasetTransformationType type) {
		return typeToInstanceMap.get(type);
	}

	protected void addTwoComponentRow(JPanel addTarget, JComponent first, JComponent second) {
		if (!(addTarget.getLayout() instanceof GridBagLayout)) {
			throw new RuntimeException("JPanel with GridBagLayout is mandatory!");
		}

		GridBagConstraints itemConstraint = new GridBagConstraints();
		itemConstraint.gridx = 0;
		itemConstraint.weightx = 0.0;
		itemConstraint.gridwidth = 1;
		itemConstraint.anchor = GridBagConstraints.WEST;
		itemConstraint.insets = standardInset;

		addTarget.add(first, itemConstraint);

		itemConstraint = new GridBagConstraints();
		itemConstraint.gridx = 1;
		itemConstraint.weightx = 1.0;
		itemConstraint.gridwidth = GridBagConstraints.REMAINDER; // end row
		itemConstraint.fill = GridBagConstraints.HORIZONTAL;
		itemConstraint.insets = standardInset;

		addTarget.add(second, itemConstraint);
	}

	protected Insets getStandardInsets() {
		return standardInset;
	}

	protected void addThreeComponentRow(JPanel addTarget, JLabel label, JComponent second, JComponent third) {
		if (!(addTarget.getLayout() instanceof GridBagLayout)) {
			throw new RuntimeException("JPanel with GridBagLayout is mandatory!");
		}

		GridBagConstraints itemConstraint = new GridBagConstraints();

		itemConstraint = new GridBagConstraints();
		itemConstraint.gridx = 0;
		itemConstraint.weightx = 0.0;
		itemConstraint.gridwidth = 1;
		itemConstraint.insets = standardInset;
		itemConstraint.anchor = GridBagConstraints.LINE_START;

		addTarget.add(label, itemConstraint);

		itemConstraint = new GridBagConstraints();
		itemConstraint.gridx = 1;
		itemConstraint.weightx = 1.0;
		itemConstraint.gridwidth = GridBagConstraints.RELATIVE;
		itemConstraint.fill = GridBagConstraints.HORIZONTAL;
		itemConstraint.insets = standardInset;

		addTarget.add(second, itemConstraint);

		itemConstraint = new GridBagConstraints();
		itemConstraint.weightx = 1;
		itemConstraint.gridwidth = GridBagConstraints.REMAINDER;
		itemConstraint.anchor = GridBagConstraints.EAST;
		itemConstraint.fill = GridBagConstraints.HORIZONTAL;
		itemConstraint.insets = standardInset;

		addTarget.add(third, itemConstraint);
	}

	protected void addSeperatorToPanel(JPanel addTarget) {
		if (!(addTarget.getLayout() instanceof GridBagLayout)) {
			throw new RuntimeException("JPanel with GridBagLayout is mandatory!");
		}

		JSeparator separator = new JSeparator(SwingConstants.HORIZONTAL);

		GridBagConstraints itemConstraint = new GridBagConstraints();
		itemConstraint.gridx = GridBagConstraints.RELATIVE;
		itemConstraint.weightx = 1.0;
		itemConstraint.gridwidth = GridBagConstraints.REMAINDER; // end row
		itemConstraint.fill = GridBagConstraints.HORIZONTAL;
		itemConstraint.insets = new Insets(0, 5, 5, 5);

		addTarget.add(separator, itemConstraint);
	}

	protected Icon createColoredRectangleIcon(Color color) {
		// create buffered image for colored icon
		BufferedImage bufferedImage = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2 = bufferedImage.createGraphics();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, // Anti-alias!
				RenderingHints.VALUE_ANTIALIAS_ON);

		if (color != null) {
			// fill image with item color
			Color newColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
			g2.setColor(newColor);
		} else {
			g2.setColor(Color.gray);
		}
		g2.fillRect(0, 0, 10, 10);

		return new ImageIcon(bufferedImage);
	}

	// protected Icon createSeriesFormatIcon(SeriesFormat format) {
	//
	// // Create an image that supports arbitrary levels of transparency
	// BufferedImage bufferedImage = new Buffered
	// g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, // Anti-alias!
	// RenderingHints.VALUE_ANTIALIAS_ON);
	//
	// // fill image with item color
	// Color newColor = new Color(fillPaint.getRed(), fillPaint.getGreen(), fillPaint.getBlue(),
	// format.getOpacity());
	// g2.setColor(newColor);
	// g2.fill(shape);
	// return new ImageIcon();
	// }

	public void disableAllComponents() {
		if (enabled) {
			SwingTools.setEnabledRecursive(this, false);
			enabled = false;
		}
	}

	public void enableAllComponents() {
		if (!enabled) {
			SwingTools.setEnabledRecursive(this, true);
			enabled = true;
		}
	}

	protected void processPlotConfigurationMetaChange(PlotConfigurationChangeEvent change) {
		for (PlotConfigurationChangeEvent e : change.getPlotConfigChangeEvents()) {
			plotConfigurationChanged(e);
		}
	}

	protected abstract void adaptGUI();
}
