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
import com.diasemi.codelesslib.CodelessProfile.GapScannedDevice;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <code>AT+GAPSCAN</code> command implementation.
 * @see CodelessEvent.GapScanResult
 * @see <a href="https://lpccs-docs.renesas.com/UM-140-DA145x-CodeLess/index.html">AT commands documentation</a>
 */
public class GapScanCommand extends CodelessCommand {
    public static final String TAG = "GapScanCommand";

    public static final String COMMAND = "GAPSCAN";
    public static final String NAME = CodelessProfile.PREFIX_LOCAL + COMMAND;
    public static final CommandID ID = CommandID.GAPSCAN;

    public static final String RESPONSE_PATTERN_STRING = "^\\( \\) ((?:[0-9a-fA-F]{2}:){5}[0-9a-fA-F]{2}),([PR]), Type: (\\bADV\\b|\\bRSP\\b), RSSI:(-?\\d+)$"; // <address> <type> <typeScan> <rssi>
    public static final Pattern RESPONSE_PATTERN = Pattern.compile(RESPONSE_PATTERN_STRING);

    public static final String PATTERN_STRING = "^GAPSCAN$";
    public static final Pattern PATTERN = Pattern.compile(PATTERN_STRING);

    /** The list of found devices contained in the response. */
    private ArrayList<GapScannedDevice> devices = new ArrayList<>();

    /**
     * Default constructor.
     * @see CodelessCommand#CodelessCommand(CodelessManager)
     */
    public GapScanCommand(CodelessManager manager) {
        super(manager);
    }

    /**
     * Default constructor.
     * @see CodelessCommand#CodelessCommand(CodelessManager, String, boolean)
     */
    public GapScanCommand(CodelessManager manager, String command, boolean parse) {
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
        GapScannedDevice device = new GapScannedDevice();
        if (!response.contains("Scanning") && !response.contains("Scan Completed") && !RESPONSE_PATTERN.matcher(response).matches()) {
            invalid = true;
        } else if (!response.contains("Scanning") && !response.contains("Scan Completed")) {
            try {
                Matcher matcher = RESPONSE_PATTERN.matcher(response);
                device.address = matcher.group(1);
                device.addressType = matcher.group(2).equals(Command.GAP_ADDRESS_TYPE_PUBLIC_STRING) ? Command.GAP_ADDRESS_TYPE_PUBLIC : Command.GAP_ADDRESS_TYPE_RANDOM;
                device.type = matcher.group(3).equals(Command.GAP_SCAN_TYPE_ADV_STRING) ? Command.GAP_SCAN_TYPE_ADV : Command.GAP_SCAN_TYPE_RSP;
                device.rssi = Integer.parseInt(matcher.group(4));
                devices.add(device);
            } catch (NullPointerException | IndexOutOfBoundsException | NumberFormatException e) {
                invalid = true;
            }
        }
        if (invalid) {
            Log.i(TAG, "Received invalid scan response: " + response);
        } else if (CodelessLibLog.COMMAND)
            Log.i(TAG, "Scanned device: Address:" + device.address + " Address type:" + (device.addressType == Command.GAP_ADDRESS_TYPE_PUBLIC ? Command.GAP_ADDRESS_TYPE_PUBLIC_STRING : Command.GAP_ADDRESS_TYPE_RANDOM_STRING) + " Type:" + (device.type == Command.GAP_SCAN_TYPE_ADV ? Command.GAP_SCAN_TYPE_ADV_STRING : Command.GAP_SCAN_TYPE_RSP_STRING) + " RSSI:" + device.rssi);
    }

    @Override
    public void onSuccess() {
        super.onSuccess();
        if (isValid())
            EventBus.getDefault().post(new CodelessEvent.GapScanResult(this));
    }

    /** Returns the list of found devices contained in the response. */
    public ArrayList<GapScannedDevice> getDevices() {
        return devices;
    }
}
