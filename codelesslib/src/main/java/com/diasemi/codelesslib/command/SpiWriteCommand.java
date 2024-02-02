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

import com.diasemi.codelesslib.CodelessLibConfig;
import com.diasemi.codelesslib.CodelessManager;
import com.diasemi.codelesslib.CodelessProfile;
import com.diasemi.codelesslib.CodelessProfile.CommandID;

import java.util.regex.Pattern;

/**
 * <code>AT+SPIWR</code> command implementation.
 * @see <a href="https://lpccs-docs.renesas.com/UM-140-DA145x-CodeLess/index.html">AT commands documentation</a>
 */
public class SpiWriteCommand extends CodelessCommand {
    public static final String TAG = "SpiWriteCommand";

    public static final String COMMAND = "SPIWR";
    public static final String NAME = CodelessProfile.PREFIX_LOCAL + COMMAND;
    public static final CommandID ID = CommandID.SPIWR;

    public static final String PATTERN_STRING = "^SPIWR=(?:0[xX])?([0-9a-fA-F]+)$"; // <hexString>
    public static final Pattern PATTERN = Pattern.compile(PATTERN_STRING);

    /** The data to write argument (byte array as hex string). */
    private String hexString;

    /**
     * Creates an <code>AT+SPIWR</code> command.
     * @param manager   the associated manager
     * @param hexString the data to write argument (byte array as hex string)
     */
    public SpiWriteCommand(CodelessManager manager, String hexString) {
        this(manager);
        setHexString(hexString);
    }

    /**
     * Default constructor.
     * @see CodelessCommand#CodelessCommand(CodelessManager)
     */
    public SpiWriteCommand(CodelessManager manager) {
        super(manager);
    }

    /**
     * Default constructor.
     * @see CodelessCommand#CodelessCommand(CodelessManager, String, boolean)
     */
    public SpiWriteCommand(CodelessManager manager, String command, boolean parse) {
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
        return hexString;
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
        String hexString = matcher.group(1);
        if (CodelessLibConfig.CHECK_SPI_HEX_STRING_WRITE) {
            if (hexString == null || hexString.length() == 0)
                return "Invalid hex string";
            int charSize = hexString.length();
            if (hexString.startsWith("0x") || hexString.startsWith("0X"))
                charSize -= 2;
            if (charSize < CodelessLibConfig.SPI_HEX_STRING_CHAR_SIZE_MIN || charSize > CodelessLibConfig.SPI_HEX_STRING_CHAR_SIZE_MAX)
                return "Invalid hex string";
        }
        this.hexString = hexString;

        return null;
    }

    /** Returns the data to write argument (byte array as hex string). */
    public String getHexString() {
        return hexString;
    }

    /** Sets the data to write argument (byte array as hex string). */
    public void setHexString(String hexString) {
        this.hexString = hexString;
        if (CodelessLibConfig.CHECK_SPI_HEX_STRING_WRITE) {
            if (hexString == null) {
                invalid = true;
                return;
            }
            int charSize = hexString.length();
            if (hexString.startsWith("0x") || hexString.startsWith("0X"))
                charSize -= 2;
            if (charSize < CodelessLibConfig.SPI_HEX_STRING_CHAR_SIZE_MIN || charSize > CodelessLibConfig.SPI_HEX_STRING_CHAR_SIZE_MAX)
                invalid = true;
        }
    }
}
