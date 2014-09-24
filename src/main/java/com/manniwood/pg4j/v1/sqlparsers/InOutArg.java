package com.manniwood.pg4j.v1.sqlparsers;

public class InOutArg {
    private final String getter;
    private final String setter;

    public InOutArg(String slashArgs) {
        String getter = null;
        String setter = null;
        String[] args = slashArgs.split("/");
        for (String arg : args) {
            if (arg.startsWith("get")) {
                getter = arg;
            }
            if (arg.startsWith("set")) {
                setter = arg;
            }
        }
        this.getter = getter;
        this.setter = setter;
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
