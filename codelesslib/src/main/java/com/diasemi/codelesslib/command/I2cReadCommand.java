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

import java.util.Arrays;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * <code>AT+I2CREAD</code> command implementation.
 * @see CodelessEvent.I2cRead
 * @see <a href="https://lpccs-docs.renesas.com/UM-140-DA145x-CodeLess/index.html">AT commands documentation</a>
 */
public class I2cReadCommand extends CodelessCommand {
    public static final String TAG = "I2cReadCommand";

    public static final String COMMAND = "I2CREAD";
    public static final String NAME = CodelessProfile.PREFIX_LOCAL + COMMAND;
    public static final CommandID ID = CommandID.I2CREAD;

    public static final String PATTERN_STRING = "^I2CREAD=(0[xX][0-9a-fA-F]+|\\d+),(0[xX][0-9a-fA-F]+|\\d+)(?:,(\\d+))?$"; // <address> <register> <bytes>
    public static final Pattern PATTERN = Pattern.compile(PATTERN_STRING);

    /** The I2C address argument. */
    private int address;
    /** The I2C register argument. */
    private int register;
    /** The byte count argument. */
    private int byteCount;
    /** The read data response. */
    private int[] data = new int[0];

    /**
     * Creates an <code>AT+I2CREAD</code> command.
     * @param manager   the associated manager
     * @param address   the I2C address argument
     * @param register  the I2C register argument
     */
    public I2cReadCommand(CodelessManager manager, int address, int register) {
        super(manager);
        this.address = address;
        this.register = register;
        byteCount = -1;
    }

    /**
     * Creates an <code>AT+I2CREAD</code> command.
     * @param manager   the associated manager
     * @param address   the I2C address argument
     * @param register  the I2C register argument
     * @param byteCount the byte count argument
     */
    public I2cReadCommand(CodelessManager manager, int address, int register, int byteCount) {
        this(manager, address, register);
        this.byteCount = byteCount;
    }

    /**
     * Default constructor.
     * @see CodelessCommand#CodelessCommand(CodelessManager)
     */
    public I2cReadCommand(CodelessManager manager) {
        super(manager);
    }

    /**
     * Default constructor.
     * @see CodelessCommand#CodelessCommand(CodelessManager, String, boolean)
     */
    public I2cReadCommand(CodelessManager manager, String command, boolean parse) {
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
    protected boolean hasArguments() {
        return true;
    }

    @Override
    protected String getArguments() {
        if (byteCount == -1)
            return String.format(Locale.US, "0x%02X,0x%02X", address, register);
        return String.format(Locale.US, "0x%02X,0x%02X,%d", address, register, byteCount);
    }

    @Override
    public void parseResponse(String response) {
        super.parseResponse(response);
        if (responseLine() == 1) {
            String[] values = response.split(",");
            data = new int[values.length];
            try {
                for (int i = 0; i < values.length; i++) {
                    data[i] = Integer.decode(values[i]);
                }
                if (CodelessLibLog.COMMAND)
                    Log.d(TAG, "Read data: " + Arrays.toString(data));
            } catch (NumberFormatException e) {
                Log.e(TAG, "Received invalid data: " + response);
                invalid = true;
            }
        }
    }

    @Override
    public void onSuccess() {
        super.onSuccess();
        if (isValid())
            EventBus.getDefault().post(new CodelessEvent.I2cRead(this));
    }

    @Override
    protected boolean requiresArguments() {
        return true;
    }

    @Override
    protected boolean checkArgumentsCount() {
        int count = CodelessProfile.countArguments(command, ",");
        return count == 2 || count == 3;
    }

    @Override
    protected String parseArguments() {
        byteCount = -1;
        int count = CodelessProfile.countArguments(command, ",");

        Integer num = decodeNumberArgument(1);
        if (num == null)
            return "Invalid slave address";
        address = num;

        num = decodeNumberArgument(2);
        if (num == null)
            return "Invalid register";
        register = num;

        if (count == 3) {
            num = decodeNumberArgument(3);
            if (num == null)
                return "Invalid number of bytes";
            byteCount = num;
        }

        return null;
    }

    /** Returns the I2C address argument. */
    public int getAddress() {
        return address;
    }

    /** Sets the I2C address argument. */
    public void setAddress(int address) {
        this.address = address;
    }

    /** Returns the I2C register argument. */
    public int getRegister() {
        return register;
    }

    /** Sets the I2C register argument. */
    public void setRegister(int register) {
        this.register = register;
    }

    /** Returns the byte count argument. */
    public int getByteCount() {
        return byteCount;
    }

    /** Sets the byte count argument. */
    public void setByteCount(int byteCount) {
        this.byteCount = byteCount;
    }

    /** Returns the read data response. */
    public int[] getData() {
        return data;
    }
}
