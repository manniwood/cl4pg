package com.manniwood.cl4pg.v1.sqlparsers;

/**
 * Holds getter/setter names of bean properties that correspond to IN, OUT, and
 * INOUT arguments used by a stored procedure. When we call a stored procedure
 * and want to populate its IN arguments using a bean's getters, or when we want
 * to fetch its OUT arguments into a bean using the bean's setters, this class
 * stores the names of the setter and/or getter for one of the bean's
 * properties.
 *
 * @author mwood
 *
 */
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
        return "InOutArg [getter=" + getter + ", setter=" + setter + "]";
    }
}
