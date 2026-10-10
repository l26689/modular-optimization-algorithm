package oa.examples.pointspread;

import java.util.Random;

import oa.components.problems.coninuousproblem.ContinuousProblem;
import sa.core.SAPerturbation;
import sa.core.SAState;

/**
 * 针对「点散布」问题的 SA 扰动算子。
 * <p>
 * 每次扰动：随机挑选一个点，沿随机方向移动一小段距离。
 * 移动幅度随当前温度自适应——温度高时大步探索，温度低时小步精调。
 * 越界坐标裁剪回 [0, 1]。
 * <p>
 * 符合框架约束：返回全新对象、只使用注入的 {@link Random}、方向均匀无偏。
 */
public class PointMovePerturbation implements SAPerturbation<double[], ContinuousProblem, SAState<double[]>> {

    private ContinuousProblem problem;
    private Random random;
    private double[] lowerBounds;
    private double[] upperBounds;

    @Override
    public void init(ContinuousProblem problem, Random random) {
        this.problem = problem;
        this.random = random;
        this.lowerBounds = problem.getLowerBounds();
        this.upperBounds = problem.getUpperBounds();
    }

    @Override
    public double[] search(SAState<double[]> state) {
        double[] x = problem.copyX(state.getCurrentXs()[0]);

        int p = random.nextInt(PointSpreadProblem.POINT_COUNT);

        // 步长随温度缩放：温度 ~1 时约 0.26，温度趋近 0 时约 0.01 的精细微调
        double t = state.getTemperature();
        double mag = 0.01 + 0.25 * Math.min(1.0, t);

        double angle = random.nextDouble() * 2.0 * Math.PI;
        double dx = mag * Math.cos(angle);
        double dy = mag * Math.sin(angle);

        int ix = 2 * p;
        int iy = 2 * p + 1;
        x[ix] = clamp(x[ix] + dx, lowerBounds[ix], upperBounds[ix]);
        x[iy] = clamp(x[iy] + dy, lowerBounds[iy], upperBounds[iy]);

        return x;
    }

    private static double clamp(double v, double lo, double hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }
}
