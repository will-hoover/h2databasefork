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
import org.h2.value.ValueNull;
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
                return getDateTimeRange(session, IntervalQualifier.HOUR_TO_SECOND);
            case Value.DATE:
                return getDateTimeRange(session, IntervalQualifier.DAY);
            case Value.TIMESTAMP:
            case Value.TIMESTAMP_TZ:
                return getDateTimeRange(session, IntervalQualifier.DAY_TO_SECOND);
            default:
                return max.subtract(min);
        }
    }

    private Value getDateTimeRange(SessionLocal session, IntervalQualifier qualifier) {
        long[] maxArr = DateTimeUtils.dateAndTimeFromValue(max, session);
        long[] minArr = DateTimeUtils.dateAndTimeFromValue(min, session);

        BigInteger maxNanos = BigInteger.valueOf(DateTimeUtils.absoluteDayFromDateValue(maxArr[0])).multiply(IntervalUtils.NANOS_PER_DAY_BI).add(BigInteger.valueOf(maxArr[1]));
        BigInteger minNanos = BigInteger.valueOf(DateTimeUtils.absoluteDayFromDateValue(minArr[0])).multiply(IntervalUtils.NANOS_PER_DAY_BI).add(BigInteger.valueOf(minArr[1]));

        long offset = 0;
        switch (min.getValueType()) {
            case Value.TIME_TZ: {
                ValueTimeTimeZone lo = (ValueTimeTimeZone) min;
                ValueTimeTimeZone hi = (ValueTimeTimeZone) max;
                offset = (lo.getTimeZoneOffsetSeconds() - hi.getTimeZoneOffsetSeconds()) * DateTimeUtils.NANOS_PER_SECOND;
                break;
            }
            case Value.TIMESTAMP_TZ: {
                ValueTimestampTimeZone lo = (ValueTimestampTimeZone) min;
                ValueTimestampTimeZone hi = (ValueTimestampTimeZone) max;
                offset = (lo.getTimeZoneOffsetSeconds() - hi.getTimeZoneOffsetSeconds()) * DateTimeUtils.NANOS_PER_SECOND;
                break;
            }
        }

        BigInteger diff = maxNanos.subtract(minNanos).add(BigInteger.valueOf(offset));
        return IntervalUtils.intervalFromAbsolute(qualifier, diff);
    }

}
