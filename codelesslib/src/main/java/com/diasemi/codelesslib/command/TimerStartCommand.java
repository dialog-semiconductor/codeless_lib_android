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

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * <code>AT+TMRSTART</code> command implementation.
 * @see <a href="https://lpccs-docs.renesas.com/UM-140-DA145x-CodeLess/index.html">AT commands documentation</a>
 */
public class TimerStartCommand extends CodelessCommand {
    public static final String TAG = "TimerStartCommand";

    public static final String COMMAND = "TMRSTART";
    public static final String NAME = CodelessProfile.PREFIX_LOCAL + COMMAND;
    public static final CommandID ID = CommandID.TMRSTART;

    public static final String PATTERN_STRING = "^TMRSTART=(\\d+),(\\d+),(\\d+)$"; // <timer> <command> <delay>
    public static final Pattern PATTERN = Pattern.compile(PATTERN_STRING);

    /** The timer index argument (0-3). */
    private int timerIndex;
    /** The command slot index argument (0-3). */
    private int commandIndex;
    /** The timer delay argument (multiples of 10 ms). */
    private int delay;

    /**
     * Creates an <code>AT+TMRSTART</code> command.
     * @param manager       the associated manager
     * @param timerIndex    the timer index argument (0-3)
     * @param commandIndex  the command slot index argument (0-3)
     * @param delay         the timer delay argument (ms)
     */
    public TimerStartCommand(CodelessManager manager, int timerIndex, int commandIndex, int delay) {
        this(manager);
        setTimerIndex(timerIndex);
        setCommandIndex(commandIndex);
        setDelay(delay);
    }

    /**
     * Default constructor.
     * @see CodelessCommand#CodelessCommand(CodelessManager)
     */
    public TimerStartCommand(CodelessManager manager) {
        super(manager);
    }

    /**
     * Default constructor.
     * @see CodelessCommand#CodelessCommand(CodelessManager, String, boolean)
     */
    public TimerStartCommand(CodelessManager manager, String command, boolean parse) {
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
        return String.format(Locale.US, "%d,%d,%d", timerIndex, commandIndex, delay);
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
        if (num == null || CodelessLibConfig.CHECK_TIMER_INDEX && (num < CodelessLibConfig.TIMER_INDEX_MIN || num > CodelessLibConfig.TIMER_INDEX_MAX))
            return "Invalid timer index";
        timerIndex = num;

        num = decodeNumberArgument(2);
        if (num == null || CodelessLibConfig.CHECK_COMMAND_INDEX && (num < CodelessLibConfig.COMMAND_INDEX_MIN || num > CodelessLibConfig.COMMAND_INDEX_MAX))
            return "Invalid command index";
        commandIndex = num;

        num = decodeNumberArgument(3);
        if (num == null)
            return "Invalid delay";
        delay = num;

        return null;
    }

    /** Returns the timer index argument (0-3). */
    public int getTimerIndex() {
        return timerIndex;
    }

    /** Sets the timer index argument (0-3). */
    public void setTimerIndex(int timerIndex) {
        this.timerIndex = timerIndex;
        if (CodelessLibConfig.CHECK_TIMER_INDEX) {
            if (timerIndex < CodelessLibConfig.TIMER_INDEX_MIN || timerIndex > CodelessLibConfig.TIMER_INDEX_MAX)
                invalid = true;
        }
    }

    /** Returns the command slot index argument (0-3). */
    public int getCommandIndex() {
        return commandIndex;
    }

    /** Sets the command slot index argument (0-3). */
    public void setCommandIndex(int commandIndex) {
        this.commandIndex = commandIndex;
        if (CodelessLibConfig.CHECK_COMMAND_INDEX) {
            if (commandIndex < CodelessLibConfig.COMMAND_INDEX_MIN || commandIndex > CodelessLibConfig.COMMAND_INDEX_MAX)
                invalid = true;
        }
    }

    /** Returns the timer delay argument (ms). */
    public int getDelay() {
        return delay * 10;
    }

    /** Sets the timer delay argument (ms). */
    public void setDelay(int delay) {
        this.delay = delay / 10;
        if (delay % 10 != 0)
            this.delay++;
    }
}
