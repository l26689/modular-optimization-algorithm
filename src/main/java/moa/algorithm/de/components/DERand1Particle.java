package moa.algorithm.de.components;

import java.util.Random;

import moa.problem.continuous.ContinuousProblem;
import moa.algorithm.pso.api.PSOState;
import moa.algorithm.pso.api.Particle;
import moa.problem.continuous.components.RandomInitializer;

/**
 * DE/rand/1 变异粒子 —— 最经典的差分进化变异策略。
 *
 * <p>继承 RandomInitializer，实现 Particle<X:double[], Prob:ContinuousProblem, S:PSOState<double[]>>。是否为最终类：是。
 *
 * <p>变异公式：v = x_r1 + F * (x_r2 - x_r3)。所有基向量均随机选取。
 *
 * <p><b>提示</b>
 * <ul>
 *   <li>约束绑定 —— X 固定为 double[]，Prob 固定为 ContinuousProblem。</li>
 * </ul>
 *
 * <p><b>参见文档 2</b>
 */
public final class DERand1Particle extends RandomInitializer<PSOState<double[]>> implements Particle<double[],ContinuousProblem,PSOState<double[]>> {
    private final double F;
    private final double CR;
    private double[] currentPosition;
    private ContinuousProblem problem;
    private int myIndex;

    public DERand1Particle(double F, double CR,int myIndex) {
        this.F = F;
        this.CR = CR;
        this.myIndex = myIndex;
    }
    
    @Override
    public void init(ContinuousProblem problem,Random random) {
        this.problem = problem;
        super.init(problem,random);
    }

    @Override
    public double[] initialX() {
        currentPosition = super.initialX();
        return currentPosition;
    }


    @Override
    public double[] search(PSOState<double[]> state) {
        double[][] currentXs = state.getCurrentXs();
        int popSize = currentXs.length;

        int r1, r2, r3;
        r1 = random.nextInt(popSize - 1);
        if (r1 >= myIndex) r1++;
        r2 = random.nextInt(popSize - 2);
        if (r2 >= Math.min(myIndex, r1)) r2++;
        if (r2 >= Math.max(myIndex, r1)) r2++;
        r3 = random.nextInt(popSize - 3);
        if (r3 >= Math.min(Math.min(myIndex, r1), r2)) r3++;
        if (r3 >= Math.max(Math.max(myIndex, r1), r2) - 1) r3++;
        if (r3 >= Math.max(Math.max(myIndex, r1), r2)) r3++;

        double[] xr1 = currentXs[r1];
        double[] xr2 = currentXs[r2];
        double[] xr3 = currentXs[r3];

        int dim = xr1.length;
        double[] v = new double[dim];
        for (int i = 0; i < dim; i++) {
            v[i] = xr3[i] + F * (xr1[i] - xr2[i]);
            if (v[i] < lowerBounds[i]) v[i] = lowerBounds[i];
            if (v[i] > upperBounds[i]) v[i] = upperBounds[i];
        }

        double[] trial = problem.copyX(currentPosition);
        int j = random.nextInt(dim);
        for (int i = 0; i < dim; i++) {
            if (random.nextDouble() < CR || i == j) {
                trial[i] = v[i];
            }
        }

        if (problem.compare(trial, currentPosition) > 0) {
            currentPosition = trial;
        }
        return problem.copyX(currentPosition);
    }
    
}