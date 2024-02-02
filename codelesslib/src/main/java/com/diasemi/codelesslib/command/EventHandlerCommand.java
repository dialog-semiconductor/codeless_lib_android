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
import com.diasemi.codelesslib.CodelessProfile.EventHandler;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Pattern;

import static com.diasemi.codelesslib.CodelessProfile.Command.CONNECTION_EVENT_HANDLER;
import static com.diasemi.codelesslib.CodelessProfile.Command.DISCONNECTION_EVENT_HANDLER;
import static com.diasemi.codelesslib.CodelessProfile.Command.WAKEUP_EVENT_HANDLER;

/**
 * <code>AT+HNDL</code> command implementation.
 * @see CodelessEvent.EventCommandsTable
 * @see CodelessEvent.EventCommands
 * @see <a href="https://lpccs-docs.renesas.com/UM-140-DA145x-CodeLess/index.html">AT commands documentation</a>
 */
public class EventHandlerCommand extends CodelessCommand {
    public static final String TAG = "EventHandlerCommand";

    public static final String COMMAND = "HNDL";
    public static final String NAME = CodelessProfile.PREFIX_LOCAL + COMMAND;
    public static final CommandID ID = CommandID.HNDL;

    public static final String PATTERN_STRING = "^HNDL(?:=(\\d+)(?:,((?:[^;]+;?)*))?)?$"; // <event> <command>
    public static final Pattern PATTERN = Pattern.compile(PATTERN_STRING);

    /** The predefined event handler configuration argument. */
    private EventHandler eventHandler;
    /** The predefined event handlers configuration response. */
    private ArrayList<EventHandler> eventHandlerTable;
    /** <code>true</code> if the command has arguments, <code>false</code> for no arguments. */
    private boolean hasArguments;

    /**
     * Creates an <code>AT+HNDL</code> command.
     * @param manager   the associated manager
     * @param event     the predefined event type argument
     * @param commands  the predefined event handler commands argument
     */
    public EventHandlerCommand(CodelessManager manager, int event, ArrayList<CodelessCommand> commands) {
        this(manager, event);
        eventHandler.commands = commands;
    }

    /**
     * Creates an <code>AT+HNDL</code> command.
     * @param manager       the associated manager
     * @param event         the predefined event type argument
     * @param commandString the predefined event handler commands (semicolon separated) argument
     */
    public EventHandlerCommand(CodelessManager manager, int event, String commandString) {
        this(manager, event);
        eventHandler.commands = parseCommandString(commandString);
    }

    /**
     * Creates an <code>AT+HNDL</code> command.
     * @param manager   the associated manager
     * @param event     the predefined event type argument
     */
    public EventHandlerCommand(CodelessManager manager, int event) {
        this(manager);
        eventHandler = new EventHandler();
        setEvent(event);
        eventHandler.commands = new ArrayList<>();
        hasArguments = true;
    }

    /**
     * Creates an <code>AT+HNDL</code> command.
     * @param manager       the associated manager
     * @param eventHandler  the predefined event handler configuration argument
     */
    public EventHandlerCommand(CodelessManager manager, EventHandler eventHandler) {
        this(manager);
        setEventHandler(eventHandler);
        hasArguments = true;
    }

    /**
     * Default constructor.
     * @see CodelessCommand#CodelessCommand(CodelessManager)
     */
    public EventHandlerCommand(CodelessManager manager) {
        super(manager);
    }

    /**
     * Default constructor.
     * @see CodelessCommand#CodelessCommand(CodelessManager, String, boolean)
     */
    public EventHandlerCommand(CodelessManager manager, String command, boolean parse) {
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
        if (!hasArguments)
            return null;
        String arguments = String.valueOf(eventHandler.event);
        if (eventHandler.commands.size() > 0)
            arguments += String.format(Locale.US, ",%s", packCommandList(eventHandler.commands));
        return arguments;
    }

    @Override
    public void parseResponse(String response) {
        super.parseResponse(response);
        if (eventHandlerTable == null)
            eventHandlerTable = new ArrayList<>();
        String errorMsg = "Received invalid Event Handler response: " + response;
        try {
            EventHandler eventHandler = new EventHandler();
            int splitPos = response.indexOf(",");
            int event = Integer.parseInt(response.substring(0, splitPos));
            if (event != CONNECTION_EVENT_HANDLER && event != DISCONNECTION_EVENT_HANDLER && event != WAKEUP_EVENT_HANDLER) {
                Log.e(TAG, errorMsg);
                invalid = true;
                return;
            }
            eventHandler.event = event;
            String commandString = response.substring(splitPos + 1);
            eventHandler.commands = parseCommandString(commandString.equals("<empty>") ? "" : commandString);
            eventHandlerTable.add(eventHandler);
        } catch (IndexOutOfBoundsException | NumberFormatException e) {
            Log.e(TAG, errorMsg);
            invalid = true;
        }
    }

    @Override
    public void onSuccess() {
        super.onSuccess();
        if (isValid())
            EventBus.getDefault().post(hasArguments ? new CodelessEvent.EventCommands(this) : new CodelessEvent.EventCommandsTable(this));
    }

    @Override
    protected boolean checkArgumentsCount() {
        int count = CodelessProfile.countArguments(command, ",");
        return count == 0 || count == 1 || count == 2;
    }

    @Override
    protected String parseArguments() {
        eventHandler = new EventHandler();
        eventHandler.commands = new ArrayList<>();

        int count = CodelessProfile.countArguments(command, ",");
        if (count == 0)
            return null;
        hasArguments = true;

        Integer num = decodeNumberArgument(1);
        if (num == null || num != CONNECTION_EVENT_HANDLER && num != DISCONNECTION_EVENT_HANDLER && num != WAKEUP_EVENT_HANDLER)
            return "Invalid event";
        eventHandler.event = num;

        if (count == 2) {
            String commandString = matcher.group(2);
            eventHandler.commands = parseCommandString(commandString);
        }

        return null;
    }

    /** Returns the predefined event handler configuration argument. */
    public EventHandler getEventHandler() {
        return eventHandler;
    }

    /** Sets the predefined event handler configuration argument. */
    public void setEventHandler(EventHandler eventHandler) {
        this.eventHandler = eventHandler;
        if (eventHandler.event != CONNECTION_EVENT_HANDLER && eventHandler.event != DISCONNECTION_EVENT_HANDLER && eventHandler.event != WAKEUP_EVENT_HANDLER)
            invalid = true;
    }

    /** Returns the predefined event type argument. */
    public int getEvent() {
        return eventHandler.event;
    }

    /** Sets the predefined event type argument. */
    public void setEvent(int event) {
        eventHandler.event = event;
        if (event != CONNECTION_EVENT_HANDLER && event != DISCONNECTION_EVENT_HANDLER && event != WAKEUP_EVENT_HANDLER)
            invalid = true;
    }

    /** Returns the predefined event handler commands argument. */
    public ArrayList<CodelessCommand> getCommands() {
        return eventHandler.commands;
    }

    /** Sets the predefined event handler commands argument. */
    public void setCommands(ArrayList<CodelessCommand> commands) {
        eventHandler.commands = commands;
    }

    /** Returns the predefined event handler commands (semicolon separated) argument. */
    public String getCommandString() {
        return packCommandList(eventHandler.commands);
    }

    /** Sets the predefined event handler commands (semicolon separated) argument. */
    public void setCommandString(String commandString) {
        eventHandler.commands = parseCommandString(commandString);
    }

    /** Returns the predefined event handlers configuration response. */
    public ArrayList<EventHandler> getEventHandlerTable() {
        return eventHandlerTable;
    }

    /**
     * Parses a handler commands text to a list of parsed commands.
     * @param commandString the handler commands text (semicolon separated)
     */
    private ArrayList<CodelessCommand> parseCommandString(String commandString) {
        String[] commandArray = commandString.split(";");
        ArrayList<CodelessCommand> commandList = new ArrayList<>();
        for (String command : commandArray) {
            if (!command.isEmpty())
                commandList.add(manager.parseTextCommand(command));
        }
        return commandList;
    }

    /**
     * Packs a list of handler commands to the corresponding handler commands text (semicolon separated).
     * @param commands the list of handler commands
     */
    private String packCommandList(ArrayList<CodelessCommand> commands) {
        StringBuilder stringBuilder = new StringBuilder();
        for (CodelessCommand command : commands) {
            String commandString = command.getPrefix() != null ? command.getPrefix() + command.getCommand() : command.getCommand();
            stringBuilder.append(stringBuilder.length() > 0 ? ";" + commandString : commandString);
        }
        return stringBuilder.toString();
    }
}
