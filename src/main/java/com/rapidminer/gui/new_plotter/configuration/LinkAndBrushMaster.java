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
package com.rapidminer.gui.new_plotter.configuration;

import com.rapidminer.gui.new_plotter.PlotConfigurationError;
import com.rapidminer.gui.new_plotter.configuration.DimensionConfig.PlotDimension;
import com.rapidminer.gui.new_plotter.data.DimensionConfigData;
import com.rapidminer.gui.new_plotter.engine.jfreechart.link_and_brush.listener.LinkAndBrushListener;
import com.rapidminer.gui.new_plotter.engine.jfreechart.link_and_brush.listener.LinkAndBrushSelection;
import com.rapidminer.gui.new_plotter.engine.jfreechart.link_and_brush.listener.LinkAndBrushSelection.SelectionType;
import com.rapidminer.gui.new_plotter.engine.jfreechart.link_and_brush.listener.LinkAndBrushSelectionListener;
import com.rapidminer.gui.new_plotter.utility.ContinuousColorProvider;
import com.rapidminer.tools.container.Pair;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.jfree.data.Range;


/**
 * @author Nils Woehler
 * @deprecated since 9.2.0
 */
@Deprecated
public class LinkAndBrushMaster implements LinkAndBrushSelectionListener {

	private final PlotConfiguration plotConfig;

	private boolean zoomedIn = false;
	private boolean useGrayForOutliers = false;

	private Map<Integer, Range> rangeAxisIndexToZoomMap = new HashMap<Integer, Range>();
	private Range domainAxisZoom;

	/** the list of {@link LinkAndBrushListener}s */
	private transient List<WeakReference<LinkAndBrushListener>> listeners = new LinkedList<WeakReference<LinkAndBrushListener>>();

	public LinkAndBrushMaster(PlotConfiguration plotConfig) {
		this.plotConfig = plotConfig;
	}

	public List<PlotConfigurationError> getErrors() {
		List<PlotConfigurationError> errors = new LinkedList<PlotConfigurationError>();

		return errors;
	}

	public List<PlotConfigurationError> getWarnings() {
		List<PlotConfigurationError> warnings = new LinkedList<PlotConfigurationError>();

		if (zoomedIn) {
			warnings.add(new PlotConfigurationError("zoomed_in"));
		}

		return warnings;
	}

	public boolean isZoomedIn() {
		return zoomedIn;
	}

	public void clearZooming(boolean fireEvent) {
		zoomedIn = false;
		domainAxisZoom = null;
		rangeAxisIndexToZoomMap.clear();
		if (fireEvent) {
			informLinkAndBrushListeners(new LinkAndBrushSelection(SelectionType.RESTORE_AUTO_BOUNDS,
					new LinkedList<Pair<Integer, Range>>(), new LinkedList<Pair<Integer, Range>>()));
		}
	}

	public void clearRangeAxisZooming(boolean fireEvent) {
		rangeAxisIndexToZoomMap.clear();
		zoomedIn = (domainAxisZoom != null);
		if (fireEvent) {
			informLinkAndBrushListeners(new LinkAndBrushSelection(SelectionType.RESTORE_AUTO_BOUNDS,
					new LinkedList<Pair<Integer, Range>>(), new LinkedList<Pair<Integer, Range>>()));
		}
	}

	public void clearDomainAxisZooming(boolean fireEvent) {
		domainAxisZoom = null;
		zoomedIn = (rangeAxisIndexToZoomMap.keySet().size() > 0);
		if (fireEvent) {
			informLinkAndBrushListeners(new LinkAndBrushSelection(SelectionType.RESTORE_AUTO_BOUNDS,
					new LinkedList<Pair<Integer, Range>>(), new LinkedList<Pair<Integer, Range>>()));
		}
	}

	/**
	 * Returns <code>null</code> if isZoomedIn() returns <code>false</code>.
	 */
	public Range getDomainZoom() {
		return domainAxisZoom;
	}

	/**
	 * Returns <code>null</code> if isZommedIn() returns <code>false</code> or if checking for a at
	 * zoom time unknown {@link RangeAxisConfig}.
	 */
	public Range getRangeAxisZoom(RangeAxisConfig rangeAxisConfig, PlotConfiguration plotConfig) {
		int indexOf = plotConfig.getIndexOfRangeAxisConfigById(rangeAxisConfig.getId());
		return rangeAxisIndexToZoomMap.get(indexOf);
	}

	@Override
	public void selectedLinkAndBrushRectangle(LinkAndBrushSelection e) {
		if (e.getType() == SelectionType.ZOOM_IN) {
			zoomedIn = true;

			// fetch domain axis range
			Pair<Integer, Range> domainAxisRange = e.getDomainAxisRange();
			if (domainAxisRange != null) {
				setDomainAxisZoom(domainAxisRange.getSecond(), null);
			}

			// fetch range axis config ranges
			List<RangeAxisConfig> rangeAxisConfigs = plotConfig.getRangeAxisConfigs();
			List<Pair<Integer, Range>> valueAxisRanges = e.getValueAxisRanges();
			if (valueAxisRanges.size() > 0) {
				for (Pair<Integer, Range> newRangeAxisRangePair : valueAxisRanges) {
					RangeAxisConfig rangeAxisConfig = rangeAxisConfigs.get(newRangeAxisRangePair.getFirst());
					int indexOf = plotConfig.getIndexOfRangeAxisConfigById(rangeAxisConfig.getId());
					setRangeAxisZoom(indexOf, newRangeAxisRangePair.getSecond(), null);
				}
			}

		}

		if (e.getType() == SelectionType.RESTORE_AUTO_BOUNDS) {
			clearZooming(false);
		}

		if (e.getType() == SelectionType.COLOR_ZOOM) {
			Double minColorValue = e.getMinColorValue();
			Double maxColorValue = e.getMaxColorValue();
			if (e.getPlotInstance() != null) {
				DimensionConfigData dimensionConfigData = e.getPlotInstance().getPlotData()
						.getDimensionConfigData(plotConfig.getDefaultDimensionConfigs().get(PlotDimension.COLOR));
				ContinuousColorProvider colProv = null;
				if (minColorValue != null && dimensionConfigData != null
						&& dimensionConfigData.getColorProvider() instanceof ContinuousColorProvider) {
					colProv = (ContinuousColorProvider) dimensionConfigData.getColorProvider();
					colProv.setMinValue(minColorValue);
				}
				if (maxColorValue != null && dimensionConfigData != null
						&& dimensionConfigData.getColorProvider() instanceof ContinuousColorProvider) {
					colProv = (ContinuousColorProvider) dimensionConfigData.getColorProvider();
					colProv.setMaxValue(maxColorValue);
				}
				if (colProv != null) {
					colProv.setUseGrayForOutliers(useGrayForOutliers);
				}
			}
		} else if (e.getType() == SelectionType.COLOR_SELECTION) {
			Double minColorValue = e.getMinColorValue();
			Double maxColorValue = e.getMaxColorValue();
			if (e.getPlotInstance() != null) {
				DimensionConfigData dimensionConfigData = e.getPlotInstance().getPlotData()
						.getDimensionConfigData(plotConfig.getDefaultDimensionConfigs().get(PlotDimension.COLOR));
				ContinuousColorProvider colProv = null;
				if (minColorValue != null && dimensionConfigData != null
						&& dimensionConfigData.getColorProvider() instanceof ContinuousColorProvider) {
					colProv = (ContinuousColorProvider) dimensionConfigData.getColorProvider();
					colProv.setMinValue(minColorValue);
				}
				if (maxColorValue != null && dimensionConfigData != null
						&& dimensionConfigData.getColorProvider() instanceof ContinuousColorProvider) {
					colProv = (ContinuousColorProvider) dimensionConfigData.getColorProvider();
					colProv.setMaxValue(maxColorValue);
				}
				if (colProv != null) {
					colProv.setUseGrayForOutliers(useGrayForOutliers);
				}
			}
		}

		if (e.getType() == SelectionType.RESTORE_COLOR) {
			if (e.getPlotInstance() != null) {
				DimensionConfigData dimensionConfigData = e.getPlotInstance().getPlotData()
						.getDimensionConfigData(plotConfig.getDefaultDimensionConfigs().get(PlotDimension.COLOR));
				if (dimensionConfigData != null && dimensionConfigData.getColorProvider() instanceof ContinuousColorProvider) {
					ContinuousColorProvider colProv = (ContinuousColorProvider) dimensionConfigData.getColorProvider();
					colProv.revertMinAndMaxValuesBackToOriginalValues();
				}
			}
		}

		informLinkAndBrushListeners(e);
	}

	/**
	 * Sets the domain axis zoom.
	 * 
	 * @param domainAxisRange
	 * @param e
	 *            if {@code null}, will not inform listeners
	 */
	public void setDomainAxisZoom(Range domainAxisZoom, LinkAndBrushSelection e) {
		if (domainAxisZoom == null) {
			throw new IllegalArgumentException("domainAxisRange must not be null!");
		}

		this.domainAxisZoom = domainAxisZoom;
		if (e != null) {
			informLinkAndBrushListeners(e);
		}
	}

	/**
	 * Sets the range axis zoom of the range axis specified by the given index.
	 * 
	 * @param indexOfRangeAxis
	 * @param rangeAxisZoom
	 * @param e
	 *            if {@code null}, will not inform listeners
	 */
	public void setRangeAxisZoom(int indexOfRangeAxis, Range rangeAxisZoom, LinkAndBrushSelection e) {
		if (indexOfRangeAxis < 0) {
			throw new IllegalArgumentException("indexOfRangeAxis must not be < 0");
		}

		rangeAxisIndexToZoomMap.put(indexOfRangeAxis, rangeAxisZoom);
		if (e != null) {
			informLinkAndBrushListeners(e);
		}
	}

	protected LinkAndBrushMaster clone(PlotConfiguration plotConfig) {
		LinkAndBrushMaster clone = new LinkAndBrushMaster(plotConfig);

		clone.domainAxisZoom = this.domainAxisZoom;

		Map<Integer, Range> clonedRangeAxisIndexToZoomMap = new HashMap<Integer, Range>();

		for (Integer key : rangeAxisIndexToZoomMap.keySet()) {
			Range value = rangeAxisIndexToZoomMap.get(key);
			clonedRangeAxisIndexToZoomMap.put(key, new Range(value.getLowerBound(), value.getUpperBound()));
		}
		clone.rangeAxisIndexToZoomMap = clonedRangeAxisIndexToZoomMap;

		clone.zoomedIn = this.zoomedIn;

		return clone;
	}

	/**
	 * Adds the given {@link LinkAndBrushListener} to this {@link LinkAndBrushMaster}.
	 * 
	 * @param l
	 */
	public void addLinkAndBrushListener(LinkAndBrushListener l) {
		listeners.add(new WeakReference<LinkAndBrushListener>(l));
	}

	/**
	 * Removes the given {@link LinkAndBrushListener} from this {@link LinkAndBrushMaster}.
	 * 
	 * @param l
	 */
	public void removeLinkAndBrushListener(LinkAndBrushListener l) {
		Iterator<WeakReference<LinkAndBrushListener>> it = listeners.iterator();
		while (it.hasNext()) {
			LinkAndBrushListener listener = it.next().get();
			if (l != null) {
				if (listener != null && listener.equals(l)) {
					it.remove();
				}
			} else {
				it.remove();
			}
		}
	}

	/**
	 * Informs all {@link LinkAndBrushListener}s of an update.
	 * 
	 * @param e
	 */
	private void informLinkAndBrushListeners(LinkAndBrushSelection e) {
		Iterator<WeakReference<LinkAndBrushListener>> it = listeners.iterator();
		while (it.hasNext()) {
			WeakReference<LinkAndBrushListener> wrl = it.next();
			LinkAndBrushListener l = wrl.get();
			if (l != null) {
				l.linkAndBrushUpdate(e);
			} else {
				it.remove();
			}
		}
	}

	public boolean isUseGrayForOutliers() {
		return useGrayForOutliers;
	}

	public void setUseGrayForOutliers(boolean useGrayForOutliers) {
		this.useGrayForOutliers = useGrayForOutliers;
	}
}
