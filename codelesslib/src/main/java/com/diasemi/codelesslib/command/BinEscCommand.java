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

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.diasemi.codelesslib.CodelessProfile.Command.BINESC_TIME_AFTER_DEFAULT;
import static com.diasemi.codelesslib.CodelessProfile.Command.BINESC_TIME_PRIOR_DEFAULT;

/**
 * <code>AT+BINESC</code> command implementation.
 * @see CodelessEvent.BinEsc
 * @see <a href="https://lpccs-docs.renesas.com/UM-140-DA145x-CodeLess/index.html">AT commands documentation</a>
 */
public class BinEscCommand extends CodelessCommand {
    public static final String TAG = "BinEscCommand";

    public static final String COMMAND = "BINESC";
    public static final String NAME = CodelessProfile.PREFIX_LOCAL + COMMAND;
    public static final CommandID ID = CommandID.BINESC;

    public static final String PATTERN_STRING = "^BINESC(?:=(\\d+),(0[xX][0-9a-fA-F]{1,6}|\\d+),(\\d+))?$"; // <time_prior> <seq> <time_after>
    public static final Pattern PATTERN = Pattern.compile(PATTERN_STRING);

    private static final String RESPONSE_PATTERN_STRING = "^(\\d+).([0-9a-fA-F]{1,6}).(\\d+)$"; // <time_prior> <seq> <time_after>
    private static final Pattern RESPONSE_PATTERN = Pattern.compile(RESPONSE_PATTERN_STRING);

    /** The 3-byte escape sequence argument/response (24-bit). */
    private int sequence;
    /** The idle time before the escape sequence argument/response (ms). */
    private int timePrior;
    /** The idle time after the escape sequence argument/response (ms). */
    private int timeAfter;
    /** <code>true</code> if the command has arguments, <code>false</code> for no arguments. */
    private boolean hasArguments;

    /**
     * Creates an <code>AT+BINESC</code> command.
     * @param manager   the associated manager
     * @param sequence  the 3-byte escape sequence argument (24-bit)
     * @param timePrior the idle time before the escape sequence argument (ms)
     * @param timeAfter the idle time after the escape sequence argument (ms)
     */
    public BinEscCommand(CodelessManager manager, int sequence, int timePrior, int timeAfter) {
        this(manager);
        setSequence(sequence);
        setTimePrior(timePrior);
        setTimeAfter(timeAfter);
        hasArguments = true;
    }

    /**
     * Creates an <code>AT+BINESC</code> command.
     * @param manager   the associated manager
     * @param sequence  the 3-byte escape sequence argument (24-bit)
     */
    public BinEscCommand(CodelessManager manager, int sequence) {
        this(manager, sequence, BINESC_TIME_PRIOR_DEFAULT, BINESC_TIME_AFTER_DEFAULT);
    }

    /**
     * Default constructor.
     * @see CodelessCommand#CodelessCommand(CodelessManager)
     */
    public BinEscCommand(CodelessManager manager) {
        super(manager);
    }

    /**
     * Default constructor.
     * @see CodelessCommand#CodelessCommand(CodelessManager, String, boolean)
     */
    public BinEscCommand(CodelessManager manager, String command, boolean parse) {
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
        return hasArguments ? String.format(Locale.US, "%d,%#x,%d", timePrior, sequence, timeAfter) : null;
    }

    @Override
    public void parseResponse(String response) {
        super.parseResponse(response);
        if (responseLine() == 1) {
            Matcher matcher = RESPONSE_PATTERN.matcher(response);
            if (matcher.matches()) {
                try {
                    timePrior = Integer.parseInt(matcher.group(1));
                    sequence = Integer.parseInt(matcher.group(2), 16);
                    timeAfter = Integer.parseInt(matcher.group(3));
                } catch (NumberFormatException e) {
                    invalid = true;
                }
            } else {
                invalid = true;
            }
            if (sequence > 0xffffff || timePrior > 0xffff || timeAfter > 0xffff)
                invalid = true;
            if (invalid)
                Log.e(TAG, "Received invalid escape parameters: " + response);
            else if (CodelessLibLog.COMMAND)
                Log.d(TAG, "Escape sequence: 0x" + Integer.toHexString(sequence) + " (\"" + getSequenceString() + "\") time=" + timePrior + "," + timeAfter);
        }
    }

    @Override
    public void onSuccess() {
        super.onSuccess();
        if (isValid())
            EventBus.getDefault().post(new CodelessEvent.BinEsc(this));
    }

    @Override
    protected boolean checkArgumentsCount() {
        int count = CodelessProfile.countArguments(command, ",");
        return count == 0 || count == 3;
    }

    @Override
    protected String parseArguments() {
        if (!CodelessProfile.hasArguments(command))
            return null;
        hasArguments = true;

        Integer num = decodeNumberArgument(1);
        if (num == null || num > 0xffff)
            return "Invalid escape time prior";
        timePrior = num;

        num = decodeNumberArgument(2);
        if (num == null || num > 0xffffff)
            return "Invalid escape sequence";
        sequence = num;

        num = decodeNumberArgument(3);
        if (num == null || num > 0xffff)
            return "Invalid escape time after";
        timeAfter = num;

        return null;
    }

    /** Returns the escape sequence argument/response as text. */
    public String getSequenceString() {
        byte[] sequenceBytes;
        if (sequence > 0xffff)
            sequenceBytes = new byte[] { (byte) sequence, (byte) (sequence >>> 8), (byte) (sequence >>> 16) };
        else if (sequence > 0xff)
            sequenceBytes = new byte[] { (byte) sequence, (byte) (sequence >>> 8) };
        else
            sequenceBytes = new byte[] { (byte) sequence };
        return new String(sequenceBytes, StandardCharsets.US_ASCII);
    }

    /** Returns the 3-byte escape sequence argument/response (24-bit). */
    public int getSequence() {
        return sequence;
    }

    /** Sets the 3-byte escape sequence argument (24-bit). */
    public void setSequence(int sequence) {
        this.sequence = sequence & 0xffffff;
    }

    /** Returns the idle time before the escape sequence argument/response (ms). */
    public int getTimePrior() {
        return timePrior;
    }

    /** Sets the idle time before the escape sequence argument (ms). */
    public void setTimePrior(int timePrior) {
        this.timePrior = timePrior & 0xffff;
    }

    /** Returns the idle time after the escape sequence argument/response (ms). */
    public int getTimeAfter() {
        return timeAfter;
    }

    /** Sets the idle time after the escape sequence argument (ms). */
    public void setTimeAfter(int timeAfter) {
        this.timeAfter = timeAfter & 0xffff;
    }
}
