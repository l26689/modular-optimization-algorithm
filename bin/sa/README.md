# 模块化模拟退火（MSA）

[![Java](https://img.shields.io/badge/Java-17+-blue.svg)](https://www.java.com)

一个严格遵循 **最少信息原则** 的模块化模拟退火算法框架。
将算法拆解为 **初始化、扰动、冷却、终止** 四大可替换组件，像乐高一样自由组合。

## ✨ 设计思想

### 为什么还要造轮子？

现有实现要么是工业级黑盒（如 Optuna），要么是高度耦合的教学代码。
**MSA** 面向学习、实验和定制化：每一块组件都拥有清晰的接口契约，可以独立阅读、测试和替换。

### 核心原则：最少信息

组件的接口**只传递它绝对无法自行推导的信息**。

| 不传递 | 原因 |
|--------|------|
| 目标函数值 `Y` | `evaluate()` 已从 `Problem` 移除，组件应通过 `compare()` 比较解 |
| 是否改进 | 组件可通过 `Problem.compare()` 自行比较 |
| 迭代次数 | 组件内部维护计数器，通过方法调用次数推导 |

**只传递三样原子事实**：
1. `temperature` -- 只有主循环知道
2. `currentX` -- 当前解（组件无法感知外部状态）
3. `isAccepted` -- 上一次概率接受的结果（只有主循环拥有随机数）

### 架构概览

```
SimulatedAnnealing<X>
|-- SAInitializer<X, Prob>     -> initialX() + initialTemperature()
|-- SAPerturbation<X, Prob>    -> perturb(SAState)
|-- SACoolingSchedule<X, Prob> -> cool(SAState)
+-- SATerminationCondition<X, Prob> -> check(SAState)
```

主循环流程：
```
初始化 -> 迭代[扰动 -> 评估 -> Metropolis接受 -> 冷却 -> 检查终止] -> 通过Recorder输出结果
```

## 📦 SAState -- 迭代状态封装

`SAState<X>` 是模拟退火算法在单次迭代中的状态快照，封装了三个核心字段：

| 字段 | 访问方法 | 说明 |
|------|----------|------|
| `currentX` | `state.currentX()` | 当前解（只读，不可原地修改） |
| `temperature` | `state.temperature()` | 当前系统温度 |
| `isAccepted` | `state.isAccepted()` | 上一轮迭代是否接受了新解 |

**冷启动规定**：
- `perturb()` 和 `check()` 的首次调用中，`isAccepted` 为 `false`，表示"尚无历史"
- `cool()` 的首次调用发生在第一轮迭代**之后**，此时 `isAccepted` 已是 Metropolis 准则的**真实结果**，不是默认 `false`

组件在接收到 `isAccepted = false` 时，应将其视为冷启动信号，采用默认保守策略。

## 📁 模块结构

```
src/sa/
|-- core/                          # 核心框架
|   |-- SimulatedAnnealing         # 主循环控制器
|   |-- SAState                    # SA 迭代状态封装
|   |-- SAInitializer              # 初始化器抽象类
|   |-- SAPerturbation             # 扰动器抽象类
|   |-- SACoolingSchedule          # 冷却策略抽象类
|   +-- SATerminationCondition     # 终止条件抽象类
|-- components/
|   +-- basiccomponents/           # 内置基础实现
|       |-- SABasicInitializer
|       |-- SABasicPerturbation
|       |-- SABasicCoolingSchedule
|       +-- SABasicTerminationCondition
|-- AGENTS.md                      # AI Agent 使用指南
+-- README.md                      # 本文档
```

| 模块 | 说明 |
|------|------|
| `core/` | 框架核心：主循环 + 四大组件接口契约 + 状态封装 |
| `components/basiccomponents/` | 开箱即用的基础实现，可直接使用或作为自定义参考 |

## 🚀 快速开始

```java
import oa.examples.continuousproblem.myproblem.MyProblem;
import oa.components.Recoders.BestRecorder;
import sa.core.SimulatedAnnealing;
import sa.components.basiccomponents.*;

// 1. 定义问题
MyProblem problem = new MyProblem(2);

// 2. 组装组件
SimulatedAnnealing<double[]> sa =
    new SimulatedAnnealing<>(
        problem,
        new SABasicInitializer(100),
        new SABasicPerturbation(),
        new SABasicCoolingSchedule(0.99, 100),
        new SABasicTerminationCondition(10000)
    );

// 3. 创建记录器并启动算法
BestRecorder<double[]> recorder = new BestRecorder<>(problem);
sa.solve(recorder);

// 4. 获取结果
System.out.println(problem.evaluate(recorder.getBestX()));
```

## 🧩 四大组件详解

### 1. SAInitializer -- 初始化器

负责生成搜索的起始解和初始温度。

```java
public abstract class SAInitializer<X, Prob extends Problem<X>> {
    protected abstract void init(Prob problem, Random random);
    protected abstract X initialX();
    protected abstract double initialTemperature();
}
```

**核心方法**：
- `init(problem, random)` -- 绑定问题实例，获取维度、边界等元数据
- `initialX()` -- 生成起始解（必须是独立新对象）
- `initialTemperature()` -- 计算起始温度（应足够高以保证早期探索能力）

### 2. SAPerturbation -- 扰动器

定义如何从当前解生成邻域候选解。

```java
public abstract class SAPerturbation<X, Prob extends Problem<X>> {
    protected abstract void init(Prob problem, Random random);
    protected abstract X perturb(SAState<X> state);
}
```

**核心方法**：
- `perturb(SAState<X> state)` -- 生成候选解
  - `state.currentX()` -- 当前解（只读，不可原地修改）
  - `state.temperature()` -- 当前温度（可用于控制扰动幅度）
  - `state.isAccepted()` -- 上一轮接受结果（首次为 `false`，详见 [SAState](#sastate----迭代状态封装)）

**要求**：必须返回全新对象，不得原地修改 `currentX`。

### 3. SACoolingSchedule -- 冷却策略

定义温度如何随迭代逐步降低。

```java
public abstract class SACoolingSchedule<X, Prob extends Problem<X>> {
    protected abstract void init(Prob problem, Random random);
    protected abstract double cool(SAState<X> state);
}
```

**核心方法**：
- `cool(SAState<X> state)` -- 计算下一轮温度
  - 每次迭代调用一次，调用次数等于算法总迭代次数
  - 经典几何冷却：`temperature *= coolingRate`（`coolingRate` 略小于 1）
  - 注意：`cool()` 的首次调用发生在第一轮迭代**之后**，此时 `isAccepted` 已是真实结果（详见 [SAState](#sastate----迭代状态封装)）

### 4. SATerminationCondition -- 终止条件

决定算法何时停止迭代。

```java
public abstract class SATerminationCondition<X, Prob extends Problem<X>> {
    protected abstract void init(Prob problem, Random random);
    protected abstract boolean check(SAState<X> state);
}
```

**核心方法**：
- `check(SAState<X> state)` -- 判断是否终止
  - 返回 `true` 表示满足终止条件，算法停止
  - 常见实现：最大迭代次数、温度低于阈值、连续未接受等
  - 首次调用时 `isAccepted` 为 `false`（详见 [SAState](#sastate----迭代状态封装)）

## 🧩 自定义组件

所有组件只需继承对应的抽象类并实现核心方法。以下是一个线性冷却策略示例：

```java
public class LinearCoolingSchedule extends SACoolingSchedule<double[], ContinuousProblem> {
    private double coolingRate;
    private int currentIteration;
    private int maxIterations;

    public LinearCoolingSchedule(double coolingRate, int maxIterations) {
        this.coolingRate = coolingRate;
        this.maxIterations = maxIterations;
        this.currentIteration = 0;
    }

    @Override
    protected void init(ContinuousProblem problem, Random random) {
        // 此实现无需额外操作
    }

    @Override
    protected double cool(SAState<double[]> state) {
        double temperature = state.temperature();
        currentIteration++;
        if (currentIteration > maxIterations) {
            currentIteration = 0;
            return temperature * coolingRate;
        }
        return temperature;
    }
}
```

## 🎯 自定义问题

要让 MSA 优化你的问题，只需创建一个类实现 `Problem<X>` 接口：

```java
public class MyProblem extends ContinuousProblem {

    public MyProblem(int dimension) {
        super(createBounds(dimension, -100), createBounds(dimension, 100));
    }

    @Override
    public Double evaluate(double[] x) {
        double sum = 0.0;
        for (double v : x) {
            sum += v * v;
        }
        return sum;
    }

    @Override
    public double[] copyX(double[] x) {
        return x.clone();
    }

    private static double[] createBounds(int dim, double value) {
        double[] bounds = new double[dim];
        java.util.Arrays.fill(bounds, value);
        return bounds;
    }
}
```

## ⚠️ 设计约束

**冷启动**：详见 [SAState -- 迭代状态封装](#sastate----迭代状态封装)。

**线程安全**：框架默认单线程运行，所有组件内部可变状态需自行同步。

**不可变性**：传入组件的 `currentX` 解不应被原地修改；扰动方法必须返回新对象。

**纯函数**：`compare()` 内部调用的评估逻辑必须是纯函数（相同输入 -> 相同输出）。若实现了 `Evaluable`，`evaluate()` 也必须是纯函数且每次返回独立新对象。

**随机数复用**：所有组件共享主算法注入的同一 `Random` 实例，不应自行创建独立的随机数生成器。

**通配符设计**：构造函数接受 `SA*<X, ? super Prob>`，遵循 PECS 原则，使组件可跨问题类型复用。

## 📋 核心方法语义

### Problem.compare(X x1, X x2)

比较两个解的目标值的优劣，返回带符号的差值以指示优劣程度。本方法内部可能会调用 `evaluate()` 获取目标值后进行对比：

| 返回值 | 含义 |
|--------|------|
| `> 0`（正值） | `evaluate(x1)` 优于 `evaluate(x2)` |
| `< 0`（负值） | `evaluate(x1)` 劣于 `evaluate(x2)` |
| `= 0` | 两者等优 **或** 无法比较（无支配关系） |

- 返回值的**绝对值**表示优劣差距的大小，绝对值越大差距越显著
- 此方法**不保证**反对称性（即 `compare(A, B) != -compare(B, A)` 可能成立）
- 示例：最小化问题中 `compare(x1, x2) = evaluate(x2) - evaluate(x1)`，当 `evaluate(x1) < evaluate(x2)` 时返回正值

### SATerminationCondition.check(SAState state)

判断算法是否应当终止：

| 返回值 | 含义 |
|--------|------|
| `true` | 满足终止条件，算法将停止迭代 |
| `false` | 继续迭代 |

- 首次调用时 `state.isAccepted()` 为 `false`（冷启动），不应据此决定是否终止
- 实现可通过内部计数器统计调用次数来推导迭代次数

## 🔮 未来演进

- 离散问题适配示例（TSP、背包）
- 提供缓存功能的快速上手示例
- 更多内置组件实现（自适应扰动、模拟退火+局部搜索混合等）

## 📄 许可

本项目采用 [MIT License](../../LICENSE) 许可证。

欢迎 Issue 和 PR。如果你也喜欢"让代码自己说话"的风格，这个项目就是为你准备的。