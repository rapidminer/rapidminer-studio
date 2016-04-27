/**
 * Copyright (c) 2007-2014 The LIBLINEAR Project. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted
 * provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions
 * and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other materials provided with
 * the distribution.
 *
 * 3. Neither name of copyright holders nor the names of its contributors may be used to endorse or
 * promote products derived from this software without specific prior written permission.
 *
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF
 * THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package de.bwaldvogel.liblinear;

import static de.bwaldvogel.liblinear.Linear.info;


/**
 * Trust Region Newton Method optimization
 */
class Tron {

	private final Function fun_obj;

	private final double eps;

	private final int max_iter;

	public Tron(final Function fun_obj) {
		this(fun_obj, 0.1);
	}

	public Tron(final Function fun_obj, double eps) {
		this(fun_obj, eps, 1000);
	}

	public Tron(final Function fun_obj, double eps, int max_iter) {
		this.fun_obj = fun_obj;
		this.eps = eps;
		this.max_iter = max_iter;
	}

	void tron(double[] w) {
		// Parameters for updating the iterates.
		double eta0 = 1e-4, eta1 = 0.25, eta2 = 0.75;

		// Parameters for updating the trust region size delta.
		double sigma1 = 0.25, sigma2 = 0.5, sigma3 = 4;

		int n = fun_obj.get_nr_variable();
		int i, cg_iter;
		double delta, snorm, one = 1.0;
		double alpha, f, fnew, prered, actred, gs;
		int search = 1, iter = 1;
		double[] s = new double[n];
		double[] r = new double[n];
		double[] w_new = new double[n];
		double[] g = new double[n];

		for (i = 0; i < n; i++) {
			w[i] = 0;
		}

		f = fun_obj.fun(w);
		fun_obj.grad(w, g);
		delta = euclideanNorm(g);
		double gnorm1 = delta;
		double gnorm = gnorm1;

		if (gnorm <= eps * gnorm1) {
			search = 0;
		}

		iter = 1;

		while (iter <= max_iter && search != 0) {
			cg_iter = trcg(delta, g, s, r);

			System.arraycopy(w, 0, w_new, 0, n);
			daxpy(one, s, w_new);

			gs = dot(g, s);
			prered = -0.5 * (gs - dot(s, r));
			fnew = fun_obj.fun(w_new);

			// Compute the actual reduction.
			actred = f - fnew;

			// On the first iteration, adjust the initial step bound.
			snorm = euclideanNorm(s);
			if (iter == 1) {
				delta = Math.min(delta, snorm);
			}

			// Compute prediction alpha*snorm of the step.
			if (fnew - f - gs <= 0) {
				alpha = sigma3;
			} else {
				alpha = Math.max(sigma1, -0.5 * (gs / (fnew - f - gs)));
			}

			// Update the trust region bound according to the ratio of actual to
			// predicted reduction.
			if (actred < eta0 * prered) {
				delta = Math.min(Math.max(alpha, sigma1) * snorm, sigma2 * delta);
			} else if (actred < eta1 * prered) {
				delta = Math.max(sigma1 * delta, Math.min(alpha * snorm, sigma2 * delta));
			} else if (actred < eta2 * prered) {
				delta = Math.max(sigma1 * delta, Math.min(alpha * snorm, sigma3 * delta));
			} else {
				delta = Math.max(delta, Math.min(alpha * snorm, sigma3 * delta));
			}

			info("iter %2d act %5.3e pre %5.3e delta %5.3e f %5.3e |g| %5.3e CG %3d%n", iter, actred, prered, delta, f,
					gnorm, cg_iter);

			if (actred > eta0 * prered) {
				iter++;
				System.arraycopy(w_new, 0, w, 0, n);
				f = fnew;
				fun_obj.grad(w, g);

				gnorm = euclideanNorm(g);
				if (gnorm <= eps * gnorm1) {
					break;
				}
			}
			if (f < -1.0e+32) {
				info("WARNING: f < -1.0e+32%n");
				break;
			}
			if (Math.abs(actred) <= 0 && prered <= 0) {
				info("WARNING: actred and prered <= 0%n");
				break;
			}
			if (Math.abs(actred) <= 1.0e-12 * Math.abs(f) && Math.abs(prered) <= 1.0e-12 * Math.abs(f)) {
				info("WARNING: actred and prered too small%n");
				break;
			}
		}
	}

	private int trcg(double delta, double[] g, double[] s, double[] r) {
		int n = fun_obj.get_nr_variable();
		double one = 1;
		double[] d = new double[n];
		double[] Hd = new double[n];
		double rTr, rnewTrnew, cgtol;

		for (int i = 0; i < n; i++) {
			s[i] = 0;
			r[i] = -g[i];
			d[i] = r[i];
		}
		cgtol = 0.1 * euclideanNorm(g);

		int cg_iter = 0;
		rTr = dot(r, r);

		while (true) {
			if (euclideanNorm(r) <= cgtol) {
				break;
			}
			cg_iter++;
			fun_obj.Hv(d, Hd);

			double alpha = rTr / dot(d, Hd);
			daxpy(alpha, d, s);
			if (euclideanNorm(s) > delta) {
				info("cg reaches trust region boundary%n");
				alpha = -alpha;
				daxpy(alpha, d, s);

				double std = dot(s, d);
				double sts = dot(s, s);
				double dtd = dot(d, d);
				double dsq = delta * delta;
				double rad = Math.sqrt(std * std + dtd * (dsq - sts));
				if (std >= 0) {
					alpha = (dsq - sts) / (std + rad);
				} else {
					alpha = (rad - std) / dtd;
				}
				daxpy(alpha, d, s);
				alpha = -alpha;
				daxpy(alpha, Hd, r);
				break;
			}
			alpha = -alpha;
			daxpy(alpha, Hd, r);
			rnewTrnew = dot(r, r);
			double beta = rnewTrnew / rTr;
			scale(beta, d);
			daxpy(one, r, d);
			rTr = rnewTrnew;
		}

		return cg_iter;
	}

	/**
	 * constant times a vector plus a vector
	 *
	 * <pre>
	 * vector2 += constant * vector1
	 * </pre>
	 *
	 * @since 1.8
	 */
	private static void daxpy(double constant, double vector1[], double vector2[]) {
		if (constant == 0) {
			return;
		}

		assert vector1.length == vector2.length;
		for (int i = 0; i < vector1.length; i++) {
			vector2[i] += constant * vector1[i];
		}
	}

	/**
	 * returns the dot product of two vectors
	 *
	 * @since 1.8
	 */
	private static double dot(double vector1[], double vector2[]) {

		double product = 0;
		assert vector1.length == vector2.length;
		for (int i = 0; i < vector1.length; i++) {
			product += vector1[i] * vector2[i];
		}
		return product;

	}

	/**
	 * returns the euclidean norm of a vector
	 *
	 * @since 1.8
	 */
	private static double euclideanNorm(double vector[]) {

		int n = vector.length;

		if (n < 1) {
			return 0;
		}

		if (n == 1) {
			return Math.abs(vector[0]);
		}

		// this algorithm is (often) more accurate than just summing up the squares and taking the
		// square-root afterwards

		double scale = 0; // scaling factor that is factored out
		double sum = 1; // basic sum of squares from which scale has been factored out
		for (int i = 0; i < n; i++) {
			if (vector[i] != 0) {
				double abs = Math.abs(vector[i]);
				// try to get the best scaling factor
				if (scale < abs) {
					double t = scale / abs;
					sum = 1 + sum * (t * t);
					scale = abs;
				} else {
					double t = abs / scale;
					sum += t * t;
				}
			}
		}

		return scale * Math.sqrt(sum);
	}

	/**
	 * scales a vector by a constant
	 *
	 * @since 1.8
	 */
	private static void scale(double constant, double vector[]) {
		if (constant == 1.0) {
			return;
		}
		for (int i = 0; i < vector.length; i++) {
			vector[i] *= constant;
		}

	}
}
