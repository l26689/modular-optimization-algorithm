package de;

import java.util.Random;

import oa.components.initializers.RandomInitializer;
import oa.components.problems.coninuousproblem.ContinuousProblem;
import pso.core.PSOState;
import pso.core.Particle;

public final class DERand2Particle extends RandomInitializer<PSOState<double[]>> implements Particle<double[],ContinuousProblem,PSOState<double[]>> {
    private final double F;
    private final double CR;
    private double[] currentPosition;
    private ContinuousProblem problem;
    private int myIndex;

    public DERand2Particle(double F, double CR,int myIndex) {
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

        int r1, r2, r3, r4, r5;
        r1 = random.nextInt(popSize - 1);
        if (r1 >= myIndex) r1++;
        r2 = random.nextInt(popSize - 2);
        if (r2 >= Math.min(myIndex, r1)) r2++;
        if (r2 >= Math.max(myIndex, r1)) r2++;
        r3 = random.nextInt(popSize - 3);
        if (r3 >= Math.min(Math.min(myIndex, r1), r2)) r3++;
        if (r3 >= Math.max(Math.max(myIndex, r1), r2) - 1) r3++;
        if (r3 >= Math.max(Math.max(myIndex, r1), r2)) r3++;
        r4 = random.nextInt(popSize - 4);
        int[] sorted = {myIndex, r1, r2, r3};
        java.util.Arrays.sort(sorted);
        for (int k = 3; k >= 0; k--) {
            if (r4 >= sorted[k]) r4++;
        }
        r5 = random.nextInt(popSize - 5);
        int[] sorted5 = {myIndex, r1, r2, r3, r4};
        java.util.Arrays.sort(sorted5);
        for (int k = 4; k >= 0; k--) {
            if (r5 >= sorted5[k]) r5++;
        }

        double[] xr1 = currentXs[r1];
        double[] xr2 = currentXs[r2];
        double[] xr3 = currentXs[r3];
        double[] xr4 = currentXs[r4];
        double[] xr5 = currentXs[r5];

        int dim = xr1.length;
        double[] v = new double[dim];
        for (int i = 0; i < dim; i++) {
            v[i] = xr5[i] + F * (xr1[i] - xr2[i]) + F * (xr3[i] - xr4[i]);
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