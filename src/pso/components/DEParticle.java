package pso.components;

import java.util.Random;

import oa.components.initializers.RandomInitializer;
import oa.components.problems.coninuousproblem.ContinuousProblem;
import pso.core.PSOState;
import pso.core.Particle;

public final class DEParticle extends RandomInitializer<PSOState<double[]>> implements Particle<double[],ContinuousProblem,PSOState<double[]>> {
    private final double F;
    private final double CR;
    private double[] currentPosition;
    private ContinuousProblem problem;
    private int myIndex;

    public DEParticle(double F, double CR,int myIndex) {
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
        /*
         * DE变异
         * 1. 随机选择两个粒子
         * 2. 计算差向量并乘上F
         * 3. 计算新位置（独立的）
         * 4. 拉回新位置到搜索空间内
         */
        //1. 随机选择两个粒子
        int randomIndices1 = random.nextInt(currentXs.length-1);
        int randomIndices2 = random.nextInt(currentXs.length-2);
        if(randomIndices1 >= myIndex) {
            randomIndices1++;
        }
        if(randomIndices2 >= Math.min(myIndex,randomIndices1)) {
            randomIndices2++;
        }
        if(randomIndices2 >= Math.max(myIndex,randomIndices1)) {
            randomIndices2++;
        }
        double[] x1 = currentXs[randomIndices1];
        double[] x2 = currentXs[randomIndices2];
        //2. 计算差向量并乘上F
        double[] diff = new double[x1.length];
        for(int i = 0;i<x1.length;i++) {
            diff[i] = x1[i] - x2[i];
            diff[i] *= F;
        }
        //3. 计算新位置（独立的）
        double[] v = new double[x1.length];
        for(int i = 0;i<x1.length;i++) {
            v[i] = currentPosition[i] + diff[i];
            //拉回新位置到搜索空间内
            if(v[i] < lowerBounds[i]) { 
                v[i] = lowerBounds[i];
            }
            if(v[i] > upperBounds[i]) {
                v[i] = upperBounds[i];
            }
        }
        
        /*
         * DE交叉
         * 每个维度有CR的概率交叉为新位置的维度，并且必定交叉一个维度
         * 
         */
        double[] newPosition = problem.copyX(currentPosition);
        int j = random.nextInt(v.length);
        for(int i = 0;i<v.length;i++) {
            if(random.nextDouble() < CR||i == j) {
                newPosition[i] = v[i];
            }
        }
        if(problem.compare(newPosition, currentPosition)>0) {
            currentPosition = newPosition;
        }
        return problem.copyX(currentPosition);
    }
    
}
