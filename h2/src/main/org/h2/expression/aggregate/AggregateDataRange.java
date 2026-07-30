/*
 * Copyright 2004-2025 H2 Group. Multiple-Licensed under the MPL 2.0,
 * and the EPL 1.0 (https://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.expression.aggregate;

import java.math.BigInteger;

import org.h2.api.IntervalQualifier;
import org.h2.engine.SessionLocal;
import org.h2.util.DateTimeUtils;
import org.h2.util.IntervalUtils;
import org.h2.value.Value;
import org.h2.value.ValueDate;
import org.h2.value.ValueInterval;
import org.h2.value.ValueNull;
import org.h2.value.ValueTime;
import org.h2.value.ValueTimeTimeZone;
import org.h2.value.ValueTimestampTimeZone;

/**
 * Data stored while calculating RANGE(expression).
 */
final class AggregateDataRange extends AggregateData {

    private Value min;
    private Value max;

    @Override
    void add(SessionLocal session, Value v) {
        if (v == ValueNull.INSTANCE) {
            return;
        }

        if (min == null) {
            min = v;
            max = v;
            return;
        }

        if (session.compare(v, min) < 0) {
            min = v;
        }

        if (session.compare(v, max) > 0) {
            max = v;
        }
    }

    @Override
    Value getValue(SessionLocal session) {
        if (min == null || max == null) {
            return ValueNull.INSTANCE;
        }
        switch (min.getValueType()) {
        case Value.TIME:
        case Value.TIME_TZ:
            return getTimeRange();
        case Value.DATE:
            return getDateRange();
        case Value.TIMESTAMP:
        case Value.TIMESTAMP_TZ:
            return getTimestampRange(session);
        default:
            return max.subtract(min);
        }
    }

    private Value getTimeRange() {
        long diff;
        if (min.getValueType() == Value.TIME) {
            diff = ((ValueTime) max).getNanos() - ((ValueTime) min).getNanos();
        } else {
            ValueTimeTimeZone lo = (ValueTimeTimeZone) min, hi = (ValueTimeTimeZone) max;
            diff = hi.getNanos() - lo.getNanos() + (lo.getTimeZoneOffsetSeconds() - hi.getTimeZoneOffsetSeconds())
                    * DateTimeUtils.NANOS_PER_SECOND;
        }
        boolean negative = diff < 0;
        if (negative) {
            diff = -diff;
        }
        return ValueInterval.from(IntervalQualifier.HOUR_TO_SECOND, negative, diff / DateTimeUtils.NANOS_PER_HOUR,
                diff % DateTimeUtils.NANOS_PER_HOUR);
    }

    private Value getDateRange() {
        long diff = DateTimeUtils.absoluteDayFromDateValue(((ValueDate) max).getDateValue())
                - DateTimeUtils.absoluteDayFromDateValue(((ValueDate) min).getDateValue());
        boolean negative = diff < 0;
        if (negative) {
            diff = -diff;
        }
        return ValueInterval.from(IntervalQualifier.DAY, negative, diff, 0L);
    }

    private Value getTimestampRange(SessionLocal session) {
        BigInteger diff = nanosFromValue(session, max).subtract(nanosFromValue(session, min));
        if (min.getValueType() == Value.TIMESTAMP_TZ) {
            ValueTimestampTimeZone lo = (ValueTimestampTimeZone) min, hi = (ValueTimestampTimeZone) max;
            diff = diff.add(BigInteger.valueOf((lo.getTimeZoneOffsetSeconds() - hi.getTimeZoneOffsetSeconds())
                    * DateTimeUtils.NANOS_PER_SECOND));
        }
        return IntervalUtils.intervalFromAbsolute(IntervalQualifier.DAY_TO_SECOND, diff);
    }

    private static BigInteger nanosFromValue(SessionLocal session, Value v) {
        long[] a = DateTimeUtils.dateAndTimeFromValue(v, session);
        return BigInteger.valueOf(DateTimeUtils.absoluteDayFromDateValue(a[0]))
                .multiply(IntervalUtils.NANOS_PER_DAY_BI).add(BigInteger.valueOf(a[1]));
    }

}
