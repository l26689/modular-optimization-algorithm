package oa.examples.deploymentproblem;

import java.util.Arrays;
import java.util.Random;

import oa.api.spi.component.SearchOperator;
import sa.core.SAState;

/**
 * 部署问题 SA 扰动算子，通过随机增/删/替换 actionId 生成邻域解。
 *
 * <p>
 * 每次调用 {@link #search(SAState)} 执行以下三种操作之一：
 * <ul>
 *   <li>增加一个随机 actionId（当前解大小小于 maxTotalCount 时）</li>
 *   <li>删除一个随机 actionId（当前解大小大于 1 时）</li>
 *   <li>替换一个随机 actionId（当前解大小在 1 到 maxTotalCount 之间时）</li>
 * </ul>
 * 操作的选择概率：增 40%、删 30%、替换 30%。
 *
 * <h3>设计约束</h3>
 * <ul>
 *   <li>返回全新创建的数组，不原地修改输入解。</li>
 *   <li>使用框架注入的 {@link Random} 实例。</li>
 *   <li>新增的 actionId 不与当前解中已有的重复。</li>
 * </ul>
 */
public class DeploymentPerturbation implements SearchOperator<int[], DeploymentProblem, SAState<int[]>> {

    private final int actionCount;
    private final int maxTotalCount;
    private Random random;

    public DeploymentPerturbation(int actionCount, int maxTotalCount) {
        this.actionCount = actionCount;
        this.maxTotalCount = maxTotalCount;
    }

    @Override
    public void init(DeploymentProblem problem, Random random) {
        this.random = random;
    }

    @Override
    public int[] search(SAState<int[]> state) {
        int[] current = state.getCurrentXs()[0];
        int size = current.length;

        if (size <= 1) {
            return addAction(current);
        }
        if (size >= maxTotalCount) {
            return removeAction(current);
        }

        double r = random.nextDouble();
        if (r < 0.4) {
            return addAction(current);
        } else if (r < 0.7) {
            return removeAction(current);
        } else {
            return replaceAction(current);
        }
    }

    private int[] addAction(int[] current) {
        int newId = generateNewId(current);
        int[] result = Arrays.copyOf(current, current.length + 1);
        result[result.length - 1] = newId;
        return result;
    }

    private int[] removeAction(int[] current) {
        int removeIndex = random.nextInt(current.length);
        int[] result = new int[current.length - 1];
        int j = 0;
        for (int i = 0; i < current.length; i++) {
            if (i != removeIndex) {
                result[j++] = current[i];
            }
        }
        return result;
    }

    private int[] replaceAction(int[] current) {
        int replaceIndex = random.nextInt(current.length);
        int newId = generateNewId(current);
        int[] result = Arrays.copyOf(current, current.length);
        result[replaceIndex] = newId;
        return result;
    }

    private int generateNewId(int[] current) {
        java.util.Set<Integer> used = new java.util.HashSet<>();
        for (int id : current) {
            used.add(id);
        }
        int newId;
        do {
            newId = 1 + random.nextInt(actionCount);
        } while (used.contains(newId));
        return newId;
    }
}