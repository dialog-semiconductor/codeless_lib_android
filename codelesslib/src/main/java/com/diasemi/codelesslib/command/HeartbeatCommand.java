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

import java.util.regex.Pattern;

import static com.diasemi.codelesslib.CodelessProfile.Command.HEARTBEAT_DISABLED;
import static com.diasemi.codelesslib.CodelessProfile.Command.HEARTBEAT_ENABLED;

/**
 * <code>AT+HRTBT</code> command implementation.
 * @see CodelessEvent.Heartbeat
 * @see <a href="https://lpccs-docs.renesas.com/UM-140-DA145x-CodeLess/index.html">AT commands documentation</a>
 */
public class HeartbeatCommand extends CodelessCommand {
    public static final String TAG = "HeartbeatCommand";

    public static final String COMMAND = "HRTBT";
    public static final String NAME = CodelessProfile.PREFIX_LOCAL + COMMAND;
    public static final CommandID ID = CommandID.HRTBT;

    public static final String PATTERN_STRING = "^HRTBT(?:=(\\d))?$"; // <heartbeat>
    public static final Pattern PATTERN = Pattern.compile(PATTERN_STRING);

    /** The heartbeat signal status argument/response (<code>true</code> for enabled, <code>false</code> for disabled). */
    private boolean enabled;
    /** <code>true</code> if the command has arguments, <code>false</code> for no arguments. */
    private boolean hasArguments;

    /**
     * Creates an <code>AT+HRTBT</code> command.
     * @param manager   the associated manager
     * @param enabled   the heartbeat signal status argument (<code>true</code> for enabled, <code>false</code> for disabled)
     */
    public HeartbeatCommand(CodelessManager manager, boolean enabled) {
        this(manager);
        this.enabled = enabled;
        hasArguments = true;
    }

    /**
     * Default constructor.
     * @see CodelessCommand#CodelessCommand(CodelessManager)
     */
    public HeartbeatCommand(CodelessManager manager) {
        super(manager);
    }

    /**
     * Default constructor.
     * @see CodelessCommand#CodelessCommand(CodelessManager, String, boolean)
     */
    public HeartbeatCommand(CodelessManager manager, String command, boolean parse) {
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
        return hasArguments ? Integer.toString(enabled ? HEARTBEAT_ENABLED : HEARTBEAT_DISABLED) : null;
    }

    @Override
    public void parseResponse(String response) {
        super.parseResponse(response);
        if (responseLine() == 1) {
            try {
                int num = Integer.parseInt(response);
                if (num != HEARTBEAT_ENABLED && num != HEARTBEAT_DISABLED)
                    invalid = true;
                enabled = num != HEARTBEAT_DISABLED;
            } catch (NumberFormatException e) {
                invalid = true;
            }
            if (invalid)
                Log.e(TAG, "Received invalid heartbeat state: " + response);
            else if (CodelessLibLog.COMMAND)
                Log.d(TAG, "Heartbeat state: " + (enabled ? "enabled" : "disabled"));
        }
    }

    @Override
    public void onSuccess() {
        super.onSuccess();
        if (isValid())
            EventBus.getDefault().post(new CodelessEvent.Heartbeat(this));
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
        if (num == null || num != HEARTBEAT_ENABLED && num != HEARTBEAT_DISABLED)
            return "Invalid heartbeat state";
        enabled = num != HEARTBEAT_DISABLED;

        return null;
    }

    /** Returns the heartbeat signal status argument/response (<code>true</code> for enabled, <code>false</code> for disabled). */
    public boolean enabled() {
        return enabled;
    }

    /** Sets the heartbeat signal status argument (<code>true</code> for enabled, <code>false</code> for disabled). */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
