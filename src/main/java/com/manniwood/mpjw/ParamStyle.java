package com.manniwood.mpjw;

public enum ParamStyle {
    /**
     * Parameters are a variadic list of scalars (including none).
     * The order of the scalars matter; they are used in the SQL statement
     * in the order they are listed, and their types are named in
     * the SQL statement using the special <pre>#{}</pre> notation.
     */
    VARIADIC,

    /**
     * Parameter is a single bean whose getters are used in the
     * SQL statement using the special <pre>#{}</pre> notation.
     */
    BEAN
}
