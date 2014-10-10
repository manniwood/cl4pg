package com.manniwood.cl4pg.v1.test.etc;

public class TwoInts {
    private int first;
    private int second;
    public int getFirst() {
        return first;
    }
    public void setFirst(int first) {
        this.first = first;
    }
    public int getSecond() {
        return second;
    }
    public void setSecond(int second) {
        this.second = second;
    }
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if ( ! (obj instanceof TwoInts)) {
            return false;
        }
        TwoInts other = (TwoInts) obj;
        return (first == other.first && second == other.second);
    }

}
