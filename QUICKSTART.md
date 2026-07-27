# 快速开始

本文档帮助你在 5 分钟内运行第一个模拟退火优化示例。

## 📋 前置条件

- Java 17 或更高版本
- 已克隆本仓库并进入项目根目录

## 🔧 1. 编译项目

```bash
# 在项目根目录下执行
javac -d bin src/oa/api/*.java src/oa/components/Recoders/*.java src/oa/examples/continuousproblem/*.java src/oa/examples/continuousproblem/myproblem/*.java src/oa/examples/continuousproblem/rosenbrock/*.java src/sa/core/*.java src/sa/components/basiccomponents/*.java
```

## ▶️ 2. 运行示例

### 示例一：简单平方和问题

```bash
java -cp bin oa.examples.continuousproblem.myproblem.MyProblemDemo
```

该示例在 2 维空间中最小化 `f(x) = x₁² + x₂²`，最优解为 `[0, 0]`，最优值为 `0.0`。

### 示例二：Rosenbrock 函数

```bash
java -cp bin oa.examples.continuousproblem.rosenbrock.RosenbrockDemo
```

该示例优化经典的 Rosenbrock 函数，最优解为 `[1, 1]`，最优值为 `0.0`。

## ✍️ 3. 编写自己的优化程序

### 步骤一：定义问题

创建一个类实现 `Problem<X>` 接口（只需 `copyX()` 和 `compare()`）。如需向用户展示目标函数值，额外实现 `Evaluable<X, Y>`。为方便起见，连续优化问题可直接继承 `ContinuousProblem`（已同时实现 `Problem` 和 `Evaluable`）：

```java
package mypackage;

import oa.examples.continuousproblem.ContinuousProblem;

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
}
```

> **注**：`createBounds()` 和 `copyX()` 已在 `ContinuousProblem` 中定义，子类可直接使用。非连续问题可直接实现 `Problem<X>` 接口，无需继承 `ContinuousProblem`。

### 步骤二：组装算法并运行

```java
package mypackage;

import oa.components.Recoders.BestRecorder;
import sa.core.SimulatedAnnealing;
import sa.components.basiccomponents.*;

public class MyDemo {
    public static void main(String[] args) {
        // 1. 定义问题
        MyProblem problem = new MyProblem(2);

        // 2. 组装 SA 算法
        SimulatedAnnealing<double[]> sa =
            new SimulatedAnnealing<>(
                problem,
                new SABasicInitializer(100),
                new SABasicPerturbation(),
                new SABasicCoolingSchedule(0.99, 100),
                new SABasicTerminationCondition(10000)
            );

        // 3. 创建记录器并启动优化
        BestRecorder<double[]> recorder = new BestRecorder<>(problem);
        sa.solve(recorder);

        // 4. 输出结果
        System.out.println("最优解目标值: " + problem.evaluate(recorder.getBestX()));
        System.out.println("最优解: " + java.util.Arrays.toString(recorder.getBestX()));
    }
}
```

### 步骤三：编译并运行

```bash
javac -d bin src/mypackage/*.java
java -cp bin mypackage.MyDemo
```

## 🔁 4. 可复现的优化结果

如果需要每次运行得到相同结果，可传入带固定种子的 `Random`：

```java
import java.util.Random;

SimulatedAnnealing<double[]> sa =
    new SimulatedAnnealing<>(
        new Random(42),  // 固定种子
        problem,
        new SABasicInitializer(100),
        new SABasicPerturbation(),
        new SABasicCoolingSchedule(0.99, 100),
        new SABasicTerminationCondition(10000)
    );
```

## 🚀 5. 下一步

- 阅读 [README.md](README.md) 了解框架设计思想和核心概念
- 阅读 [src/sa/README.md](src/sa/README.md) 深入了解 SA 模块
- 阅读 [src/sa/AGENTS.md](src/sa/AGENTS.md) 了解 AI Agent 使用指南
- 尝试自定义组件（冷却策略、扰动器等）