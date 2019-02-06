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
package com.rapidminer.operator.clustering.clusterer;

import com.rapidminer.operator.Operator;
import com.rapidminer.operator.learner.functions.kernel.AbstractMySVMLearner;
import com.rapidminer.operator.learner.functions.kernel.jmysvm.examples.SVMExample;
import com.rapidminer.operator.learner.functions.kernel.jmysvm.examples.SVMExamples;
import com.rapidminer.operator.learner.functions.kernel.jmysvm.kernel.Kernel;
import com.rapidminer.operator.learner.functions.kernel.jmysvm.optimizer.QuadraticProblem;
import com.rapidminer.operator.learner.functions.kernel.jmysvm.optimizer.QuadraticProblemSMO;
import com.rapidminer.operator.learner.functions.kernel.jmysvm.svm.SVMInterface;
import com.rapidminer.operator.learner.functions.kernel.jmysvm.util.MaxHeap;
import com.rapidminer.operator.learner.functions.kernel.jmysvm.util.MinHeap;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.LogService;


/**
 * SVClustering.
 *
 * @author Stefan Rueping, Ingo Mierswa
 */
public class SVClusteringAlgorithm implements SVMInterface {

	private static final int[] RAPID_MINER_VERBOSITY = { LogService.STATUS, LogService.STATUS, LogService.STATUS,
			LogService.MINIMUM, LogService.MINIMUM };

	protected Kernel kernel;

	protected SVCExampleSet examples;

	double[] alphas;

	protected int examples_total;

	protected int target_count;

	protected double convergence_epsilon = 1e-3;

	protected double lambda_factor;

	protected int[] at_bound;

	protected double[] sum;

	protected double[] K;

	protected int[] working_set;

	protected double[] primal;

	protected double sum_alpha;

	protected double lambda_eq;

	protected int to_shrink;

	protected double feasible_epsilon;

	protected double lambda_WS;

	boolean shrinked;

	private int max_iterations = 100000;

	protected int working_set_size = 10;

	protected int parameters_working_set_size = 10;

	protected double is_zero = 1e-10;

	protected int shrink_const = 55;

	protected double C = 0.0d;

	protected double descend = 1e-15;

	MinHeap heap_min;

	MaxHeap heap_max;

	protected QuadraticProblem qp;

	private Operator paramOperator;

	public SVClusteringAlgorithm() {
		// Empty constructor
	}

	public SVClusteringAlgorithm(Operator paramOperator, Kernel new_kernel, SVCExampleSet new_examples)
			throws UndefinedParameterError {
		init(new_kernel, new_examples);
		this.paramOperator = paramOperator;
		examples = new_examples;
		max_iterations = paramOperator.getParameterAsInt(AbstractMySVMLearner.PARAMETER_MAX_ITERATIONS);
		convergence_epsilon = paramOperator.getParameterAsDouble(AbstractMySVMLearner.PARAMETER_CONVERGENCE_EPSILON);
		C = 1.0 / (examples_total * paramOperator.getParameterAsDouble(SVClustering.PARAMETER_P));
	};

	public void init(Kernel new_kernel, SVCExampleSet new_examples) {
		kernel = new_kernel;
		examples = new_examples;
		examples_total = examples.count_examples();
		parameters_working_set_size = working_set_size;
		lambda_factor = 1.0;
		lambda_eq = 0;
		target_count = 0;
		sum_alpha = 0;
		feasible_epsilon = convergence_epsilon;
		alphas = examples.get_alphas();
	};

	/**
	 * Train the SVM
	 */
	@Override
	public void train() {
		if (examples_total <= 0) {
			// exception?
			examples.set_b(Double.NaN);
			examples.set_R(0);
			return;
		}
		if (examples_total == 1) {
			examples.set_b(kernel.calculate_K(0, 0));
			examples.set_R(0);
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
			optimize();
			put_optimizer_values();
			converged = convergence();
			if (converged) {
				project_to_constraint();
				if (shrinked) {
					// check convergence for all alphas
					logln(2, "***** Checking convergence for all variables");
					reset_shrinked();
					converged = convergence();
				}
				if (converged) {
					logln(1, "*** Convergence");
					break M;
				}
				// set variables free again
				shrink_const += 10;
				target_count = 0;
				for (int i = 0; i < examples_total; i++) {
					at_bound[i] = 0;
				}
			}

			shrink();
			calculate_working_set();
			update_working_set();
		}

		int i;
		if (iteration >= max_iterations && !converged) {
			logln(1, "*** No convergence: Time up.");
			if (shrinked) {
				// set sums for all variables for statistics
				reset_shrinked();
			}
		}

		// calculate R and b
		double new_b = 0;
		double new_R = 0;
		int new_R_count = 0;
		// check();
		for (i = 0; i < examples_total; i++) {
			new_b += alphas[i] * sum[i];
			if (alphas[i] - C < -is_zero && alphas[i] > is_zero) {
				new_R += K[i] - 2 * sum[i];
				new_R_count++;
			}
		}

		examples.set_b(new_b);
		if (new_R_count > 0) {
			new_R = new_b + new_R / new_R_count;
			if (new_R < 0.0) {
				new_R = 0;
			}
			;
			examples.set_R(Math.sqrt(new_R));
		} else {
			// unlikely
			double minR = Double.MIN_VALUE;
			double maxR = Double.MAX_VALUE;
			for (i = 0; i < examples_total; i++) {
				new_R = K[i] - 2 * sum[i] + new_b;
				if (alphas[i] <= is_zero && new_R > minR) {
					minR = new_R;
				}

				if (alphas[i] - C >= -is_zero && new_R < maxR) {
					maxR = new_R;
				}
			}
			if (minR > Double.MIN_VALUE) {
				if (maxR < Double.MAX_VALUE) {
					new_R = (minR + maxR) / 2.0;
				} else {
					new_R = minR;
				}
			} else {
				new_R = maxR;
			}
			examples.set_R(Math.sqrt(new_R));
		}
		// if(verbosity>= 2){
		logln(2, "Done training: " + iteration + " iterations.");
		print_statistics();
		exit_optimizer();
	}

	/**
	 * print statistics about result
	 */
	protected void print_statistics() {
		int dim = examples.get_dim();
		int i, j;
		double alpha;
		double[] x;
		int svs = 0;
		int bsv = 0;
		double min_lambda = Double.MAX_VALUE;
		double R = examples.get_R();
		for (i = 0; i < examples_total; i++) {
			if (lambda(i) < min_lambda) {
				min_lambda = lambda(i);
			}
			;
			alpha = alphas[i];
			if (alpha != 0) {
				svs++;
				if (alpha == C) {
					bsv++;
				}
				;
			}
			;
		}
		;
		logln(1, "Error on KKT is " + -min_lambda);
		logln(1, svs + " SVs");
		logln(1, bsv + " BSVs");
		logln(1, "R = " + R);
		// if(verbosity >= 2){
		// print hyperplane
		double[] w = new double[dim];
		for (j = 0; j < dim; j++) {
			w[j] = 0;
		}
		for (i = 0; i < examples_total; i++) {
			x = examples.get_example(i).toDense(dim);
			alpha = alphas[i];
			for (j = 0; j < dim; j++) {
				w[j] += alpha * x[j];
			}
			;
		}
		;
		for (j = 0; j < dim; j++) {
			logln(2, "a[" + j + "] = " + w[j]);
		}
		;
	};

	/** Returns the value of b. */
	@Override
	public double getB() {
		return examples.get_b();
	}

	/** Returns the value of R. */
	public double getR() {
		return examples.get_R();
	}

	/**
	 * init the optimizer
	 */
	protected void init_optimizer() {
		primal = new double[working_set_size];
		sum = new double[examples_total];
		K = new double[examples_total];
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
		int i;
		for (i = 0; i < working_set_size; i++) {
			qp.l[i] = 0; // -is_zero;
		}
		;
		double s = 1.0d;
		i = 0;
		while (i < examples_total && s > 0) {
			if (s < C) {
				alphas[i] = s;
				break;
			}
			;
			alphas[i] = C;
			s -= C;
			i++;
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
						examples.swap(i, last_pos);
						kernel.swap(i, last_pos);
						sum[i] = sum[last_pos];
						K[i] = K[last_pos];
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
					kernel.set_examples_size(examples_total);
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
		examples_total = examples.count_examples();
		kernel.set_examples_size(examples_total);
		// unshrink, recalculate sum for all variables
		int i, j;
		// reset all sums
		for (i = old_ex_tot; i < examples_total; i++) {
			sum[i] = 0;
			K[i] = kernel.calculate_K(i, i);
			at_bound[i] = 0;
		}
		;
		double alpha;
		double[] kernel_row;
		for (i = 0; i < examples_total; i++) {
			alpha = alphas[i];
			if (alpha != 0) {
				kernel_row = kernel.get_row(i);
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
		double alpha_sum = sum_alpha - 1;
		int SVcount = 0;
		double alpha;
		int i;
		for (i = 0; i < examples_total; i++) {
			alpha = alphas[i];
			alpha_sum += alpha;
			if (alpha > 0 && alpha < C) {
				SVcount++;
			}
			;
		}
		;
		if (alpha_sum != 0.0d) {
			double alpha_delta;
			if (SVcount > 0) {
				// project
				double[] kernel_row;
				alpha_delta = alpha_sum / SVcount;
				alpha_sum = sum_alpha - 1;
				for (i = 0; i < examples_total; i++) {
					alpha = alphas[i];
					if (alpha > 0 && alpha < C) {
						alphas[i] -= alpha_delta;
						kernel_row = kernel.get_row(i);
						for (int j = 0; j < examples_total; j++) {
							sum[j] -= alpha_delta * kernel_row[j];
						}
						;
					}
					;
					alpha_sum += alpha;
				}
				;
			}
			;
			if (Math.abs(alpha_sum) > is_zero) {
				// project more aggressive
				i = 0;
				while (i < examples_total && alpha_sum != 0.0) {
					alpha = alphas[i];
					if (alpha_sum > 0) {
						if (alpha > 0) {
							if (alpha < alpha_sum) {
								alpha_sum -= alpha;
								alphas[i] = 0;
							} else {
								alphas[i] -= alpha_sum;
								alpha_sum = 0;
							}
							;
						}
						;
					} else {
						if (alpha < C) {
							if (C - alpha < -alpha_sum) {
								alpha_sum += C - alpha;
								alphas[i] = C;
							} else {
								alphas[i] -= alpha_sum;
								alpha_sum = 0;
							}
							;
						}
						;
					}
					;
				}
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
		double the_nabla;
		boolean is_feasible;
		int j;
		if (target_count < 3) {
			while (i < examples_total) {
				is_feasible = feasible(i);
				if (is_feasible) {
					the_nabla = nabla(i);
					// add to heaps
					heap_max.add(the_nabla, i);
					heap_min.add(the_nabla, i);
				}
				;
				i++;
			}
			;
		} else {
			while (i < examples_total) {
				is_feasible = feasible(i);
				if (is_feasible) {
					the_nabla = lambda(i);
					// add to heaps
					heap_max.add(the_nabla, i);
					heap_min.add(the_nabla, i);
				}
				;
				i++;
			}
			;
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
		if (!heap_min.empty() && !heap_max.empty()) {
			if (heap_min.top_value() >= heap_max.top_value()) {
				// there could be the same values in the min- and maxheap,
				// sort them out (this is very unlikely)
				j = 0;
				i = 0;
				while (i < pos) {
					// working_set[i] also in max-heap?
					j = pos;
					while (j < working_set_size && working_set[j] != working_set[i]) {
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
		if (target_count > 1) {
			// convergence error on last iteration?
			// some more tests on WS
			// unlikely to happen, so speed isn't so important
			// are all variables at the bound?
			int pos_abs;
			boolean bounded_pos = true;
			boolean bounded_neg = true;
			pos = 0;
			double alpha;
			while (pos < working_set_size && (bounded_pos || bounded_neg)) {
				pos_abs = working_set[pos];
				alpha = alphas[pos_abs];
				if (alpha - C < -is_zero) {
					bounded_pos = false;
				}
				;
				if (alpha > is_zero) {
					bounded_neg = false;
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
					if (alpha - C < -is_zero) {
						if (lambda(pos_abs) < max_lambda) {
							max_lambda = lambda(pos_abs);
							max_pos = pos_abs;
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
					if (alpha > is_zero) {
						if (lambda(pos_abs) < max_lambda) {
							max_lambda = lambda(pos_abs);
							max_pos = pos_abs;
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
		if (working_set_size < parameters_working_set_size && working_set_size < examples_total) {
			// use full working set
			pos = (int) (Math.random() * examples_total);
			int ok;
			while (working_set_size < parameters_working_set_size && working_set_size < examples_total) {
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
		for (pos_i = 0; pos_i < working_set_size; pos_i++) {
			i = working_set[pos_i];
			// put row sort_i in hessian
			kernel_row = kernel.get_row(i);
			sum_WS = 0;
			for (pos_j = 0; pos_j < pos_i; pos_j++) {
				j = working_set[pos_j];
				// put all elements K(i,j) in hessian, where j in WS
				qp.H[pos_i * working_set_size + pos_j] = 2 * kernel_row[j];
				qp.H[pos_j * working_set_size + pos_i] = 2 * kernel_row[j];
			}
			;
			for (pos_j = 0; pos_j < working_set_size; pos_j++) {
				j = working_set[pos_j];
				sum_WS += alphas[j] * kernel_row[j];
			}
			;
			// set main diagonal
			qp.H[pos_i * working_set_size + pos_i] = 2 * kernel_row[i];
			// linear and box constraints
			qp.A[pos_i] = 1;
			qp.c[pos_i] = 2 * (sum[i] - sum_WS) - K[i];
			primal[pos_i] = alphas[i];
			qp.u[pos_i] = C;
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
		project_to_constraint();
		// calculate sum
		int i, j;
		double[] kernel_row;
		double alpha;
		for (i = 0; i < examples_total; i++) {
			sum[i] = 0;
			at_bound[i] = 0;
			K[i] = kernel.calculate_K(i, i);
		}
		for (i = 0; i < examples_total; i++) {
			alpha = alphas[i];
			if (alpha != 0.0d) {
				kernel_row = kernel.get_row(i);
				for (j = 0; j < examples_total; j++) {
					sum[j] += alpha * kernel_row[j];
				}
				;
			}
			;
		}
		;
		// first working set is random
		j = 0;
		i = 0;
		while (i < working_set_size && j < examples_total) {
			working_set[i] = j;
			i++;
			j++;
		}
		;
		working_set[working_set_size - 1] = examples_total - 1;
		update_working_set();
	};

	/**
	 * Calls the optimizer
	 */
	protected void optimize() {
		// check();
		// optimizer-specific call
		int i;
		int j;
		// equality constraint
		qp.b[0] = 0;
		for (i = 0; i < working_set_size; i++) {
			qp.b[0] += alphas[working_set[i]];
		}
		;
		double[] my_primal = primal;
		// set initial optimization parameters
		double new_target = 0;
		double old_target = 0;
		double target_tmp;
		for (i = 0; i < working_set_size; i++) {
			target_tmp = my_primal[i] * qp.H[i * working_set_size + i] / 2;
			for (j = 0; j < i; j++) {
				target_tmp += my_primal[j] * qp.H[j * working_set_size + i];
			}
			;
			target_tmp += qp.c[i];
			old_target += target_tmp * my_primal[i];
		}
		;
		double new_constraint_sum = 0;
		double my_is_zero = is_zero;
		int sv_count = working_set_size;
		// optimize
		boolean KKTerror = true; // KKT not yet satisfied
		boolean convError = false; // KKT can still be satisfied
		qp.max_allowed_error = convergence_epsilon;
		qp.x = my_primal;
		qp.lambda_eq = lambda_eq;
		qp.solve();
		my_primal = qp.x;
		lambda_WS = qp.lambda_eq;
		// loop while some KKT condition is not valid (alpha=0)
		while (KKTerror) {
			// clip
			sv_count = working_set_size;
			new_constraint_sum = qp.b[0];
			for (i = 0; i < working_set_size; i++) {
				// check if at bound
				if (my_primal[i] <= my_is_zero) {
					// at lower bound
					my_primal[i] = qp.l[i];
					sv_count--;
				} else if (qp.u[i] - my_primal[i] <= my_is_zero) {
					// at upper bound
					my_primal[i] = qp.u[i];
					sv_count--;
				}
				;
				new_constraint_sum -= qp.A[i] * my_primal[i];
			}
			;
			// enforce equality constraint
			if (sv_count > 0) {
				new_constraint_sum /= sv_count;
				logln(5, "adjusting " + sv_count + " alphas by " + new_constraint_sum);
				for (i = 0; i < working_set_size; i++) {
					if (my_primal[i] > qp.l[i] && my_primal[i] < qp.u[i]) {
						// real sv
						my_primal[i] += qp.A[i] * new_constraint_sum;
					}
					;
				}
				;
			} else if (Math.abs(new_constraint_sum) > working_set_size * is_zero) {
				// error, can't get feasible point
				logln(5, "WARNING: No SVs, constraint_sum = " + new_constraint_sum);
				old_target = Double.MIN_VALUE;
				new_target = Double.MAX_VALUE;
				convError = true;
				break;
			}
			;
			// test descend
			new_target = 0;
			for (i = 0; i < working_set_size; i++) {
				// attention: optimizer changes one triangle of H!
				target_tmp = my_primal[i] * qp.H[i * working_set_size + i] / 2.0;
				for (j = 0; j < i; j++) {
					target_tmp += my_primal[j] * qp.H[j * working_set_size + i];
				}
				;
				target_tmp += qp.c[i];
				new_target += target_tmp * my_primal[i];
			}
			;
			if (new_target < old_target) {
				KKTerror = false;
				if (descend < old_target - new_target) {
					target_count = 0;
				} else {
					convError = true;
				}
				;
				logln(5, "descend = " + (old_target - new_target));
			} else if (sv_count > 0) {
				// less SVs
				// set my_is_zero to min_i(primal[i]-qp.l[i], qp.u[i]-primal[i])
				my_is_zero = Double.MAX_VALUE;
				for (i = 0; i < working_set_size; i++) {
					if (my_primal[i] > qp.l[i] && my_primal[i] < qp.u[i]) {
						if (my_primal[i] - qp.l[i] < my_is_zero) {
							my_is_zero = my_primal[i] - qp.l[i];
						}
						;
						if (qp.u[i] - my_primal[i] < my_is_zero) {
							my_is_zero = qp.u[i] - my_primal[i];
						}
						;
					}
					;
				}
				;
				if (target_count == 0) {
					my_is_zero *= 2;
				}
				;
				logln(5, "WARNING: no descend (" + (old_target - new_target) + " <= " + descend + "), adjusting is_zero to "
						+ my_is_zero);
				logln(5, "new_target = " + new_target);
			} else {
				// nothing we can do
				logln(5, "WARNING: no descend (" + (old_target - new_target) + " <= " + descend + "), stopping (it = "
						+ target_count + ").");
				KKTerror = false;
				convError = true;
			}
			;
		}
		;
		if (convError) {
			target_count++;
			if (old_target < new_target) {
				for (i = 0; i < working_set_size; i++) {
					my_primal[i] = qp.A[i] * alphas[working_set[i]];
				}
				;
				logln(5, "WARNING: Convergence error, restoring old primals");
			}
			;
		}
		;
		if (target_count > 50) {
			// non-recoverable numerical error
			convergence_epsilon *= 2;
			feasible_epsilon = convergence_epsilon;
			logln(1, "WARNING: reducing KKT precision to " + convergence_epsilon);
			target_count = 0;
		}
		;
	};

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
			the_new_alpha = primal[pos_i];
			// next three statements: keep this order!
			i = working_set[pos_i];
			alpha_diff = the_new_alpha - alphas[i];
			alphas[i] = the_new_alpha;
			if (alpha_diff != 0) {
				// update sum ( => nabla)
				kernel_row = kernel.get_row(i);
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
			if (alpha > 0 && alpha < C) {
				the_lambda_eq += -nabla(i);
				total++;
			}
			;
		}
		;
		logln(4, "lambda_eq = " + the_lambda_eq / total);
		if (total > 0) {
			lambda_eq = the_lambda_eq / total;
		} else {
			double l_min = Double.MAX_VALUE;
			double l_max = Double.MIN_VALUE;
			double nabla;
			for (i = 0; i < examples_total; i++) {
				nabla = -nabla(i);
				if (alphas[i] < is_zero) {
					if (nabla > l_max) {
						l_max = nabla;
					}
					;
				} else {
					if (nabla < l_min) {
						l_min = nabla;
					}
					;
				}
				;
			}
			;
			lambda_eq = (l_min + l_max) / 2.0; // goal: l_max <= lambda_eq <=
			// l_min
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
					lambda_eq = -nabla(i);
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
		if (java.lang.Math.abs(alpha_sum + sum_alpha - 1) > convergence_epsilon) {
			// equality constraint violated
			logln(4, "No convergence: equality constraint violated: |" + (alpha_sum + sum_alpha - 1) + "| >> 0");
			project_to_constraint();
			result = false;
		}
		;
		i = 0;
		while (i < examples_total && result != false) {
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

	protected final double nabla(int i) {
		return -K[i] + 2 * sum[i];
	};

	/**
	 * lagrangian multiplier of variable i
	 *
	 * @param i
	 *            variable index
	 * @return lambda
	 */
	protected double lambda(int i) {
		double alpha;
		double result;
		result = -java.lang.Math.abs(nabla(i) + lambda_eq);
		alpha = alphas[i];
		if (alpha > is_zero) {
			if (alpha - C >= -is_zero) {
				// upper bound active
				result = -lambda_eq - nabla(i);
			}
			;
		} else {
			// lower bound active
			result = nabla(i) + lambda_eq;
		}
		;
		return result;
	};

	protected boolean feasible(int i) {
		boolean is_feasible = true;
		double alpha = alphas[i];
		double the_lambda = lambda(i);
		if (alpha - C >= -is_zero) {
			if (the_lambda >= 0) {
				at_bound[i]++;
				if (at_bound[i] == shrink_const) {
					to_shrink++;
				}
			} else {
				at_bound[i] = 0;
			}
			;
		} else if (alpha <= is_zero) {
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
		} else {
			// not at bound
			at_bound[i] = 0;
		}
		;
		// if((the_lambda >= feasible_epsilon) || (at_bound[i] >=
		// shrink_const)){
		if (at_bound[i] >= shrink_const) {
			is_feasible = false;
		}
		;
		return is_feasible;
	};

	/**
	 * log the output plus newline
	 *
	 * @param level
	 *            warning level
	 * @param message
	 *            Message test
	 */
	protected void logln(int level, String message) {
		paramOperator.getLog().log(message, RAPID_MINER_VERBOSITY[level - 1]);
	};

	/**
	 * predict values on the testset with model
	 */
	@Override
	public void predict(SVMExamples to_predict) {
		int i;
		double prediction;
		SVMExample sVMExample;
		int size = to_predict.count_examples();
		for (i = 0; i < size; i++) {
			sVMExample = to_predict.get_example(i);
			prediction = predict(sVMExample);
			to_predict.set_y(i, prediction);
		}
		;
		logln(4, "Prediction generated");
	};

	/**
	 * predict a single example
	 */
	@Override
	public double predict(SVMExample sVMExample) {
		int i;
		int[] sv_index;
		double[] sv_att;
		double the_sum = examples.get_b() + kernel.calculate_K(sVMExample, sVMExample);
		double alpha;
		for (i = 0; i < examples_total; i++) {
			alpha = alphas[i];
			if (alpha != 0) {
				sv_index = examples.index[i];
				sv_att = examples.atts[i];
				the_sum -= 2 * alpha * kernel.calculate_K(sv_index, sv_att, sVMExample.index, sVMExample.att);
			}
			;
		}
		;
		if (the_sum < 0) {
			// numerical error
			the_sum = 0;
		}
		;
		return Math.sqrt(the_sum);
	};

	/**
	 * check internal variables, for debugging only
	 */
	protected void check() {
		double tsum;
		int i, j;
		double s = 0;
		for (i = 0; i < examples_total; i++) {
			tsum = 0;
			s += alphas[i];
			for (j = 0; j < examples.count_examples(); j++) {
				tsum += alphas[j] * kernel.calculate_K(i, j);
			}
			;
			if (Math.abs(tsum - sum[i]) > is_zero) {
				logln(1, "ERROR: sum[" + i + "] off by " + (tsum - sum[i]) + " (is " + sum[i] + ", should be " + tsum);
				// throw(new Exception("ERROR: sum["+i+"] off by
				// "+(tsum-sum[i])));
				// System.exit(1);
			}
			;
		}
		;
		tsum = 0.0;
		for (j = 0; j < examples.count_examples(); j++) {
			tsum += alphas[j];
		}
		;
		if (Math.abs(tsum - 1) > is_zero) {
			logln(1, "ERROR: sum over all alphas is off by " + (tsum - 1));
		}
		if (Math.abs(s + sum_alpha - 1) > is_zero) {
			logln(1, "ERROR: sum_alpha is off by " + (s + sum_alpha - 1));
		}
		;
	};

	/**
	 *
	 */
	@Override
	public double[] getWeights() {
		return null;
	}

	/**
	 *
	 */
	@Override
	public void init(Kernel kernel_, SVMExamples examples_) {
		init(kernel_, (SVCExampleSet) examples_);
	}
};
