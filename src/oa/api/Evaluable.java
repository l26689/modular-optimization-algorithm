package oa.api;
/**
 * 可评估的接口，用于评估解的目标函数值。
 * @param <Y> 目标函数值的类型
 */
public interface Evaluable<X,Y> {
    /**
     * 评估解的目标函数值必需为纯函数，即相同的输入永远返回相同的输出且每次返回的都是新的对象。
     * @return 目标函数值，类型为 {@code Y}
     */
    Y evaluate(X x);
}
