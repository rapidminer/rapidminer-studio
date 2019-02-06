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
package com.rapidminer.operator.learner.functions.kernel.jmysvm.svm;

import com.rapidminer.example.Attribute;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.learner.functions.kernel.AbstractMySVMLearner;
import com.rapidminer.operator.learner.functions.kernel.JMySVMLearner;
import com.rapidminer.operator.learner.functions.kernel.jmysvm.examples.SVMExample;
import com.rapidminer.operator.learner.functions.kernel.jmysvm.examples.SVMExamples;
import com.rapidminer.operator.learner.functions.kernel.jmysvm.kernel.Kernel;
import com.rapidminer.operator.learner.functions.kernel.jmysvm.optimizer.QuadraticProblem;
import com.rapidminer.operator.learner.functions.kernel.jmysvm.optimizer.QuadraticProblemSMO;
import com.rapidminer.operator.learner.functions.kernel.jmysvm.util.MaxHeap;
import com.rapidminer.operator.learner.functions.kernel.jmysvm.util.MinHeap;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.LogService;
import com.rapidminer.tools.RandomGenerator;
import com.rapidminer.tools.WrapperLoggingHandler;

import java.util.Iterator;
import java.util.logging.Level;


/**
 * Abstract base class for all SVMs
 * 
 * @author Stefan Rueping, Ingo Mierswa
 */
public abstract class SVM implements SVMInterface {

	private static final int[] RAPID_MINER_VERBOSITY = { LogService.STATUS, LogService.STATUS, LogService.STATUS,
			LogService.MINIMUM, LogService.MINIMUM };

	protected Kernel the_kernel;

	protected SVMExamples the_examples;

	double[] alphas;

	double[] ys;

	protected int examples_total;

	// protected int verbosity;
	protected int target_count;

	protected double convergence_epsilon = 1e-3;

	protected double lambda_factor;

	protected int[] at_bound;

	protected double[] sum;

	protected boolean[] which_alpha;

	protected int[] working_set;

	protected double[] primal;

	protected double sum_alpha;

	protected double lambda_eq;

	protected int to_shrink;

	protected double feasible_epsilon;

	protected double lambda_WS;

	protected boolean quadraticLossPos = false;

	protected boolean quadraticLossNeg = false;

	boolean shrinked;

	protected double epsilon_pos = 0.0d;

	protected double epsilon_neg = 0.0d;

	private int max_iterations = 100000;

	protected int working_set_size = 10;

	protected int parameters_working_set_size = 10; // was set in parameters

	protected double is_zero = 1e-10;

	protected int shrink_const = 50;

	protected double C = 1.0d;

	protected double[] cPos;

	protected double[] cNeg;

	protected double descend = 1e-15;

	MinHeap heap_min;

	MaxHeap heap_max;

	protected QuadraticProblem qp;

	private Operator paramOperator;

	private RandomGenerator randomGenerator;

	public SVM() {}

	/**
	 * class constructor. Throws an operator exception if a non-optional parameter was not set and
	 * has no default value.
	 */
	public SVM(Operator paramOperator, Kernel new_kernel, SVMExamples new_examples,
			com.rapidminer.example.ExampleSet rapidMinerExamples, RandomGenerator randomGenerator)
			throws UndefinedParameterError {
		this.paramOperator = paramOperator;
		the_examples = new_examples;
		this.randomGenerator = randomGenerator;
		max_iterations = paramOperator.getParameterAsInt(AbstractMySVMLearner.PARAMETER_MAX_ITERATIONS);
		convergence_epsilon = paramOperator.getParameterAsDouble(AbstractMySVMLearner.PARAMETER_CONVERGENCE_EPSILON);
		quadraticLossPos = paramOperator.getParameterAsBoolean(JMySVMLearner.PARAMETER_QUADRATIC_LOSS_POS);
		quadraticLossNeg = paramOperator.getParameterAsBoolean(JMySVMLearner.PARAMETER_QUADRATIC_LOSS_NEG);

		cPos = new double[rapidMinerExamples.size()];
		cNeg = new double[rapidMinerExamples.size()];
		Attribute weightAttribute = rapidMinerExamples.getAttributes().getWeight();
		if (weightAttribute != null) {
			Iterator<com.rapidminer.example.Example> reader = rapidMinerExamples.iterator();
			int index = 0;
			while (reader.hasNext()) {
				com.rapidminer.example.Example example = reader.next();
				cPos[index] = cNeg[index] = example.getValue(weightAttribute);
				index++;
			}
			if (paramOperator.getParameterAsBoolean(JMySVMLearner.PARAMETER_BALANCE_COST)) {
				logWarning("Since the example set contains a weight attribute, the parameter balance_cost will be ignored.");
			}
			logln(1, "Use defined weight attribute for example weights.");
		} else {
			Attribute weightPosAttribute = rapidMinerExamples.getAttributes().getSpecial("weight_pos");
			Attribute weightNegAttribute = rapidMinerExamples.getAttributes().getSpecial("weight_neg");
			if ((weightPosAttribute != null) && (weightNegAttribute != null)) {
				Iterator<com.rapidminer.example.Example> reader = rapidMinerExamples.iterator();
				int index = 0;
				while (reader.hasNext()) {
					com.rapidminer.example.Example example = reader.next();
					cPos[index] = example.getValue(weightPosAttribute);
					cNeg[index] = example.getValue(weightNegAttribute);
					index++;
				}
				if (paramOperator.getParameterAsBoolean(JMySVMLearner.PARAMETER_BALANCE_COST)) {
					logWarning("Since the example set contains a weight attribute, the parameter balance_cost will be ignored.");
				}
				logln(1, "Use defined weight_pos and weight_neg attributes for example weights.");
			} else {
				double generalCpos = paramOperator.getParameterAsDouble(JMySVMLearner.PARAMETER_L_POS);
				double generalCneg = paramOperator.getParameterAsDouble(JMySVMLearner.PARAMETER_L_NEG);
				if (paramOperator.getParameterAsBoolean(JMySVMLearner.PARAMETER_BALANCE_COST)) {
					generalCpos *= the_examples.count_examples()
							/ (2.0d * (the_examples.count_examples() - the_examples.count_pos_examples()));
					generalCneg *= ((the_examples.count_examples()) / (2.0d * the_examples.count_pos_examples()));
				}
				for (int i = 0; i < cPos.length; i++) {
					cPos[i] = generalCpos;
					cNeg[i] = generalCneg;
				}
			}
		}

		this.C = paramOperator.getParameterAsDouble(AbstractMySVMLearner.PARAMETER_C);

		double epsilonValue = paramOperator.getParameterAsDouble(JMySVMLearner.PARAMETER_EPSILON);
		if (epsilonValue != -1) {
			epsilon_pos = epsilon_neg = epsilonValue;
		} else {
			epsilon_pos = paramOperator.getParameterAsDouble(JMySVMLearner.PARAMETER_EPSILON_PLUS);
			epsilon_neg = paramOperator.getParameterAsDouble(JMySVMLearner.PARAMETER_EPSILON_MINUS);
		}
	};

	/**
	 * Init the SVM
	 * 
	 * @param new_kernel
	 *            new kernel function.
	 * @param new_examples
	 *            the data container
	 */
	@Override
	public void init(Kernel new_kernel, SVMExamples new_examples) {
		the_kernel = new_kernel;
		the_examples = new_examples;
		examples_total = the_examples.count_examples();
		parameters_working_set_size = working_set_size;

		if (this.C <= 0.0d) {
			this.C = 0.0d;
			for (int i = 0; i < examples_total; i++) {
				this.C += the_kernel.calculate_K(i, i);
			}
			;
			this.C = examples_total / this.C;
			logln(3, "C set to " + this.C);
		}

		if (cPos != null) {
			for (int i = 0; i < cPos.length; i++) {
				cPos[i] *= this.C;
				cNeg[i] *= this.C;
			}
		}

		lambda_factor = 1.0;
		lambda_eq = 0;
		target_count = 0;
		sum_alpha = 0;
		feasible_epsilon = convergence_epsilon;

		alphas = the_examples.get_alphas();
		ys = the_examples.get_ys();
	};

	/**
	 * Train the SVM
	 */
	@Override
	public void train() {
		if (examples_total <= 0) {
			// exception?
			the_examples.set_b(Double.NaN);
			return;
		}
		if (examples_total == 1) {
			the_examples.set_b(ys[0]);
			return;
		}
		target_count = 0;
		shrinked = false;
		init_optimizer();
		init_working_set();
		int iteration = 0;
		boolean converged = false;
		M: while (iteration < max_iterations) {
			iteration++;
			logln(4, "optimizer iteration " + iteration);
			// log(3,".");
			optimize();
			put_optimizer_values();
			converged = convergence();
			if (converged) {
				logln(3, ""); // dots
				project_to_constraint();

				if (shrinked) {
					// check convergence for all alphas
					logln(2, "***** Checking convergence for all variables");
					reset_shrinked();
					converged = convergence();
				}
				;

				if (converged) {
					logln(1, "*** Convergence");
					break M;
				}
				;

				// set variables free again
				shrink_const += 10;
				target_count = 0;
				for (int i = 0; i < examples_total; i++) {
					at_bound[i] = 0;
				}
				;
			}
			;
			shrink();
			calculate_working_set();
			update_working_set();
		}
		;

		int i;
		if ((iteration >= max_iterations) && (!converged)) {
			logln(1, "*** No convergence: Time up.");
			if (shrinked) {
				// set sums for all variables for statistics
				reset_shrinked();
			}
			;
		}
		;

		// calculate b
		double new_b = 0;
		int new_b_count = 0;
		for (i = 0; i < examples_total; i++) {
			if ((alphas[i] - cNeg[i] < -is_zero) && (alphas[i] > is_zero)) {
				new_b += ys[i] - sum[i] - epsilon_neg;
				new_b_count++;
			} else if ((alphas[i] + cPos[i] > is_zero) && (alphas[i] < -is_zero)) {
				new_b += ys[i] - sum[i] + epsilon_pos;
				new_b_count++;
			}
			;
		}
		;

		if (new_b_count > 0) {
			the_examples.set_b(new_b / new_b_count);
		} else {
			// unlikely
			for (i = 0; i < examples_total; i++) {
				if ((alphas[i] < is_zero) && (alphas[i] > -is_zero)) {
					new_b += ys[i] - sum[i];
					new_b_count++;
				}
				;
			}
			;
			if (new_b_count > 0) {
				the_examples.set_b(new_b / new_b_count);
			} else {
				// even unlikelier
				for (i = 0; i < examples_total; i++) {
					new_b += ys[i] - sum[i];
					new_b_count++;
				}
				;
				the_examples.set_b(new_b / new_b_count);
			}
			;
		}
		;

		// if(verbosity>= 2){
		logln(2, "Done training: " + iteration + " iterations.");
		// if(verbosity >= 2){
		double now_target = 0;
		double now_target_dummy = 0;
		for (i = 0; i < examples_total; i++) {
			now_target_dummy = sum[i] / 2 - ys[i];
			if (is_alpha_neg(i)) {
				now_target_dummy += epsilon_pos;
			} else {
				now_target_dummy -= epsilon_neg;
			}
			;
			now_target += alphas[i] * now_target_dummy;
		}
		;
		logln(2, "Target function: " + now_target);
		// };
		// };

		print_statistics();

		exit_optimizer();
	};

	/**
	 * print statistics about result
	 */
	protected void print_statistics() {
		int dim = the_examples.get_dim();
		int i, j;
		double alpha;
		double[] x;
		int svs = 0;
		int bsv = 0;
		double mae = 0;
		double mse = 0;
		int countpos = 0;
		int countneg = 0;
		double y;
		double prediction;
		double min_lambda = Double.MAX_VALUE;
		double b = the_examples.get_b();

		for (i = 0; i < examples_total; i++) {
			if (lambda(i) < min_lambda) {
				min_lambda = lambda(i);
			}
			;
			y = ys[i];
			prediction = sum[i] + b;
			mae += Math.abs(y - prediction);
			mse += (y - prediction) * (y - prediction);
			alpha = alphas[i];
			if (y < prediction - epsilon_pos) {
				countpos++;
			} else if (y > prediction + epsilon_neg) {
				countneg++;
			}
			;
			if (alpha != 0) {
				svs++;
				if ((alpha == cPos[i]) || (alpha == -cNeg[i])) {
					bsv++;
				}
				;
			}
			;
		}
		;
		mae /= examples_total;
		mse /= examples_total;
		min_lambda = -min_lambda;

		logln(1, "Error on KKT is " + min_lambda);
		logln(1, svs + " SVs");
		logln(1, bsv + " BSVs");
		logln(1, "MAE " + mae);
		logln(1, "MSE " + mse);
		logln(1, countpos + " pos loss");
		logln(1, countneg + " neg loss");

		// if(verbosity >= 2){
		// print hyperplane
		double[] w = new double[dim];
		for (j = 0; j < dim; j++) {
			w[j] = 0;
		}
		for (i = 0; i < examples_total; i++) {
			x = the_examples.get_example(i).toDense(dim);
			alpha = alphas[i];
			for (j = 0; j < dim; j++) {
				w[j] += alpha * x[j];
			}
			;
		}
		;
		// double[] Exp = the_examples.Exp;
		// double[] Dev = the_examples.Dev;
		// if(Exp != null){
		// for(j=0;j<dim;j++){
		// if(Dev[j] != 0){
		// w[j] /= Dev[j];
		// };
		// if(0 != Dev[dim]){
		// w[j] *= Dev[dim];
		// };
		// b -= w[j]*Exp[j];
		// };
		// b += Exp[dim];
		// };
		// logln(2," ");
		for (j = 0; j < dim; j++) {
			logln(2, "w[" + j + "] = " + w[j]);
		}
		;
		logln(2, "b = " + b);
		if (dim == 1) {
			logln(2, "y = " + w[0] + "*x+" + b);
		}
		;
		// };
	};

	/** Return the weights of the features. */
	@Override
	public double[] getWeights() {
		int dim = the_examples.get_dim();
		double[] w = new double[dim];
		for (int j = 0; j < dim; j++) {
			w[j] = 0;
		}
		for (int i = 0; i < examples_total; i++) {
			double[] x = the_examples.get_example(i).toDense(dim);
			double alpha = alphas[i];
			for (int j = 0; j < dim; j++) {
				w[j] += alpha * x[j];
			}
		}
		return w;
	}

	/** Returns the value of b. */
	@Override
	public double getB() {
		return the_examples.get_b();
	}

	/**
	 * Gets the complexity constant of the svm.
	 */
	public double getC() {
		return C;
	}

	/**
	 * init the optimizer
	 */
	protected void init_optimizer() {
		which_alpha = new boolean[working_set_size];
		primal = new double[working_set_size];
		sum = new double[examples_total];
		at_bound = new int[examples_total];

		// init variables
		if (working_set_size > examples_total) {
			working_set_size = examples_total;
		}

		qp = new QuadraticProblemSMO(is_zero / 100, convergence_epsilon / 100, working_set_size * working_set_size);
		qp.set_n(working_set_size);

		// reserve workspace for calculate_working_set
		working_set = new int[working_set_size];
		heap_max = new MaxHeap(0);
		heap_min = new MinHeap(0);

		// if(the_examples.Dev != null){
		// double dev = the_examples.Dev[the_examples.get_dim()];
		// if(dev != 0){
		// epsilon_pos /= dev;
		// epsilon_neg /= dev;
		// };
		// };

		int i;
		for (i = 0; i < working_set_size; i++) {
			qp.l[i] = 0; // -is_zero;
		}
		;

		if (quadraticLossPos) {
			for (i = 0; i < cPos.length; i++) {
				cPos[i] = Double.MAX_VALUE;
			}
		}
		;
		if (quadraticLossNeg) {
			for (i = 0; i < cNeg.length; i++) {
				cNeg[i] = Double.MAX_VALUE;
			}
		}
		;

		for (i = 0; i < examples_total; i++) {
			alphas[i] = 0.0;
		}
		;

		lambda_WS = 0;
		to_shrink = 0;

		qp.set_n(working_set_size);
	};

	/**
	 * exit the optimizer
	 */
	protected void exit_optimizer() {
		qp = null;
	};

	/**
	 * shrink the variables
	 */
	protected void shrink() {
		// move shrinked examples to back
		if (to_shrink > examples_total / 10) {
			int i;
			int last_pos = examples_total;
			if (last_pos > working_set_size) {
				for (i = 0; i < last_pos; i++) {
					if (at_bound[i] >= shrink_const) {
						// shrink i
						sum_alpha += alphas[i];
						last_pos--;
						the_examples.swap(i, last_pos);
						the_kernel.swap(i, last_pos);
						sum[i] = sum[last_pos];
						at_bound[i] = at_bound[last_pos];
						if (last_pos <= working_set_size) {
							break;
						}
						;
					}
					;
				}
				;
				to_shrink = 0;
				shrinked = true;
				if (last_pos < examples_total) {
					examples_total = last_pos;
					the_kernel.set_examples_size(examples_total);
				}
				;
			}
			;
			logln(4, "shrinked to " + examples_total + " variables");
		}
		;
	};

	/**
	 * reset the shrinked variables
	 */
	protected void reset_shrinked() {
		int old_ex_tot = examples_total;
		target_count = 0;
		examples_total = the_examples.count_examples();
		the_kernel.set_examples_size(examples_total);
		// unshrink, recalculate sum for all variables
		int i, j;
		// reset all sums
		for (i = old_ex_tot; i < examples_total; i++) {
			sum[i] = 0;
			at_bound[i] = 0;
		}
		;
		double alpha;
		double[] kernel_row;
		for (i = 0; i < examples_total; i++) {
			alpha = alphas[i];
			if (alpha != 0) {
				kernel_row = the_kernel.get_row(i);
				for (j = old_ex_tot; j < examples_total; j++) {
					sum[j] += alpha * kernel_row[j];
				}
				;
			}
			;
		}
		;
		sum_alpha = 0;
		shrinked = false;
		logln(5, "Resetting shrinked from " + old_ex_tot + " to " + examples_total);
	};

	/**
	 * Project variables to constraints
	 */
	protected void project_to_constraint() {
		// project alphas to match the constraint
		double alpha_sum = sum_alpha;
		int SVcount = 0;
		double alpha;
		int i;
		for (i = 0; i < examples_total; i++) {
			alpha = alphas[i];
			alpha_sum += alpha;
			if (((alpha > is_zero) && (alpha - cNeg[i] < -is_zero)) || ((alpha < -is_zero) && (alpha + cPos[i] > is_zero))) {
				SVcount++;
			}
			;
		}
		;
		if (SVcount > 0) {
			// project
			alpha_sum /= SVcount;
			for (i = 0; i < examples_total; i++) {
				alpha = alphas[i];
				if (((alpha > is_zero) && (alpha - cNeg[i] < -is_zero))
						|| ((alpha < -is_zero) && (alpha + cPos[i] > is_zero))) {
					alphas[i] -= alpha_sum;
				}
				;
			}
			;
		}
		;
	};

	/**
	 * Calculates the working set
	 * 
	 * @exception Exception
	 *                on any error
	 */
	protected void calculate_working_set() {
		// reset WSS
		if (working_set_size < parameters_working_set_size) {
			working_set_size = parameters_working_set_size;
			if (working_set_size > examples_total) {
				working_set_size = examples_total;
			}
		}
		;

		heap_min.init(working_set_size / 2);
		heap_max.init(working_set_size / 2 + working_set_size % 2);

		int i = 0;
		double sort_value;

		double the_nabla;
		boolean is_feasible;
		int j;

		while (i < examples_total) {
			is_feasible = feasible(i);
			if (is_feasible) {
				the_nabla = nabla(i);
				if (is_alpha_neg(i)) {
					sort_value = -the_nabla; // - : maximum inconsistency
					// approach
				} else {
					sort_value = the_nabla;
				}
				;
				// add to heaps
				heap_min.add(sort_value, i);
				heap_max.add(sort_value, i);
			}
			;
			i++;
		}
		;

		int[] new_ws = heap_min.get_values();
		working_set_size = 0;
		int pos;
		j = heap_min.size();
		for (i = 0; i < j; i++) {
			working_set[working_set_size] = new_ws[i];
			working_set_size++;
		}
		;
		pos = working_set_size;
		new_ws = heap_max.get_values();
		j = heap_max.size();
		for (i = 0; i < j; i++) {
			working_set[working_set_size] = new_ws[i];
			working_set_size++;
		}
		;
		if ((!heap_min.empty()) && (!heap_max.empty())) {
			if (heap_min.top_value() >= heap_max.top_value()) {
				// there could be the same values in the min- and maxheap,
				// sort them out (this is very unlikely)
				j = 0;
				i = 0;
				while (i < pos) {
					// working_set[i] also in max-heap?
					j = pos;
					while ((j < working_set_size) && (working_set[j] != working_set[i])) {
						j++;
					}
					;
					if (j < working_set_size) {
						// working_set[i] equals working_set[j]
						// remove j from WS
						working_set[j] = working_set[working_set_size - 1];
						working_set_size--;
					} else {
						i++;
					}
					;
				}
				;
			}
			;
		}
		;

		if (target_count > 0) {
			// convergence error on last iteration?
			// some more tests on WS
			// unlikely to happen, so speed isn't so important
			// are all variables at the bound?
			int pos_abs;
			boolean bounded_pos = true;
			boolean bounded_neg = true;
			pos = 0;
			double alpha;
			while ((pos < working_set_size) && (bounded_pos || bounded_neg)) {
				pos_abs = working_set[pos];
				alpha = alphas[pos_abs];
				if (is_alpha_neg(pos_abs)) {
					if (alpha - cNeg[pos_abs] < -is_zero) {
						bounded_pos = false;
					}
					;
					if (alpha > is_zero) {
						bounded_neg = false;
					}
					;
				} else {
					if (alpha + cNeg[pos_abs] > is_zero) {
						bounded_neg = false;
					}
					;
					if (alpha < -is_zero) {
						bounded_pos = false;
					}
					;
				}
				;
				pos++;
			}
			;
			if (bounded_pos) {
				// all alphas are at upper bound
				// need alpha that can be moved upward
				// use alpha with smallest lambda
				double max_lambda = Double.MAX_VALUE;
				int max_pos = examples_total;
				for (pos_abs = 0; pos_abs < examples_total; pos_abs++) {
					alpha = alphas[pos_abs];
					if (is_alpha_neg(pos_abs)) {
						if (alpha - cNeg[pos_abs] < -is_zero) {
							if (lambda(pos_abs) < max_lambda) {
								max_lambda = lambda(pos_abs);
								max_pos = pos_abs;
							}
							;
						}
						;
					} else {
						if (alpha < -is_zero) {
							if (lambda(pos_abs) < max_lambda) {
								max_lambda = lambda(pos_abs);
								max_pos = pos_abs;
							}
							;
						}
						;
					}
					;
				}
				;
				if (max_pos < examples_total) {
					if (working_set_size < parameters_working_set_size) {
						working_set_size++;
					}
					;
					working_set[working_set_size - 1] = max_pos;
				}
				;
			} else if (bounded_neg) {
				// all alphas are at lower bound
				// need alpha that can be moved downward
				// use alpha with smallest lambda
				double max_lambda = Double.MAX_VALUE;
				int max_pos = examples_total;
				for (pos_abs = 0; pos_abs < examples_total; pos_abs++) {
					alpha = alphas[pos_abs];
					if (is_alpha_neg(pos_abs)) {
						if (alpha > is_zero) {
							if (lambda(pos_abs) < max_lambda) {
								max_lambda = lambda(pos_abs);
								max_pos = pos_abs;
							}
							;
						}
						;
					} else {
						if (alpha + cNeg[pos_abs] > is_zero) {
							if (lambda(pos_abs) < max_lambda) {
								max_lambda = lambda(pos_abs);
								max_pos = pos_abs;
							}
							;
						}
						;
					}
					;
				}
				;
				if (max_pos < examples_total) {
					if (working_set_size < parameters_working_set_size) {
						working_set_size++;
					}
					;
					working_set[working_set_size - 1] = max_pos;
				}
				;
			}
			;
		}
		;

		if ((working_set_size < parameters_working_set_size) && (working_set_size < examples_total)) {
			// use full working set
			pos = (int) (randomGenerator.nextDouble() * examples_total);
			int ok;
			while ((working_set_size < parameters_working_set_size) && (working_set_size < examples_total)) {
				// add pos into WS if it isn't already
				ok = 1;
				for (i = 0; i < working_set_size; i++) {
					if (working_set[i] == pos) {
						ok = 0;
						i = working_set_size;
					}
					;
				}
				;
				if (1 == ok) {
					working_set[working_set_size] = pos;
					working_set_size++;
				}
				;
				pos = (pos + 1) % examples_total;
			}
			;
		}
		;

		int ipos;
		for (ipos = 0; ipos < working_set_size; ipos++) {
			which_alpha[ipos] = is_alpha_neg(working_set[ipos]);
		}
		;
	};

	/**
	 * Updates the working set
	 */
	protected void update_working_set() {
		// setup subproblem
		int i, j;
		int pos_i, pos_j;
		double[] kernel_row;
		double sum_WS;
		boolean[] my_which_alpha = which_alpha;
		for (pos_i = 0; pos_i < working_set_size; pos_i++) {
			i = working_set[pos_i];

			// put row sort_i in hessian
			kernel_row = the_kernel.get_row(i);

			sum_WS = 0;
			// for(pos_j=0;pos_j<working_set_size;pos_j++){
			for (pos_j = 0; pos_j < pos_i; pos_j++) {
				j = working_set[pos_j];
				// put all elements K(i,j) in hessian, where j in WS
				if (((!my_which_alpha[pos_j]) && (!my_which_alpha[pos_i]))
						|| ((my_which_alpha[pos_j]) && (my_which_alpha[pos_i]))) {
					// both i and j positive or negative
					(qp.H)[pos_i * working_set_size + pos_j] = kernel_row[j];
					(qp.H)[pos_j * working_set_size + pos_i] = kernel_row[j];
				} else {
					// one of i and j positive, one negative
					(qp.H)[pos_i * working_set_size + pos_j] = -kernel_row[j];
					(qp.H)[pos_j * working_set_size + pos_i] = -kernel_row[j];
				}
				;
			}
			;
			for (pos_j = 0; pos_j < working_set_size; pos_j++) {
				j = working_set[pos_j];
				sum_WS += alphas[j] * kernel_row[j];
			}
			;
			// set main diagonal
			(qp.H)[pos_i * working_set_size + pos_i] = kernel_row[i];

			// linear and box constraints
			if (!my_which_alpha[pos_i]) {
				// alpha
				(qp.A)[pos_i] = -1;
				// lin(alpha) = y_i+eps-sum_{i not in WS} alpha_i K_{ij}
				// = y_i+eps-sum_i+sum_{i in WS}
				(qp.c)[pos_i] = ys[i] + epsilon_pos - sum[i] + sum_WS;
				primal[pos_i] = -alphas[i];
				(qp.u)[pos_i] = cPos[i];
			} else {
				// alpha^*
				(qp.A)[pos_i] = 1;
				(qp.c)[pos_i] = -ys[i] + epsilon_neg + sum[i] - sum_WS;
				primal[pos_i] = alphas[i];
				(qp.u)[pos_i] = cNeg[i];
			}
			;
		}
		;
		if (quadraticLossNeg) {
			for (pos_i = 0; pos_i < working_set_size; pos_i++) {
				i = working_set[pos_i];
				if (my_which_alpha[pos_i]) {
					(qp.H)[pos_i * (working_set_size + 1)] += 1 / cNeg[i];
					(qp.u)[pos_i] = Double.MAX_VALUE;
				}
				;
			}
			;
		}
		;
		if (quadraticLossPos) {
			for (pos_i = 0; pos_i < working_set_size; pos_i++) {
				i = working_set[pos_i];
				if (!my_which_alpha[pos_i]) {
					(qp.H)[pos_i * (working_set_size + 1)] += 1 / cPos[i];
					(qp.u)[pos_i] = Double.MAX_VALUE;
				}
				;
			}
			;
		}
		;
	};

	/**
	 * Initialises the working set
	 * 
	 * @exception Exception
	 *                on any error
	 */
	protected void init_working_set() {
		// calculate sum
		int i, j;

		project_to_constraint();
		// skip kernel calculation as all alphas = 0
		for (i = 0; i < examples_total; i++) {
			sum[i] = 0;
			at_bound[i] = 0;
		}
		;

		// first working set is random
		j = 0;
		i = 0;
		while ((i < working_set_size) && (j < examples_total)) {
			working_set[i] = j;
			if (is_alpha_neg(j)) {
				which_alpha[i] = true;
			} else {
				which_alpha[i] = false;
			}
			;
			i++;
			j++;
		}
		;
		update_working_set();
	};

	/**
	 * Calls the optimizer
	 */
	protected abstract void optimize();

	/**
	 * Stores the optimizer results
	 */
	protected void put_optimizer_values() {
		// update nabla, sum, examples.
		// sum[i] += (primal_j^*-primal_j-alpha_j^*+alpha_j)K(i,j)
		// check for |nabla| < is_zero (nabla <-> nabla*)
		int i = 0;
		int j = 0;
		int pos_i;
		double the_new_alpha;
		double[] kernel_row;
		double alpha_diff;
		double my_sum[] = sum;

		pos_i = working_set_size;
		while (pos_i > 0) {
			pos_i--;
			if (which_alpha[pos_i]) {
				the_new_alpha = primal[pos_i];
			} else {
				the_new_alpha = -primal[pos_i];
			}
			;
			// next three statements: keep this order!
			i = working_set[pos_i];
			alpha_diff = the_new_alpha - alphas[i];
			alphas[i] = the_new_alpha;

			if (alpha_diff != 0) {
				// update sum ( => nabla)
				kernel_row = the_kernel.get_row(i);
				for (j = examples_total - 1; j >= 0; j--) {
					my_sum[j] += alpha_diff * kernel_row[j];
				}
				;
			}
			;
		}
		;
	};

	/**
	 * Checks if the optimization converged
	 * 
	 * @return boolean true optimzation if converged
	 */
	protected boolean convergence() {
		double the_lambda_eq = 0;
		int total = 0;
		double alpha_sum = 0;
		double alpha = 0;
		int i;
		boolean result = true;
		// actual convergence-test
		total = 0;
		alpha_sum = 0;

		for (i = 0; i < examples_total; i++) {
			alpha = alphas[i];
			alpha_sum += alpha;
			if ((alpha > is_zero) && (alpha - cNeg[i] < -is_zero)) {
				// alpha^* = - nabla
				the_lambda_eq += -nabla(i); // all_ys[i]-epsilon_neg-sum[i];
				total++;
			} else if ((alpha < -is_zero) && (alpha + cPos[i] > is_zero)) {
				// alpha = nabla
				the_lambda_eq += nabla(i); // all_ys[i]+epsilon_pos-sum[i];
				total++;
			}
			;
		}
		;

		logln(4, "lambda_eq = " + (the_lambda_eq / total));
		if (total > 0) {
			lambda_eq = the_lambda_eq / total;
		} else {
			// keep WS lambda_eq
			lambda_eq = lambda_WS; // (lambda_eq+4*lambda_WS)/5;
			logln(4, "*** no SVs in convergence(), lambda_eq = " + lambda_eq + ".");
		}
		;

		if (target_count > 2) {
			// estimate lambda from WS
			if (target_count > 20) {
				// desperate attempt to get good lambda!
				lambda_eq = ((40 - target_count) * lambda_eq + (target_count - 20) * lambda_WS) / 20;
				logln(5, "Re-Re-calculated lambda from WS: " + lambda_eq);
				if (target_count > 40) {
					// really desperate, kick one example out!
					i = working_set[target_count % working_set_size];
					if (is_alpha_neg(i)) {
						lambda_eq = -nabla(i);
					} else {
						lambda_eq = nabla(i);
					}
					;
					logln(5, "set lambda_eq to nabla(" + i + "): " + lambda_eq);
				}
				;
			} else {
				lambda_eq = lambda_WS;
				logln(5, "Re-calculated lambda_eq from WS: " + lambda_eq);
			}
			;
		}
		;

		// check linear constraint
		if (java.lang.Math.abs(alpha_sum + sum_alpha) > convergence_epsilon) {
			// equality constraint violated
			logln(4, "No convergence: equality constraint violated: |" + (alpha_sum + sum_alpha) + "| >> 0");
			project_to_constraint();
			result = false;
		}
		;

		i = 0;
		while ((i < examples_total) && (result != false)) {
			if (lambda(i) >= -convergence_epsilon) {
				i++;
			} else {
				result = false;
			}
			;
		}
		;

		return result;
	};

	protected abstract double nabla(int i);

	/**
	 * lagrangion multiplier of variable i
	 * 
	 * @param i
	 *            variable index
	 * @return lambda
	 */
	protected double lambda(int i) {
		double alpha;
		double result;
		if (is_alpha_neg(i)) {
			result = -java.lang.Math.abs(nabla(i) + lambda_eq);
		} else {
			result = -java.lang.Math.abs(nabla(i) - lambda_eq);
		}
		;
		// default = not at bound

		alpha = alphas[i];

		if (alpha > is_zero) {
			// alpha*
			if (alpha - cNeg[i] >= -is_zero) {
				// upper bound active
				result = -lambda_eq - nabla(i);
			}
			;
		} else if (alpha >= -is_zero) {
			// lower bound active
			if (is_alpha_neg(i)) {
				result = nabla(i) + lambda_eq;
			} else {
				result = nabla(i) - lambda_eq;
			}
			;
		} else if (alpha + cPos[i] <= is_zero) {
			// upper bound active
			result = lambda_eq - nabla(i);
		}
		;
		return result;
	};

	protected boolean feasible(int i) {
		boolean is_feasible = true;
		double alpha = alphas[i];
		double the_lambda = lambda(i);

		if (alpha - cNeg[i] >= -is_zero) {
			// alpha* at upper bound
			if (the_lambda >= 0) {
				at_bound[i]++;
				if (at_bound[i] == shrink_const) {
					to_shrink++;
				}
			} else {
				at_bound[i] = 0;
			}
			;
		} else if ((alpha <= is_zero) && (alpha >= -is_zero)) {
			// lower bound active
			if (the_lambda >= 0) {
				at_bound[i]++;
				if (at_bound[i] == shrink_const) {
					to_shrink++;
				}
			} else {
				at_bound[i] = 0;
			}
			;
		} else if (alpha + cPos[i] <= is_zero) {
			// alpha at upper bound
			if (the_lambda >= 0) {
				at_bound[i]++;
				if (at_bound[i] == shrink_const) {
					to_shrink++;
				}
			} else {
				at_bound[i] = 0;
			}
			;
		} else {
			// not at bound
			at_bound[i] = 0;
		}
		;
		if ((the_lambda >= feasible_epsilon) || (at_bound[i] >= shrink_const)) {
			is_feasible = false;
		}
		;
		return is_feasible;
	}

	protected abstract boolean is_alpha_neg(int i);

	/**
	 * log the output plus newline
	 * 
	 * @param level
	 *            warning level
	 * @param message
	 *            Message test
	 */
	protected void logln(int level, String message) {
		Level mappedLevel = WrapperLoggingHandler.LEVELS[RAPID_MINER_VERBOSITY[level - 1]];
		if (paramOperator != null) {
			paramOperator.getLogger().log(mappedLevel, message);
		} else {
			LogService.getRoot().log(mappedLevel, message);
		}
	}

	protected void logWarning(String message) {
		paramOperator.getLogger().warning(message);
	}

	/**
	 * predict values on the testset with model
	 */
	@Override
	public void predict(SVMExamples to_predict) {
		int i;
		double prediction;
		SVMExample sVMExample;
		// int size = the_examples.count_examples(); // IM 04/02/12
		int size = to_predict.count_examples();
		for (i = 0; i < size; i++) {
			sVMExample = to_predict.get_example(i);
			prediction = predict(sVMExample);
			to_predict.set_y(i, prediction);
		}
		;
		logln(4, "Prediction generated");
	};

	public double predict(int i) {
		return predict(the_examples.get_example(i));
	}

	/**
	 * predict a single example
	 */
	@Override
	public double predict(SVMExample sVMExample) {
		int i;
		int[] sv_index;
		double[] sv_att;
		double the_sum = the_examples.get_b();
		double alpha;
		for (i = 0; i < examples_total; i++) {
			alpha = alphas[i];
			if (alpha != 0) {
				sv_index = the_examples.index[i];
				sv_att = the_examples.atts[i];
				the_sum += alpha * the_kernel.calculate_K(sv_index, sv_att, sVMExample.index, sVMExample.att);
			}
			;
		}
		;
		return the_sum;
	};

	/**
	 * check internal variables, for debugging only
	 */
	protected void check() {
		double tsum;
		int i, j;
		double s = 0;
		for (i = 0; i < examples_total; i++) {
			s += alphas[i];
			tsum = 0;
			for (j = 0; j < the_examples.count_examples(); j++) {
				tsum += alphas[j] * the_kernel.calculate_K(i, j);
			}
			;
			if (Math.abs(tsum - sum[i]) > is_zero) {
				logln(1, "ERROR: sum[" + i + "] off by " + (tsum - sum[i]));
				// throw(new Exception("ERROR: sum["+i+"] off by
				// "+(tsum-sum[i])));
				// System.exit(1);
			}
			;
		}
		;
		if (Math.abs(s + sum_alpha) > is_zero) {
			logln(1, "ERROR: sum_alpha is off by " + (s + sum_alpha));
			// throw(new Exception("ERROR: sum_alpha is off by
			// "+(s+sum_alpha)));
			// System.exit(1);
		}
		;
	};

	/**
	 * Returns a double array of estimated performance values. These are accuracy, precision and
	 * recall. Works only for classification SVMs.
	 */
	public double[] getXiAlphaEstimation(Kernel kernel) {
		double r_delta = 0.0d;

		for (int j = 0; j < examples_total; j++) {
			double norm_x = kernel.calculate_K(j, j);
			for (int i = 0; i < examples_total; i++) {
				double r_current = norm_x - kernel.calculate_K(i, j);
				if (r_current > r_delta) {
					r_delta = r_current;
				}
			}
		}

		int total_pos = 0;
		int total_neg = 0;
		int estim_pos = 0;
		int estim_neg = 0;
		double xi = 0.0d;

		for (int i = 0; i < examples_total; i++) {
			double alpha = the_examples.get_alpha(i);
			double prediction = predict(i);
			double y = the_examples.get_y(i);

			if (y > 0) {
				if (prediction > 1) {
					xi = 0;
				} else {
					xi = 1 - prediction;
				}
				;
				if (2 * alpha * r_delta + xi >= 1) {
					estim_pos++;
				}
				;
				total_pos++;
			} else {
				if (prediction < -1) {
					xi = 0;
				} else {
					xi = 1 + prediction;
				}
				;
				if (2 * (-alpha) * r_delta + xi >= 1) {
					estim_neg++;
				}
				;
				total_neg++;
			}
			;
		}
		;

		double[] result = new double[3];

		result[0] = 1.0d - (double) (estim_pos + estim_neg) / (double) (total_pos + total_neg);
		result[1] = (double) (total_pos - estim_pos) / (double) (total_pos - estim_pos + estim_neg);
		result[2] = 1.0d - (double) estim_pos / (double) total_pos;

		return result;
	}
};
