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
 * <code>AT+BAUD</code> command implementation.
 * @see CodelessEvent.BaudRate
 * @see <a href="https://lpccs-docs.renesas.com/UM-140-DA145x-CodeLess/index.html">AT commands documentation</a>
 */
public class BaudRateCommand extends CodelessCommand {
    public static final String TAG = "BaudRateCommand";

    public static final String COMMAND = "BAUD";
    public static final String NAME = CodelessProfile.PREFIX_LOCAL + COMMAND;
    public static final CommandID ID = CommandID.BAUD;

    public static final String PATTERN_STRING = "^BAUD(?:=(\\d+))?$"; // <baud>
    public static final Pattern PATTERN = Pattern.compile(PATTERN_STRING);

    /** The baud rate argument/response. */
    private int baudRate;
    /** <code>true</code> if the command has arguments, <code>false</code> for no arguments. */
    private boolean hasArguments;

    /**
     * Creates an <code>AT+BAUD</code> command.
     * @param manager   the associated manager
     * @param baudRate  the baud rate argument
     */
    public BaudRateCommand(CodelessManager manager, int baudRate) {
        this(manager);
        setBaudRate(baudRate);
        hasArguments = true;
    }

    /**
     * Default constructor.
     * @see CodelessCommand#CodelessCommand(CodelessManager)
     */
    public BaudRateCommand(CodelessManager manager) {
        super(manager);
    }

    /**
     * Default constructor.
     * @see CodelessCommand#CodelessCommand(CodelessManager, String, boolean)
     */
    public BaudRateCommand(CodelessManager manager, String command, boolean parse) {
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
        return hasArguments;
    }

    @Override
    protected String getArguments() {
        return hasArguments ? Integer.toString(baudRate) : null;
    }

    @Override
    public void parseResponse(String response) {
        super.parseResponse(response);
        if (responseLine() == 1) {
            try {
                baudRate = Integer.parseInt(response);
                if (!validBaudRate(baudRate))
                    invalid = true;
            } catch (NumberFormatException e) {
                invalid = true;
            }
            if (invalid)
                Log.e(TAG, "Received invalid baud rate: " + response);
            else if (CodelessLibLog.COMMAND)
                Log.d(TAG, "Baud rate: " + baudRate);
        }
    }

    @Override
    public void onSuccess() {
        super.onSuccess();
        if (isValid())
            EventBus.getDefault().post(new CodelessEvent.BaudRate(this));
    }

    @Override
    protected boolean checkArgumentsCount() {
        int count = CodelessProfile.countArguments(command, ",");
        return count == 0 || count == 1;
    }

    @Override
    protected String parseArguments() {
        if (!CodelessProfile.hasArguments(command))
            return null;
        hasArguments = true;

        Integer num = decodeNumberArgument(1);
        if (num == null || !validBaudRate(num))
            return "Invalid baud rate";
        baudRate = num;

        return null;
    }

    /** Returns the baud rate argument/response. */
    public int getBaudRate() {
        return baudRate;
    }

    /** Sets the baud rate argument. */
    public void setBaudRate(int baudRate) {
        this.baudRate = baudRate;
        if (!validBaudRate(baudRate))
            invalid = true;
    }

    /** Checks if the baud rate value is valid. */
    private boolean validBaudRate(int baudRate) {
        return baudRate == Command.BAUD_RATE_2400
                || baudRate == Command.BAUD_RATE_4800
                || baudRate == Command.BAUD_RATE_9600
                || baudRate == Command.BAUD_RATE_19200
                || baudRate == Command.BAUD_RATE_38400
                || baudRate == Command.BAUD_RATE_57600
                || baudRate == Command.BAUD_RATE_115200
                || baudRate == Command.BAUD_RATE_230400;
    }
}
