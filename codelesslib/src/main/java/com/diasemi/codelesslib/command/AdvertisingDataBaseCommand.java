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

import com.diasemi.codelesslib.CodelessManager;
import com.diasemi.codelesslib.CodelessProfile;
import com.diasemi.codelesslib.CodelessUtil;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Base class with common implementation of <code>AT+ADVDATA</code> and <code>AT+ADVRESP</code> commands.
 * @see AdvertisingDataCommand
 * @see AdvertisingResponseCommand
 * @see <a href="https://lpccs-docs.renesas.com/UM-140-DA145x-CodeLess/index.html">AT commands documentation</a>
 */
public abstract class AdvertisingDataBaseCommand extends CodelessCommand {

    protected static final String DATA_PATTERN_STRING = "(?:[0-9a-fA-F]{2}:)*[0-9a-fA-F]{2}";

    protected static final String RESPONSE_PATTERN_STRING = "^(" + DATA_PATTERN_STRING + ")$"; // <data>
    protected static final Pattern RESPONSE_PATTERN = Pattern.compile(RESPONSE_PATTERN_STRING);

    protected static final String DATA_ARGUMENT_PATTERN_STRING = "^(?:" + DATA_PATTERN_STRING + ")?$";
    protected static final Pattern DATA_ARGUMENT_PATTERN = Pattern.compile(DATA_ARGUMENT_PATTERN_STRING);

    /**
     * Checks if an advertising data hex string is valid.
     * @param data the advertising data hex string
     */
    public static boolean validData(String data) {
        return DATA_ARGUMENT_PATTERN.matcher(data).matches();
    }

    /** The advertising or scan response data argument/response. */
    protected byte[] data;
    /** <code>true</code> if the command has arguments, <code>false</code> for no arguments. */
    protected boolean hasArguments;

    /**
     * Creates an <code>AT+ADVDATA</code> or <code>AT+ADVRESP</code> command.
     * @param manager   the associated manager
     * @param data      the advertising or scan response data argument
     */
    public AdvertisingDataBaseCommand(CodelessManager manager, byte[] data) {
        this(manager);
        this.data = data;
        hasArguments = true;
    }

    /**
     * Default constructor.
     * @see CodelessCommand#CodelessCommand(CodelessManager)
     */
    public AdvertisingDataBaseCommand(CodelessManager manager) {
        super(manager);
    }

    /**
     * Default constructor.
     * @see CodelessCommand#CodelessCommand(CodelessManager, String, boolean)
     */
    public AdvertisingDataBaseCommand(CodelessManager manager, String command, boolean parse) {
        super(manager, command, parse);
    }

    @Override
    protected boolean hasArguments() {
        return hasArguments;
    }

    @Override
    protected String getArguments() {
        return hasArguments ? CodelessUtil.hexArray(data, true, false).replace(" ", ":") : null;
    }

    @Override
    public void parseResponse(String response) {
        super.parseResponse(response);
        if (responseLine() == 1) {
            Matcher matcher = RESPONSE_PATTERN.matcher(response);
            if (matcher.matches()) {
                data = CodelessUtil.hex2bytes(response);
                if (data == null)
                    invalid = true;
            } else {
                invalid = true;
            }
        }
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

        if (DATA_ARGUMENT_PATTERN.matcher(matcher.group(1)).matches()) {
            data = CodelessUtil.hex2bytes(matcher.group(1));
        } else {
            return "Invalid advertising data";
        }

        return null;
    }

    /** Returns the advertising or scan response data argument/response. */
    public byte[] getData() {
        return data;
    }

    /** Sets the advertising or scan response data argument. */
    public void setData(byte[] data) {
        this.data = data;
    }

    /** Returns the advertising or scan response data argument/response as a hex string. */
    public String getDataString() {
        return CodelessUtil.hexArray(data, true, false);
    }
}
