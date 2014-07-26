package com.manniwood.mpjw;

public enum ReturnStyle {

    /**
     * scalar whose class is explicitly named using SQL SELECT's AS keyword
     */
    SCALAR_EXPLICIT,
    /**
     * scalar whose class is guessed from the return column's metadata
     */

    SCALAR_GUESSED,

    /**
     * bean that is instantiated using its null constructor,
     * and whose setters are explicitly named using SQL SELECT's AS keyword
     */
    BEAN_EXPLICIT_SETTERS,

    /**
     * bean that is instantiated using its null constructor,
     * and whose setters are guessed based on the column names
     */
    BEAN_GUESSED_SETTERS,

    /**
     * bean that is instantiated using a costructor
     * whose argument types are explicitly named using
     * SQL SELECT's AS keyword,
     * and are used in the order that they are found in the SQL statement
     */
    BEAN_EXPLICIT_CONS_ARGS,

    /**
     * bean that is instantiated using a constructor
     * whose argument types are guessed from each column's metadata,
     * and are used in the order that they are found in the SQL statement
     */
    BEAN_GUESSED_CONS_ARGS
}
