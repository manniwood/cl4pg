package com.manniwood.mpjw;

/**
 * Based on column names, how should the bean be constructed?
 * @author mwood
 *
 */
public enum BeanBuildStyle {
    GUESS_SETTERS,
    GUESS_CONSTRUCTOR,
    SPECIFY_SETTERS,
    SPECIFY_CONSTRUCTOR
}
