package com.stockflow.util;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class Fmt {
    public static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    public static final DateTimeFormatter DATETIME = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    private Fmt() { }

    public static String money(BigDecimal v) {
        BigDecimal value = v == null ? BigDecimal.ZERO : v;
        return "৳ " + new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.US)).format(value);
    }

    public static String number(long n) {
        return String.format(Locale.US, "%,d", n);
    }

    public static String date(LocalDate d) { return d == null ? "" : DATE.format(d); }

    public static String dateTime(LocalDateTime d) { return d == null ? "" : DATETIME.format(d); }

    public static String purchaseRef(int id) { return String.format("PUR-%04d", id); }

    public static String invoiceNo(int id) { return String.format("INV-%04d", id); }

    public static String or(String v, String fallback) { return v == null || v.isBlank() ? fallback : v; }
}
