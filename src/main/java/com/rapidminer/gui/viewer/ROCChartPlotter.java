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

import com.rapidminer.gui.actions.export.PrintableComponent;
import com.rapidminer.gui.plotter.ColorProvider;
import com.rapidminer.gui.plotter.PlotterAdapter;
import com.rapidminer.report.Renderable;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.math.ROCData;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.swing.JPanel;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.DeviationRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.YIntervalSeries;
import org.jfree.data.xy.YIntervalSeriesCollection;
import org.jfree.ui.HorizontalAlignment;
import org.jfree.ui.RectangleEdge;
import org.jfree.ui.RectangleInsets;


/**
 * This is the ROC chart plotter.
 * 
 * @author Ingo Mierswa
 */
public class ROCChartPlotter extends JPanel implements Renderable, PrintableComponent {

	private static final long serialVersionUID = -5819082000307077237L;

	private static final int NUMBER_OF_POINTS = 500;

	/** The data set used for the plotter. */
	private YIntervalSeriesCollection dataset = null;

	private final Map<String, List<ROCData>> rocDataLists = new HashMap<>();

	private final ColorProvider colorProvider = new ColorProvider();

	public ROCChartPlotter() {
		super();
		setBackground(Color.white);
	}

	public void addROCData(String name, ROCData singleROCData) {
		List<ROCData> tempList = new LinkedList<>();
		tempList.add(singleROCData);
		addROCData(name, tempList);
	}

	public void addROCData(String name, List<ROCData> averageROCData) {
		rocDataLists.put(name, averageROCData);
	}

	private JFreeChart createChart(XYDataset dataset) {
		// create the chart...
		JFreeChart chart = ChartFactory.createXYLineChart(null,      // chart title
				null,                      // x axis label
				null,                      // y axis label
				dataset,                  // data
				PlotOrientation.VERTICAL, true,                     // include legend
				true,                     // tooltips
				false                     // urls
				);

		chart.setBackgroundPaint(Color.white);

		// get a reference to the plot for further customisation...
		XYPlot plot = (XYPlot) chart.getPlot();
		plot.setBackgroundPaint(Color.WHITE);
		plot.setAxisOffset(new RectangleInsets(5.0, 5.0, 5.0, 5.0));
		plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
		plot.setRangeGridlinePaint(Color.LIGHT_GRAY);

		ValueAxis valueAxis = plot.getRangeAxis();
		valueAxis.setLabelFont(PlotterAdapter.LABEL_FONT_BOLD);
		valueAxis.setTickLabelFont(PlotterAdapter.LABEL_FONT);

		ValueAxis domainAxis = plot.getDomainAxis();
		domainAxis.setLabelFont(PlotterAdapter.LABEL_FONT_BOLD);
		domainAxis.setTickLabelFont(PlotterAdapter.LABEL_FONT);

		DeviationRenderer renderer = new DeviationRenderer(true, false);
		Stroke stroke = new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
		if (dataset.getSeriesCount() == 1) {
			renderer.setSeriesStroke(0, stroke);
			renderer.setSeriesPaint(0, Color.RED);
			renderer.setSeriesFillPaint(0, Color.RED);
		} else if (dataset.getSeriesCount() == 2) {
			renderer.setSeriesStroke(0, stroke);
			renderer.setSeriesPaint(0, Color.RED);
			renderer.setSeriesFillPaint(0, Color.RED);

			renderer.setSeriesStroke(1, stroke);
			renderer.setSeriesPaint(1, Color.BLUE);
			renderer.setSeriesFillPaint(1, Color.BLUE);
		} else {
			for (int i = 0; i < dataset.getSeriesCount(); i++) {
				renderer.setSeriesStroke(i, stroke);
				Color color = colorProvider.getPointColor((double) i / (double) (dataset.getSeriesCount() - 1));
				renderer.setSeriesPaint(i, color);
				renderer.setSeriesFillPaint(i, color);
			}
		}
		renderer.setAlpha(0.12f);
		plot.setRenderer(renderer);

		// legend settings
		LegendTitle legend = chart.getLegend();
		if (legend != null) {
			legend.setPosition(RectangleEdge.TOP);
			legend.setFrame(BlockBorder.NONE);
			legend.setHorizontalAlignment(HorizontalAlignment.LEFT);
			legend.setItemFont(PlotterAdapter.LABEL_FONT);
		}
		return chart;
	}

	private void prepareData() {

		this.dataset = new YIntervalSeriesCollection();

		Iterator<Map.Entry<String, List<ROCData>>> r = rocDataLists.entrySet().iterator();
		boolean showThresholds = true;
		if (rocDataLists.size() > 1) {
			showThresholds = false;
		}

		while (r.hasNext()) {
			Map.Entry<String, List<ROCData>> entry = r.next();
			YIntervalSeries rocSeries = new YIntervalSeries(entry.getKey());
			YIntervalSeries thresholdSeries = new YIntervalSeries(entry.getKey() + " (Thresholds)");
			List<ROCData> dataList = entry.getValue();
			for (int i = 0; i <= NUMBER_OF_POINTS; i++) {

				double rocSum = 0.0d;
				double rocSquaredSum = 0.0d;
				double thresholdSum = 0.0d;
				double thresholdSquaredSum = 0.0d;
				for (ROCData data : dataList) {
					double rocValue = data.getInterpolatedTruePositives(i / (double) NUMBER_OF_POINTS)
							/ data.getTotalPositives();
					rocSum += rocValue;
					rocSquaredSum += rocValue * rocValue;

					double thresholdValue = data.getInterpolatedThreshold(i / (double) NUMBER_OF_POINTS);
					thresholdSum += thresholdValue;
					thresholdSquaredSum += thresholdValue * thresholdValue;
				}

				double rocMean = rocSum / dataList.size();
				double rocDeviation = Math.sqrt(rocSquaredSum / dataList.size() - (rocMean * rocMean));
				rocSeries.add(i / (double) NUMBER_OF_POINTS, rocMean, rocMean - rocDeviation, rocMean + rocDeviation);

				double thresholdMean = thresholdSum / dataList.size();
				double thresholdDeviation = Math.sqrt(thresholdSquaredSum / dataList.size()
						- (thresholdMean * thresholdMean));
				thresholdSeries.add(i / (double) NUMBER_OF_POINTS, thresholdMean, thresholdMean - thresholdDeviation,
						thresholdMean + thresholdDeviation);

			}
			dataset.addSeries(rocSeries);

			if (showThresholds) {
				dataset.addSeries(thresholdSeries);
			}
		}
	}

	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		paintDeviationChart(g, getWidth(), getHeight());
	}

	public void paintDeviationChart(Graphics graphics, int width, int height) {
		prepareData();

		JFreeChart chart = createChart(this.dataset);

		// set the background color for the chart...
		chart.setBackgroundPaint(Color.white);

		// legend settings
		LegendTitle legend = chart.getLegend();
		if (legend != null) {
			legend.setPosition(RectangleEdge.TOP);
			legend.setFrame(BlockBorder.NONE);
			legend.setHorizontalAlignment(HorizontalAlignment.LEFT);
		}

		Rectangle2D drawRect = new Rectangle2D.Double(0, 0, width, height);
		chart.draw((Graphics2D) graphics, drawRect);
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
		paintDeviationChart(graphics, width, height);
	}

	@Override
	public Component getExportComponent() {
		return this;
	}

	@Override
	public String getExportName() {
		return I18N.getGUIMessage("gui.cards.result_view.roc_curve.title");
	}

	@Override
	public String getIdentifier() {
		return null;
	}

	@Override
	public String getExportIconName() {
		return I18N.getGUIMessage("gui.cards.result_view.roc_curve.icon");
	}
}
