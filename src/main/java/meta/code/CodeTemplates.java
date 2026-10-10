package meta.code;

import java.util.List;

/**
 * 提供给用户的满意度函数代码模板。
 *
 * <h3>为什么是「完整的类」而不是代码片段</h3>
 * 完整类允许用户添加辅助方法与字段（例如把身高数组写成静态字段），
 * 这是片段式输入做不到的。代价是必须约定类名，见
 * {@link UserCodeCompiler#CLASS_NAME}。
 *
 * <h3>模板即教程</h3>
 * 每个模板的代码里都带中文注释，点开就能看到正确写法，
 * 比另写一份文档更贴近使用场景。
 */
public final class CodeTemplates {

    /**
     * 一个可快捷选择的代码模板。
     *
     * @param id                  稳定标识
     * @param name                显示名
     * @param description         一句话说明（含注意事项）
     * @param suggestedDimension  选此模板时预填的数据维度
     * @param defaultLower        预填的下界
     * @param defaultUpper        预填的上界
     * @param code                完整 Java 源码
     */
    public record Template(
            String id,
            String name,
            String description,
            int suggestedDimension,
            double defaultLower,
            double defaultUpper,
            String code) {
    }

    private static final List<Template> ALL = buildAll();

    /**
     * 单独用一个方法构建列表——不能写成字段初始化器直接引用下方的模板常量，
     * Java 禁止在字段初始化器里用简单名前向引用同类的静态字段（JLS 8.3.3）。
     */
    private static List<Template> buildAll() {
        return List.of(
            new Template("SPHERE", "平方和 Sphere",
                    "f(x)=Σxi²，最小 0。最短的入门模板，先跑通流程用它",
                    2, -100.0, 100.0, SPHERE),

            new Template("ROSENBROCK", "Rosenbrock 香蕉函数",
                    "f(x)=Σ[100(x[i+1]-xi²)²+(1-xi)²]，演示如何访问相邻维度",
                    2, -2.048, 2.048, ROSENBROCK),

            new Template("ACKLEY", "Ackley",
                    "多峰函数，全局最小 0，演示 Math 的指数与三角函数",
                    2, -32.768, 32.768, ACKLEY),

            new Template("RASTRIGIN", "Rastrigin",
                    "多峰函数，局部最优极密集，最容易早熟",
                    2, -5.12, 5.12, RASTRIGIN),

            new Template("POINT_SPREAD", "点分布（每 2 维 1 个点）",
                    "演示双重循环与聚合。注意 32 维很慢，调试时先把维度改小",
                    32, 0.0, 1.0, POINT_SPREAD),

            new Template("STUDENT_PERMUTATION", "排列学生",
                    "演示排列问题的连续编码法：排序取秩得到合法排列。维度 = 学生人数",
                    16, 0.0, 1.0, STUDENT_PERMUTATION));
    }

    private CodeTemplates() {
    }

    /** 全部模板，顺序即界面上的展示顺序。 */
    public static List<Template> all() {
        return ALL;
    }

    // ------------------------------------------------------------------
    // 以下为模板源码。类名必须是 UserObjective，且不得写 package 声明。
    // ------------------------------------------------------------------

    private static final String SPHERE = """
            import meta.ObjectiveFunction;

            /**
             * 平方和 Sphere：f(x) = Σxi²，最小值 0（在原点）。
             * 最简单的凸函数，适合先跑通流程。
             */
            public class UserObjective implements ObjectiveFunction {
                @Override
                public double evaluate(double[] x) {
                    double sum = 0.0;
                    for (double v : x) {
                        sum += v * v;
                    }
                    // 约定：返回值越大越满意。这里要最小化 sum，所以取负。
                    return -sum;
                }
            }
            """;

    private static final String ROSENBROCK = """
            import meta.ObjectiveFunction;

            /**
             * Rosenbrock 香蕉函数：f(x) = Σ[100(x[i+1]-xi²)² + (1-xi)²]，
             * 最小值 0（在全部为 1 的点）。狭长弯曲的山谷，考验收敛能力。
             */
            public class UserObjective implements ObjectiveFunction {
                @Override
                public double evaluate(double[] x) {
                    double sum = 0.0;
                    for (int i = 0; i < x.length - 1; i++) {
                        double a = x[i + 1] - x[i] * x[i];
                        double b = 1.0 - x[i];
                        sum += 100.0 * a * a + b * b;
                    }
                    return -sum;
                }
            }
            """;

    private static final String ACKLEY = """
            import meta.ObjectiveFunction;

            /**
             * Ackley：多峰的经典测试函数，全局最小 0（在原点），
             * 中央漏斗之外布满局部最优。
             */
            public class UserObjective implements ObjectiveFunction {
                @Override
                public double evaluate(double[] x) {
                    int n = x.length;
                    double sumSq = 0.0;
                    double sumCos = 0.0;
                    for (double v : x) {
                        sumSq += v * v;
                        sumCos += Math.cos(2.0 * Math.PI * v);
                    }
                    double f = -20.0 * Math.exp(-0.2 * Math.sqrt(sumSq / n))
                            - Math.exp(sumCos / n)
                            + 20.0 + Math.E;
                    return -f;
                }
            }
            """;

    private static final String RASTRIGIN = """
            import meta.ObjectiveFunction;

            /**
             * Rastrigin：f(x) = 10n + Σ(xi² - 10cos(2πxi))，全局最小 0（在原点）。
             * 局部最优极其密集，是最容易陷入早熟的测试函数之一。
             */
            public class UserObjective implements ObjectiveFunction {
                @Override
                public double evaluate(double[] x) {
                    int n = x.length;
                    double sum = 10.0 * n;
                    for (double v : x) {
                        sum += v * v - 10.0 * Math.cos(2.0 * Math.PI * v);
                    }
                    return -sum;
                }
            }
            """;

    private static final String POINT_SPREAD = """
            import meta.ObjectiveFunction;

            /**
             * 点分布：把解每 2 维看作一个平面点，让这些点尽量互相散开。
             * 目标 f = 最大点距 / 最小点距，越小越均匀（4×4 网格基准约 4.2426）。
             *
             * 这个模板演示「完整类」的优势：双重循环与聚合逻辑，
             * 是单行表达式写不出来的。
             *
             * 注意：维度必须是偶数（每 2 维构成 1 个点）。
             */
            public class UserObjective implements ObjectiveFunction {
                @Override
                public double evaluate(double[] x) {
                    if (x.length % 2 != 0) {
                        throw new IllegalArgumentException("维度必须为偶数，当前为 " + x.length);
                    }
                    int n = x.length / 2;
                    double min = Double.POSITIVE_INFINITY;
                    double max = 0.0;
                    for (int i = 0; i < n; i++) {
                        for (int j = i + 1; j < n; j++) {
                            double dx = x[2 * i] - x[2 * j];
                            double dy = x[2 * i + 1] - x[2 * j + 1];
                            double d = Math.sqrt(dx * dx + dy * dy);
                            if (d < min) {
                                min = d;
                            }
                            if (d > max) {
                                max = d;
                            }
                        }
                    }
                    // 两点重合时 min 趋近 0，除法会得到 Infinity。
                    // 契约要求必须返回有限值，所以这里给一个有限的大惩罚。
                    if (min < 1e-12) {
                        return -1e12;
                    }
                    return -(max / min);
                }
            }
            """;

    private static final String STUDENT_PERMUTATION = """
            import java.util.Arrays;
            import java.util.Comparator;

            import meta.ObjectiveFunction;

            /**
             * 排列学生：把一排学生按身高排好。
             *
             * 关键手法 —— 排列问题的连续编码：
             * 参数 x 是连续向量（长度 = 学生人数），代码先对它的下标按 x 的值排序，
             * 得到的「秩」就是一个合法排列，且天然满足「每个学号恰好出现一次」。
             * 这样连续型优化算法就能求解排列问题，不必自己处理重复与遗漏。
             *
             * 满意度：每个学生若被更高的同学挡在身前就不满意，
             * 罚分随「比他高的人数」与「高出的总幅度」增长。
             *
             * describe() 负责把最优解翻译成「第几位站几号学生」——
             * 解码规则只写在这里，框架并不知道那串小数代表一个排列。
             */
            public class UserObjective implements ObjectiveFunction {

                /** 各学生身高（下标 = 学号）。真实场景可改为从文件读入。 */
                private static final double[] HEIGHTS = {
                    165, 178, 152, 190, 171, 183, 158, 176,
                    168, 185, 160, 173, 156, 181, 169, 188,
                };

                @Override
                public double evaluate(double[] x) {
                    if (x.length != HEIGHTS.length) {
                        throw new IllegalArgumentException(
                                "维度必须等于学生人数 " + HEIGHTS.length + "，当前为 " + x.length);
                    }

                    int[] order = permutationOf(x);

                    // 统计罚分：order[pos] 是站在第 pos 位的学生
                    double penalty = 0.0;
                    for (int pos = 0; pos < order.length; pos++) {
                        double h = HEIGHTS[order[pos]];
                        for (int front = 0; front < pos; front++) {
                            double hf = HEIGHTS[order[front]];
                            if (hf > h) {
                                penalty += 1.0 + (hf - h);   // 人数 + 超过程度
                            }
                        }
                    }
                    // 罚分越小越满意，故取负
                    return -penalty;
                }

                /**
                 * 把连续解解码成排列：order[pos] = 站在第 pos 位的学生编号。
                 * 做法是按解的各分量对下标排序取「秩」——秩天然是 0..n-1 的一个排列。
                 */
                private static int[] permutationOf(double[] x) {
                    int n = x.length;
                    Integer[] idx = new Integer[n];
                    for (int i = 0; i < n; i++) {
                        idx[i] = i;
                    }
                    Arrays.sort(idx, Comparator.comparingDouble(i -> x[i]));

                    int[] order = new int[n];
                    for (int i = 0; i < n; i++) {
                        order[i] = idx[i];
                    }
                    return order;
                }

                /** 结果展示：把最优解翻译成「第几位 → 几号学生」。 */
                @Override
                public String describe(double[] x) {
                    int[] order = permutationOf(x);
                    StringBuilder sb = new StringBuilder("站位（第N位→N号学生）：");
                    for (int pos = 0; pos < order.length; pos++) {
                        if (pos > 0) {
                            sb.append(' ');
                        }
                        sb.append(pos + 1).append("→").append(order[pos]);
                    }
                    return sb.toString();
                }
            }
            """;
}
