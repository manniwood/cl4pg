package com.manniwood.mpjw;

/**
 * Based on column names, how should the bean be constructed?
 * @author mwood
 *
 */
public enum BeanBuildStyle {
    GUESS_SETTERS,
    GUESS_CONSTRUCTOR,
    USE_NAMED_SETTERS,
    USE_NAMED_CLASSES_FOR_CONSTRUCTOR
}
