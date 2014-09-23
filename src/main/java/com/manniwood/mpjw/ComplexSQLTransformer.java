package com.manniwood.mpjw;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ComplexSQLTransformer extends BaseSQLTransformer implements SQLTransformer {

    private final static Logger log = LoggerFactory.getLogger(ComplexSQLTransformer.class);

    private final List<InOutArg> args = new ArrayList<>();

    public ComplexSQLTransformer(String sql) {
        super(sql);
    }

    @Override
    public int extractArg(
                          char[] chrs,
                          int chrsLen,
                          int i) {
        StringBuilder arg = new StringBuilder();
        while (i < chrsLen && chrs[i] != '}') {
            i++;
            if (chrs[i] != '}') {
                arg.append(chrs[i]);
            }
        }
        if (chrs[i] == '}') {
            InOutArg ca = new InOutArg(arg.toString());
            log.debug("adding complex arg: {}", ca);
            args.add(ca);
            ca = null; // done with this; hint to gc
        }
        return i;
    }

    public List<InOutArg> getArgs() {
        return args;
    }

    public ParsedSQLWithComplexArgs getParsedSql() {
        return new ParsedSQLWithComplexArgs(transformedSQL, args);
    }
}
