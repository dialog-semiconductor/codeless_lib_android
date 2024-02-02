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

/**
 * <code>AT+GAPSTATUS</code> command implementation with incoming command support.
 * <p> When incoming, is responds with [GAP central, connected] status.
 * @see CodelessEvent.GapStatus
 * @see <a href="https://lpccs-docs.renesas.com/UM-140-DA145x-CodeLess/index.html">AT commands documentation</a>
 */
public class GapStatusCommand extends CodelessCommand {
    public static final String TAG = "GapStatusCommand";

    public static final String COMMAND = "GAPSTATUS";
    public static final String NAME = CodelessProfile.PREFIX_LOCAL + COMMAND;
    public static final CommandID ID = CommandID.GAPSTATUS;

    public static final String PATTERN_STRING = "^GAPSTATUS$";
    public static final Pattern PATTERN = Pattern.compile(PATTERN_STRING);

    /** The GAP role response (0: peripheral, 1: central). */
    private int gapRole = -1;
    /** The connection status response. */
    private boolean connected;

    /**
     * Default constructor.
     * @see CodelessCommand#CodelessCommand(CodelessManager)
     */
    public GapStatusCommand(CodelessManager manager) {
        super(manager);
    }

    /**
     * Default constructor.
     * @see CodelessCommand#CodelessCommand(CodelessManager, String, boolean)
     */
    public GapStatusCommand(CodelessManager manager, String command, boolean parse) {
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
    public void parseResponse(String response) {
        super.parseResponse(response);
        if (responseLine() == 1) {
            String[] status = response.split(",");
            try {
                int gapRole = Integer.parseInt(status[0]);
                if (gapRole != Command.GAP_ROLE_PERIPHERAL && gapRole != Command.GAP_ROLE_CENTRAL)
                    invalid = true;
                this.gapRole = gapRole;
                int connected = Integer.parseInt(status[1]);
                if (connected != Command.GAP_STATUS_DISCONNECTED && connected != Command.GAP_STATUS_CONNECTED)
                    invalid = true;
                this.connected = connected == Command.GAP_STATUS_CONNECTED;
            } catch (IndexOutOfBoundsException | NumberFormatException e) {
                invalid = true;
            }
            if (invalid)
                Log.i(TAG, "Received invalid GAP status response: " + response);
            else if (CodelessLibLog.COMMAND)
                Log.d(TAG, "GAP status response: " + response);
        }
    }

    @Override
    public void onSuccess() {
        super.onSuccess();
        if (isValid())
            EventBus.getDefault().post(new CodelessEvent.GapStatus(this));
    }

    @Override
    public void processInbound() {
        if (gapRole == -1) {
            gapRole = Command.GAP_ROLE_CENTRAL;
            connected = manager.isConnected();
        }
        String response = String.format(Locale.US, "%d,%d", gapRole, connected ? Command.GAP_STATUS_CONNECTED : Command.GAP_STATUS_DISCONNECTED);
        if (CodelessLibLog.COMMAND)
            Log.d(TAG, "GAP status: " + response);
        sendSuccess(response);
    }

    /** Returns the GAP role response (0: peripheral, 1: central). */
    public int getGapRole() {
        return gapRole;
    }

    /** Sets the GAP role response (0: peripheral, 1: central). */
    public void setGapRole(int gapRole) {
        this.gapRole = gapRole;
        if (gapRole != Command.GAP_ROLE_PERIPHERAL && gapRole != Command.GAP_ROLE_CENTRAL)
            invalid = true;
    }

    /** Returns the connection status response. */
    public boolean connected() {
        return connected;
    }

    /** Sets the connection status response. */
    public void setConnected(boolean connected) {
        this.connected = connected;
    }
}
