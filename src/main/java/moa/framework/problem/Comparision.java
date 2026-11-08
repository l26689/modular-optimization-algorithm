package moa.framework.problem;

public class Comparision {
    /**
     * 比较结果枚举，定义了三种可能的比较关系：
     * <ul>
     *   <li>{@link #LESS}：表示 {@code x1} 劣于 {@code x2}，
     *   <li>{@link #GREATER}：表示 {@code x1} 优于 {@code x2}，
     *   <li>{@link #EQUAL}：表示两者等优。
     *   <li>{@link #UNCOMPARABLE}：表示无法比较（无支配关系）。
     * </ul>
     */
    public enum Relation {
        LESS, GREATER, EQUAL, UNCOMPARABLE
    }   
    public final Relation relation;
    public final double gap;
    public Comparision(Relation relation, double gap) {
        this.relation = relation;
        this.gap = gap;
    }
}
