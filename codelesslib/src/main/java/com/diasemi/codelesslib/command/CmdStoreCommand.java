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

import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * <code>AT+CMDSTORE</code> command implementation.
 * @see <a href="https://lpccs-docs.renesas.com/UM-140-DA145x-CodeLess/index.html">AT commands documentation</a>
 */
public class CmdStoreCommand extends CodelessCommand {
    public static final String TAG = "CmdStoreCommand";

    public static final String COMMAND = "CMDSTORE";
    public static final String NAME = CodelessProfile.PREFIX_LOCAL + COMMAND;
    public static final CommandID ID = CommandID.CMDSTORE;

    public static final String PATTERN_STRING = "^CMDSTORE=(\\d+),((?:[^;]+;?)+)$"; // <index> <command>
    public static final Pattern PATTERN = Pattern.compile(PATTERN_STRING);

    /** The command slot index argument. */
    private int index;
    /** The stored commands argument (parsed). */
    private ArrayList<CodelessCommand> commands;
    /** The stored commands argument (semicolon separated). */
    private String commandString;

    /**
     * Creates an <code>AT+CMDSTORE</code> command.
     * @param manager   the associated manager
     * @param index     the command slot index argument (0-3)
     * @param commands  the stored commands argument
     */
    public CmdStoreCommand(CodelessManager manager, int index, ArrayList<CodelessCommand> commands) {
        this(manager);
        setIndex(index);
        this.commands = commands;
        commandString = packCommandList(commands);
    }

    /**
     * Creates an <code>AT+CMDSTORE</code> command.
     * @param manager       the associated manager
     * @param index         the command slot index argument (0-3)
     * @param commandString the stored commands argument (semicolon separated)
     */
    public CmdStoreCommand(CodelessManager manager, int index, String commandString) {
        this(manager);
        setIndex(index);
        this.commandString = commandString;
        commands = parseCommandString(commandString);
    }

    /**
     * Default constructor.
     * @see CodelessCommand#CodelessCommand(CodelessManager)
     */
    public CmdStoreCommand(CodelessManager manager) {
        super(manager);
    }

    /**
     * Default constructor.
     * @see CodelessCommand#CodelessCommand(CodelessManager, String, boolean)
     */
    public CmdStoreCommand(CodelessManager manager, String command, boolean parse) {
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
        return String.format(Locale.US, "%d,%s", index, commandString);
    }

    @Override
    protected boolean requiresArguments() {
        return true;
    }

    @Override
    protected boolean checkArgumentsCount() {
        return CodelessProfile.countArguments(command, ",") == 2;
    }

    @Override
    protected String parseArguments() {
        Integer num = decodeNumberArgument(1);
        if (num == null || CodelessLibConfig.CHECK_COMMAND_STORE_INDEX && (num < CodelessLibConfig.COMMAND_STORE_INDEX_MIN || num > CodelessLibConfig.COMMAND_STORE_INDEX_MAX))
            return "Invalid index";
        index = num;
        String commandString = matcher.group(2);
        if (commandString == null)
            return "Invalid command strings";
        this.commandString = commandString;
        this.commands = parseCommandString(commandString);
        return null;
    }

    /** Returns the command slot index argument. */
    public int getIndex() {
        return index;
    }

    /** Sets the command slot index argument (0-3). */
    public void setIndex(int index) {
        this.index = index;
        if (CodelessLibConfig.CHECK_COMMAND_STORE_INDEX) {
            if (index < CodelessLibConfig.COMMAND_STORE_INDEX_MIN || index > CodelessLibConfig.COMMAND_STORE_INDEX_MAX)
                invalid = true;
        }
    }

    /** Returns the stored commands argument (semicolon separated). */
    public String getCommandString() {
        return commandString;
    }

    /** Sets the stored commands argument (semicolon separated). */
    public void setCommandString(String commandString) {
        this.commandString = commandString;
    }

    /** Returns the stored commands argument (parsed). */
    public ArrayList<CodelessCommand> getCommands() {
        return commands;
    }

    /** Sets the stored commands argument. */
    public void setCommands(ArrayList<CodelessCommand> commands) {
        this.commands = commands;
    }

    /**
     * Parses a stored commands text to a list of parsed commands.
     * @param commandString the stored commands text (semicolon separated)
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
     * Packs a list of stored commands to the corresponding stored commands text (semicolon separated).
     * @param commands the list of stored commands
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
