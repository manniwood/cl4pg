package com.manniwood.mpjw.converters;

import java.lang.reflect.Constructor;
import java.util.List;

public class ConstructorAndConverters {

    private final Constructor<?> constructor;
    private final List<Converter<?>> converters;

    public ConstructorAndConverters(Constructor<?> constructor,
            List<Converter<?>> converters) {
        super();
        this.constructor = constructor;
        this.converters = converters;
    }

    public Constructor<?> getConstructor() {
        return constructor;
    }

    public List<Converter<?>> getConverters() {
        return converters;
    }

}
