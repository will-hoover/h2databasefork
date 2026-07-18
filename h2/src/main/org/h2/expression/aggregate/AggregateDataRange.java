/*
 * Copyright 2004-2025 H2 Group. Multiple-Licensed under the MPL 2.0,
 * and the EPL 1.0 (https://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.expression.aggregate;

import org.h2.engine.SessionLocal;
import org.h2.value.Value;
import org.h2.value.ValueNull;

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

        return max.subtract(min);
    }

}