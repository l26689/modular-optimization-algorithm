package oa.examples.pointspread;

import java.util.Random;

import oa.components.recoders.BestRecorder;
import oa.components.terminationcondition.MaxCallTerminationCondition;
import sa.basicsa.SimulatedAnnealing;
import sa.components.GeometricCoolingSchedule;
import sa.components.continuousproblem.SABasicInitializer;

/**
 * 演示：在 2D 单位正方形中放置 16 个点，最小化「最大点距 / 最小点距」的比值。
 * <p>
 * 组装 MOA 框架的模拟退火算法（网格初始化器 + 点移动扰动器 + 几何冷却 + 调用次数终止），
 * 通过 {@link BestRecorder} 输出历史最优解。同时对比随机初始化与网格热启动，
 * 多轮重启后保留全局最优。
 */
public class PointSpreadDemo {

    private static final double T0 = 1.0;          // 初始温度
    private static final int ITERATIONS = 200_000; // 单次迭代次数
    private static final int RESTARTS = 12;         // 每种初始化方式的重启次数

    public static void main(String[] args) {
        PointSpreadProblem problem = new PointSpreadProblem();
        double coolingRate = Math.pow(1e-3 / T0, 1.0 / ITERATIONS);

        double bestRatio = Double.POSITIVE_INFINITY;
        double[] bestX = null;
        String bestLabel = "";

        // 方式一：随机散布初始化
        for (int run = 0; run < RESTARTS; run++) {
            SimulatedAnnealing<double[], PointSpreadProblem> sa = new SimulatedAnnealing<>(
                    new Random(1000 + run),
                    problem,
                    new SABasicInitializer(T0),
                    new PointMovePerturbation(),
                    new GeometricCoolingSchedule(coolingRate),
                    new MaxCallTerminationCondition<double[]>(ITERATIONS));

            BestRecorder<double[]> recorder = new BestRecorder<>();
            sa.solve(recorder);
            double r = problem.evaluate(recorder.getBestX());
            if (r < bestRatio) { bestRatio = r; bestX = recorder.getBestX().clone(); bestLabel = "random-" + run; }
        }
        System.out.printf("[random init ] best ratio = %.6f%n", bestRatio);

        // 方式二：4×4 网格热启动
        for (int run = 0; run < RESTARTS; run++) {
            SimulatedAnnealing<double[], PointSpreadProblem> sa = new SimulatedAnnealing<>(
                    new Random(2000 + run),
                    problem,
                    new GridInitializer(0.04, T0),
                    new PointMovePerturbation(),
                    new GeometricCoolingSchedule(coolingRate),
                    new MaxCallTerminationCondition<double[]>(ITERATIONS));

            BestRecorder<double[]> recorder = new BestRecorder<>();
            sa.solve(recorder);
            double r = problem.evaluate(recorder.getBestX());
            if (r < bestRatio) { bestRatio = r; bestX = recorder.getBestX().clone(); bestLabel = "grid-" + run; }
        }
        System.out.printf("[grid   init ] best ratio = %.6f%n", bestRatio);

        double[] mm = problem.minMaxDist(bestX);
        System.out.println();
        System.out.println("=== 最终结果 ===");
        System.out.printf("来源: %s%n", bestLabel);
        System.out.printf("最大点距 = %.6f%n", mm[1]);
        System.out.printf("最小点距 = %.6f%n", mm[0]);
        System.out.printf("比值 (max/min) = %.6f   (4×4 网格基准: 3√2 ≈ 4.2426)%n", bestRatio);
        System.out.println("16 个点的坐标:");
        for (int i = 0; i < PointSpreadProblem.POINT_COUNT; i++) {
            System.out.printf("  P%02d = (%.6f, %.6f)%n", i, bestX[2 * i], bestX[2 * i + 1]);
        }
    }
}
