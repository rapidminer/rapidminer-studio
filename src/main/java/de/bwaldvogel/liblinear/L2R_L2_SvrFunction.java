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

/**
 * @since 1.91
 */
public class L2R_L2_SvrFunction extends L2R_L2_SvcFunction {

	private double p;

	public L2R_L2_SvrFunction(Problem prob, double[] C, double p) {
		super(prob, C);
		this.p = p;
	}

	@Override
	public double fun(double[] w) {
		double f = 0;
		double[] y = prob.y;
		int l = prob.l;
		int w_size = get_nr_variable();
		double d;

		Xv(w, z);

		for (int i = 0; i < w_size; i++) {
			f += w[i] * w[i];
		}
		f /= 2;
		for (int i = 0; i < l; i++) {
			d = z[i] - y[i];
			if (d < -p) {
				f += C[i] * (d + p) * (d + p);
			} else if (d > p) {
				f += C[i] * (d - p) * (d - p);
			}
		}

		return f;
	}

	@Override
	public void grad(double[] w, double[] g) {
		double[] y = prob.y;
		int l = prob.l;
		int w_size = get_nr_variable();

		sizeI = 0;
		for (int i = 0; i < l; i++) {
			double d = z[i] - y[i];

			// generate index set I
			if (d < -p) {
				z[sizeI] = C[i] * (d + p);
				I[sizeI] = i;
				sizeI++;
			} else if (d > p) {
				z[sizeI] = C[i] * (d - p);
				I[sizeI] = i;
				sizeI++;
			}

		}
		subXTv(z, g);

		for (int i = 0; i < w_size; i++) {
			g[i] = w[i] + 2 * g[i];
		}

	}

}
