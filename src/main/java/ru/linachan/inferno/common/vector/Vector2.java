package ru.linachan.inferno.common.vector;

public class Vector2<X, Y> {

    private X x;
    private Y y;

    public Vector2(X x, Y y) {
        this.x = x;
        this.y = y;
    }

    public X getX() {
        return x;
    }

    public Y getY() {
        return y;
    }

    public void setX(X x) {
        this.x = x;
    }

    public void setY(Y y) {
        this.y = y;
    }

    @Override
    public int hashCode() {
        return x.hashCode() + y.hashCode();
    }

    @Override
    public boolean equals(Object target){
        if (target == null) return false;
        if (target == this) return true;
        if (!(target instanceof Vector2<?,?>)) return false;

        return x.equals(((Vector2) target).getX()) && y.equals(((Vector2) target).getY());
    }

    @Override
    public String toString() {
        return String.format("Vector2(%s, %s)", x, y);
    }
}
