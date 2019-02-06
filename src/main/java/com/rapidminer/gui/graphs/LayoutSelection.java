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

import java.awt.event.ActionEvent;
import java.lang.reflect.Constructor;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;

import javax.swing.JComboBox;

import org.apache.commons.collections15.Transformer;

import com.rapidminer.tools.LogService;

import edu.uci.ics.jung.algorithms.layout.BalloonLayout;
import edu.uci.ics.jung.algorithms.layout.CircleLayout;
import edu.uci.ics.jung.algorithms.layout.FRLayout2;
import edu.uci.ics.jung.algorithms.layout.ISOMLayout;
import edu.uci.ics.jung.algorithms.layout.KKLayout;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.algorithms.layout.RadialTreeLayout;
import edu.uci.ics.jung.algorithms.layout.SpringLayout2;
import edu.uci.ics.jung.algorithms.layout.TreeLayout;
import edu.uci.ics.jung.graph.Forest;
import edu.uci.ics.jung.graph.Graph;


/**
 * The layout selection for the {@link GraphViewer}.
 *
 * @author Ingo Mierswa
 */
public class LayoutSelection<V, E> extends JComboBox<String> {

	private static final long serialVersionUID = 8924517975475876102L;

	private GraphViewer<V, E> graphViewer;

	private transient Graph<V, E> graph;

	@SuppressWarnings("rawtypes")
	private Map<String, Class<? extends Layout>> layoutMap = null;

	private boolean animate = true;

	private Layout<V, E> layout;

	public LayoutSelection(GraphViewer<V, E> graphViewer, Graph<V, E> graph) {
		super();
		this.graphViewer = graphViewer;
		this.graph = graph;
		this.layout = new ISOMLayout<V, E>(graph);

		layoutMap = new LinkedHashMap<>();

		if (graph instanceof Forest) {
			layoutMap.put("Tree", ShapeBasedTreeLayout.class);
			layoutMap.put("Tree (Tight)", TreeLayout.class);
			layoutMap.put("Radial", RadialTreeLayout.class);
			layoutMap.put("Balloon", BalloonLayout.class);
		}

		layoutMap.put("ISOM", ISOMLayout.class);
		layoutMap.put("KKLayout", KKLayout.class);
		layoutMap.put("FRLayout", FRLayout2.class);
		layoutMap.put("Circle", CircleLayout.class);
		layoutMap.put("Spring", SpringLayout2.class);

		Iterator<String> it = layoutMap.keySet().iterator();
		while (it.hasNext()) {
			addItem(it.next());
		}

		addActionListener(this);
	}

	public Layout<V, E> getSelectedLayout() {
		return this.layout;
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		setLayout();
	}

	public void setAnimate(boolean animate) {
		this.animate = animate;
	}

	public boolean getAnimate() {
		return this.animate;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void setLayout() {
		String layoutName = (String) getSelectedItem();
		Class<? extends Layout> layoutClass = null;
		try {
			layoutClass = layoutMap.get(layoutName);
		} catch (Exception e) {
			LogService.getRoot().log(Level.SEVERE,
					"com.rapidminer.gui.graphs.LayoutSelection.layout_could_not_be_initialized", e.getMessage());
		}

		if (layoutClass != null) {
			try {
				Constructor<? extends Layout> constructor = null;
				Layout<V, E> layout = null;
				if (layoutClass == ShapeBasedTreeLayout.class) {
					constructor = layoutClass.getConstructor(new Class[] { Forest.class, Transformer.class });
					layout = constructor.newInstance(graph,
							new ExtendedVertexShapeTransformer<V>(graphViewer.getGraphCreator()));
				} else if (layoutClass == TreeLayout.class || layoutClass == BalloonLayout.class
						|| layoutClass == RadialTreeLayout.class) {
					constructor = layoutClass.getConstructor(new Class[] { Forest.class });
					layout = constructor.newInstance(graph);
				} else {
					constructor = layoutClass.getConstructor(new Class[] { Graph.class });
					layout = constructor.newInstance(graph);
				}
				if (layout != null) {
					this.layout = layout;
					this.graphViewer.changeLayout(layout, animate, 0, 0);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
