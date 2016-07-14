package ru.linachan.inferno.common.vector;

public class Vector4<X, Y, Z, W> {

    private X x;
    private Y y;
    private Z z;
    private W w;

    public Vector4(X x, Y y, Z z, W w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
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

    public W getW() {
        return w;
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

    public void setW(W w) {
        this.w = w;
    }

    @Override
    public int hashCode() {
        return x.hashCode() + y.hashCode() + z.hashCode() + w.hashCode();
    }

    @Override
    public boolean equals(Object target){
        if (target == null) return false;
        if (target == this) return true;
        if (!(target instanceof Vector4<?,?,?,?>)) return false;

        return x.equals(((Vector4) target).getX()) && y.equals(((Vector4) target).getY()) && z.equals(((Vector4) target).getZ()) && w.equals(((Vector4) target).getW());
    }

    @Override
    public String toString() {
        return String.format("Vector4(%s, %s, %s, %s)", x, y, z, w);
    }
}
