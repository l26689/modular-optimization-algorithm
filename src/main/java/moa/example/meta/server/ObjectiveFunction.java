package moa.example.meta.server;

/**
 * 目标函数契约：用户粘贴的 Java 代码要实现这个接口。
 * <p>
 * <b>语义约定：{@link #evaluate} 返回「成本」，越小越优。</b>
 * 这和框架的 {@code moa.problem.continuous.ContinuousProblem} 完全一致——
 * 预设问题（Sphere/Rosenbrock/…）和用户粘贴的代码走同一个语义，不需要负号转换。
 * <p>
 * 用户代码的使用约定（写死在 {@link UserCodeCompiler} 里）：
 * <ul>
 *   <li>类名必须是 {@code UserObjective}；</li>
 *   <li><b>不要写 {@code package} 声明</b>（放在默认包，编译后类名就是 UserObjective）；</li>
 *   <li>实现本接口，且 {@code evaluate} 必须是<b>纯函数</b>（相同输入相同输出，
 *       不能出现 {@code Math.random()}、读文件、读全局可变状态等）。</li>
 * </ul>
 * <p>
 * 本接口带一个 {@link #describe} 默认方法，把解翻译成人话。排列这类离散问题可以
 * 重写它（把连续解排序取秩，翻译成"第N位→N号元素"），否则默认打印各分量。
 */
public interface ObjectiveFunction {

    /**
     * 计算一个解的成本。
     *
     * @param x 一个解（长度 = 网页里填的维度）
     * @return 成本值，越小越优；必须有限，且是纯函数
     */
    double evaluate(double[] x);

    /**
     * 把一个解翻译成人话（可选重写）。
     *
     * @param x 一个解
     * @return 人类可读的描述；默认打印各分量
     */
    default String describe(double[] x) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < x.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(String.format("%.6f", x[i]));
        }
        return sb.append("]").toString();
    }
}
