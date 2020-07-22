package com.portgo.database;

import android.util.Log;

import com.portgo.database.PortCursorJoiner;

public class JoinIntegerIdCompare implements PortCursorJoiner.JoinerCompare{

    @Override
    public int compare(String... values) {
        if ((values.length % 2) != 0) {
            throw new IllegalArgumentException("you must specify an even number of values");
        }

        for (int index = 0; index < values.length; index+=2) {
            if (values[index] == null) {
                if (values[index+1] == null) continue;
                return -1;
            }

            if (values[index+1] == null) {
                return 1;
            }

            Integer left = Integer.parseInt(values[index]);
            Integer right = Integer.parseInt(values[index+1]);
            int comp = left-right;
            if (comp != 0) {
                return comp < 0 ? -1 : 1;
            }
        }

        return 0;
    }
}