package org.openstreetmap.josm.plugins.contourmerge.util;

import java.text.MessageFormat;

public class Assert {
    public static void checkArg(boolean cond, String msg, Object... values){
        if (!cond){
            throw new IllegalArgumentException(
                    MessageFormat.format(msg, values)
            );
        }
    }
}
