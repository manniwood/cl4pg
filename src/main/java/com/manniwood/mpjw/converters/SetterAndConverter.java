package com.manniwood.mpjw.converters;

import java.lang.reflect.Method;

public class SetterAndConverter {

    private final Converter<?> converter;
    private final Method setter;

    public SetterAndConverter(Converter<?> converter, Method setter) {
        super();
        this.converter = converter;
        this.setter = setter;
    }

    public Converter<?> getConverter() {
        return converter;
    }

    public Method getSetter() {
        return setter;
    }

}
