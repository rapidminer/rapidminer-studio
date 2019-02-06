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

import com.rapidminer.operator.Operator;
import com.rapidminer.operator.learner.functions.kernel.JMySVMLearner;
import com.rapidminer.operator.learner.functions.kernel.jmysvm.examples.SVMExamples;
import com.rapidminer.operator.learner.functions.kernel.jmysvm.kernel.Kernel;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.RandomGenerator;


/**
 * Class for pattern recognition SVM
 * 
 * @author Stefan Rueping, Ingo Mierswa
 */
public class SVMpattern extends SVM {

	private boolean calculateXiAlpha = false;

	public SVMpattern() {}

	public SVMpattern(Operator paramOperator, Kernel kernel, SVMExamples sVMExamples,
			com.rapidminer.example.ExampleSet rapidMinerExamples, RandomGenerator randomGenerator)
			throws UndefinedParameterError {
		super(paramOperator, kernel, sVMExamples, rapidMinerExamples, randomGenerator);
		this.calculateXiAlpha = paramOperator.getParameterAsBoolean(JMySVMLearner.PARAMETER_ESTIMATE_PERFORMANCE);
	};

	/**
	 * Calls the optimizer
	 */
	@Override
	protected void optimize() {
		// optimizer-specific call

		// qp.n = working_set_size;

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
					if ((my_primal[i] > qp.l[i]) && (my_primal[i] < qp.u[i])) {
						// real sv
						my_primal[i] += qp.A[i] * new_constraint_sum;
					}
					;
				}
				;
			} else if (Math.abs(new_constraint_sum) > working_set_size * is_zero) {
				// error, can't get feasible point
				logln(5, "WARNING: No SVs, constraint_sum = " + new_constraint_sum);
				old_target = -Double.MIN_VALUE;
				convError = true;
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
					if ((my_primal[i] > qp.l[i]) && (my_primal[i] < qp.u[i])) {
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
				logln(5, "WARNING: no descend (" + (old_target - new_target) + " <= " + descend + "), stopping.");
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

	protected double[] getAlphas() {
		return alphas;
	}

	@Override
	protected final boolean is_alpha_neg(int i) {
		boolean result;
		if (ys[i] > 0) {
			result = true;
		} else {
			result = false;
		}
		;
		return result;
	};

	@Override
	protected final double nabla(int i) {
		double result;
		if (is_alpha_neg(i)) {
			result = (sum[i] - 1);
		} else {
			result = (-sum[i] - 1);
		}
		;
		return result;
	};

	@Override
	protected void print_statistics() {
		int dim = the_examples.get_dim();
		int i, j;
		double alpha;
		double[] x;
		int svs = 0;
		int bsv = 0;
		int correct_pos = 0;
		int correct_neg = 0;
		int total_pos = 0;
		int total_neg = 0;
		double y;
		double prediction;
		double min_lambda = Double.MAX_VALUE;
		double b = the_examples.get_b();
		double xi;
		double r_delta = 0;
		boolean do_xi_alpha = false;
		double norm_w = 0;
		double max_norm_x = 0;
		double min_norm_x = 1e20;
		double norm_x = 0;
		int estim_pos = 0;
		int estim_neg = 0;

		// xi-alpha estimators
		if (calculateXiAlpha) {
			do_xi_alpha = true;
			for (i = 0; i < examples_total; i++) {
				// needed before test-loop for performance estimators
				norm_w += alphas[i] * sum[i];

				alpha = alphas[i];
				if (alpha != 0) {
					norm_x = the_kernel.calculate_K(i, i);
					if (norm_x > max_norm_x) {
						max_norm_x = norm_x;
					}
					;
					if (norm_x < min_norm_x) {
						min_norm_x = norm_x;
					}
					;
				}
				;
			}
			;

			r_delta = 0;
			double r_current;
			for (j = 0; j < examples_total; j++) {
				norm_x = the_kernel.calculate_K(j, j);
				for (i = 0; i < examples_total; i++) {
					r_current = norm_x - the_kernel.calculate_K(i, j);
					if (r_current > r_delta) {
						r_delta = r_current;
					}
					;
				}
				;
			}
			;
		}
		;

		for (i = 0; i < examples_total; i++) {
			if (lambda(i) < min_lambda) {
				min_lambda = lambda(i);
			}
			;
			y = ys[i];
			prediction = sum[i] + b;
			alpha = alphas[i];
			if (y > 0) {
				if (prediction > 0) {
					correct_pos++;
				}
				;
				if (do_xi_alpha) {
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
				}
				;
				total_pos++;
			} else {
				if (prediction <= 0) {
					correct_neg++;
				}
				;
				if (do_xi_alpha) {
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
				}
				;
				total_neg++;
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
		min_lambda = -min_lambda;

		logln(1, "Error on KKT is " + min_lambda);
		logln(1, svs + " SVs");
		logln(1, bsv + " BSVs");
		logln(1, "Accuracy : " + ((double) (correct_pos + correct_neg) / (double) (total_pos + total_neg)));
		logln(1, "Precision: " + ((double) correct_pos / (double) (correct_pos + total_neg - correct_neg)));
		logln(1, "Recall   : " + ((double) correct_pos / (double) total_pos));
		logln(1, "Pred:\t+\t-");
		logln(1, "\t" + correct_pos + "\t" + (total_pos - correct_pos) + "\t(true pos)");
		logln(1, "\t" + (total_neg - correct_neg) + "\t" + correct_neg + "\t(true neg)");
		if (do_xi_alpha) {
			logln(1, "Xi-Alpha Accuracy" + (1 - ((double) (estim_pos + estim_neg)) / ((double) (total_pos + total_neg))));
			logln(1, "Xi-Alpha Precision"
					+ (((double) (total_pos - estim_pos)) / ((double) (total_pos - estim_pos + estim_neg))));
			logln(1, "Xi-Alpha Recall" + (1 - (double) estim_pos / ((double) total_pos)));
		}
		;

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
	};
	// };
};
