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
import com.diasemi.codelesslib.CodelessLibConfig;
import com.diasemi.codelesslib.CodelessLibLog;
import com.diasemi.codelesslib.CodelessManager;
import com.diasemi.codelesslib.CodelessProfile;
import com.diasemi.codelesslib.CodelessProfile.CommandID;

import org.greenrobot.eventbus.EventBus;

import java.util.Arrays;
import java.util.regex.Pattern;

/**
 * <code>AT+SPIRD</code> command implementation.
 * @see CodelessEvent.SpiRead
 * @see <a href="https://lpccs-docs.renesas.com/UM-140-DA145x-CodeLess/index.html">AT commands documentation</a>
 */
public class SpiReadCommand extends CodelessCommand {
    public static final String TAG = "SpiReadCommand";

    public static final String COMMAND = "SPIRD";
    public static final String NAME = CodelessProfile.PREFIX_LOCAL + COMMAND;
    public static final CommandID ID = CommandID.SPIRD;

    public static final String PATTERN_STRING = "^SPIRD=(\\d+)$"; // <bytes>
    public static final Pattern PATTERN = Pattern.compile(PATTERN_STRING);

    /** The number of bytes to read argument. */
    private int byteNumber;
    /** The read data response. */
    private int[] data;

    /**
     * Creates an <code>AT+SPIRD</code> command.
     * @param manager       the associated manager
     * @param byteNumber    the number of bytes to read argument
     */
    public SpiReadCommand(CodelessManager manager, int byteNumber) {
        super(manager);
        setByteNumber(byteNumber);
    }

    /**
     * Default constructor.
     * @see CodelessCommand#CodelessCommand(CodelessManager)
     */
    public SpiReadCommand(CodelessManager manager) {
        super(manager);
    }

    /**
     * Default constructor.
     * @see CodelessCommand#CodelessCommand(CodelessManager, String, boolean)
     */
    public SpiReadCommand(CodelessManager manager, String command, boolean parse) {
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
        return Integer.toString(byteNumber);
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
            EventBus.getDefault().post(new CodelessEvent.SpiRead(this));
    }

    @Override
    protected boolean requiresArguments() {
        return true;
    }

    @Override
    protected boolean checkArgumentsCount() {
        return CodelessProfile.countArguments(command, ",") == 1;
    }

    @Override
    protected String parseArguments() {
        Integer num = decodeNumberArgument(1);
        if (num == null || CodelessLibConfig.CHECK_SPI_READ_SIZE && num > CodelessLibConfig.SPI_MAX_BYTE_READ_SIZE)
            return "Invalid byte number";
        byteNumber = num;

        return null;
    }

    /** Returns the number of bytes to read argument. */
    public int getByteNumber() {
        return byteNumber;
    }

    /** Sets the number of bytes to read argument. */
    public void setByteNumber(int byteNumber) {
        this.byteNumber = byteNumber;
        if (CodelessLibConfig.CHECK_SPI_READ_SIZE && byteNumber > CodelessLibConfig.SPI_MAX_BYTE_READ_SIZE)
            invalid = true;
    }

    /** Returns the read data response. */
    public int[] getData() {
        return data;
    }
}
