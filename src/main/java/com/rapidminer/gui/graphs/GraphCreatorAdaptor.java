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
package com.rapidminer.gui.graphs;

import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.renderers.Renderer.EdgeLabel;
import edu.uci.ics.jung.visualization.renderers.Renderer.Vertex;
import edu.uci.ics.jung.visualization.renderers.Renderer.VertexLabel;

import java.awt.Paint;

import javax.swing.JComponent;

import org.apache.commons.collections15.Transformer;


/**
 * An adaptor for the graph creator interface. Subclasses might want to override some of the
 * implemented methods and have to define the {@link #createGraph()} method.
 * 
 * @author Ingo Mierswa
 */
public abstract class GraphCreatorAdaptor implements GraphCreator<String, String> {

	/** Returns null. */
	@Override
	public EdgeLabel<String, String> getEdgeLabelRenderer() {
		return null;
	}

	/** Returns null. */
	@Override
	public String getEdgeName(String id) {
		return null;
	}

	/** Returns 1. */
	@Override
	public double getEdgeStrength(String id) {
		return 1.0d;
	}

	/** Returns the shape of the edges. */
	@Override
	public int getEdgeShape() {
		return EDGE_SHAPE_LINE;
	}

	/** Returns -1. */
	@Override
	public int getLabelOffset() {
		return -1;
	}

	/** Returns -1. */
	@Override
	public int getMinLeafHeight() {
		return -1;
	}

	/** Returns -1. */
	@Override
	public int getMinLeafWidth() {
		return -1;
	}

	/** Returns 0. */
	@Override
	public int getNumberOfOptionComponents() {
		return 0;
	}

	/** Returns null. */
	@Override
	public Object getObject(String id) {
		return null;
	}

	/** Returns null. */
	@Override
	public GraphObjectViewer getObjectViewer() {
		return null;
	}

	/** Returns null. */
	@Override
	public JComponent getOptionComponent(GraphViewer<?, ?> viewer, int index) {
		return null;
	}

	/** Returns null. */
	@Override
	public VertexLabel<String, String> getVertexLabelRenderer() {
		return null;
	}

	/** Returns null. */
	@Override
	public String getVertexName(String id) {
		return null;
	}

	/** Returns null. */
	@Override
	public Transformer<String, Paint> getVertexPaintTransformer(VisualizationViewer<String, String> viewer) {
		return null;
	}

	/** Returns null. */
	@Override
	public Vertex<String, String> getVertexRenderer() {
		return null;
	}

	/** Returns null. */
	@Override
	public String getVertexToolTip(String id) {
		return null;
	}

	/** Returns false. */
	@Override
	public boolean isBold(String id) {
		return false;
	}

	/** Returns false. */
	@Override
	public boolean isEdgeLabelDecorating() {
		return false;
	}

	/** Returns false. */
	@Override
	public boolean isLeaf(String id) {
		return false;
	}

	/** Returns true. */
	@Override
	public boolean isRotatingEdgeLabels() {
		return true;
	}

	/** Returns true. */
	@Override
	public boolean showEdgeLabelsDefault() {
		return true;
	}

	/** Returns true. */
	@Override
	public boolean showVertexLabelsDefault() {
		return true;
	}
}
