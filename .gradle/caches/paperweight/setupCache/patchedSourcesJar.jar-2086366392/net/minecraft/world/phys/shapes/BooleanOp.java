package net.minecraft.world.phys.shapes;

public interface BooleanOp {
    BooleanOp FALSE = (a, b) -> {
        return false;
    };
    BooleanOp NOT_OR = (a, b) -> {
        return !a && !b;
    };
    BooleanOp ONLY_SECOND = (a, b) -> {
        return b && !a;
    };
    BooleanOp NOT_FIRST = (a, b) -> {
        return !a;
    };
    BooleanOp ONLY_FIRST = (a, b) -> {
        return a && !b;
    };
    BooleanOp NOT_SECOND = (a, b) -> {
        return !b;
    };
    BooleanOp NOT_SAME = (a, b) -> {
        return a != b;
    };
    BooleanOp NOT_AND = (a, b) -> {
        return !a || !b;
    };
    BooleanOp AND = (a, b) -> {
        return a && b;
    };
    BooleanOp SAME = (a, b) -> {
        return a == b;
    };
    BooleanOp SECOND = (a, b) -> {
        return b;
    };
    BooleanOp CAUSES = (a, b) -> {
        return !a || b;
    };
    BooleanOp FIRST = (a, b) -> {
        return a;
    };
    BooleanOp CAUSED_BY = (a, b) -> {
        return a || !b;
    };
    BooleanOp OR = (a, b) -> {
        return a || b;
    };
    BooleanOp TRUE = (a, b) -> {
        return true;
    };

    boolean apply(boolean a, boolean b);
}
