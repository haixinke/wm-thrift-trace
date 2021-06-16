package com.wm.rpc.thrift;

import com.facebook.swift.codec.internal.coercion.FromThrift;
import com.facebook.swift.codec.internal.coercion.ToThrift;

import java.math.BigDecimal;
import java.time.*;
import java.util.Date;

/*
添加Date和BigDecimal的默认支持
 */
public class AdditionalCoercions {
    @FromThrift
    public static Date i64ToDate(long value)
    {
        return new Date(value);
    }

    @ToThrift
    public static long dateToI64(Date value)
    {
        return value.getTime();
    }

    @FromThrift
    public static ZonedDateTime i64ToZonedDateTime(long value)
    {
        return ZonedDateTime.ofInstant(Instant.ofEpochSecond(value), ZoneId.systemDefault());
    }

    @ToThrift
    public static long zonedDateTimeToI64(ZonedDateTime value)
    {
        return value.toEpochSecond();
    }

    @FromThrift
    public static LocalDate i64ToLocalDate(long value)
    {
        return LocalDate.ofEpochDay(value);
    }

    @ToThrift
    public static long localDateToI64(LocalDate value)
    {
        return value.toEpochDay();
    }

    @FromThrift
    public static Instant i64ToInstant(long value)
    {
        return Instant.ofEpochSecond(value);
    }

    @ToThrift
    public static long instantToI64(Instant value)
    {
        return value.getEpochSecond();
    }

    @FromThrift
    public static BigDecimal stringToBigDecimal(String value)
    {
        return new BigDecimal(value);
    }

    @ToThrift
    public static String bigDecimalToString(BigDecimal  value)
    {
        return value.toPlainString();
    }
}
