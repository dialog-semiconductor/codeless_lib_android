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

import static com.diasemi.codelesslib.CodelessProfile.Command.SECURITY_MODE_0;
import static com.diasemi.codelesslib.CodelessProfile.Command.SECURITY_MODE_1;
import static com.diasemi.codelesslib.CodelessProfile.Command.SECURITY_MODE_2;
import static com.diasemi.codelesslib.CodelessProfile.Command.SECURITY_MODE_3;

/**
 * <code>AT+SEC</code> command implementation.
 * @see CodelessEvent.SecurityMode
 * @see <a href="https://lpccs-docs.renesas.com/UM-140-DA145x-CodeLess/index.html">AT commands documentation</a>
 */
public class SecurityModeCommand extends CodelessCommand {
    public static final String TAG = "SecurityModeCommand";

    public static final String COMMAND = "SEC";
    public static final String NAME = CodelessProfile.PREFIX_LOCAL + COMMAND;
    public static final CommandID ID = CommandID.SEC;

    public static final String PATTERN_STRING = "^SEC(?:=(\\d+))?$"; // <mode>
    public static final Pattern PATTERN = Pattern.compile(PATTERN_STRING);

    /** The security {@link CodelessProfile.Command#SECURITY_MODE_0 mode} argument/response. */
    private int mode;
    /** <code>true</code> if the command has arguments, <code>false</code> for no arguments. */
    private boolean hasArguments;

    /**
     * Creates an <code>AT+SEC</code> command.
     * @param manager   the associated manager
     * @param mode      the security {@link CodelessProfile.Command#SECURITY_MODE_0 mode} argument
     */
    public SecurityModeCommand(CodelessManager manager, int mode) {
        this(manager);
        setMode(mode);
        hasArguments = true;
    }

    /**
     * Default constructor.
     * @see CodelessCommand#CodelessCommand(CodelessManager)
     */
    public SecurityModeCommand(CodelessManager manager) {
        super(manager);
    }

    /**
     * Default constructor.
     * @see CodelessCommand#CodelessCommand(CodelessManager, String, boolean)
     */
    public SecurityModeCommand(CodelessManager manager, String command, boolean parse) {
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
        return hasArguments ? Integer.toString(mode) : null;
    }

    @Override
    public void parseResponse(String response) {
        super.parseResponse(response);
        if (responseLine() == 1) {
            try {
                mode = Integer.parseInt(response);
                if (mode != SECURITY_MODE_0 && mode != SECURITY_MODE_1 && mode != SECURITY_MODE_2 && mode != SECURITY_MODE_3)
                    invalid = true;
            } catch (NumberFormatException e) {
                invalid = true;
            }
            if (invalid)
                Log.e(TAG, "Received invalid security mode: " + response);
            else if (CodelessLibLog.COMMAND)
                Log.d(TAG, "Security mode: " + mode);
        }
    }

    @Override
    public void onSuccess() {
        super.onSuccess();
        if (isValid())
            EventBus.getDefault().post(new CodelessEvent.SecurityMode(this));
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
        if (num == null || num != SECURITY_MODE_0 && num != SECURITY_MODE_1 && num != SECURITY_MODE_2 && num != SECURITY_MODE_3)
            return "Invalid security mode";
        mode = num;

        return null;
    }

    /** Returns the security {@link CodelessProfile.Command#SECURITY_MODE_0 mode} argument/response. */
    public int getMode() {
        return mode;
    }

    /** Sets the security {@link CodelessProfile.Command#SECURITY_MODE_0 mode} argument. */
    public void setMode(int mode) {
        this.mode = mode;
        if (mode != SECURITY_MODE_0 && mode != SECURITY_MODE_1 && mode != SECURITY_MODE_2 && mode != SECURITY_MODE_3)
            invalid = true;
    }
}
