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
package com.rapidminer.test.asserter;

import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.junit.Assert;
import org.junit.ComparisonFailure;

import com.rapidminer.adaption.belt.IOTable;
import com.rapidminer.adaption.belt.TableViewingTools;
import com.rapidminer.belt.table.BeltConverter;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.SparseDataRow;
import com.rapidminer.operator.IOObject;
import com.rapidminer.operator.IOObjectCollection;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.clustering.Centroid;
import com.rapidminer.operator.clustering.CentroidClusterModel;
import com.rapidminer.operator.clustering.Cluster;
import com.rapidminer.operator.learner.associations.FrequentItemSet;
import com.rapidminer.operator.learner.associations.FrequentItemSets;
import com.rapidminer.operator.learner.functions.LinearRegressionModel;
import com.rapidminer.operator.meta.ParameterSet;
import com.rapidminer.operator.meta.ParameterValue;
import com.rapidminer.operator.nio.file.FileObject;
import com.rapidminer.operator.performance.PerformanceCriterion;
import com.rapidminer.operator.performance.PerformanceVector;
import com.rapidminer.operator.visualization.dependencies.ANOVAMatrix;
import com.rapidminer.operator.visualization.dependencies.NumericalMatrix;
import com.rapidminer.test_utils.Asserter;
import com.rapidminer.test_utils.RapidAssert;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.math.AnovaCalculator.AnovaSignificanceTestResult;
import com.rapidminer.tools.math.Averagable;
import com.rapidminer.tools.math.AverageVector;


/**
 * @author Marius Helf
 *
 */
public class AsserterFactoryRapidMiner implements AsserterFactory {

	@Override
	public List<Asserter> createAsserters() {
		List<Asserter> asserters = new LinkedList<Asserter>();

		/* asserter for ParameterSet */
		asserters.add(new Asserter() {

			@Override
			public Class<?> getAssertable() {
				return ParameterSet.class;
			}

			@Override
			public void assertEquals(String message, Object expectedObj, Object actualObj) {
				ParameterSet expected = (ParameterSet) expectedObj;
				ParameterSet actual = (ParameterSet) actualObj;

				RapidAssert.assertEquals(message + " (performance vectors do not match)", expected.getPerformance(),
						actual.getPerformance());

				Iterator<ParameterValue> expectedIt = expected.getParameterValues();
				Iterator<ParameterValue> actualIt = actual.getParameterValues();

				while (expectedIt.hasNext()) {
					Assert.assertTrue(message + "(expected parameter vector is longer than actual parameter vector)",
							actualIt.hasNext());
					ParameterValue expectedParValue = expectedIt.next();
					ParameterValue actualParValue = actualIt.next();
					RapidAssert.assertEquals(message + " (parameter values)", expectedParValue, actualParValue);
				}
				Assert.assertFalse(message + "(expected parameter vector is shorter than actual parameter vector)",
						actualIt.hasNext());
			}
		});

		/* asserter for PerformanceCriterion */
		asserters.add(new Asserter() {

			/**
			 * Tests for equality by testing all averages, standard deviation and variances, as well
			 * as the fitness, max fitness and example count.
			 *
			 * @param message
			 *            message to display if an error occurs
			 * @param expected
			 *            expected criterion
			 * @param actual
			 *            actual criterion
			 */
			@Override
			public void assertEquals(String message, Object expectedObj, Object actualObj) {
				PerformanceCriterion expected = (PerformanceCriterion) expectedObj;
				PerformanceCriterion actual = (PerformanceCriterion) actualObj;

				List<Asserter> averegableAsserter = RapidAssert.ASSERTER_REGISTRY.getAsserterForClass(Averagable.class);
				if (averegableAsserter != null) {
					for (Asserter asserter : averegableAsserter) {
						asserter.assertEquals(message, expected, actual);
					}
				} else {
					throw new ComparisonFailure("Comparison of " + Averagable.class.toString() + " is not supported. ",
							expectedObj.toString(), actualObj.toString());
				}
				assertDouble(message + " (fitness is not equal)", expected.getFitness(), actual.getFitness());
				assertDouble(message + " (max fitness is not equal)", expected.getMaxFitness(), actual.getMaxFitness());
				assertDouble(message + " (example count is not equal)", expected.getExampleCount(), actual.getExampleCount());
			}

			@Override
			public Class<?> getAssertable() {
				return PerformanceCriterion.class;
			}
		});

		asserters.add(new Asserter() {

			/**
			 * Tests for equality by testing all averages, standard deviation and variances.
			 *
			 * @param message
			 *            message to display if an error occurs
			 * @param expected
			 *            expected averagable
			 * @param actual
			 *            actual averagable
			 */
			@Override
			public void assertEquals(String message, Object expectedObj, Object actualObj) {
				Averagable expected = (Averagable) expectedObj;
				Averagable actual = (Averagable) actualObj;

				assertDouble(message + " (average is not equal)", expected.getAverage(), actual.getAverage());
				assertDouble(message + " (macro average is not equal)", expected.getMakroAverage(), actual.getMakroAverage());
				assertDouble(message + " (micro average is not equal)", expected.getMikroAverage(), actual.getMikroAverage());
				assertDouble(message + " (average count is not equal)", expected.getAverageCount(), actual.getAverageCount());
				assertDouble(message + " (macro standard deviation is not equal)", expected.getMakroStandardDeviation(),
						actual.getMakroStandardDeviation());
				assertDouble(message + " (micro standard deviation is not equal)", expected.getMikroStandardDeviation(),
						actual.getMikroStandardDeviation());
				assertDouble(message + " (standard deviation is not equal)", expected.getStandardDeviation(),
						actual.getStandardDeviation());
				assertDouble(message + " (macro variance is not equal)", expected.getMakroVariance(),
						actual.getMakroVariance());
				assertDouble(message + " (micro variance is not equal)", expected.getMikroVariance(),
						actual.getMikroVariance());
				assertDouble(message + " (variance is not equal)", expected.getVariance(), actual.getVariance());

			}

			@Override
			public Class<?> getAssertable() {
				return Averagable.class;
			}
		});

		asserters.add(new Asserter() {

			/**
			 * Tests the two average vectors for equality by testing the size and each averagable.
			 *
			 * @param message
			 *            message to display if an error occurs
			 * @param expected
			 *            expected vector
			 * @param actual
			 *            actual vector
			 */
			@Override
			public void assertEquals(String message, Object expectedObj, Object actualObj) {
				AverageVector expected = (AverageVector) expectedObj;
				AverageVector actual = (AverageVector) actualObj;

				message = message + "Average vectors are not equals";

				int expSize = expected.getSize();
				int actSize = actual.getSize();
				Assert.assertEquals(message + " (size of the average vector is not equal)", expSize, actSize);
				int size = expSize;

				for (int i = 0; i < size; i++) {
					RapidAssert.assertEquals(message, expected.getAveragable(i), actual.getAveragable(i));
				}
			}

			@Override
			public Class<?> getAssertable() {
				return AverageVector.class;
			}

		});

		// Asserter for ExampleSet
		asserters.add(new Asserter() {

			/**
			 * Tests two example sets by iterating over all examples.
			 *
			 * @param message
			 *            message to display if an error occurs
			 * @param expected
			 *            expected value
			 * @param actual
			 *            actual value
			 */
			@Override
			public void assertEquals(String message, Object expectedObj, Object actualObj) {

				ExampleSet expected = null;
				ExampleSet actual = null;
				try {
					expected = expectedObj instanceof IOTable ?
							TableViewingTools.getView((IOTable) expectedObj) : (ExampleSet) expectedObj;
					actual = actualObj instanceof IOTable ? TableViewingTools.getView((IOTable) actualObj) :
							(ExampleSet) actualObj;
				} catch (BeltConverter.ConversionException e) {
					fail("Custom column " + e.getColumnName() + " of type " + e.getType().customTypeID() + " not " +
							"comparable");
				}

				message = message + " - ExampleSets are not equal";

				boolean compareAttributeDefaultValues = true;
				try {
					if (expected.getExampleTable().size() > 0) {
						compareAttributeDefaultValues =
								expected.getExampleTable().getDataRow(0) instanceof SparseDataRow;
					}
				} catch (RuntimeException e) {
					//Do not compare default values if table is not accessible
					compareAttributeDefaultValues = false;
				}

				// compare attributes
				RapidAssert.assertEquals(message, expected.getAttributes(), actual.getAttributes(),
						compareAttributeDefaultValues);

				// compare number of examples
				Assert.assertEquals(message + " (number of examples)", expected.size(), actual.size());

				// compare example values
				Iterator<Example> i1 = expected.iterator();
				Iterator<Example> i2 = actual.iterator();
				int row = 1;
				while (i1.hasNext() && i2.hasNext()) {
					RapidAssert.assertEquals(message + "(example number " + row + ", {0} value of {1})", i1.next(),
							i2.next());
					row++;
				}
			}

			@Override
			public Class<?> getAssertable() {
				return ExampleSet.class;
			}
		});

		asserters.add(new Asserter() {

			/**
			 * Tests the collection of ioobjects
			 *
			 * @param expected
			 * @param actual
			 */
			@Override
			public void assertEquals(String message, Object expectedObj, Object actualObj) {
				@SuppressWarnings("unchecked")
				IOObjectCollection<IOObject> expected = (IOObjectCollection<IOObject>) expectedObj;
				@SuppressWarnings("unchecked")
				IOObjectCollection<IOObject> actual = (IOObjectCollection<IOObject>) actualObj;

				message = message + "Collection \"" + actual.getSource() + "\" of IOObjects are not equal: ";
				Assert.assertEquals(message + " (number of items)", expected.size(), actual.size());
				RapidAssert.assertEquals(message, expected.getObjects(), actual.getObjects());
			}

			@Override
			public Class<?> getAssertable() {
				return IOObjectCollection.class;
			}

		});

		asserters.add(new Asserter() {

			/**
			 * Test two numerical matrices for equality. This contains tests about the number of
			 * columns and rows, as well as column&row names and if the matrices are marked as
			 * symmetrical and if every value within the matrix is equal.
			 *
			 * @param message
			 *            message to display if an error occurs
			 * @param expected
			 *            expected matrix
			 * @param actual
			 *            actual matrix
			 */
			@Override
			public void assertEquals(String message, Object expectedObj, Object actualObj) {
				NumericalMatrix expected = (NumericalMatrix) expectedObj;
				NumericalMatrix actual = (NumericalMatrix) actualObj;

				message = message + "Numerical matrices are not equal";

				int expNrOfCols = expected.getNumberOfColumns();
				int actNrOfCols = actual.getNumberOfColumns();
				Assert.assertEquals(message + " (column number is not equal)", expNrOfCols, actNrOfCols);

				int expNrOfRows = expected.getNumberOfRows();
				int actNrOfRows = actual.getNumberOfRows();
				Assert.assertEquals(message + " (row number is not equal)", expNrOfRows, actNrOfRows);

				int cols = expNrOfCols;
				int rows = expNrOfRows;

				for (int col = 0; col < cols; col++) {
					String expectedColName = expected.getColumnName(col);
					String actualColName = actual.getColumnName(col);
					Assert.assertEquals(message + " (column name at index " + col + " is not equal)", expectedColName,
							actualColName);
				}

				for (int row = 0; row < rows; row++) {
					String expectedRowName = expected.getRowName(row);
					String actualRowName = actual.getRowName(row);
					Assert.assertEquals(message + " (row name at index " + row + " is not equal)", expectedRowName,
							actualRowName);
				}

				Assert.assertEquals(message + " (matrix symmetry is not equal)", expected.isSymmetrical(),
						actual.isSymmetrical());

				for (int row = 0; row < rows; row++) {
					for (int col = 0; col < cols; col++) {

						double expectedVal = expected.getValue(row, col);
						double actualVal = actual.getValue(row, col);
						assertDouble(message + " (value at row " + row + " and column " + col + " is not equal)",
								expectedVal, actualVal);

					}
				}

			}

			@Override
			public Class<?> getAssertable() {
				return NumericalMatrix.class;
			}

		});

		asserters.add(new Asserter() {

			/**
			 * Tests the two performance vectors for equality by testing the size, the criteria
			 * names, the main criterion and each criterion.
			 *
			 * @param message
			 *            message to display if an error occurs
			 * @param expected
			 *            expected vector
			 * @param actual
			 *            actual vector
			 */
			@Override
			public void assertEquals(String message, Object expectedObj, Object actualObj) {
				PerformanceVector expected = (PerformanceVector) expectedObj;
				PerformanceVector actual = (PerformanceVector) actualObj;

				message = message + "Performance vectors are not equal";

				int expSize = expected.getSize();
				int actSize = actual.getSize();
				Assert.assertEquals(message + " (size of the performance vector is not equal)", expSize, actSize);
				int size = expSize;

				RapidAssert.assertArrayEquals(message, expected.getCriteriaNames(), actual.getCriteriaNames());
				RapidAssert.assertEquals(message, expected.getMainCriterion(), actual.getMainCriterion());

				for (int i = 0; i < size; i++) {
					RapidAssert.assertEquals(message, expected.getCriterion(i), actual.getCriterion(i));
				}
			}

			@Override
			public Class<?> getAssertable() {
				return PerformanceVector.class;
			}
		});

		asserters.add(new Asserter() {

			/**
			 * Tests the two file objects for equality by testing the
			 *
			 *
			 * @param message
			 *            message to display if an error occurs
			 * @param expected
			 *            expected file object
			 * @param actual
			 *            actual file object
			 */
			@Override
			public void assertEquals(String message, Object expectedObj, Object actualObj) throws RuntimeException {
				FileObject fo1 = (FileObject) expectedObj;
				FileObject fo2 = (FileObject) actualObj;
				InputStream is1 = null;
				InputStream is2 = null;
				ByteArrayOutputStream bs1 = null;
				ByteArrayOutputStream bs2 = null;
				try {
					is1 = fo1.openStream();
					is2 = fo2.openStream();
					bs1 = new ByteArrayOutputStream();
					bs2 = new ByteArrayOutputStream();
					Tools.copyStreamSynchronously(is1, bs1, true);
					Tools.copyStreamSynchronously(is2, bs2, true);
					byte[] fileData1 = bs1.toByteArray();
					byte[] fileData2 = bs2.toByteArray();
					RapidAssert.assertArrayEquals("file object data", fileData1, fileData2);
				} catch (OperatorException e) {
					throw new RuntimeException("Stream Error");
				} catch (IOException e) {
					throw new RuntimeException("Stream Error");
				} finally {
					if (is1 != null) {
						try {
							is1.close();
						} catch (IOException e) {
							// silent
						}
					}
					if (is2 != null) {
						try {
							is2.close();
						} catch (IOException e) {
							// silent
						}
					}
					if (bs1 != null) {
						try {
							bs1.close();
						} catch (IOException e) {
							// silent
						}
					}
					if (bs2 != null) {
						try {
							bs2.close();
						} catch (IOException e) {
							// silent
						}
					}
				}
			}

			@Override
			public Class<?> getAssertable() {
				return FileObject.class;
			}
		});

		// Asserter for ExampleSet
		asserters.add(new Asserter() {

			@Override
			public Class<?> getAssertable() {
				return FrequentItemSets.class;
			}

			/**
			 * Tests two FrequentItemSets by iterating over all inner Sets.
			 *
			 * @param message
			 *            message to display if an error occurs
			 * @param expected
			 *            expected value
			 * @param actual
			 *            actual value
			 */
			@Override
			public void assertEquals(String message, Object expectedObj, Object actualObj) {
				FrequentItemSets expected = (FrequentItemSets) expectedObj;
				FrequentItemSets actual = (FrequentItemSets) actualObj;

				message = message + " - FrequentItemSet \"" + actual.getSource() + "\" does not match the expected Set";

				// compare size
				Assert.assertEquals(
						message + " : size is not equal(expected <" + expected.size() + "> was <" + actual.size() + ">)",
						expected.size(), actual.size());

				// compare number of transactions
				Assert.assertEquals(
						message + " : number of transactions is not equal(expected <" + expected.getNumberOfTransactions()
								+ "> was <" + actual.getNumberOfTransactions() + ">)", expected.getNumberOfTransactions(),
						actual.getNumberOfTransactions());

				// compare example values
				expected.sortSets();
				actual.sortSets();
				Iterator<FrequentItemSet> i1 = expected.iterator();
				Iterator<FrequentItemSet> i2 = actual.iterator();
				while (i1.hasNext() && i2.hasNext()) {
					Assert.assertTrue(message, i1.next().compareTo(i2.next()) == 0);
				}
			}

		});

		// Asserter for linear regression model
		asserters.add(new Asserter() {

			@Override
			public Class<?> getAssertable() {
				return LinearRegressionModel.class;
			}

			/**
			 * Tests two linearRegression models by comparing all values
			 *
			 * @param message
			 *            message to display if an error occurs
			 * @param expected
			 *            expected value
			 * @param actual
			 *            actual value
			 */
			@Override
			public void assertEquals(String message, Object expectedObj, Object actualObj) {
				LinearRegressionModel expected = (LinearRegressionModel) expectedObj;
				LinearRegressionModel actual = (LinearRegressionModel) actualObj;

				message = message + " - Linear Regression Model \"" + actual.getSource()
						+ "\" does not match the expected Model";
				// compare coefficients
				Assert.assertArrayEquals(message + " : coefficients are not equal", expected.getCoefficients(),
						actual.getCoefficients(), 1E-15);

				// compare probabilities
				Assert.assertArrayEquals(message + " : probabilities are not equal", expected.getProbabilities(),
						actual.getProbabilities(), 1E-15);
				// compare selected attribute names
				Assert.assertArrayEquals(message + " : selected attributes are not equal",
						expected.getSelectedAttributeNames(), actual.getSelectedAttributeNames());
				// compare selected attributes
				Assert.assertArrayEquals(message + " : selected attributes are not equal", expected.getSelectedAttributes(),
						actual.getSelectedAttributes());
				// compare standard errors
				Assert.assertArrayEquals(message + " : standard errors are not equal", expected.getStandardErrors(),
						actual.getStandardErrors(), 1E-15);
				// compare standardized coefficients
				Assert.assertArrayEquals(message + " : standardized coefficients are not equal",
						expected.getStandardizedCoefficients(), actual.getStandardizedCoefficients(), 1E-15);
				// compare tolerances
				Assert.assertArrayEquals(message + " : tolerances are not equal", expected.getTolerances(),
						actual.getTolerances(), 1E-15);
				// compare t-stats
				Assert.assertArrayEquals(message + " : t statistics are not equal", expected.getTStats(),
						actual.getTStats(), 1E-15);
			}

		});

		// Asserter for ANOVA-Matrixes
		asserters.add(new Asserter() {

			@Override
			public Class<?> getAssertable() {
				return ANOVAMatrix.class;
			}

			/**
			 * Tests two ANOVA-Matrixes by comparing all values
			 *
			 * @param message
			 *            message to display if an error occurs
			 * @param expected
			 *            expected value
			 * @param actual
			 *            actual value
			 */
			@Override
			public void assertEquals(String message, Object expectedObj, Object actualObj) {
				ANOVAMatrix expected = (ANOVAMatrix) expectedObj;
				ANOVAMatrix actual = (ANOVAMatrix) actualObj;

				message = message + " - ANOVA-Matrix \"" + actual.getSource() + "\" does not match the expected Matrix";

				// compare all entries
				double[][] expectedProbabilities = expected.getProbabilities();
				double[][] actualProbabilities = actual.getProbabilities();

				for (int i = 0; i < expectedProbabilities.length; i++) {
					for (int j = 0; j < expectedProbabilities[i].length; j++) {
						Assert.assertEquals(message + " : probabilities are not equal", expectedProbabilities[i][j],
								actualProbabilities[i][j], 1E-15);
					}
				}

				Assert.assertEquals(message + " : significance levels are not equal", expected.getSignificanceLevel(),
						actual.getSignificanceLevel(), 1E-15);

			}

		});

		// Asserter for Significance Test Results
		asserters.add(new Asserter() {

			@Override
			public Class<?> getAssertable() {
				return AnovaSignificanceTestResult.class;
			}

			/**
			 * Tests two ANOVA-Significance test results
			 *
			 * @param message
			 *            message to display if an error occurs
			 * @param expected
			 *            expected value
			 * @param actual
			 *            actual value
			 */
			@Override
			public void assertEquals(String message, Object expectedObj, Object actualObj) {
				AnovaSignificanceTestResult expected = (AnovaSignificanceTestResult) expectedObj;
				AnovaSignificanceTestResult actual = (AnovaSignificanceTestResult) actualObj;

				message = message + " - ANOVA significance test result \"" + actual.getSource()
						+ "\" does not match the expected result";

				// compare alpha values
				Assert.assertEquals(message + " : alpha values are not equal", expected.getAlpha(), actual.getAlpha(), 1E-15);

				// compare DF1
				Assert.assertEquals(message + " : first degrees of freedom are equal", expected.getDf1(), actual.getDf1(),
						1E-15);

				// compare DF2
				Assert.assertEquals(message + " : second degrees of freedom are equal", expected.getDf2(), actual.getDf2(),
						1E-15);

				// compare F values
				Assert.assertEquals(message + " : F-values are not equal", expected.getFValue(), actual.getFValue(), 1E-15);

				// compare mean square residuals
				Assert.assertEquals(message + " : mean square residual values are not equal",
						expected.getMeanSquaresResiduals(), actual.getMeanSquaresResiduals(), 1E-15);

				// compare mean square between
				Assert.assertEquals(message + " : mean square between values are not equal",
						expected.getMeanSquaresBetween(), actual.getMeanSquaresBetween(), 1E-15);

				// compare probabilities
				Assert.assertEquals(message + " : probabilities are not equal", expected.getProbability(),
						actual.getProbability(), 1E-15);

				// compare sum squares between
				Assert.assertEquals(message + " : sum squares between values are not equal",
						expected.getSumSquaresBetween(), actual.getSumSquaresBetween(), 1E-15);

				// compare sum squares residuals
				Assert.assertEquals(message + " : sum squares residuals values are not equal",
						expected.getSumSquaresResiduals(), actual.getSumSquaresResiduals(), 1E-15);

			}

		});

		// Asserter for Centroid Cluster Model
		asserters.add(new Asserter() {

			@Override
			public Class<?> getAssertable() {
				return CentroidClusterModel.class;
			}

			/**
			 * Tests two Centroid Cluster Model
			 *
			 * @param message
			 *            message to display if an error occurs
			 * @param expectedObj
			 *            expected value
			 * @param actualObj
			 *            actual value
			 */
			@Override
			public void assertEquals(String message, Object expectedObj, Object actualObj) {
				CentroidClusterModel expected = (CentroidClusterModel) expectedObj;
				CentroidClusterModel actual = (CentroidClusterModel) actualObj;

				message = message + " - Centroid Cluster Model  \"" + actual.getSource()
						+ "\" does not match the expected result";


				// compare training header (basic information)
				Assert.assertEquals(message + " : training headers are not equal", expected.getTrainingHeader().toString(), actual.getTrainingHeader().toString());

				// compare distance measure type
				Assert.assertEquals(message + " : distance measure types are not equal", expected.getDistanceMeasure().getClass(), actual.getDistanceMeasure().getClass());

				// compare attribute names
				Assert.assertArrayEquals(message + " : attribute names are not equal", expected.getAttributeNames(), actual.getAttributeNames());

				// compare number of clusters
				Assert.assertEquals(message + " : number of clusters are not equal", expected.getNumberOfClusters(), actual.getNumberOfClusters());

				// compare centroids
				Assert.assertArrayEquals(message + " : centroids are not equal", expected.getCentroids().stream().map(Centroid::getCentroid).toArray(), actual.getCentroids().stream().map(Centroid::getCentroid).toArray());

				// compare number of examples
				Assert.assertArrayEquals(message + " : clusters number of examples are not equal", expected.getClusters().stream().mapToInt(Cluster::getNumberOfExamples).toArray(), actual.getClusters().stream().mapToInt(Cluster::getNumberOfExamples).toArray());

		        // compare cluster ids
				Assert.assertArrayEquals(message + " : clusters ids are not equal", expected.getClusters().stream().mapToInt(Cluster::getClusterId).toArray(), actual.getClusters().stream().mapToInt(Cluster::getClusterId).toArray());

				// compare cluster example ids
				Assert.assertArrayEquals(message + " : clusters example ids are not equal", expected.getClusters().stream().map(Cluster::getExampleIds).flatMap(Collection::stream).toArray(Object[]::new), actual.getClusters().stream().map(Cluster::getExampleIds).flatMap(Collection::stream).toArray(Object[]::new));

				// compare is adding label
				Assert.assertEquals(message + " : is adding label is not equal", expected.isAddingLabel(), actual.isAddingLabel());

				// compare is in target encoding
				Assert.assertEquals(message + " : is in target encoding is not equal", expected.isInTargetEncoding(), actual.isInTargetEncoding());

				// compare is removing
				Assert.assertEquals(message + " : is removing unknown assignments is not equal", expected.isRemovingUnknownAssignments(), actual.isRemovingUnknownAssignments());

				// compare is updatable
				Assert.assertEquals(message + " : is updatable is not equal", expected.isUpdatable(), actual.isUpdatable());

				// check string representation
				Assert.assertEquals(message + " : string representations are not equal", expected.toString(), actual.toString());
			}

		});

		return asserters;
	}

	private void assertDouble(String message, double expected, double result) {
		org.junit.Assert.assertEquals(message, expected, result, 1e-08);
	}
}
