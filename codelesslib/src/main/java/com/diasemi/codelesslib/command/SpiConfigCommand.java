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
import com.diasemi.codelesslib.CodelessProfile.Command;
import com.diasemi.codelesslib.CodelessProfile.CommandID;

import org.greenrobot.eventbus.EventBus;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * <code>AT+SPICFG</code> command implementation.
 * @see CodelessEvent.SpiConfig
 * @see <a href="https://lpccs-docs.renesas.com/UM-140-DA145x-CodeLess/index.html">AT commands documentation</a>
 */
public class SpiConfigCommand extends CodelessCommand {
    public static final String TAG = "SpiConfigCommand";

    public static final String COMMAND = "SPICFG";
    public static final String NAME = CodelessProfile.PREFIX_LOCAL + COMMAND;
    public static final CommandID ID = CommandID.SPICFG;

    public static final String PATTERN_STRING = "^SPICFG(?:=(\\d+),(\\d+),(\\d+))?$"; // <speed> <mode> <size>
    public static final Pattern PATTERN = Pattern.compile(PATTERN_STRING);

    /** The SPI clock value argument/response (0: 2 MHz, 1: 4 MHz, 2: 8 MHz). */
    private int speed;
    /** The SPI mode argument/response (clock polarity and phase). */
    private int mode;
    /** The SPI word bit-count argument/response. */
    private int size;
    /** <code>true</code> if the command has arguments, <code>false</code> for no arguments. */
    private boolean hasArguments;

    /**
     * Creates an <code>AT+SPICFG</code> command.
     * @param manager   the associated manager
     * @param speed     the SPI clock value argument (0: 2 MHz, 1: 4 MHz, 2: 8 MHz)
     * @param mode      the SPI mode argument (clock polarity and phase)
     * @param size      the SPI word bit-count argument
     */
    public SpiConfigCommand(CodelessManager manager, int speed, int mode, int size) {
        this(manager);
        setSpeed(speed);
        setMode(mode);
        setSize(size);
        hasArguments = true;
    }

    /**
     * Default constructor.
     * @see CodelessCommand#CodelessCommand(CodelessManager)
     */
    public SpiConfigCommand(CodelessManager manager) {
        super(manager);
    }

    /**
     * Default constructor.
     * @see CodelessCommand#CodelessCommand(CodelessManager, String, boolean)
     */
    public SpiConfigCommand(CodelessManager manager, String command, boolean parse) {
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
        return hasArguments ? String.format(Locale.US, "%d,%d,%d", speed, mode, size) : null;
    }

    @Override
    public void parseResponse(String response) {
        super.parseResponse(response);
        if (responseLine() == 1) {
            try {
                setSpeed(Integer.parseInt(matcher.group(0)));
                setMode(Integer.parseInt(matcher.group(1)));
                setSize(Integer.parseInt(matcher.group(2)));
            } catch (NumberFormatException e) {
                invalid = true;
            }
            if (invalid)
                Log.e(TAG, "Received invalid SPI configuration: " + response);
            else if (CodelessLibLog.COMMAND)
                Log.d(TAG, "SPI configuration: speed=" + speed + " mode=" + mode + " size=" + size);
        }
    }

    @Override
    public void onSuccess() {
        super.onSuccess();
        if (isValid())
            EventBus.getDefault().post(new CodelessEvent.SpiConfig(this));
    }

    @Override
    protected boolean checkArgumentsCount() {
        int count = CodelessProfile.countArguments(command, ",") ;
        return count == 0 || count == 3;
    }

    @Override
    protected String parseArguments() {
        if (!CodelessProfile.hasArguments(command))
            return null;
        hasArguments = true;

        Integer num = decodeNumberArgument(1);
        if (num == null || (num != Command.SPI_CLOCK_VALUE_2_MHZ && num != Command.SPI_CLOCK_VALUE_4_MHZ && num != Command.SPI_CLOCK_VALUE_8_MHZ))
            return "Invalid SPI clock value";
        speed = num;

        num = decodeNumberArgument(2);
        if (num == null || (num != Command.SPI_MODE_0 && num != Command.SPI_MODE_1 && num != Command.SPI_MODE_2 && num != Command.SPI_MODE_3))
            return "Invalid SPI mode";
        mode = num;

        num = decodeNumberArgument(3);
        if (num == null || CodelessLibConfig.CHECK_SPI_WORD_SIZE && num != CodelessLibConfig.SPI_WORD_SIZE)
            return "Invalid SPI word size";
        size = num;

        return null;
    }

    /** Returns the SPI clock value argument/response (0: 2 MHz, 1: 4 MHz, 2: 8 MHz). */
    public int getSpeed() {
        return speed;
    }

    /** Sets the SPI clock value argument (0: 2 MHz, 1: 4 MHz, 2: 8 MHz). */
    public void setSpeed(int speed) {
        this.speed = speed;
        if (speed != Command.SPI_CLOCK_VALUE_2_MHZ && speed != Command.SPI_CLOCK_VALUE_4_MHZ && speed != Command.SPI_CLOCK_VALUE_8_MHZ)
            invalid = true;
    }

    /** Returns the SPI mode argument/response (clock polarity and phase). */
    public int getMode() {
        return mode;
    }

    /** Sets the SPI mode argument (clock polarity and phase). */
    public void setMode(int mode) {
        this.mode = mode;
        if (mode != Command.SPI_MODE_0 && mode != Command.SPI_MODE_1 && mode != Command.SPI_MODE_2 && mode != Command.SPI_MODE_3)
            invalid = true;
    }

    /** Returns the SPI word bit-count argument/response. */
    public int getSize() {
        return size;
    }

    /** Sets the SPI word bit-count argument. */
    public void setSize(int size) {
        this.size = size;
        if (CodelessLibConfig.CHECK_SPI_WORD_SIZE && size != CodelessLibConfig.SPI_WORD_SIZE)
            invalid = true;
    }
}
