package com.glbajaj.campuscare.util;

/** Shared validation patterns (compile-time constants so they can be used inside annotations). */
public final class Validation {
    private Validation() {}

    /** Indian mobile number, optional +91 prefix. */
    public static final String PHONE = "^(\\+91[\\- ]?)?[6-9]\\d{9}$";
    /** 8-64 characters, at least one letter and one digit. */
    public static final String PASSWORD = "^(?=.*[A-Za-z])(?=.*\\d).{8,64}$";
    public static final String PASSWORD_MESSAGE = "Password must be 8-64 characters and contain at least one letter and one digit";
    public static final String PHONE_MESSAGE = "Enter a valid 10-digit mobile number";

    /** Optional free-text room number, e.g. "204", "Lab 3", "12A-B". Empty is allowed. */
    public static final String ROOM = "^$|^[A-Za-z0-9][A-Za-z0-9 ./-]{0,39}$";
    public static final String ROOM_MESSAGE = "Room number may contain letters, numbers, spaces and - / . only (max 40 characters)";
}
