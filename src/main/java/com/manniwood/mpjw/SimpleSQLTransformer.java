package com.manniwood.mpjw;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleSQLTransformer extends BaseSQLTransformer implements
        SQLTransformer {

    private final static Logger log  = LoggerFactory
                                             .getLogger(SimpleSQLTransformer.class);

    private final List<String>  args = new ArrayList<>();

    public SimpleSQLTransformer(String sql) {
        super(sql);
    }

    @Override
    public int extractArg(char[] chrs, int chrsLen, int i) {
        StringBuilder arg = new StringBuilder();
        while (i < chrsLen && chrs[i] != '}') {
            i++;
            if (chrs[i] != '}') {
                arg.append(chrs[i]);
            }
        }
        if (chrs[i] == '}') {
            log.debug("adding arg: {}", arg.toString());
            args.add(arg.toString());
            arg = null; // done with this; hint to gc
        }
        return i;
    }

    public List<String> getArgs() {
        return args;
    }

    public ParsedSQLWithSimpleArgs getParsedSql() {
        return new ParsedSQLWithSimpleArgs(transformedSQL, args);
    }

}
