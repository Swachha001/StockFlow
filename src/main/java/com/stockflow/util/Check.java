package com.stockflow.util;

import com.stockflow.exception.ValidationException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Pattern;

/** Small input validation helpers. All failures are ValidationExceptions with user-friendly messages. */
public final class Check {
    private static final Pattern BD_MOBILE = Pattern.compile("^(?:\\+?88)?01[3-9]\\d{8}$");
    private static final Pattern GENERAL_PHONE = Pattern.compile("^\\+?\\d{7,15}$");
    private static final Pattern EMAIL = Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");

    /** Largest value that fits DECIMAL(12,2) columns (prices, unit costs). */
    public static final BigDecimal MAX_UNIT_AMOUNT = new BigDecimal("9999999999.99");
    /** Largest value that fits DECIMAL(14,2) columns (line subtotals, invoice/purchase totals). */
    public static final BigDecimal MAX_TOTAL_AMOUNT = new BigDecimal("999999999999.99");
    /** Largest single quantity accepted on one line (stock itself is a 32-bit INT). */
    public static final int MAX_QUANTITY = 1_000_000;

    private Check() { }

    public static String required(String v, String label) {
        if (v == null || v.isBlank()) throw new ValidationException(label + " is required.");
        return v.trim();
    }

    public static String maxLen(String v, int max, String label) {
        if (v != null && v.length() > max) throw new ValidationException(label + " must be at most " + max + " characters.");
        return v;
    }

    public static String blankToNull(String v) {
        return v == null || v.isBlank() ? null : v.trim();
    }

    public static String phone(String v) {
        String p = required(v, "Phone").replaceAll("[\\s-]", "");
        if (!BD_MOBILE.matcher(p).matches() && !GENERAL_PHONE.matcher(p).matches()) {
            throw new ValidationException("Phone number looks invalid. Example: 01712345678");
        }
        return p;
    }

    public static String optionalEmail(String v) {
        String e = blankToNull(v);
        if (e != null && !EMAIL.matcher(e).matches()) throw new ValidationException("Email address looks invalid.");
        return e;
    }

    public static BigDecimal money(String text, String label) {
        String t = required(text, label).replace("৳", "").replace(",", "").trim();
        try {
            BigDecimal v = new BigDecimal(t).setScale(2, RoundingMode.HALF_UP);
            if (v.signum() < 0) throw new ValidationException(label + " cannot be negative.");
            if (v.compareTo(MAX_UNIT_AMOUNT) > 0) throw new ValidationException(label + " is too large (maximum ৳ 9,999,999,999.99).");
            return v;
        } catch (NumberFormatException e) {
            throw new ValidationException(label + " must be a valid number.");
        }
    }

    public static int integer(String text, String label, int min) {
        String t = required(text, label);
        try {
            int v = Integer.parseInt(t);
            if (v < min) throw new ValidationException(label + " must be " + (min == 0 ? "0 or more." : "at least " + min + "."));
            return v;
        } catch (NumberFormatException e) {
            throw new ValidationException(label + " must be a whole number.");
        }
    }

    public static void nonNegative(BigDecimal v, String label) {
        if (v == null || v.signum() < 0) throw new ValidationException(label + " cannot be negative.");
        if (v.compareTo(MAX_UNIT_AMOUNT) > 0) throw new ValidationException(label + " is too large (maximum ৳ 9,999,999,999.99).");
    }
}
