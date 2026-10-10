# 快速开始

5 分钟内运行你的第一个优化示例。

---

## 前置条件

- Java 25 或更高版本
- Maven 3.6+
- 已克隆本仓库

## 1. 编译

```bash
mvn compile
```

## 2. 运行示例

```bash
# 简单平方和问题
mvn exec:java -Dexec.mainClass="moa.example.SphereProblemDemo"

# Rosenbrock 函数
mvn exec:java -Dexec.mainClass="moa.example.RosenbrockDemo"
```

## 3. 编写自己的优化程序

### 定义问题

连续优化问题直接继承 ContinuousProblem：

```java
import moa.problem.continuous.ContinuousProblem;

public class MyProblem extends ContinuousProblem {

    public MyProblem(int dimension) {
        super(createBounds(dimension, -100), createBounds(dimension, 100));
    }

    @Override
    public Double evaluate(double[] x) {
        double sum = 0.0;
        for (double v : x) sum += v * v;
        return sum;
    }

    @Override
    public double[] copyX(double[] x) {
        return x.clone();
    }
}
```

非连续问题可直接实现 `Problem<X>` 接口。

### 组装算法并运行

```java
import moa.algorithm.sa.engine.BasicSA;
import moa.algorithm.sa.components.*;
import moa.problem.continuous.components.ContinuousUniformSearch;
import moa.components.recorder.BestRecorder;
import moa.components.terminator.MaxCallTerminationCondition;

public class MyDemo {
    public static void main(String[] args) {
        MyProblem problem = new MyProblem(2);

        BasicSA<double[], MyProblem> sa = new BasicSA<>(
            problem,
            new SABasicInitializer(100),
            new ContinuousUniformSearch(),
            new SABasicCoolingSchedule<>(0.99, 100),
            new MaxCallTerminationCondition<double[]>(10000)
        );

        BestRecorder<double[]> recorder = new BestRecorder<>();
        sa.solve(recorder);

        System.out.println("最优值: " + problem.evaluate(recorder.getBestX()));
        System.out.println("最优解: " + java.util.Arrays.toString(recorder.getBestX()));
    }
}
```

## 4. 固定种子的可复现运行

```java
import java.util.Random;

BasicSA<double[], MyProblem> sa = new BasicSA<>(
    new Random(42),
    problem,
    new SABasicInitializer(100),
    new ContinuousUniformSearch(),
    new SABasicCoolingSchedule<>(0.99, 100),
    new MaxCallTerminationCondition<double[]>(10000)
);
```

## 5. 下一步

- [DESIGN.md](docs/DESIGN.md) —— 完整设计文档（泛型约束、组件生命周期、安全规则）
- Javadoc —— 各组件类的详细说明（含参数绑定和适用场景）