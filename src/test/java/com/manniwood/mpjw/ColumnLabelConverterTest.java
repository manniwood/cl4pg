package com.manniwood.mpjw;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.manniwood.mpjw.util.ColumnLabelConverter;

public class ColumnLabelConverterTest {

    @Test
    public void test() {
        String s = "updated_on";
        String setter = ColumnLabelConverter.convert(s);
        Assert.assertEquals(setter, "setUpdatedOn", "setter needs to work");
    }
}
