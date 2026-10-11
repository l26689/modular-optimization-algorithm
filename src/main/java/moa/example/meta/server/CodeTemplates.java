package moa.example.meta.server;

import java.util.List;

/**
 * 用户粘贴代码的<b>示例模板</b>。每个模板是一段完整的 {@code UserObjective} 类，
 * 供用户在代码框里快捷填充、再自行修改。
 * <p>
 * 模板只作为起点，不改动后端逻辑。加/改模板就在 {@link #ALL} 里加一项。
 *
 * @param id          稳定标识（前端用来引用）
 * @param name        展示名
 * @param description 一句话说明它是什么问题
 * @param code        完整 Java 源码（类名 UserObjective、无 package、实现 ObjectiveFunction）
 */
public final class CodeTemplates {

    private CodeTemplates() {
    }

    public record Template(String id, String name, String description, String code) {
    }

    /** 全部模板列表。 */
    public static final List<Template> ALL = List.of(
            sphere(),
            rastrigin(),
            permutation()
    );

    private static Template sphere() {
        return new Template(
                "SPHERE",
                "Sphere（平方和）",
                "各分量平方求和，全局最优在原点，最简单连续问题",
                """
                import moa.example.meta.server.ObjectiveFunction;

                public class UserObjective implements ObjectiveFunction {
                    @Override
                    public double evaluate(double[] x) {
                        double sum = 0.0;
                        for (double v : x) {
                            sum += v * v;
                        }
                        return sum;
                    }
                }
                """);
    }

    private static Template rastrigin() {
        return new Template(
                "RASTRIGIN",
                "Rastrigin（多峰）",
                "带余弦调制的多峰函数，全局最优在原点，适合看算法逃出局部最优的能力",
                """
                import moa.example.meta.server.ObjectiveFunction;

                public class UserObjective implements ObjectiveFunction {
                    @Override
                    public double evaluate(double[] x) {
                        double sum = 10.0 * x.length;
                        for (int i = 0; i < x.length; i++) {
                            sum += x[i] * x[i] - 10.0 * Math.cos(2.0 * Math.PI * x[i]);
                        }
                        return sum;
                    }
                }
                """);
    }

    private static Template permutation() {
        return new Template(
                "PERMUTATION",
                "排列（random-key）",
                "把连续解排序取秩得到一个排列，用相邻元素编号差平方和当成本；演示 describe 的重写",
                """
                import moa.example.meta.server.ObjectiveFunction;
                import java.util.Arrays;

                public class UserObjective implements ObjectiveFunction {
                    @Override
                    public double evaluate(double[] x) {
                        // random-key：连续小数 -> 排序取秩 -> 一个排列
                        Integer[] order = new Integer[x.length];
                        for (int i = 0; i < x.length; i++) order[i] = i;
                        Arrays.sort(order, (a, b) -> Double.compare(x[a], x[b]));
                        double cost = 0.0;
                        for (int i = 0; i < order.length - 1; i++) {
                            double d = order[i + 1] - order[i];
                            cost += d * d;
                        }
                        return cost;
                    }

                    @Override
                    public String describe(double[] x) {
                        Integer[] order = new Integer[x.length];
                        for (int i = 0; i < x.length; i++) order[i] = i;
                        Arrays.sort(order, (a, b) -> Double.compare(x[a], x[b]));
                        return "排列: " + Arrays.toString(order);
                    }
                }
                """);
    }
}
