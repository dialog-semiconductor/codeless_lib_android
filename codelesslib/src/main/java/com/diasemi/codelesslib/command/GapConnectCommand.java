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

import android.util.Log;

import com.diasemi.codelesslib.CodelessEvent;
import com.diasemi.codelesslib.CodelessLibLog;
import com.diasemi.codelesslib.CodelessManager;
import com.diasemi.codelesslib.CodelessProfile;
import com.diasemi.codelesslib.CodelessProfile.Command;
import com.diasemi.codelesslib.CodelessProfile.CommandID;

import org.greenrobot.eventbus.EventBus;

import java.util.regex.Pattern;

/**
 * <code>AT+GAPCONNECT</code> command implementation.
 * @see CodelessEvent.DeviceConnected
 * @see <a href="https://lpccs-docs.renesas.com/UM-140-DA145x-CodeLess/index.html">AT commands documentation</a>
 */
public class GapConnectCommand extends CodelessCommand {
    public static final String TAG = "GapConnectCommand";

    public static final String COMMAND = "GAPCONNECT";
    public static final String NAME = CodelessProfile.PREFIX_LOCAL + COMMAND;
    public static final CommandID ID = CommandID.GAPCONNECT;

    public static final String ADDRESS_PATTERN_STRING = "(?:[0-9a-fA-F]{2}:){5}[0-9a-fA-F]{2}";
    public static final Pattern ADDRESS_PATTERN = Pattern.compile("^" + ADDRESS_PATTERN_STRING + "$");

    public static final String PATTERN_STRING = "^GAPCONNECT=("+ ADDRESS_PATTERN_STRING +"),([PR])$"; // <address> <type>
    public static final Pattern PATTERN = Pattern.compile(PATTERN_STRING);

    /** The Bluetooth address argument. */
    private String address;
    /** The Bluetooth address type argument. */
    private int addressType;
    /** <code>true</code> if the response indicates that the connection was established. */
    private boolean connected;

    /**
     * Creates an <code>AT+GAPCONNECT</code> command.
     * @param manager       the associated manager
     * @param address       the Bluetooth address argument
     * @param addressType   the Bluetooth address {@link CodelessProfile.Command#GAP_ADDRESS_TYPE_PUBLIC type} argument
     */
    public GapConnectCommand(CodelessManager manager, String address, int addressType) {
        this(manager);
        setAddress(address);
        setAddressType(addressType);
    }

    /**
     * Default constructor.
     * @see CodelessCommand#CodelessCommand(CodelessManager)
     */
    public GapConnectCommand(CodelessManager manager) {
        super(manager);
    }

    /**
     * Default constructor.
     * @see CodelessCommand#CodelessCommand(CodelessManager, String, boolean)
     */
    public GapConnectCommand(CodelessManager manager, String command, boolean parse) {
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
        if (!response.equals("Connected") && !response.contains("Connecting")) {
            invalid = true;
        } else if (response.equals("Connected")) {
            connected = true;
        }
        if (invalid) {
            Log.e(TAG, "Received invalid response: " + response);
        } else if (CodelessLibLog.COMMAND)
            Log.d(TAG, "Connect status: " + response);
    }

    @Override
    public void onSuccess() {
        super.onSuccess();
        if (isValid() & connected)
            EventBus.getDefault().post(new CodelessEvent.DeviceConnected(this));
    }

    @Override
    protected boolean hasArguments() {
        return true;
    }

    @Override
    protected String getArguments() {
        return address + "," + (addressType == Command.GAP_ADDRESS_TYPE_PUBLIC ? Command.GAP_ADDRESS_TYPE_PUBLIC_STRING : Command.GAP_ADDRESS_TYPE_RANDOM_STRING);
    }

    @Override
    protected boolean requiresArguments() {
        return true;
    }

    @Override
    protected boolean checkArgumentsCount() {
        return CodelessProfile.countArguments(command, ",") == 2;
    }

    @Override
    protected String parseArguments() {
        String address = matcher.group(1);
        if (address == null)
            return "Invalid address";
        this.address = address;

        String addressTypeString = matcher.group(2);
        addressType = addressTypeString.equals(Command.GAP_ADDRESS_TYPE_PUBLIC_STRING) ? Command.GAP_ADDRESS_TYPE_PUBLIC : Command.GAP_ADDRESS_TYPE_RANDOM;

        return null;
    }

    /** Returns the Bluetooth address argument. */
    public String getAddress() {
        return address;
    }

    /** Sets the Bluetooth address argument. */
    public void setAddress(String address) {
        this.address = address;
        if (!ADDRESS_PATTERN.matcher(address).matches())
            invalid = true;
    }

    /** Returns the Bluetooth address {@link CodelessProfile.Command#GAP_ADDRESS_TYPE_PUBLIC type} argument. */
    public int getAddressType() {
        return addressType;
    }

    /** Sets the Bluetooth address {@link CodelessProfile.Command#GAP_ADDRESS_TYPE_PUBLIC type} argument. */
    public void setAddressType(int addressType) {
        this.addressType = addressType;
        if (addressType != Command.GAP_ADDRESS_TYPE_PUBLIC && addressType != Command.GAP_ADDRESS_TYPE_RANDOM)
            invalid = true;
    }

    /** Checks if the connection was established. */
    public boolean isConnected() {
        return connected;
    }
}
