package com.stockflow.service;

import com.stockflow.model.Contact;
import com.stockflow.util.Check;

final class ContactValidator {
    private ContactValidator() { }

    static void normalize(Contact c, String label) {
        c.setName(Check.maxLen(Check.required(c.getName(), label + " name"), 120, label + " name"));
        c.setPhone(Check.phone(c.getPhone()));
        c.setEmail(Check.maxLen(Check.optionalEmail(c.getEmail()), 100, "Email"));
        c.setAddress(Check.maxLen(Check.blankToNull(c.getAddress()), 255, "Address"));
    }
}
