package com.manniwood.mpjw;

public class ComplexArg {
    private final String getter;
    private final String setter;
    public ComplexArg(String arg1, String arg2) {
        super();
        if (arg1 != null && arg1.startsWith("get")) {
            this.getter = arg1;
        } else if (arg2 != null && arg2.startsWith("get")) {
            this.getter = arg2;
        } else {
            this.getter = null;
        }
        if (arg1 != null && arg1.startsWith("set")) {
            this.setter = arg1;
        } else if (arg2 != null && arg2.startsWith("set")) {
            this.setter = arg2;
        } else {
            this.setter = null;
        }
    }
    public String getGetter() {
        return getter;
    }
    public String getSetter() {
        return setter;
    }
    @Override
    public String toString() {
        return "ComplexArg [getter=" + getter + ", setter=" + setter + "]";
    }
}
