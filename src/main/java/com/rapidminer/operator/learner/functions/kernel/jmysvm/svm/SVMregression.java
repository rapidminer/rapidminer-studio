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
import com.rapidminer.operator.learner.functions.kernel.jmysvm.examples.SVMExamples;
import com.rapidminer.operator.learner.functions.kernel.jmysvm.kernel.Kernel;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.RandomGenerator;


/**
 * Class for regression SVM
 * 
 * @author Stefan Rueping, Ingo Mierswa
 */
public class SVMregression extends SVM {

	public SVMregression() {}

	public SVMregression(Operator paramOperator, Kernel kernel, SVMExamples sVMExamples,
			com.rapidminer.example.ExampleSet rapidMinerExamples, RandomGenerator randomGenerator)
			throws UndefinedParameterError {
		super(paramOperator, kernel, sVMExamples, rapidMinerExamples, randomGenerator);
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

		double[] my_primal = primal;
		// equality constraint
		qp.b[0] = 0;
		for (i = 0; i < working_set_size; i++) {
			qp.b[0] += alphas[working_set[i]];
		}
		;

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

		qp.x = primal;
		qp.solve();
		primal = qp.x;
		lambda_WS = qp.lambda_eq;
		my_primal = primal;

		// loop while some KKT condition is not valid (alpha=0)

		int it = 3;

		double lambda_lo;
		while (KKTerror && (it > 0)) {
			// iterate optimization 3 times with changed sign on variables, if
			// KKT conditions are not satisfied
			KKTerror = false;
			it--;
			for (i = 0; i < working_set_size; i++) {
				if (my_primal[i] < is_zero) {
					lambda_lo = epsilon_neg + epsilon_pos - qp.c[i];
					for (j = 0; j < working_set_size; j++) {
						lambda_lo -= my_primal[j] * qp.H[i * working_set_size + j];
					}
					;
					if (qp.A[i] > 0) {
						lambda_lo -= lambda_WS;
					} else {
						lambda_lo += lambda_WS;
					}
					;

					if (lambda_lo < -convergence_epsilon) {
						// change sign of i
						KKTerror = true;
						qp.A[i] = -qp.A[i];
						which_alpha[i] = !which_alpha[i];
						my_primal[i] = -my_primal[i];
						qp.c[i] = epsilon_neg + epsilon_pos - qp.c[i];
						if (qp.A[i] > 0) {
							qp.u[i] = cNeg[working_set[i]];
						} else {
							qp.u[i] = cPos[working_set[i]];
						}
						;
						for (j = 0; j < working_set_size; j++) {
							qp.H[i * working_set_size + j] = -qp.H[i * working_set_size + j];
							qp.H[j * working_set_size + i] = -qp.H[j * working_set_size + i];
						}
						;
						if (quadraticLossNeg) {
							if (which_alpha[i]) {
								(qp.H)[i * (working_set_size + 1)] += 1 / cNeg[working_set[i]];
								(qp.u)[i] = Double.MAX_VALUE;
							} else {
								// previous was neg
								(qp.H)[i * (working_set_size + 1)] -= 1 / cNeg[working_set[i]];
							}
							;
						}
						;
						if (quadraticLossPos) {
							if (!which_alpha[i]) {
								(qp.H)[i * (working_set_size + 1)] += 1 / cPos[working_set[i]];
								(qp.u)[i] = Double.MAX_VALUE;
							} else {
								// previous was pos
								(qp.H)[i * (working_set_size + 1)] -= 1 / cPos[working_set[i]];
							}
							;
						}
						;
					}
					;
				}
				;
			}
			;
			qp.x = my_primal;
			qp.solve();
			my_primal = qp.x;
			lambda_WS = qp.lambda_eq;
		}
		;

		KKTerror = true;

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

	@Override
	protected final boolean is_alpha_neg(int i) {
		boolean result;
		double alpha = alphas[i];
		if (alpha > 0) {
			result = true;
		} else if (alpha == 0) {
			if (sum[i] - ys[i] + lambda_eq > 0) {
				result = false;
			} else {
				result = true;
			}
			;
		} else {
			result = false;
		}
		;
		return result;
	};

	@Override
	protected final double nabla(int i) {
		double alpha = alphas[i];
		double y = ys[i];
		double result;
		if (alpha > 0) {
			result = (sum[i] - y + epsilon_neg);
		} else if (alpha == 0) {
			if (is_alpha_neg(i)) {
				result = (sum[i] - y + epsilon_neg);
			} else {
				result = (-sum[i] + y + epsilon_pos);
			}
			;
		} else {
			result = (-sum[i] + y + epsilon_pos);
		}
		;
		return result;
	};

};
