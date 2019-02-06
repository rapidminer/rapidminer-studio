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
package com.rapidminer.gui.plotter.som;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;

import com.rapidminer.datatable.DataTable;
import com.rapidminer.datatable.DataTableRow;
import com.rapidminer.gui.plotter.ColorProvider;
import com.rapidminer.gui.plotter.ExamplePlotterPoint;
import com.rapidminer.gui.plotter.PlotterAdapter;
import com.rapidminer.gui.plotter.PlotterConfigurationModel;
import com.rapidminer.gui.plotter.conditions.BasicPlotterCondition;
import com.rapidminer.gui.plotter.conditions.PlotterCondition;
import com.rapidminer.gui.properties.PropertyPanel;
import com.rapidminer.gui.tools.SwingTools;
import com.rapidminer.operator.ProcessStoppedException;
import com.rapidminer.tools.I18N;
import com.rapidminer.tools.RandomGenerator;
import com.rapidminer.tools.math.MathFunctions;
import com.rapidminer.tools.math.som.KohonenNet;
import com.rapidminer.tools.math.som.ProgressListener;
import com.rapidminer.tools.math.som.RandomDataContainer;
import com.rapidminer.tools.math.som.RitterAdaptation;


/**
 * This is the main class for the SOMPlotter. It uses the KohonenNet class for generating a
 * self-organizing map. Different properties of the resulting map may be shown as background while
 * the examples are shown as points. There are different styled visualizations of the properties.
 *
 * @author Sebastian Land, Ingo Mierswa
 * @deprecated since 9.2.0
 */
@Deprecated
public class SOMPlotter extends PlotterAdapter implements ProgressListener {

	private static final long serialVersionUID = -1936359032703929998L;

	private static final String[] MATRIX_TYPES = new String[] { "U-Matrix", "P-Matrix", "U*-Matrix" };

	public static final int MATRIX_U = 0;
	public static final int MATRIX_P = 1;
	public static final int MATRIX_U_STAR = 2;

	protected static final int IMAGE_WIDTH = 400;

	protected static final int IMAGE_HEIGHT = 300;

	protected int[] dimensions = { 40, 30 };

	private ArrayList<ExamplePlotterPoint> exampleCoordinates = new ArrayList<>();

	private boolean examplesApplied = false;

	private double[][] uMatrix;

	private double maxU;

	private double[][] pMatrix;

	private double maxP;

	private double[][] uStarMatrix;

	private double maxUStar;

	protected transient DataTable dataTable;

	protected boolean show = false;

	private String currentToolTip = null;

	private double toolTipX = 0.0d;

	private double toolTipY = 0.0d;

	private int showMatrix = 0;

	private int showColor = 0;

	protected int colorColumn = -1;

	private transient RandomDataContainer data = new RandomDataContainer();

	protected transient KohonenNet net;

	private JButton approveButton = new JButton(I18N.getGUILabel("SOMPlotter.calculateButton.label"));

	private JButton abortButton = new JButton(I18N.getGUILabel("SOMPlotter.abortionButton.label"));

	private JComboBox<String> matrixSelection = new JComboBox<String>(MATRIX_TYPES);

	private JComboBox<String> colorSelection = new JComboBox<String>(
			new String[] { "Landscape", "GrayScale", "Fire and Ice" });

	private JTextField roundSelection = new JTextField("25");

	private JTextField radiusSelection = new JTextField("15");

	private JTextField dimensionX = new JTextField("40");

	private JTextField dimensionY = new JTextField("30");

	private JProgressBar progressBar = new JProgressBar();

	private boolean coloredPoints = true;

	private transient SOMMatrixColorizer[] colorizer = new SOMMatrixColorizer[] { new SOMLandscapeColorizer(),
			new SOMGreyColorizer(), new SOMFireColorizer() };

	private int jitterAmount = 0;

	protected transient BufferedImage image = null;

	public SOMPlotter(PlotterConfigurationModel settings) {
		super(settings);
		setBackground(Color.WHITE);

		approveButton.setToolTipText(I18N.getGUILabel("SOMPlotter.calculateButton.tip"));
		approveButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				startCalculation(true);
			}
		});

		abortButton.setToolTipText(I18N.getGUILabel("SOMPlotter.abortionButton.tip"));
		abortButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (net != null) {
					net.stopTrain();
				}
			}
		});

		matrixSelection.setPreferredSize(new Dimension(matrixSelection.getPreferredSize().width,
				PropertyPanel.VALUE_CELL_EDITOR_HEIGHT));
		matrixSelection.setToolTipText("Select the matrix type which should be visualized.");
		matrixSelection.addItemListener(new ItemListener() {

			@Override
			public void itemStateChanged(ItemEvent event) {
				showMatrix = matrixSelection.getSelectedIndex();
				if (showMatrix < 0) {
					showMatrix = 0;
				}
				recalculateBackgroundImage();
				repaint();
			}
		});

		colorSelection.setPreferredSize(new Dimension(colorSelection.getPreferredSize().width,
				PropertyPanel.VALUE_CELL_EDITOR_HEIGHT));
		colorSelection.setToolTipText("Select the color scheme used for the visualization of the matrix values.");
		colorSelection.addItemListener(new ItemListener() {

			@Override
			public void itemStateChanged(ItemEvent event) {
				showColor = colorSelection.getSelectedIndex();
				if (showColor < 0) {
					showColor = 0;
				}
				recalculateBackgroundImage();
				repaint();
			}
		});

		progressBar.setToolTipText("Shows the progress of the SOM calculation.");
		progressBar.setMinimum(0);
		progressBar.setMaximum(100);
		this.setDoubleBuffered(true);
	}

	@Override
	public void forcePlotGeneration() {
		startCalculation(false);
	}

	protected Object readResolve() {
		this.data = new RandomDataContainer();
		this.colorizer = new SOMMatrixColorizer[] { new SOMLandscapeColorizer(), new SOMGreyColorizer(),
				new SOMFireColorizer() };
		return this;
	}

	public void setColoredPoints(boolean coloredPoints) {
		this.coloredPoints = coloredPoints;
	}

	public void setMatrixType(int matrixType) {
		this.showMatrix = matrixType;
	}

	public void startCalculation(boolean threadMode) {
		show = false;
		try {
			dimensions[0] = Integer.parseInt(dimensionX.getText());
		} catch (NumberFormatException ex) {
			SwingTools.showVerySimpleErrorMessage("som.only_nr_width");
			return;
		}
		try {
			dimensions[1] = Integer.parseInt(dimensionY.getText());
		} catch (NumberFormatException ex) {
			SwingTools.showVerySimpleErrorMessage("som.only_nr_height");
			return;
		}
		int adaptationRadius = 15;
		try {
			adaptationRadius = Integer.parseInt(radiusSelection.getText());
		} catch (NumberFormatException ex) {
			SwingTools.showVerySimpleErrorMessage("som.only_nr_radius");
			return;
		}
		int trainRounds = 25;
		try {
			trainRounds = Integer.parseInt(roundSelection.getText());
		} catch (NumberFormatException ex) {
			SwingTools.showVerySimpleErrorMessage("som.only_nr_rounds");
			return;
		}
		prepareSOM(dataTable, adaptationRadius, trainRounds, threadMode);
	}

	@Override
	public PlotterCondition getPlotterCondition() {
		return new BasicPlotterCondition();
	}

	@Override
	public void paintComponent(Graphics graphics) {
		super.paintComponent(graphics);

		// painting only if approved
		if (show) {
			paintSom(graphics);
		}
	}

	public void paintSom(Graphics graphics) {
		// init graphics
		Graphics2D g = (Graphics2D) graphics;

		int pixWidth = getWidth() - 2 * MARGIN;
		int pixHeight = getHeight() - 2 * MARGIN;

		// painting background
		g.drawImage(this.image, MARGIN, MARGIN, pixWidth, pixHeight, Color.WHITE, null);

		// painting points
		drawPoints(g);

		// painting Legend
		drawLegend(graphics, this.dataTable, colorColumn);

		// paint Tooltip
		drawToolTip((Graphics2D) graphics);
	}

	protected void drawPoints(Graphics2D g) {
		int pixWidth = getWidth() - 2 * MARGIN;
		int pixHeight = getHeight() - 2 * MARGIN;
		// painting data points
		if (colorColumn >= 0) {
			Iterator<DataTableRow> iterator = dataTable.iterator();
			// Finding Scalevalue for Color Column
			double minColorValue = Double.POSITIVE_INFINITY;
			double maxColorValue = Double.NEGATIVE_INFINITY;

			while (iterator.hasNext()) {
				double value = iterator.next().getValue(colorColumn);
				minColorValue = MathFunctions.robustMin(minColorValue, value);
				maxColorValue = MathFunctions.robustMax(maxColorValue, value);
			}

			// remember point positions
			if (!examplesApplied) {
				int[][] exampleCount = new int[dimensions[0]][dimensions[1]];
				exampleCoordinates.clear();
				iterator = dataTable.iterator();
				int index = 0;
				while (iterator.hasNext()) {
					DataTableRow row = iterator.next();
					// deleting special attributes of a row (ID, Class)
					double[] example = getDoubleArrayFromRow(row, dataTable);
					int[] coords = net.apply(example);
					exampleCoordinates.add(new ExamplePlotterPoint(index, coords[0], coords[1]));
					exampleCount[coords[0]][coords[1]]++;
					index++;
				}
				examplesApplied = true;
				// painting if already applied
			}

			// draw points
			double fieldWidth = pixWidth / (double) dimensions[0];
			double fieldHeight = pixHeight / (double) dimensions[1];
			Iterator<ExamplePlotterPoint> exampleIterator = exampleCoordinates.iterator();
			RandomGenerator random = RandomGenerator.getRandomGenerator(true, 2001);
			ColorProvider colorProvider = getColorProvider();
			while (exampleIterator.hasNext()) {
				ExamplePlotterPoint point = exampleIterator.next();
				double color = 1.0d;
				Color borderColor = Color.BLACK;
				if (coloredPoints) {
					color = colorProvider.getPointColorValue(this.dataTable, dataTable.getRow(point.getDataTableIndex()),
							colorColumn, minColorValue, maxColorValue);
					borderColor = colorProvider.getPointBorderColor(this.dataTable,
							dataTable.getRow(point.getDataTableIndex()), colorColumn);
				}
				double pertX = 0.0d;
				double pertY = 0.0d;
				if (jitterAmount > 0) {
					pertX = random.nextDoubleInRange(-fieldWidth / 2.0d, fieldWidth / 2.0d) * (jitterAmount / 50.0d);
					pertY = random.nextDoubleInRange(-fieldHeight / 2.0d, fieldHeight / 2.0d) * (jitterAmount / 50.0d);
				}
				point.setCurrentPertubatedX((int) (MARGIN + pertX + point.getX() * fieldWidth + fieldWidth / 2.0d));
				point.setCurrentPertubatedY((int) (MARGIN + pertY + point.getY() * fieldHeight + fieldHeight / 2.0d));

				drawPoint(g, point.getCurrentPertubatedX(), point.getCurrentPertubatedY(),
						colorProvider.getPointColor(color), borderColor);
			}
		}
	}

	@Override
	public void setDataTable(DataTable dataTable) {
		super.setDataTable(dataTable);
		this.dataTable = dataTable;
	}

	/** Returns true. */
	@Override
	public boolean canHandleJitter() {
		return true;
	}

	/** Sets the level of jitter and initiates a repaint. */
	@Override
	public void setJitter(int jitter) {
		this.jitterAmount = jitter;
		repaint();
	}

	public void prepareSOM(DataTable dataTable, double adaptationRadius, int trainRounds, boolean threadMode) {
		// reseting Data already applied flag
		examplesApplied = false;
		// generating data for SOM
		int dataDimension = 0;
		synchronized (dataTable) {
			Iterator<DataTableRow> iterator = dataTable.iterator();
			dataDimension = dataTable.getNumberOfColumns() - dataTable.getNumberOfSpecialColumns();

			iterator = dataTable.iterator();
			while (iterator.hasNext()) {
				data.addData(getDoubleArrayFromRow(iterator.next(), dataTable));
			}
		}
		// generating SOM
		net = new KohonenNet(data);
		RitterAdaptation adaptationFunction = new RitterAdaptation();
		adaptationFunction.setAdaptationRadiusStart(adaptationRadius);
		adaptationFunction.setLearnRateStart(0.8);
		net.setAdaptationFunction(adaptationFunction);
		net.init(dataDimension, dimensions, false);

		net.setTrainingRounds(trainRounds);

		// train SOM
		if (threadMode) {
			// registering this as ProgressListener
			net.addProgressListener(this);
			Thread trainThread = new Thread() {

				@Override
				public void run() {
					try {
						net.train(null);
					} catch (ProcessStoppedException e) {
						calculationAborted();
					}
				}
			};
			trainThread.start();
		} else {
			net.train();
			createMatrices();
			try {
				// necessary for preventing graphical errors in reporting
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// do nothing
			}
		}
	}

	/**
	 * This Method removes the ProgressListener from the current {@link KohonenNet} and sets it to
	 * null to prevent the use of an incomplete {@link KohonenNet}.
	 */
	private void calculationAborted() {
		net.removeProgressListener(this);
		net = null;
		setProgress(0);
	}

	protected void createMatrices() {
		uMatrix = getUMatrix(net, dimensions);
		pMatrix = getPMatrix(net, data, dimensions);

		// searching maximum distance between nodes
		maxU = 0;
		for (int i = 0; i < dimensions[0]; i++) {
			for (int j = 0; j < dimensions[1]; j++) {
				maxU = Math.max(maxU, uMatrix[i][j]);
			}
		}

		// searching maximum density and mean density
		maxP = 0;
		double meanP = 0;
		for (int i = 0; i < dimensions[0]; i++) {
			for (int j = 0; j < dimensions[1]; j++) {
				maxP = maxP < pMatrix[i][j] ? pMatrix[i][j] : maxP;
				meanP += pMatrix[i][j];
			}
		}
		meanP /= dimensions[0] * dimensions[1];

		// calculating u* Matrix
		uStarMatrix = getUStarMatrix(uMatrix, pMatrix, meanP, maxP, dimensions);

		// searching maximum of U* matrix
		maxUStar = 0;
		for (int i = 0; i < dimensions[0]; i++) {
			for (int j = 0; j < dimensions[1]; j++) {
				maxUStar = Math.max(maxUStar, uStarMatrix[i][j]);
			}
		}

		// create background image
		recalculateBackgroundImage();

		this.show = true;
	}

	protected void recalculateBackgroundImage() {
		Vector<double[][]> printMatrix = new Vector<double[][]>();
		double[] printScale = { maxU, maxP, maxUStar };
		printMatrix.add(uMatrix);
		printMatrix.add(pMatrix);
		printMatrix.add(uStarMatrix);
		// painting Matrix
		int image_width = IMAGE_WIDTH / dimensions[0] * dimensions[0];
		int image_height = IMAGE_HEIGHT / dimensions[1] * dimensions[1];
		this.image = new BufferedImage(image_width, image_height, BufferedImage.TYPE_INT_RGB);
		int width = image_width / dimensions[0];
		int height = image_height / dimensions[1];
		for (int i = 0; i < dimensions[0]; i++) {
			for (int j = 0; j < dimensions[1]; j++) {
				interpolateRect(image, width * i, height * j, width, height, printMatrix.elementAt(showMatrix), i, j,
						printScale[showMatrix], colorizer[showColor]);
			}
		}
	}

	protected void interpolateRect(BufferedImage image, int posX, int posY, double width, double height, double[][] matrix,
			int matrixX, int matrixY, double colorScale, SOMMatrixColorizer colorizer) {
		// top-left
		if (matrix != null) {
			double p11 = matrix[matrixX][matrixY];
			double p21 = matrix[(matrixX + 1) % this.dimensions[0]][matrixY];
			double p12 = matrix[matrixX][(matrixY + 1) % this.dimensions[1]];
			double p22 = matrix[(matrixX + 1) % this.dimensions[0]][(matrixY + 1) % this.dimensions[1]];
			for (int i = 0; i < width; i += 1) {
				for (int j = 0; j < height; j += 1) {
					double interpolatedValue = (p11 * (width - i) * (height - j) + p21 * i * (height - j) + p12
							* (width - i) * j + p22 * i * j)
							/ (height * width);
					double colorValue = interpolatedValue / colorScale;
					// colorValue = Math.min(1.0d, Math.max(0.0d, colorValue));
					int rgbColor = colorizer.getPointColor(colorValue).getRGB();
					image.setRGB(posX + i, posY + j, rgbColor);
				}
			}
		}
	}

	private double[] getDoubleArrayFromRow(DataTableRow row, DataTable table) {
		double[] doubleRow = new double[table.getNumberOfColumns() - table.getNumberOfSpecialColumns()];
		int index = 0;
		for (int i = 0; i < row.getNumberOfValues(); i++) {
			if (!table.isSpecial(i)) {
				doubleRow[index] = row.getValue(i);
				index++;
			}
		}
		return doubleRow;
	}

	private double[][] getUMatrix(KohonenNet net, int[] dimensions) {
		double[][] uMatrix = new double[dimensions[0]][dimensions[1]];
		// getting distances between nodes
		for (int i = 0; i < dimensions[0]; i++) {
			for (int j = 0; j < dimensions[1]; j++) {
				uMatrix[i][j] = net.getNodeDistance(net.getIndexOfCoordinates(new int[] { i, j }));
			}
		}
		return uMatrix;
	}

	private double[][] getPMatrix(KohonenNet net, RandomDataContainer data, int[] dimensions) {
		// calculating real paretoradius
		int n = data.countData();
		double optimalMedian = 0.2013 * n;
		double estimatedRadius = 0;
		// calculating distances between every example
		double[] distances = new double[n * n];
		for (int i = 0; i < n; i++) {
			for (int j = 0; j < n; j++) {
				distances[i * n + j] = net.getDistance(data.get(i), data.get(j));
			}
		}
		Arrays.sort(distances);
		double percentilSetDifference = Double.POSITIVE_INFINITY;
		// finding percentil, closest to paretoradius
		double radius;
		for (int percentil = 0; percentil < 100; percentil++) {
			int[] nn = new int[n];
			radius = distances[(int) Math.round((double) (percentil * n * n) / 100)];
			for (int i = 0; i < n; i++) {
				for (int j = 0; j < n; j++) {
					if (net.getDistance(data.get(i), data.get(j)) <= radius) {
						nn[i]++;
					}
				}
			}
			Arrays.sort(nn);
			int currentMedian = nn[n / 2] - 1; // point himself is no real neighbour, but always
			// nearest point
			if (Math.abs(currentMedian - optimalMedian) <= percentilSetDifference) {
				percentilSetDifference = Math.abs(currentMedian - optimalMedian);
			} else {
				estimatedRadius = radius;
				break;
			}
		}
		// generating P Matrix
		double[][] pMatrix = new double[dimensions[0]][dimensions[1]];
		for (int i = 0; i < dimensions[0]; i++) {
			for (int j = 0; j < dimensions[1]; j++) {
				double nodeWeight[] = net.getNodeWeights(new int[] { i, j });
				int neighbours = 0;
				for (int x = 0; x < n; x++) {
					if (net.getDistance(data.get(x), nodeWeight) < estimatedRadius) {
						neighbours++;
					}
				}
				pMatrix[i][j] = (double) neighbours / n;
			}
		}
		return pMatrix;
	}

	private double[][] getUStarMatrix(double[][] uMatrix, double[][] pMatrix, double meanP, double maxP, int[] dimensions) {
		double[][] uStarMatrix = new double[dimensions[0]][dimensions[1]];
		for (int i = 0; i < dimensions[0]; i++) {
			for (int j = 0; j < dimensions[1]; j++) {
				uStarMatrix[i][j] = uMatrix[i][j] * ((pMatrix[i][j] - meanP) / (meanP - maxP) + 1);
			}
		}
		return uStarMatrix;
	}

	@Override
	public JComponent getOptionsComponent(int index) {
		switch (index) {
			case 0:
				JLabel label = new JLabel("Matrix");
				label.setToolTipText("Select the matrix type which should be visualized.");
				return label;
			case 1:
				return matrixSelection;
			case 2:
				label = new JLabel("Style");
				label.setToolTipText("Select the color scheme used for the visualization of the matrix values.");
				return label;
			case 3:
				return colorSelection;
			case 4:
				JPanel dimensionLabelPanel = new JPanel();
				dimensionLabelPanel.setToolTipText("Set the dimensions of the Kohonen net.");
				dimensionLabelPanel.setLayout(new GridLayout());
				dimensionLabelPanel.add(new JLabel("Net Width"));
				dimensionLabelPanel.add(new JLabel("Net Height"));
				return dimensionLabelPanel;
			case 5:
				JPanel dimensionPanel = new JPanel();
				dimensionPanel.setLayout(new GridLayout());
				dimensionX.setToolTipText("Set the dimensions of the Kohonen net.");
				dimensionY.setToolTipText("Set the dimensions of the Kohonen net.");
				dimensionPanel.add(dimensionX);
				dimensionPanel.add(dimensionY);
				return dimensionPanel;
			case 6:
				JPanel roundPanel = new JPanel();
				roundPanel.setToolTipText("Set the number of training rounds of the Kohonen net.");
				roundSelection.setToolTipText("Set the number of training rounds of the Kohonen net.");
				roundPanel.setLayout(new GridLayout());
				roundPanel.add(new JLabel("Training Rounds"));
				roundPanel.add(roundSelection);
				return roundPanel;
			case 7:
				JPanel radiusPanel = new JPanel();
				radiusPanel.setToolTipText("Set the adaptation radius of the Kohonen net.");
				radiusSelection.setToolTipText("Set the adaptation radius of the Kohonen net.");
				radiusPanel.setLayout(new GridLayout());
				radiusPanel.add(new JLabel("Adaptation Radius"));
				radiusPanel.add(radiusSelection);
				return radiusPanel;
			case 8:
				return progressBar;
			case 9:
				return approveButton;
			case 10:
				return abortButton;
		}
		return null;
	}

	@Override
	public void setPlotColumn(int column, boolean plot) {
		if (plot) {
			colorColumn = column;
			repaint();
		}
	}

	@Override
	public boolean getPlotColumn(int dimension) {
		if (dimension == colorColumn) {
			return true;
		}
		return false;
	}

	@Override
	public String getPlotName() {
		return "Point Color";
	}

	@Override
	public void setProgress(int value) {
		progressBar.setValue(value);
	}

	@Override
	public void progressFinished() {
		net.removeProgressListener(this);
		createMatrices();
		setProgress(100);
		repaint();
	}

	@Override
	public String getIdForPos(int x, int y) {
		if (this.show) {
			ExamplePlotterPoint point = getPlotterPointForPos(x, y);
			if (point != null) {
				return dataTable.getRow(point.getDataTableIndex()).getId();
			}
		}
		return null;
	}

	private ExamplePlotterPoint getPlotterPointForPos(int x, int y) {
		Iterator<ExamplePlotterPoint> exampleIterator = exampleCoordinates.iterator();
		while (exampleIterator.hasNext()) {
			ExamplePlotterPoint point = exampleIterator.next();
			if (point.contains(x, y)) {
				return point;
			}
		}
		return null;
	}

	/** Sets the mouse position in the shown data space. */
	@Override
	public void setMousePosInDataSpace(int x, int y) {
		if (show) {
			ExamplePlotterPoint point = getPlotterPointForPos(x, y);
			if (point != null) {
				String id = dataTable.getRow(point.getDataTableIndex()).getId();
				if (id != null) {
					setToolTip(id, point.getCurrentPertubatedX(), point.getCurrentPertubatedY());
				} else {
					setToolTip(null, 0.0d, 0.0d);
				}
			} else {
				setToolTip(null, 0.0d, 0.0d);
			}
		}
	}

	private void setToolTip(String toolTip, double x, double y) {
		this.currentToolTip = toolTip;
		this.toolTipX = x;
		this.toolTipY = y;
		repaint();
	}

	protected void drawToolTip(Graphics2D g) {
		if (currentToolTip != null) {
			g.setFont(LABEL_FONT);
			Rectangle2D stringBounds = LABEL_FONT.getStringBounds(currentToolTip, g.getFontRenderContext());
			g.setColor(TOOLTIP_COLOR);
			Rectangle2D bg = new Rectangle2D.Double(toolTipX - stringBounds.getWidth() - 15, toolTipY
					- stringBounds.getHeight() / 2, stringBounds.getWidth() + 6, Math.abs(stringBounds.getHeight()) + 4);
			g.fill(bg);
			g.setColor(Color.black);
			g.draw(bg);
			g.drawString(currentToolTip, (float) (toolTipX - stringBounds.getWidth() - 12),
					(float) (toolTipY + stringBounds.getHeight() * 0.5 + 1));
		}
	}

	@Override
	public String getPlotterName() {
		return PlotterConfigurationModel.SOM_PLOT;
	}
}
