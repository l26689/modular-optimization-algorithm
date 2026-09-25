package oa.examples.deploymentproblem;

import java.util.Random;

import oa.components.recoders.BestRecorder;
import oa.components.terminationcondition.MaxCallTerminationCondition;
import sa.BasicSA.SimulatedAnnealing;
import sa.components.SAAdaptiveCoolingSchedule;

/**
 * 部署问题优化器主入口，通过命令行参数接收场景信息，运行 SA 优化并输出结果。
 *
 * <p>
 * 命令行参数：
 * <pre>
 * java -cp bin oa.examples.deploymentproblem.DeploymentOptimizer \
 *     --sessionId=abc123 \
 *     --actionCount=930 \
 *     --maxTotal=12 \
 *     --objective=balanced \
 *     --serverUrl=http://localhost:3000
 * </pre>
 *
 * <h3>输出格式</h3>
 * 向 stdout 输出最优解的 actionId 数组（JSON 格式），例如：
 * <pre>
 * [3, 17, 42, 88]
 * </pre>
 * JS 端读取此输出后，使用缓存的场景数据生成可视化结果。
 */
public class DeploymentOptimizer {
    private static final long DEFAULT_SEED = 42L;

    public static void main(String[] args) {
        try {
            Params params = parseArgs(args);
            runOptimization(params);
        } catch (Exception e) {
            System.err.println("优化失败: " + e.getMessage());
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }

    private static void runOptimization(Params params) {
        DeploymentUtil util = new DeploymentUtil(params.serverUrl);
        DeploymentProblem problem = new DeploymentProblem(
            params.sessionId, params.objective, params.maxTotal, util);

        SimulatedAnnealing<int[], DeploymentProblem> sa = new SimulatedAnnealing<>(
                new Random(params.seed),
                problem,
                new DeploymentInitializer(
                params.actionCount, params.maxTotal, 50.0),
                new DeploymentPerturbation(
                params.actionCount, params.maxTotal), 
                new SAAdaptiveCoolingSchedule<int[]>(0.44,0.99,0.8,1.4,100, 100), 
                new MaxCallTerminationCondition<>(10000));

        BestRecorder<int[]> recorder = new BestRecorder<>();

        // 运行 SA 优化并计时
        long startTime = System.currentTimeMillis();
        sa.solve(recorder);
        long elapsedMs = System.currentTimeMillis() - startTime;

        int[] bestActionIds = recorder.getBestX();
        double[] bestObjective = problem.evaluate(bestActionIds);

        // 输出 SA 优化结果
        System.err.printf("SA 优化完成: 耗时 %dms, 最优解 = %d 个设备, [detection=%.1f%%, attack=%.1f%%]%n",
                elapsedMs, bestActionIds.length, bestObjective[0], bestObjective[1]);

        outputActionIds(bestActionIds);
    }

    private static void outputActionIds(int[] actionIds) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < actionIds.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(actionIds[i]);
        }
        sb.append("]");
        System.out.println(sb.toString());
    }

    private static Params parseArgs(String[] args) {
        Params params = new Params();
        for (String arg : args) {
            if (arg.startsWith("--sessionId=")) {
                params.sessionId = arg.substring("--sessionId=".length());
            } else if (arg.startsWith("--actionCount=")) {
                params.actionCount = Integer.parseInt(arg.substring("--actionCount=".length()));
            } else if (arg.startsWith("--maxTotal=")) {
                params.maxTotal = Integer.parseInt(arg.substring("--maxTotal=".length()));
            } else if (arg.startsWith("--objective=")) {
                params.objective = arg.substring("--objective=".length());
            } else if (arg.startsWith("--serverUrl=")) {
                params.serverUrl = arg.substring("--serverUrl=".length());
            } else if (arg.startsWith("--seed=")) {
                params.seed = Long.parseLong(arg.substring("--seed=".length()));
            }
        }
        validate(params);
        return params;
    }

    private static void validate(Params params) {
        if (params.sessionId == null || params.sessionId.isEmpty()) {
            throw new IllegalArgumentException("缺少 --sessionId 参数");
        }
        if (params.actionCount <= 0) {
            throw new IllegalArgumentException("--actionCount 必须为正整数");
        }
        if (params.maxTotal <= 0) {
            throw new IllegalArgumentException("--maxTotal 必须为正整数");
        }
        if (params.serverUrl == null || params.serverUrl.isEmpty()) {
            throw new IllegalArgumentException("缺少 --serverUrl 参数");
        }
    }

    private static class Params {
        String sessionId;
        int actionCount;
        int maxTotal = 12;
        String objective = "balanced";
        String serverUrl = "http://localhost:3000";
        long seed = DEFAULT_SEED;
    }
}