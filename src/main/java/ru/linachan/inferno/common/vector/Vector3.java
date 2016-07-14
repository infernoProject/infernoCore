package ru.linachan.inferno.common.vector;

public class Vector3<X, Y, Z> {

    private X x;
    private Y y;
    private Z z;

    public Vector3(X x, Y y, Z z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public X getX() {
        return x;
    }

    public Y getY() {
        return y;
    }

    public Z getZ() {
        return z;
    }

    public void setX(X x) {
        this.x = x;
    }

    public void setY(Y y) {
        this.y = y;
    }

    public void setZ(Z z) {
        this.z = z;
    }

    @Override
    public int hashCode() {
        return x.hashCode() + y.hashCode() + z.hashCode();
    }

    @Override
    public boolean equals(Object target){
        if (target == null) return false;
        if (target == this) return true;
        if (!(target instanceof Vector3<?,?,?>)) return false;

        return x.equals(((Vector3) target).getX()) && y.equals(((Vector3) target).getY()) && z.equals(((Vector3) target).getZ());
    }

    @Override
    public String toString() {
        return String.format("Vector3(%s, %s, %s)", x, y, z);
    }
}
