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

import com.diasemi.codelesslib.CodelessEvent;
import com.diasemi.codelesslib.CodelessManager;
import com.diasemi.codelesslib.CodelessProfile;
import com.diasemi.codelesslib.CodelessProfile.CommandID;

import org.greenrobot.eventbus.EventBus;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * <code>AT+I2CCFG</code> command implementation.
 * @see CodelessEvent.I2cConfig
 * @see <a href="https://lpccs-docs.renesas.com/UM-140-DA145x-CodeLess/index.html">AT commands documentation</a>
 */
public class I2cConfigCommand extends CodelessCommand {
    public static final String TAG = "I2cConfigCommand";

    public static final String COMMAND = "I2CCFG";
    public static final String NAME = CodelessProfile.PREFIX_LOCAL + COMMAND;
    public static final CommandID ID = CommandID.I2CCFG;

    public static final String PATTERN_STRING = "^I2CCFG=(\\d+),(\\d+),(\\d+)$"; // <count> <rate> <width>
    public static final Pattern PATTERN = Pattern.compile(PATTERN_STRING);

    /** The I2C address bit-count argument. */
    private int bitCount;
    /** The I2C bus bitrate argument (KHz). */
    private int bitRate;
    /** The I2C register bit-count argument. */
    private int registerWidth;

    /**
     * Creates an <code>AT+I2CCFG</code> command.
     * @param manager       the associated manager
     * @param bitCount      the I2C address bit-count argument
     * @param bitRate       the I2C bus bitrate argument (KHz)
     * @param registerWidth the I2C register bit-count argument
     */
    public I2cConfigCommand(CodelessManager manager, int bitCount, int bitRate, int registerWidth) {
        this(manager);
        this.bitCount = bitCount;
        this.bitRate = bitRate;
        this.registerWidth = registerWidth;
    }

    /**
     * Default constructor.
     * @see CodelessCommand#CodelessCommand(CodelessManager)
     */
    public I2cConfigCommand(CodelessManager manager) {
        super(manager);
    }

    /**
     * Default constructor.
     * @see CodelessCommand#CodelessCommand(CodelessManager, String, boolean)
     */
    public I2cConfigCommand(CodelessManager manager, String command, boolean parse) {
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
        return String.format(Locale.US, "%d,%d,%d", bitCount, bitRate, registerWidth);
    }

    @Override
    public void onSuccess() {
        super.onSuccess();
        if (isValid())
            EventBus.getDefault().post(new CodelessEvent.I2cConfig(this));
    }

    @Override
    protected boolean requiresArguments() {
        return true;
    }

    @Override
    protected boolean checkArgumentsCount() {
        return CodelessProfile.countArguments(command, ",") == 3;
    }

    @Override
    protected String parseArguments() {
        Integer num = decodeNumberArgument(1);
        if (num == null)
            return "Invalid slave addressing bit count";
        bitCount = num;

        num = decodeNumberArgument(2);
        if (num == null)
            return "Invalid bit rate";
        bitRate = num;

        num = decodeNumberArgument(3);
        if (num == null)
            return "Invalid register width";
        registerWidth = num;

        return null;
    }

    /** Returns the I2C address bit-count argument. */
    public int getBitCount() {
        return bitCount;
    }

    /** Sets the I2C address bit-count argument. */
    public void setBitCount(int bitCount) {
        this.bitCount = bitCount;
    }

    /** Returns the I2C bus bitrate argument (KHz). */
    public int getBitRate() {
        return bitRate;
    }

    /** Sets the I2C bus bitrate argument (KHz). */
    public void setBitRate(int bitRate) {
        this.bitRate = bitRate;
    }

    /** Returns the I2C register bit-count argument. */
    public int getRegisterWidth() {
        return registerWidth;
    }

    /** Sets the I2C register bit-count argument. */
    public void setRegisterWidth(int registerWidth) {
        this.registerWidth = registerWidth;
    }
}
