/*
 **********************************************************************************
 *
 * SPDX-License-Identifier: BSD-3-Clause
 *
 * Copyright (c) 2020-2024 Renesas Electronics Corporation and/or its affiliates
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. Neither the name of Renesas nor the names of its contributors may be
 *    used to endorse or promote products derived from this software without
 *    specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY RENESAS "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, NONINFRINGEMENT, AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL RENESAS OR CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 **********************************************************************************
 */

package com.diasemi.codelesslib;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import java.util.HashMap;

/** Utility and helper methods. */
public class CodelessUtil {

    private static final String HEX_DIGITS_LC = "0123456789abcdef";
    private static final String HEX_DIGITS_UC = "0123456789ABCDEF";
    private static HashMap<Character, Integer> HEX_DIGITS_MAP = new HashMap<>(32);
    static {
        for (int i = 0; i < 16; ++i) {
            HEX_DIGITS_MAP.put(HEX_DIGITS_LC.charAt(i), i);
            HEX_DIGITS_MAP.put(HEX_DIGITS_UC.charAt(i), i);
        }
    }

    /**
     * Converts a byte array to a hex string.
     * @param v         the byte array to convert
     * @param uppercase <code>true</code> for uppercase hex characters, <code>false</code> for lowercase
     * @return the hex string
     */
    public static String hex(byte[] v, boolean uppercase) {
        if (v == null)
            return "<null>";
        String hexDigits = uppercase ? HEX_DIGITS_UC : HEX_DIGITS_LC;
        StringBuilder buffer = new StringBuilder(v.length * 2);
        for (byte b : v) {
            buffer.append(hexDigits.charAt((b >> 4) & 0x0f)).append(hexDigits.charAt(b & 0x0f));
        }
        return buffer.toString();
    }

    /**
     * Converts a byte array to an uppercase hex string.
     * @param v the byte array to convert
     * @return the hex string
     */
    public static String hex(byte[] v) {
        return hex(v, true);
    }

    /**
     * Converts a byte array to a hex string with spaces between bytes, optionally contained in brackets.
     * @param v         the byte array to convert
     * @param uppercase <code>true</code> for uppercase hex characters, <code>false</code> for lowercase
     * @param brackets  <code>true</code> for adding brackets, <code>false</code> for no brackets
     * @return the hex string
     */
    public static String hexArray(byte[] v, boolean uppercase, boolean brackets) {
        if (v == null)
            return "[]";
        String hexDigits = uppercase ? HEX_DIGITS_UC : HEX_DIGITS_LC;
        StringBuilder buffer = new StringBuilder(v.length * 3 + 3);
        if (brackets)
            buffer.append("[ ");
        for (byte b : v) {
            buffer.append(hexDigits.charAt((b >> 4) & 0x0f)).append(hexDigits.charAt(b & 0x0f)).append(" ");
        }
        if (brackets)
            buffer.append("]");
        else if (buffer.length() > 0)
            buffer.setLength(buffer.length() - 1);
        return buffer.toString();
    }

    /**
     * Converts a byte array to an uppercase hex string with spaces between bytes.
     * @param v the byte array to convert
     * @return the hex string
     */
    public static String hexArray(byte[] v) {
        return hexArray(v, true, false);
    }

    /**
     * Converts a byte array to an lowercase hex string contained in brackets with spaces between bytes.
     * <p> Used by the library to log data byte arrays.
     * @param v the byte array to convert
     * @return the hex string
     */
    public static String hexArrayLog(byte[] v) {
        return hexArray(v, false, true);
    }

    /**
     * Converts a hex string to a byte array.
     * <p> Any non-hex characters and "0x" prefix are ignored.
     * @param s the hex string to convert (its length must be even)
     * @return the byte array
     */
    public static byte[] hex2bytes(String s) {
        s = s.replace("0x", "");
        s = s.replaceAll("[^a-fA-F0-9]", "");
        if (s.length() % 2 != 0)
            return null;
        byte[] b = new byte[s.length() / 2];
        for (int i = 0; i < s.length(); ++i) {
            Integer d = HEX_DIGITS_MAP.get(s.charAt(i));
            if (d == null)
                return null;
            b[i / 2] |= i % 2 == 0 ? d << 4 : d;
        }
        return b;
    }

    /**
     * Helper method that wraps a call to {@link BluetoothDevice#getName()}, ignoring
     * exception due to missing BLUETOOTH_CONNECT permission on Android 12 and above.
     * @param device the Bluetooth device
     * @return the device name, or <code>null</code> if an exception occured
     */
    public static String getName(BluetoothDevice device) {
        try {
            return device.getName();
        } catch (SecurityException e) {
            return null;
        }
    }

    /** Checks if BLUETOOTH_CONNECT permission is granted (on Android 12 and above). */
    public static boolean checkConnectPermission(Context context) {
        return Build.VERSION.SDK_INT < 31 || context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
    }
}
