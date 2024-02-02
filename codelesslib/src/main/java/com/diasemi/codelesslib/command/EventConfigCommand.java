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
import com.diasemi.codelesslib.CodelessManager;
import com.diasemi.codelesslib.CodelessProfile;
import com.diasemi.codelesslib.CodelessProfile.CommandID;
import com.diasemi.codelesslib.CodelessProfile.EventConfig;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Pattern;

import static com.diasemi.codelesslib.CodelessProfile.Command.ACTIVATE_EVENT;
import static com.diasemi.codelesslib.CodelessProfile.Command.CONNECTION_EVENT;
import static com.diasemi.codelesslib.CodelessProfile.Command.DEACTIVATE_EVENT;
import static com.diasemi.codelesslib.CodelessProfile.Command.DISCONNECTION_EVENT;
import static com.diasemi.codelesslib.CodelessProfile.Command.INITIALIZATION_EVENT;
import static com.diasemi.codelesslib.CodelessProfile.Command.WAKEUP_EVENT;

/**
 * <code>AT+EVENT</code> command implementation.
 * @see CodelessEvent.EventStatusTable
 * @see CodelessEvent.EventStatus
 * @see <a href="https://lpccs-docs.renesas.com/UM-140-DA145x-CodeLess/index.html">AT commands documentation</a>
 */
public class EventConfigCommand extends CodelessCommand {
    public static final String TAG = "EventConfigCommand";

    public static final String COMMAND = "EVENT";
    public static final String NAME = CodelessProfile.PREFIX_LOCAL + COMMAND;
    public static final CommandID ID = CommandID.EVENT;

    public static final String PATTERN_STRING = "^EVENT(?:=(\\d+),(\\d))?$"; // <event> <status>
    public static final Pattern PATTERN = Pattern.compile(PATTERN_STRING);

    /** The predefined events configuration response. */
    private ArrayList<EventConfig> eventStatusTable;
    /** The predefined event configuration argument. */
    private EventConfig eventConfig;
    /** <code>true</code> if the command has arguments, <code>false</code> for no arguments. */
    private boolean hasArguments;

    /**
     * Creates an <code>AT+EVENT</code> command.
     * @param manager   the associated manager
     * @param eventType the predefined event type argument
     * @param status    the predefined event activation status argument
     */
    public EventConfigCommand(CodelessManager manager, int eventType, boolean status) {
        this(manager, new EventConfig(eventType, status));
    }

    /**
     * Creates an <code>AT+EVENT</code> command.
     * @param manager       the associated manager
     * @param eventConfig   the predefined event configuration argument
     */
    public EventConfigCommand(CodelessManager manager, EventConfig eventConfig) {
        this(manager);
        setEventConfig(eventConfig);
        hasArguments = true;
    }

    /**
     * Default constructor.
     * @see CodelessCommand#CodelessCommand(CodelessManager)
     */
    public EventConfigCommand(CodelessManager manager) {
        super(manager);
    }

    /**
     * Default constructor.
     * @see CodelessCommand#CodelessCommand(CodelessManager, String, boolean)
     */
    public EventConfigCommand(CodelessManager manager, String command, boolean parse) {
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
        return hasArguments ? String.format(Locale.US, "%d,%d", eventConfig.type, eventConfig.status ? ACTIVATE_EVENT : DEACTIVATE_EVENT) : null;
    }

    @Override
    public void parseResponse(String response) {
        super.parseResponse(response);
        if (eventStatusTable == null)
            eventStatusTable = new ArrayList<>();
        String errorMsg = "Received invalid Event status response: " + response;
        try {
            EventConfig eventConfig = new EventConfig();
            String[] eventData = response.split(",");
            int type = Integer.parseInt(eventData[0]);
            if (type != INITIALIZATION_EVENT && type != CONNECTION_EVENT && type != DISCONNECTION_EVENT && type != WAKEUP_EVENT) {
                Log.e(TAG, errorMsg);
                invalid = true;
                return;
            }
            eventConfig.type = type;
            int status = Integer.parseInt(eventData[1]);
            if (status != DEACTIVATE_EVENT && status != ACTIVATE_EVENT) {
                Log.e(TAG, errorMsg);
                invalid = true;
                return;
            }
            eventConfig.status = status == ACTIVATE_EVENT;
            eventStatusTable.add(eventConfig);
        } catch (IndexOutOfBoundsException | NumberFormatException e) {
            Log.e(TAG, errorMsg);
            invalid = true;
        }
    }

    @Override
    public void onSuccess() {
        super.onSuccess();
        if (isValid())
            EventBus.getDefault().post(hasArguments ? new CodelessEvent.EventStatus(this) : new CodelessEvent.EventStatusTable(this));
    }

    @Override
    protected boolean checkArgumentsCount() {
        int count = CodelessProfile.countArguments(command, ",");
        return count == 0 || count == 2;
    }

    @Override
    protected String parseArguments() {
        eventConfig = new EventConfig();

        if (!CodelessProfile.hasArguments(command))
            return null;
        hasArguments = true;

        Integer num = decodeNumberArgument(1);
        if (num == null || num != INITIALIZATION_EVENT && num != CONNECTION_EVENT && num != DISCONNECTION_EVENT && num != WAKEUP_EVENT)
            return "Invalid event number";
        eventConfig.type = num;

        num = decodeNumberArgument(2);
        if (num == null || num != DEACTIVATE_EVENT && num != ACTIVATE_EVENT)
            return "Invalid event status";
        eventConfig.status = num == ACTIVATE_EVENT;

        return null;
    }

    /** Returns the predefined event configuration argument. */
    public EventConfig getEventConfig() {
        return eventConfig;
    }

    /** Sets the predefined event configuration argument. */
    public void setEventConfig(EventConfig eventConfig) {
        this.eventConfig = eventConfig;
        if (eventConfig.type != INITIALIZATION_EVENT && eventConfig.type != CONNECTION_EVENT && eventConfig.type != DISCONNECTION_EVENT && eventConfig.type != WAKEUP_EVENT)
            invalid = true;
    }

    /** Returns the predefined event type argument. */
    public int getType() {
        return eventConfig.type;
    }

    /** Sets the predefined event type argument. */
    public void setType(int type) {
        eventConfig.type = type;
        if (type != INITIALIZATION_EVENT && type != CONNECTION_EVENT && type != DISCONNECTION_EVENT && type != WAKEUP_EVENT)
            invalid = true;
    }

    /** Returns the predefined event activation status argument. */
    public boolean getStatus() {
        return eventConfig.status;
    }

    /** Sets the predefined event activation status argument. */
    public void setStatus(boolean status) {
        eventConfig.status = status;
    }

    /** Returns the predefined events configuration response. */
    public ArrayList<EventConfig> getEventStatusTable() {
        return eventStatusTable;
    }
}
