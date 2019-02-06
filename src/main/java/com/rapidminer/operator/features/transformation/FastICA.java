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
package com.rapidminer.operator.features.transformation;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.Statistics;
import com.rapidminer.operator.Model;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ProcessSetupError.Severity;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetPassThroughRule;
import com.rapidminer.operator.ports.metadata.ExampleSetPrecondition;
import com.rapidminer.operator.ports.metadata.GenerateNewMDRule;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.operator.ports.metadata.SimpleMetaDataError;
import com.rapidminer.operator.ports.quickfix.ParameterSettingQuickFix;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.parameter.conditions.EqualTypeCondition;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.RandomGenerator;
import com.rapidminer.tools.math.MathFunctions;

import Jama.Matrix;
import Jama.SingularValueDecomposition;


/**
 * This operator performs the independent componente analysis (ICA). Implementation of the
 * FastICA-algorithm of Hyvaerinen und Oja. The operator outputs a <code>FastICAModel</code>. With
 * the <code>ModelApplier</code> you can transform the features.
 *
 * @author Daniel Hakenjos, Ingo Mierswa
 * @see FastICAModel
 */
public class FastICA extends Operator {

	/**
	 * The parameter name for &quot;Number components to be extracted (-1 number of attributes is
	 * used).&quot;
	 */
	public static final String PARAMETER_NUMBER_OF_COMPONENTS = "number_of_components";

	/**
	 * The parameter name for &quot;If 'parallel' the components are extracted simultaneously,
	 * 'deflation' the components are extracted one at a time&quot;
	 */
	public static final String PARAMETER_ALGORITHM_TYPE = "algorithm_type";

	/**
	 * The parameter name for &quot;The functional form of the G function used in the approximation
	 * to neg-entropy&quot;
	 */
	public static final String PARAMETER_FUNCTION = "function";

	/**
	 * The parameter name for &quot;constant in range [1, 2] used in approximation to neg-entropy
	 * when fun="logcosh"&quot;
	 */
	public static final String PARAMETER_ALPHA = "alpha";

	/** The parameter name for &quot;Indicates whether rows of the data matrix &quot; */
	public static final String PARAMETER_ROW_NORM = "row_norm";

	/** The parameter name for &quot;maximum number of iterations to perform&quot; */
	public static final String PARAMETER_MAX_ITERATION = "max_iteration";

	/** The parameter name for &quot;A positive scalar giving the tolerance at which &quot; */
	public static final String PARAMETER_TOLERANCE = "tolerance";

	public static final String PARAMETER_REDUCTION_TYPE = "dimensionality_reduction";

	public static final String[] REDUCTION_METHODS = new String[] { "none", "fixed number" };

	public static final int REDUCTION_NONE = 0;
	public static final int REDUCTION_FIXED = 1;

	private static final String[] ALGORITHM_TYPE = new String[] { "deflation", "parallel" };

	private static final String[] FUNCTION = new String[] { "logcosh", "exp" };

	private InputPort exampleSetInput = getInputPorts().createPort("example set input");

	private OutputPort exampleSetOutput = getOutputPorts().createPort("example set output");
	private OutputPort originalOutput = getOutputPorts().createPort("original");
	private OutputPort modelOutput = getOutputPorts().createPort("preprocessing model");

	public FastICA(OperatorDescription description) {
		super(description);

		exampleSetInput.addPrecondition(new ExampleSetPrecondition(exampleSetInput, Ontology.NUMERICAL) {

			@Override
			public void makeAdditionalChecks(ExampleSetMetaData emd) throws UndefinedParameterError {
				int desiredComponents = getParameterAsInt(PARAMETER_NUMBER_OF_COMPONENTS);
				if (desiredComponents > emd.getNumberOfRegularAttributes()
						&& getParameterAsInt(PARAMETER_REDUCTION_TYPE) == REDUCTION_FIXED) {
					if (emd.getAttributeSetRelation() != SetRelation.UNKNOWN) {
						Severity sev = Severity.ERROR;
						if (emd.getAttributeSetRelation() == SetRelation.SUPERSET) {
							sev = Severity.WARNING;
						}
						exampleSetInput.addError(new SimpleMetaDataError(sev, exampleSetInput,
								Collections.singletonList(new ParameterSettingQuickFix(FastICA.this,
										PARAMETER_NUMBER_OF_COMPONENTS, emd.getNumberOfRegularAttributes() + "")),
								"exampleset.parameters.need_more_attributes", desiredComponents,
								PARAMETER_NUMBER_OF_COMPONENTS, desiredComponents));
					}
				}
				super.makeAdditionalChecks(emd);
			}
		});

		getTransformer().addRule(new ExampleSetPassThroughRule(exampleSetInput, originalOutput, SetRelation.EQUAL));
		getTransformer().addRule(new ExampleSetPassThroughRule(exampleSetInput, exampleSetOutput, SetRelation.SUBSET) {

			@Override
			public ExampleSetMetaData modifyExampleSet(ExampleSetMetaData metaData) {
				int numberOfAttributes = 0;
				// removing all non special
				Iterator<AttributeMetaData> iterator = metaData.getAllAttributes().iterator();
				while (iterator.hasNext()) {
					AttributeMetaData current = iterator.next();
					if (!current.isSpecial()) {
						iterator.remove();
						numberOfAttributes++;
					}
				}
				// adding newly generated components
				try {
					int numberOfGeneratedAttributes;
					if (getParameterAsInt(PARAMETER_REDUCTION_TYPE) == REDUCTION_FIXED) {
						numberOfGeneratedAttributes = getParameterAsInt(PARAMETER_NUMBER_OF_COMPONENTS);
					} else {
						numberOfGeneratedAttributes = numberOfAttributes;
					}

					for (int i = 1; i <= numberOfGeneratedAttributes; i++) {
						AttributeMetaData generatedAttribute = new AttributeMetaData("ic_" + i, Ontology.NUMERICAL);
						metaData.addAttribute(generatedAttribute);
					}
					// knowing all remaining regular attributes
					metaData.attributesAreKnown();
					return metaData;
				} catch (UndefinedParameterError e) {
					return metaData;
				}
			}
		});
		getTransformer().addRule(new GenerateNewMDRule(modelOutput, Model.class));
	}

	@Override
	public void doWork() throws OperatorException {
		int algorithmType;
		int function;
		int numberOfComponents;
		double tolerance;
		double alpha;
		boolean rowNorm;
		int maxIteration;
		int numberOfSamples, numberOfAttributes;
		Attribute[] attributes;
		double[] means;
		double[][] data;
		double[][] wInit;

		// get the ExampleSet
		ExampleSet set = exampleSetInput.getData(ExampleSet.class);
		set.recalculateAllAttributeStatistics();
		numberOfSamples = set.size();
		numberOfAttributes = set.getAttributes().size();

		// all attributes numerical
		attributes = new Attribute[numberOfAttributes];
		means = new double[numberOfAttributes];
		int i = 0;
		Iterator<Attribute> atts = set.getAttributes().iterator();
		while (atts.hasNext()) {
			attributes[i] = atts.next();
			if (!attributes[i].isNumerical()) {
				throw new UserError(this, 104, new Object[] { "FastICA", attributes[i].getName() });
			}
			means[i] = set.getStatistics(attributes[i], Statistics.AVERAGE);
			i++;
		}

		// get the parameter
		algorithmType = getParameterAsInt(PARAMETER_ALGORITHM_TYPE);
		function = getParameterAsInt(PARAMETER_FUNCTION);
		tolerance = getParameterAsDouble(PARAMETER_TOLERANCE);
		alpha = getParameterAsDouble(PARAMETER_ALPHA);
		rowNorm = getParameterAsBoolean(PARAMETER_ROW_NORM);
		maxIteration = getParameterAsInt(PARAMETER_MAX_ITERATION);
		if (getParameterAsInt(PARAMETER_REDUCTION_TYPE) == REDUCTION_FIXED) {
			numberOfComponents = getParameterAsInt(PARAMETER_NUMBER_OF_COMPONENTS);
		} else {
			numberOfComponents = numberOfAttributes;
		}

		if (numberOfComponents > numberOfAttributes) {
			numberOfComponents = numberOfAttributes;
			getLogger().log(Level.WARNING,
					"The parameter 'number_of_components' is too large! Set to number of attributes.");
		}

		// get the centered data
		data = new double[numberOfSamples][numberOfAttributes];

		for (int d = 0; d < numberOfAttributes; d++) {
			Iterator<Example> reader = set.iterator();
			for (int sample = 0; sample < numberOfSamples; sample++) {
				Example example = reader.next();
				data[sample][d] = example.getValue(attributes[d]) - means[d];
			}
		}

		// init the weight matrix
		wInit = new double[numberOfComponents][numberOfComponents];
		// init w randomly
		RandomGenerator randomGenerator = RandomGenerator.getRandomGenerator(this);
		for (i = 0; i < numberOfComponents; i++) {
			for (int j = 0; j < numberOfComponents; j++) {
				wInit[i][j] = randomGenerator.nextDouble() * 2 - 1;
			}
		}

		// row normalization
		// Scaling is done by dividing the rows of the data
		// by their root-mean-square. The root-mean-square for a row
		// is obtained by computing the
		// square-root of the sum-of-squares of the values in the
		// row divided by the number of values minus one.
		if (rowNorm) {
			double rms_row;
			for (int row = 0; row < numberOfSamples; row++) {
				// compute root mean square for the row
				rms_row = 0.0d;
				for (int d = 0; d < numberOfAttributes; d++) {
					rms_row += data[row][d] * data[row][d];
				}
				rms_row = Math.sqrt(rms_row) / Math.max(1, numberOfAttributes - 1);

				for (int d = 0; d < numberOfAttributes; d++) {
					data[row][d] = data[row][d] / rms_row;
				}
			}
		}

		Matrix xMatrix = new Matrix(data).transpose();

		// Whitening
		Matrix kMatrix;
		{
			SingularValueDecomposition svd = xMatrix.times(xMatrix.transpose().timesEquals(1.0d / numberOfSamples)).svd();

			Matrix dMatrix = svd.getS();
			double[][] singularvalue = dMatrix.getArray();

			for (i = 0; i < singularvalue.length; i++) {
				singularvalue[i][i] = 1.0d / Math.sqrt(singularvalue[i][i]);
			}
			dMatrix = new Matrix(singularvalue);

			kMatrix = dMatrix.times(svd.getU().transpose());
			kMatrix = new Matrix(kMatrix.getArray(), numberOfComponents, numberOfAttributes);
		}
		// end Whitening

		Matrix a;
		if (algorithmType == 0) {
			a = deflation(kMatrix.times(xMatrix), wInit, tolerance, alpha, numberOfSamples, numberOfComponents, function,
					maxIteration);
		} else {
			a = parallel(kMatrix.times(xMatrix), wInit, tolerance, alpha, numberOfSamples, numberOfComponents, function,
					maxIteration);
		}

		Matrix w = a.times(kMatrix);

		kMatrix = kMatrix.transpose();
		Matrix wMatrix = a.transpose();
		Matrix aMatrix = w.transpose().times(w.times(w.transpose()).inverse()).transpose();

		FastICAModel model = new FastICAModel(set, numberOfComponents, means, rowNorm, kMatrix, wMatrix, aMatrix);

		if (exampleSetOutput.isConnected()) {
			exampleSetOutput.deliver(model.apply(set));
		}
		originalOutput.deliver(set);
		modelOutput.deliver(model);
	}

	private Matrix deflation(Matrix X, double[][] wInit, double tolerance, double alpha, int numberOfSamples,
			int numberOfComponents, int function, int maxIteration) throws OperatorException {
		Matrix W = new Matrix(numberOfComponents, numberOfComponents, 0.0d);

		Matrix w, t, Wu, wx, gwx, xgwx, g_wx;
		double k, rss, lim, value;
		int iter;
		int iterlog = 1;
		while (maxIteration / iterlog > 10 && maxIteration / (iterlog * 10) >= 3) {
			iterlog *= 10;
		}
		for (int i = 0; i < numberOfComponents; i++) {
			w = new Matrix(wInit[i], wInit[i].length);

			if (i > 0) {
				t = new Matrix(wInit[i].length, 1, 0.0d);
				for (int u = 1; u <= i; u++) {
					Wu = W.getMatrix(u - 1, u - 1, 0, numberOfComponents - 1);
					k = w.transpose().times(Wu.transpose()).getArray()[0][0];
					t.plusEquals(Wu.times(k).transpose());
				}
				w.minusEquals(t);
			}

			rss = Math.sqrt(w.times(w.transpose()).getArray()[0][0]);
			w.timesEquals(1.0d / rss);

			lim = 1000.0d;
			iter = 1;

			while (lim > tolerance && iter <= maxIteration) {

				wx = w.transpose().times(X);
				double[][] wxarray = wx.getArray();
				if (function == 0) {
					for (int j = 0; j < wxarray[0].length; j++) {
						wxarray[0][j] = MathFunctions.tanh(alpha * wxarray[0][j]);
					}
				} else {
					for (int j = 0; j < wxarray[0].length; j++) {
						wxarray[0][j] = wxarray[0][j] * Math.exp(-0.5d * wxarray[0][j] * wxarray[0][j]);
					}
				}
				gwx = new Matrix(wxarray);
				double[][] gwxarray = gwx.getArray();
				double[][] Xarray = X.getArray();

				for (int row = 0; row < Xarray.length; row++) {
					for (int col = 0; col < Xarray[0].length; col++) {
						Xarray[row][col] = Xarray[row][col] * gwxarray[0][col];
					}
				}
				xgwx = new Matrix(Xarray);
				Matrix v1 = new Matrix(numberOfComponents, 1, 0.0d);
				double mean;
				for (int row = 0; row < numberOfComponents; row++) {
					mean = 0;
					for (int col = 0; col < numberOfSamples; col++) {
						mean += xgwx.get(row, col);
					}
					mean = mean / numberOfSamples;
					v1.set(row, 0, mean);
				}
				g_wx = wx.copy();
				mean = 0.0d;
				if (function == 0) {
					// logcosh function
					for (int j = 0; j < wxarray[0].length; j++) {
						value = MathFunctions.tanh(alpha * g_wx.get(0, j));
						value = alpha * (1.0d - value * value);
						mean += value;
						g_wx.set(0, j, value);
					}
				} else {
					// exp function
					for (int j = 0; j < wxarray[0].length; j++) {
						value = g_wx.get(0, j);
						value = (1.0d - value * value) * Math.exp(-0.5d * value * value);
						mean += value;
						g_wx.set(0, j, value);
					}
				}
				mean /= numberOfSamples;

				Matrix v2 = w.copy();
				v2.timesEquals(mean);

				Matrix w1 = v1.minus(v2);

				if (i > 0) {
					t = new Matrix(w1.getRowDimension(), w1.getColumnDimension(), 0.0d);
					for (int u = 1; u <= i; u++) {
						Wu = W.getMatrix(u - 1, u - 1, 0, numberOfComponents - 1);
						k = w1.transpose().times(Wu.transpose()).getArray()[0][0];
						t.plusEquals(Wu.times(k).transpose());
					}
					w1.minusEquals(t);
				}
				rss = Math.sqrt(w1.transpose().times(w1).getArray()[0][0]);
				w1.timesEquals(1.0d / rss);

				lim = Math.abs(Math.abs(w1.transpose().times(w).getArray()[0][0]) - 1.0d);

				if (iter % iterlog == 0 || lim <= tolerance) {
					log("Iteration " + iter + ", tolerance = " + lim);
				}
				iter++;

				w = w1.copy();
			}
			for (int col = 0; col < numberOfComponents; col++) {
				W.set(i, col, w.get(col, 0));
			}
			checkForStop();
		}
		return W;
	}

	private Matrix parallel(Matrix X, double[][] wInit, double tolerance, double alpha, int numberOfSamples,
			int numberOfComponents, int function, int maxIteration) {
		int p = X.getColumnDimension();
		Matrix W = new Matrix(wInit);

		SingularValueDecomposition svd = W.svd();

		double[] svalues = svd.getSingularValues();
		Matrix svaluesMatrix = new Matrix(svalues.length, svalues.length, 0.0d);
		for (int i = 0; i < svalues.length; i++) {
			svalues[i] = 1 / svalues[i];
			svaluesMatrix.set(i, i, svalues[i]);
		}
		W = svd.getU().times(svaluesMatrix).times(svd.getU().transpose()).times(W);
		Matrix W1 = W.copy();

		double lim = 1000.0d;

		int iter = 1;
		int iterlog = 1;
		while (maxIteration / iterlog > 10 && maxIteration / (iterlog * 10) >= 3) {
			iterlog *= 10;
		}

		Matrix wx, gwx, v1, g_wx, diagmean, v2;
		double value, mean;
		while (lim > tolerance && iter <= maxIteration) {
			wx = W.times(X);

			gwx = wx.copy();
			if (function == 0) {
				for (int row = 0; row < numberOfComponents; row++) {
					for (int col = 0; col < numberOfSamples; col++) {
						value = gwx.get(row, col);
						value = MathFunctions.tanh(alpha * value);
						gwx.set(row, col, value);
					}
				}
			} else {
				for (int row = 0; row < numberOfComponents; row++) {
					for (int col = 0; col < numberOfSamples; col++) {
						value = gwx.get(row, col);
						value = value * Math.exp(-0.5d * value * value);
						gwx.set(row, col, value);
					}
				}
			}

			v1 = gwx.times(X.transpose()).times(p);
			g_wx = gwx.copy();

			diagmean = new Matrix(numberOfComponents, numberOfComponents, 0.0d);
			if (function == 0) {
				// logcosh funtion
				for (int row = 0; row < numberOfComponents; row++) {
					mean = 0.0d;
					for (int col = 0; col < numberOfSamples; col++) {
						value = g_wx.get(row, col);
						value = alpha * (1.0d - value * value);
						g_wx.set(row, col, value);
						mean += value;
					}
					mean = mean / numberOfSamples;
					diagmean.set(row, row, mean);
				}
			} else {
				// exp function
				g_wx = wx.copy();
				for (int row = 0; row < numberOfComponents; row++) {
					mean = 0.0d;
					for (int col = 0; col < numberOfSamples; col++) {
						value = g_wx.get(row, col);
						value = (1.0d - value * value) * Math.exp(-0.5d * value * value);
						g_wx.set(row, col, value);
						mean += value;
					}
					mean = mean / numberOfSamples;
					diagmean.set(row, row, mean);
				}
			}

			v2 = diagmean.times(W);
			W1 = v1.minus(v2);
			svd = W1.svd();
			svalues = svd.getSingularValues();
			svaluesMatrix = new Matrix(svalues.length, svalues.length, 0.0d);
			for (int i = 0; i < svalues.length; i++) {
				svalues[i] = 1 / svalues[i];
				svaluesMatrix.set(i, i, svalues[i]);
			}
			W1 = svd.getU().times(svaluesMatrix).times(svd.getU().transpose()).times(W1);

			double[][] diag = W1.times(W.transpose()).getArray();
			value = Double.NEGATIVE_INFINITY;
			for (int row = 0; row < numberOfComponents; row++) {
				value = Math.max(value, Math.abs(Math.abs(diag[row][row]) - 1.0d));
			}
			lim = value;

			W = W1.copy();

			if (iter % iterlog == 0 || lim <= tolerance) {
				log("Iteration " + iter + ", tolerance = " + lim);
			}
			iter++;
		}
		return W;
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();

		ParameterType type = new ParameterTypeCategory(PARAMETER_REDUCTION_TYPE,
				"Indicates which type of dimensionality reduction should be applied", REDUCTION_METHODS, REDUCTION_NONE);
		type.setExpert(false);
		types.add(type);

		type = new ParameterTypeInt(PARAMETER_NUMBER_OF_COMPONENTS, "Keep this number of components.", 1, Integer.MAX_VALUE,
				true);
		type.registerDependencyCondition(
				new EqualTypeCondition(this, PARAMETER_REDUCTION_TYPE, REDUCTION_METHODS, true, REDUCTION_FIXED));
		type.setExpert(false);
		types.add(type);

		type = new ParameterTypeCategory(PARAMETER_ALGORITHM_TYPE,
				"If 'parallel' the components are extracted simultaneously, 'deflation' the components are extracted one at a time",
				ALGORITHM_TYPE, 0);
		types.add(type);

		type = new ParameterTypeCategory(PARAMETER_FUNCTION,
				"The functional form of the G function used in the approximation to neg-entropy", FUNCTION, 0);
		types.add(type);

		type = new ParameterTypeDouble(PARAMETER_ALPHA,
				"constant in range [1, 2] used in approximation to neg-entropy when fun=\"logcosh\"", 1.0d, 2.0d, 1.0d);
		types.add(type);

		type = new ParameterTypeBoolean(PARAMETER_ROW_NORM,
				"Indicates whether rows of the data matrix " + "should be standardized beforehand.", false);
		types.add(type);

		type = new ParameterTypeInt(PARAMETER_MAX_ITERATION, "maximum number of iterations to perform", 0, Integer.MAX_VALUE,
				200);
		types.add(type);

		type = new ParameterTypeDouble(PARAMETER_TOLERANCE,
				"A positive scalar giving the tolerance at which " + "the un-mixing matrix is considered to have converged.",
				0.0d, Double.POSITIVE_INFINITY, 1e-4);
		types.add(type);

		types.addAll(RandomGenerator.getRandomGeneratorParameters(this));
		return types;
	}
}
