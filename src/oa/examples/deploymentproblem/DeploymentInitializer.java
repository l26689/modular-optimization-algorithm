package oa.examples.deploymentproblem;

import java.util.Random;

import sa.core.SAInitializer;

/**
 * 部署问题 SA 初始化器，生成随机初始解和初始温度。
 *
 * <p>
 * 初始解为随机选取的 actionId 子集（大小在 1 到 maxTotalCount 之间随机），
 * 每个 actionId 从 [1, actionCount] 范围内随机生成，不重复。
 * 初始温度设定为 50，足够高使得算法早期能以较大概率接受劣解。
 *
 * <h3>设计约束</h3>
 * <ul>
 *   <li>使用框架注入的 {@link Random} 实例，保证随机性可复现。</li>
 *   <li>返回的初始解是全新创建的对象，与任何内部状态无关。</li>
 * </ul>
 */
public class DeploymentInitializer implements SAInitializer<int[], DeploymentProblem> {

    private final int actionCount;
    private final int maxTotalCount;
    private final double initialTemperature;
    private Random random;

    public DeploymentInitializer(int actionCount, int maxTotalCount, double initialTemperature) {
        this.actionCount = actionCount;
        this.maxTotalCount = maxTotalCount;
        this.initialTemperature = initialTemperature;
    }

    @Override
    public void init(DeploymentProblem problem, Random random) {
        this.random = random;
    }

    @Override
    public int[] initialX() {
        int size = 1 + random.nextInt(Math.min(maxTotalCount, actionCount));
        int[] result = new int[size];
        java.util.Set<Integer> used = new java.util.HashSet<>();
        for (int i = 0; i < size; i++) {
            int id;
            do {
                id = 1 + random.nextInt(actionCount);
            } while (used.contains(id));
            used.add(id);
            result[i] = id;
        }
        return result;
    }

    @Override
    public double initialTemperature() {
        return initialTemperature;
    }
}