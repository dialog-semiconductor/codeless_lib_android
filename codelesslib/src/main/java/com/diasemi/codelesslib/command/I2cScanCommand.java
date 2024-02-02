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
import com.diasemi.codelesslib.CodelessProfile.CommandID;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.regex.Pattern;

import androidx.annotation.NonNull;

/**
 * <code>AT+I2CSCAN</code> command implementation.
 * @see CodelessEvent.I2cScan
 * @see <a href="https://lpccs-docs.renesas.com/UM-140-DA145x-CodeLess/index.html">AT commands documentation</a>
 */
public class I2cScanCommand extends CodelessCommand {
    public static final String TAG = "I2cScanCommand";

    public static final String COMMAND = "I2CSCAN";
    public static final String NAME = CodelessProfile.PREFIX_LOCAL + COMMAND;
    public static final CommandID ID = CommandID.I2CSCAN;

    public static final String PATTERN_STRING = "^I2CSCAN$";
    public static final Pattern PATTERN = Pattern.compile(PATTERN_STRING);

    /** Information about a device detected on the I2C bus during scan. */
    public static class I2cDevice {
        /** The I2C address. */
        public int address;
        /** The contents of register 0x00, if available. */
        public int registerZero = -1;

        public I2cDevice(int address) {
            this.address = address;
        }

        public I2cDevice(int address, int registerZero) {
            this(address);
            this.registerZero = registerZero;
        }

        /** Checks if the contents of register 0x00 are available. */
        public boolean hasRegisterZero() {
            return registerZero != -1;
        }

        /** Returns the I2C address as hex string. */
        public String addressString() {
            return "0x" + Integer.toString(address, 16);
        }

        @NonNull
        @Override
        public String toString() {
            return addressString() + (hasRegisterZero() ? "(0x" + Integer.toString(registerZero, 16) + ")" : "");
        }
    }

    /** The list of found I2C devices contained in the response. */
    private ArrayList<I2cDevice> devices = new ArrayList<>();

    /**
     * Default constructor.
     * @see CodelessCommand#CodelessCommand(CodelessManager)
     */
    public I2cScanCommand(CodelessManager manager) {
        super(manager);
    }

    /**
     * Default constructor.
     * @see CodelessCommand#CodelessCommand(CodelessManager, String, boolean)
     */
    public I2cScanCommand(CodelessManager manager, String command, boolean parse) {
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
        String[] scanResults = response.split(",");
        for (String result : scanResults) {
            try {
                int index = result.indexOf(":");
                I2cDevice device = new I2cDevice(Integer.decode(index == -1 ? result : result.substring(0, index)));
                if (index != -1)
                    device.registerZero = Integer.decode(result.substring(index + 1));
                devices.add(device);
            } catch (NumberFormatException e) {
                Log.e(TAG, "Received invalid I2C scan results: " + response);
                invalid = true;
                return;
            }
        }
        if (CodelessLibLog.COMMAND)
            Log.d(TAG, "I2C scan results: " + devices);
    }

    @Override
    public void onSuccess() {
        super.onSuccess();
        if (isValid()) {
            if (CodelessLibLog.COMMAND && devices.isEmpty())
                Log.d(TAG, "No I2C devices found");
            EventBus.getDefault().post(new CodelessEvent.I2cScan(this));
        }
    }

    /** Returns the list of found I2C devices contained in the response. */
    public ArrayList<I2cDevice> getDevices() {
        return devices;
    }
}
