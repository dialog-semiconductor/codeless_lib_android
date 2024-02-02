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
 * <code>AT+PWRLVL</code> command implementation.
 * @see CodelessEvent.PowerLevel
 * @see <a href="https://lpccs-docs.renesas.com/UM-140-DA145x-CodeLess/index.html">AT commands documentation</a>
 */
public class PowerLevelConfigCommand extends CodelessCommand {
    public static final String TAG = "PowerLevelConfigCommand";

    public static final String COMMAND = "PWRLVL";
    public static final String NAME = CodelessProfile.PREFIX_LOCAL + COMMAND;
    public static final CommandID ID = CommandID.PWRLVL;

    public static final String PATTERN_STRING = "^PWRLVL(?:=(\\d+))?$"; // <level>
    public static final Pattern PATTERN = Pattern.compile(PATTERN_STRING);

    /** The Bluetooth output power level {@link CodelessProfile.Command#OUTPUT_POWER_LEVEL_MINUS_19_POINT_5_DBM index} argument/response. */
    private int powerLevel;
    /** <code>true</code> if the response indicates that power level configuration is not supported by the peer device. */
    private boolean notSupported;
    /** <code>true</code> if the command has arguments, <code>false</code> for no arguments. */
    private boolean hasArguments;

    /**
     * Creates an <code>AT+PWRLVL</code> command.
     * @param manager       the associated manager
     * @param powerLevel    the Bluetooth output power level {@link CodelessProfile.Command#OUTPUT_POWER_LEVEL_MINUS_19_POINT_5_DBM index} argument
     */
    public PowerLevelConfigCommand(CodelessManager manager, int powerLevel) {
        this(manager);
        setPowerLevel(powerLevel);
        hasArguments = true;
    }

    /**
     * Default constructor.
     * @see CodelessCommand#CodelessCommand(CodelessManager)
     */
    public PowerLevelConfigCommand(CodelessManager manager) {
        super(manager);
    }

    /**
     * Default constructor.
     * @see CodelessCommand#CodelessCommand(CodelessManager, String, boolean)
     */
    public PowerLevelConfigCommand(CodelessManager manager, String command, boolean parse) {
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
        return hasArguments ? Integer.toString(powerLevel) : null;
    }

    @Override
    protected boolean requiresArguments() {
        return true;
    }

    @Override
    protected boolean checkArgumentsCount() {
        int count = CodelessProfile.countArguments(command, ",");
        return count == 0 || count == 1;
    }

    @Override
    public void parseResponse(String response) {
        super.parseResponse(response);
        if (responseLine() == 1) {
            if (!response.equals(Command.OUTPUT_POWER_LEVEL_NOT_SUPPORTED)) {
                try {
                    setPowerLevel(Integer.parseInt(response));
                } catch (NumberFormatException e) {
                    invalid = true;
                }
                if (invalid)
                    Log.e(TAG, "Received invalid power level: " + response);
                else if (CodelessLibLog.COMMAND)
                    Log.d(TAG, "Power level: " + powerLevel);
            } else {
                Log.d(TAG, "Power level not supported");
                notSupported = true;
            }
        }
    }

    @Override
    public void onSuccess() {
        super.onSuccess();
        if (isValid())
            EventBus.getDefault().post(new CodelessEvent.PowerLevel(this));
    }

    @Override
    protected String parseArguments() {
        if (!CodelessProfile.hasArguments(command))
            return null;
        hasArguments = true;

        Integer num = decodeNumberArgument(1);
        if (num == null || !validPowerLevel(num))
            return "Invalid power level";
        powerLevel = num;

        return null;
    }

    /** Returns the Bluetooth output power level {@link CodelessProfile.Command#OUTPUT_POWER_LEVEL_MINUS_19_POINT_5_DBM index} argument/response. */
    public int getPowerLevel() {
        return powerLevel;
    }

    /** Sets the Bluetooth output power level {@link CodelessProfile.Command#OUTPUT_POWER_LEVEL_MINUS_19_POINT_5_DBM index} argument. */
    public void setPowerLevel(int powerLevel) {
        this.powerLevel = powerLevel;
        if (!validPowerLevel(powerLevel))
            invalid = true;
    }

    /** Checks if power level configuration is not supported by the peer device. */
    public boolean notSupported() {
        return notSupported;
    }

    /** Checks if a Bluetooth output power level index is valid. */
    private boolean validPowerLevel(int powerLevel) {
        return powerLevel == Command.OUTPUT_POWER_LEVEL_MINUS_19_POINT_5_DBM
                || powerLevel == Command.OUTPUT_POWER_LEVEL_MINUS_13_POINT_5_DBM
                || powerLevel == Command.OUTPUT_POWER_LEVEL_MINUS_10_DBM
                || powerLevel == Command.OUTPUT_POWER_LEVEL_MINUS_7_DBM
                || powerLevel == Command.OUTPUT_POWER_LEVEL_MINUS_5_DBM
                || powerLevel == Command.OUTPUT_POWER_LEVEL_MINUS_3_POINT_5_DBM
                || powerLevel == Command.OUTPUT_POWER_LEVEL_MINUS_2_DBM
                || powerLevel == Command.OUTPUT_POWER_LEVEL_MINUS_1_DBM
                || powerLevel == Command.OUTPUT_POWER_LEVEL_0_DBM
                || powerLevel == Command.OUTPUT_POWER_LEVEL_1_DBM
                || powerLevel == Command.OUTPUT_POWER_LEVEL_1_POINT_5_DBM
                || powerLevel == Command.OUTPUT_POWER_LEVEL_2_POINT_5_DBM;
    }
}
