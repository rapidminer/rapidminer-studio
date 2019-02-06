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

import com.rapidminer.gui.actions.export.AbstractPrintableIOObjectPanel;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.operator.clustering.HierarchicalClusterModel;
import com.rapidminer.operator.clustering.HierarchicalClusterNode;
import com.rapidminer.report.Renderable;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;


/**
 * Plots a dendrogram of a given cluster model. The nodes in the model must have different, non-NaN
 * distance values for this operator to work.
 * 
 * @author Sebastian Land, Michael Wurst
 * 
 */
public class DendrogramPlotter extends AbstractPrintableIOObjectPanel implements Renderable {

	private static final long serialVersionUID = 2892192060246909733L;

	private static final int MARGIN = 10;

	private static final int MIN_HEIGHT = 2;
	private static final int MIN_WIDTH = 5;

	private HierarchicalClusterModel hcm;

	private int numObjects;
	private double maxDistance;
	private double minDistance;

	private int maxX;
	private int maxY;

	private int count;

	private Color color = SwingTools.DARKEST_BLUE;

	public DendrogramPlotter(HierarchicalClusterModel hcm) {
		super(hcm, "dendogram_view");
		this.hcm = hcm;

		numObjects = hcm.getRootNode().getNumberOfExamplesInSubtree();
		minDistance = Double.POSITIVE_INFINITY;
		maxDistance = Double.NEGATIVE_INFINITY;
		findMinMaxDistance(hcm.getRootNode());
	}

	private void findMinMaxDistance(HierarchicalClusterNode node) {
		double distance = node.getDistance();
		maxDistance = Math.max(maxDistance, distance);
		minDistance = Math.min(minDistance, distance);

		for (HierarchicalClusterNode subNode : node.getSubNodes()) {
			findMinMaxDistance(subNode);
		}
	}

	private void drawLine(int x1, int y1, int x2, int y2, Graphics g) {
		g.setColor(color);
		g.drawLine(x1, y1, x2, y2);
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);

		if ((minDistance == maxDistance) || (Double.isNaN(minDistance)) || (Double.isInfinite(minDistance))
				|| (Double.isNaN(maxDistance)) || (Double.isInfinite(maxDistance))) {
			g.drawString("Dendrogram not available for this cluster model. Use an agglomerative clusterer.", MARGIN,
					MARGIN + 15);
			return;
		}

		this.maxX = getWidth() - 2 * MARGIN;
		this.maxY = getHeight() - 2 * MARGIN;

		g.setColor(Color.WHITE);
		g.fillRect(0, 0, getWidth(), getHeight());

		Graphics translated = g.create();
		translated.translate(MARGIN, MARGIN);

		count = 0;

		paintRecursively(hcm.getRootNode(), hcm.getRootNode().getDistance(), translated);
	}

	private int weightToYPos(double weight) {
		return (int) Math.round(maxY * (((maxDistance - weight) - minDistance) / ((maxDistance - minDistance))));
	}

	private int countToXPos(int count) {
		return (int) Math.round((((double) count) / ((double) numObjects)) * (maxX));
	}

	private int paintRecursively(HierarchicalClusterNode node, double baseDistance, Graphics g) {
		int leftPos = -1;
		int rightPos = -1;

		// doing recursive descent
		for (HierarchicalClusterNode subNode : node.getSubNodes()) {
			if ((subNode.getNumberOfSubNodes() > 0) || (subNode.getNumberOfExamplesInSubtree() > 1)) {
				int currentPos = paintRecursively(subNode, node.getDistance(), g);
				if (leftPos == -1) {
					leftPos = currentPos;
				}
				rightPos = currentPos;
			}
		}

		// drawing vertical cluster lines of one elemental clusters
		for (HierarchicalClusterNode subNode : node.getSubNodes()) {
			if ((subNode.getNumberOfExamplesInSubtree() == 1) && (subNode.getNumberOfSubNodes() == 0)) {
				int currentPos = countToXPos(count);
				drawLine(currentPos, weightToYPos(node.getDistance()), currentPos, weightToYPos(minDistance), g);
				if (leftPos == -1) {
					leftPos = currentPos;
				}
				rightPos = currentPos;
				count++;
			}
		}

		int middlePos = (rightPos + leftPos) / 2;

		// painting vertical connections of merged clusters to next cluster
		drawLine(middlePos, weightToYPos(baseDistance), middlePos, weightToYPos(node.getDistance()), g);
		// painting horizontal connections of merged clusters
		drawLine(leftPos, weightToYPos(node.getDistance()), rightPos, weightToYPos(node.getDistance()), g);

		return middlePos;
	}

	@Override
	public void prepareRendering() {}

	@Override
	public void finishRendering() {}

	@Override
	public int getRenderHeight(int preferredHeight) {
		int height = getHeight();
		if (height < 1) {
			height = preferredHeight;
		}
		return height;
	}

	@Override
	public int getRenderWidth(int preferredWidth) {
		int width = getWidth();
		if (width < 1) {
			width = preferredWidth;
		}
		return width;
	}

	@Override
	public void render(Graphics graphics, int width, int height) {
		setSize(width, height);
		paint(graphics);
	}

	@Override
	public Dimension getPreferredSize() {
		int width = MIN_WIDTH * numObjects;
		int height = MIN_HEIGHT * numObjects;

		Dimension current = super.getPreferredSize();
		width = Math.max(width, current.width);
		height = Math.max(height, current.height);
		return new Dimension(width, height);
	}

	@Override
	public Component getExportComponent() {
		return this;
	}
}
