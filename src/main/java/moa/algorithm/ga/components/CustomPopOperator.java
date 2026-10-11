package moa.algorithm.ga.components;

import java.util.Random;

import moa.algorithm.ga.api.GAState;
import moa.algorithm.ga.api.GeneType;
import moa.algorithm.ga.api.PopOperator;
import moa.framework.problem.Problem;
import moa.algorithm.ga.api.*;

/**
 * GA 自定义种群算子 —— 组合 Selection、Crossover、Mutation、Replacement。
 *
 * <p>实现 TotalPopOperator<X,Prob,S,Gene,G>。是否为最终类：是。
 *
 * <p>四个算子按序执行，init() 内部委托给各子算子。
 *
 * <p><b>提示</b>
 * <ul>
 *   <li>组装说明 —— 用于 BasicGA 的 TotalPopOperator 插槽。</li>
 * </ul>
 *
 * <p><b>参见文档 2</b>
 */
public final class CustomPopOperator<X,Prob extends Problem<X>,S extends GAState<X>,Gene,G extends GeneType<X,Gene,Prob>> 
implements PopOperator<X,Prob,S,Gene,G> {
    Selection<X,? super Prob,? super S,? super Gene,G> selection;
    Crossover<X,? super Prob,? super S,? super Gene,G> crossover;
    Mutation<X,? super Prob,? super S,? super Gene,G> mutation;
    Replacement<X,? super Prob,? super S,? super Gene,G> replacement;

    public CustomPopOperator(Selection<X,? super Prob,? super S,? super Gene,G> selection,
                              Crossover<X,? super Prob,? super S,? super Gene,G> crossover,
                              Mutation<X,? super Prob,? super S,? super Gene,G> mutation,
                              Replacement<X,? super Prob,? super S,? super Gene,G> replacement) {
        this.selection = selection;
        this.crossover = crossover;
        this.mutation = mutation;
        this.replacement = replacement;
    }
    @Override
    public void init(Prob prob, Random random) {
        selection.init(prob, random);
        crossover.init(prob, random);
        mutation.init(prob, random);
        replacement.init(prob, random);
    }

    @Override
    public void operate(S state, X[] newPopulation) {
        selection.operate(state, newPopulation);
        crossover.operate(state, newPopulation);
        mutation.operate(state, newPopulation);
        replacement.operate(state, newPopulation);
    }
    
}
