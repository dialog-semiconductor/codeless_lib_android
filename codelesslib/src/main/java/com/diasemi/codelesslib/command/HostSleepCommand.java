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

import java.util.Locale;
import java.util.regex.Pattern;

import static com.diasemi.codelesslib.CodelessProfile.Command.HOST_SLEEP_MODE_0;
import static com.diasemi.codelesslib.CodelessProfile.Command.HOST_SLEEP_MODE_1;

/**
 * <code>AT+HOSTSLP</code> command implementation.
 * @see CodelessEvent.HostSleep
 * @see <a href="https://lpccs-docs.renesas.com/UM-140-DA145x-CodeLess/index.html">AT commands documentation</a>
 */
public class HostSleepCommand extends CodelessCommand {
    public static final String TAG = "HostSleepCommand";

    public static final String COMMAND = "HOSTSLP";
    public static final String NAME = CodelessProfile.PREFIX_LOCAL + COMMAND;
    public static final CommandID ID = CommandID.HOSTSLP;

    public static final String PATTERN_STRING = "^HOSTSLP(?:=(\\d+),(\\d+),(\\d+),(\\d+))?$"; // <hst_slp_mode> <wkup_byte> <wkup_retry_interval> <wkup_retry_times>
    public static final Pattern PATTERN = Pattern.compile(PATTERN_STRING);

    /** The host sleep mode argument/response. */
    private int hostSleepMode;
    /** The wakeup byte argument/response. */
    private int wakeupByte;
    /** The wakeup retry interval argument/response (ms). */
    private int wakeupRetryInterval;
    /** The wakeup retry times argument/response. */
    private int wakeupRetryTimes;
    /** <code>true</code> if the command has arguments, <code>false</code> for no arguments. */
    private boolean hasArguments;

    /**
     * Creates an <code>AT+HOSTSLP</code> command.
     * @param manager               the associated manager
     * @param hostSleepMode         the host sleep mode argument
     * @param wakeupByte            the wakeup byte argument
     * @param wakeupRetryInterval   the wakeup retry interval argument (ms)
     * @param wakeupRetryTimes      the wakeup retry times argument
     */
    public HostSleepCommand(CodelessManager manager, int hostSleepMode, int wakeupByte, int wakeupRetryInterval, int wakeupRetryTimes) {
        super(manager);
        setHostSleepMode(hostSleepMode);
        this.wakeupByte = wakeupByte;
        this.wakeupRetryInterval = wakeupRetryInterval;
        this.wakeupRetryTimes = wakeupRetryTimes;
        hasArguments = true;
    }

    /**
     * Default constructor.
     * @see CodelessCommand#CodelessCommand(CodelessManager)
     */
    public HostSleepCommand(CodelessManager manager) {
        super(manager);
    }

    /**
     * Default constructor.
     * @see CodelessCommand#CodelessCommand(CodelessManager, String, boolean)
     */
    public HostSleepCommand(CodelessManager manager, String command, boolean parse) {
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
        return hasArguments ? String.format(Locale.US, "%d,%d,%d,%d", hostSleepMode, wakeupByte, wakeupRetryInterval, wakeupRetryTimes) : null;
    }

    @Override
    public void parseResponse(String response) {
        super.parseResponse(response);
        if (responseLine() == 1) {
            try {
                String[] parameters = response.split(" ");
                hostSleepMode = Integer.parseInt(parameters[0]);
                if (hostSleepMode != HOST_SLEEP_MODE_0 && hostSleepMode != HOST_SLEEP_MODE_1)
                    invalid = true;
                wakeupByte = Integer.parseInt(parameters[1]);
                wakeupRetryInterval = Integer.parseInt(parameters[2]);
                wakeupRetryTimes = Integer.parseInt(parameters[3]);
            } catch (IndexOutOfBoundsException | NumberFormatException e) {
                invalid = true;
            }
            if (invalid)
                Log.e(TAG, "Received invalid host sleep response");
            else if (CodelessLibLog.COMMAND)
                Log.d(TAG, "Host sleep mode:" + hostSleepMode + " wakeup byte:" + wakeupByte + " wakeup retry interval:" + wakeupRetryInterval + " wakeup retry times:" + wakeupRetryTimes);
        }
    }

    @Override
    public void onSuccess() {
        super.onSuccess();
        if (isValid())
            EventBus.getDefault().post(new CodelessEvent.HostSleep(this));
    }

    @Override
    protected boolean checkArgumentsCount() {
        int count = CodelessProfile.countArguments(command, ",");
        return count == 0 || count == 4;
    }

    @Override
    protected String parseArguments() {
        if (!CodelessProfile.hasArguments(command))
            return null;
        hasArguments = true;

        Integer num = decodeNumberArgument(1);
        if (num == null || num != HOST_SLEEP_MODE_0 && num != HOST_SLEEP_MODE_1)
            return "Invalid host sleep mode";
        hostSleepMode = num;

        num = decodeNumberArgument(2);
        if (num == null)
            return "Invalid wakeup byte";
        wakeupByte = num;

        num = decodeNumberArgument(3);
        if (num == null)
            return "Invalid wakeup retry interval";
        wakeupRetryInterval = num;

        num = decodeNumberArgument(4);
        if (num == null)
            return "Invalid wakeup retry times";
        wakeupRetryTimes = num;

        return null;
    }

    /** Returns the host sleep mode argument/response. */
    public int getHostSleepMode() {
        return hostSleepMode;
    }

    /** Sets the host sleep mode argument. */
    public void setHostSleepMode(int hostSleepMode) {
        this.hostSleepMode = hostSleepMode;
        if (hostSleepMode != HOST_SLEEP_MODE_0 && hostSleepMode != Command.HOST_SLEEP_MODE_1)
            invalid = true;
    }

    /** Returns the wakeup byte argument/response. */
    public int getWakeupByte() {
        return wakeupByte;
    }

    /** Sets the wakeup byte argument. */
    public void setWakeupByte(int wakeupByte) {
        this.wakeupByte = wakeupByte;
    }

    /** Returns the wakeup retry interval argument/response (ms). */
    public int getWakeupRetryInterval() {
        return wakeupRetryInterval;
    }

    /** Sets the wakeup retry interval argument (ms). */
    public void setWakeupRetryInterval(int wakeupRetryInterval) {
        this.wakeupRetryInterval = wakeupRetryInterval;
    }

    /** Returns the wakeup retry times argument/response. */
    public int getWakeupRetryTimes() {
        return wakeupRetryTimes;
    }

    /** Sets the wakeup retry times argument. */
    public void setWakeupRetryTimes(int wakeupRetryTimes) {
        this.wakeupRetryTimes = wakeupRetryTimes;
    }
}
