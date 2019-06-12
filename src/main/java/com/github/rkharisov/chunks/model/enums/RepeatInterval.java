package com.github.rkharisov.chunks.model.enums;

import java.time.Period;

public enum RepeatInterval {

    DAY(Period.ofDays(1)),
    WEEK(Period.ofWeeks(1)),
    MONTH(Period.ofMonths(1)),
    HALF_YEAR(Period.ofMonths(6)),
    YEAR(Period.ofYears(1));

    RepeatInterval(Period period) {
        this.period = period;
    }

    private final Period period;

    public Period getPeriod() {
        return period;
    }

    public RepeatInterval next() {
        RepeatInterval[] values = RepeatInterval.values();
        int pos = 0;
        for (RepeatInterval interval : values) {
            if(this == interval) break;
            pos++;
        }
        if(pos == values.length - 1) {
            return YEAR;
        } else {
            return values[pos + 1];
        }
    }
}
