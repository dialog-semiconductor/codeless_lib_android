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

package com.diasemi.codelesslib.command;

import android.bluetooth.BluetoothAdapter;
import android.util.Log;

import com.diasemi.codelesslib.CodelessEvent;
import com.diasemi.codelesslib.CodelessLibLog;
import com.diasemi.codelesslib.CodelessManager;
import com.diasemi.codelesslib.CodelessProfile;
import com.diasemi.codelesslib.CodelessProfile.CommandID;

import org.greenrobot.eventbus.EventBus;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <code>AT+BDADDR</code> command implementation with incoming command support.
 * <p> When incoming, it responds with the host device Bluetooth address, if available.
 * @see CodelessEvent.BluetoothAddress
 * @see <a href="https://lpccs-docs.renesas.com/UM-140-DA145x-CodeLess/index.html">AT commands documentation</a>
 */
public class BluetoothAddressCommand extends CodelessCommand {
    public static final String TAG = "BluetoothAddressCommand";

    public static final String COMMAND = "BDADDR";
    public static final String NAME = CodelessProfile.PREFIX_LOCAL + COMMAND;
    public static final CommandID ID = CommandID.BDADDR;

    public static final String PATTERN_STRING = "^BDADDR$";
    public static final Pattern PATTERN = Pattern.compile(PATTERN_STRING);

    private static final String RESPONSE_PATTERN_STRING = "^([^,]*)(?:,([PR]))?$"; // <address> <type>
    private static final Pattern RESPONSE_PATTERN = Pattern.compile(RESPONSE_PATTERN_STRING);

    /** The Bluetooth address response. */
    private String address;
    /** The Bluetooth address type response (<code>true</code> for random, <code>false</code> for public). */
    private boolean random;

    /**
     * Default constructor.
     * @see CodelessCommand#CodelessCommand(CodelessManager)
     */
    public BluetoothAddressCommand(CodelessManager manager) {
        super(manager);
    }

    /**
     * Default constructor.
     * @see CodelessCommand#CodelessCommand(CodelessManager, String, boolean)
     */
    public BluetoothAddressCommand(CodelessManager manager, String command, boolean parse) {
        super(manager, command, parse);
    }

    @Override
    protected String getTag() {
        return TAG;
    }

    @Override
    public String getID() {
        return COMMAND;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public CommandID getCommandID() {
        return ID;
    }

    @Override
    public Pattern getPattern() {
        return PATTERN;
    }

    @Override
    public void parseResponse(String response) {
        super.parseResponse(response);
        if (responseLine() == 1) {
            Matcher matcher = RESPONSE_PATTERN.matcher(response);
            if (matcher.matches()) {
                address = matcher.group(1);
                if (matcher.group(2) != null)
                    random = matcher.group(2).equals("R");
                if (!BluetoothAdapter.checkBluetoothAddress(address))
                    invalid = true;
            } else {
                invalid = true;
            }
            if (invalid)
                Log.e(TAG, "Received invalid BD address: " + response);
            else if (CodelessLibLog.COMMAND)
                Log.d(TAG, "BD address: " + address + (matcher.group(2) != null ? random ? " (random)" : " (public)" : ""));
        }
    }

    @Override
    public void onSuccess() {
        super.onSuccess();
        if (isValid())
            EventBus.getDefault().post(new CodelessEvent.BluetoothAddress(this));
    }

    @Override
    public void processInbound() {
        if (address == null) {
            address = BluetoothAdapter.getDefaultAdapter().getAddress();
        }
        // Android may restrict access to the device BD address and return a hardcoded value.
        if (!address.equals("02:00:00:00:00:00")) {
            if (CodelessLibLog.COMMAND)
                Log.d(TAG, "Send BD address: " + address);
            sendSuccess(address);
        } else {
            Log.e(TAG, "System returned invalid BD address: " + address);
            sendError("BD address not available");
        }
    }

    /** Returns the Bluetooth address response. */
    public String getAddress() {
        return address;
    }

    /** Sets the Bluetooth address response. */
    public void setAddress(String address) {
        this.address = address;
    }

    /** Checks if the Bluetooth address type response is public. */
    public boolean isPublic() {
        return !random;
    }

    /** Checks if the Bluetooth address type response is random. */
    public boolean isRandom() {
        return random;
    }

    /** Sets the Bluetooth address type response. */
    public void setRandom(boolean random) {
        this.random = random;
    }
}
