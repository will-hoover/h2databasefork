package org.h2.value;

import java.math.BigInteger;

import org.h2.api.ErrorCode;
import org.h2.engine.CastDataProvider;
import org.h2.message.DbException;

/**
 * Implementation of the UNSIGNED INT data type.
 */
public final class ValueUnsignedInt extends Value {

    /**
     * The precision in bits.
     */
    public static final int PRECISION = 32;

    /**
     * The approximate precision in decimal digits.
     */
    public static final int DECIMAL_PRECISION = 10;

    /**
     * The maximum display size of an INT.
     * Example: -2147483648
     */
    public static final int DISPLAY_SIZE = 11;

    private static final int STATIC_SIZE = 128;
    
    private static final ValueUnsignedInt[] STATIC_CACHE = new ValueUnsignedInt[STATIC_SIZE];

    private final int value;

    static {
        for (int i = 0; i < STATIC_SIZE; i++) {
            STATIC_CACHE[i] = new ValueUnsignedInt(i);
        }
    }

    private ValueUnsignedInt(int value) {
        this.value = value;
    }

    /**
     * Get or create a UNSIGNED INT value for the given int.
     *
     * @param i the int
     * @return the value
     */
    public static ValueUnsignedInt get(int i) {
        if (i >= 0 && i < STATIC_SIZE) {
            return STATIC_CACHE[i];
        }
        return new ValueUnsignedInt(i);
    }

    @Override
    public Value add(Value v) {
        long result = unsignedLong(value) + ((ValueBigint) v).getLong();
        return checkRange(result);
    }

    private static long unsignedLong(int x) {
        if (x < 0) {
            return (long) Integer.MAX_VALUE - (long) x;
        }
        return (long) x;
    }

    private static ValueUnsignedInt checkRange(long x) {
        if (x > Integer.MAX_VALUE * 2L + 1L) {
            throw DbException.get(ErrorCode.NUMERIC_VALUE_OUT_OF_RANGE_1, Long.toString(x));
        } else if (x > Integer.MAX_VALUE) {
            return ValueUnsignedInt.get((int) ((long) Integer.MAX_VALUE - x));
        }
        return ValueUnsignedInt.get((int) x);
    }

    @Override
    public int getSignum() {
        if (value == 0) {
            return 0;
        }
        return 1;
    }

    // @Override
    // public Value negate() {
    //     return checkRange(-(long) value);
    // }

    @Override
    public Value subtract(Value v) {
        return checkRange((unsignedLong(value) - ((ValueBigint) v).getLong()));
    }

    @Override
    public Value multiply(Value v) {
        return checkRange(unsignedLong(value) * ((ValueBigint) v).getLong());
    }

    @Override
    public Value divide(Value v, TypeInfo quotientType) {
        long y = ((ValueBigint) v).getLong();
        if (y == 0) {
            throw DbException.get(ErrorCode.DIVISION_BY_ZERO_1, getTraceSQL());
        }
        return ValueInteger.get((int)(unsignedLong(value) / y));
    }

    @Override
    public Value modulus(Value v) {
        long val = ((ValueBigint) v).getLong();
        if (val == 0) {
            throw DbException.get(ErrorCode.DIVISION_BY_ZERO_1, getTraceSQL());
        }
        return ValueInteger.get((int)(unsignedLong(value) % val));
    }

    @Override
    public StringBuilder getSQL(StringBuilder builder, int sqlFlags) {
        if ((sqlFlags & NO_CASTS) == 0 && value == (int) value) {
            return builder.append("CAST(").append(value).append(" AS BIGINT)");
        }
        return builder.append(unsignedLong(value));
    }

    @Override
    public long getLong() {
        return unsignedLong(value);
    }

    @Override
    public BigInteger getBigInteger() {
        return BigInteger.valueOf(unsignedLong(value));
    }

    @Override
    public TypeInfo getType() {
        return TypeInfo.TYPE_UNSIGNED_INT;
    }

    @Override
    public int getValueType() {
        return UNSIGNED_INT;
    }

    @Override
    public int hashCode() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ValueUnsignedInt && value == ((ValueUnsignedInt) other).value;
    }

    @Override
    public String getString() {
        return unsignedLong(value) + "";
    }

    @Override
    public int compareTypeSafe(Value o, CompareMode mode, CastDataProvider provider) {
        return Long.compare(unsignedLong(value), unsignedLong(((ValueUnsignedInt) o).value));
    }
    
}
