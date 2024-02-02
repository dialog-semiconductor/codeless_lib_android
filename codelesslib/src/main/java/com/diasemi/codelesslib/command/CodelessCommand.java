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
import com.diasemi.codelesslib.CodelessScript;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import androidx.annotation.NonNull;

/**
 * Base class for CodeLess command implementation.
 * <p>
 * For each supported command, a subclass of this class provides the command behavior, by overriding the required methods.
 * The library {@link #parseCommand(String) parses} the command text and creates the corresponding subclass object.
 * If a command is not recognized, a {@link CustomCommand} object is created.
 * <h2>Add a new command</h2>
 * To add a new command, create a subclass of this class. Add a {@link CodelessProfile.CommandID command ID} for the new command.
 * In the subclass, implement the default constructors and override the methods required to provide the command specific behavior.
 * <ul>
 * <li>
 * Required methods: {@link #getTag()}, {@link #getID()}, {@link #getName()}, {@link #getCommandID()}, {@link #getPattern()}
 * </li>
 * <li>
 * Parsing methods: {@link #requiresArguments()}, {@link #checkArgumentsCount()}, {@link #parseArguments()}
 * </li>
 * <li>
 * Outgoing commands: {@link #hasArguments()}, {@link #getArguments()}, {@link #parseResponse(String)}, {@link #onSuccess()}, {@link #onError(String)}
 * </li>
 * <li>
 * Incoming commands: {@link #processInbound()}, {@link #sendSuccess(String)}, {@link #sendError(String)}
 * </ul>
 * For example, see the implementation of the {@link UartPrintCommand <code>AT+PRINT</code>}
 * and the {@link DeviceInformationCommand <code>ATI</code>} commands.
 * <p>
 * Each of the library command classes contains the following static fields:
 * <ul>
 * <li><code>TAG</code>: the command log tag</li>
 * <li><code>COMMAND</code>: the command text identifier</li>
 * <li><code>NAME</code>: the command name</li>
 * <li><code>ID</code>: the command ID</li>
 * <li><code>PATTERN_STRING</code>: the command pattern (regular expression)</li>
 * </ul>
 * @see CodelessManager
 * @see #parseCommand(String)
 * @see <a href="https://lpccs-docs.renesas.com/UM-140-DA145x-CodeLess/index.html">AT commands documentation</a>
 */
public abstract class CodelessCommand {
    public static final String TAG = "CodelessCommand";

    /** The associated manager. */
    protected CodelessManager manager;
    /** The associated script, if the command is part of one. */
    protected CodelessScript script;
    /** The object that created the command (optional, used by {@link com.diasemi.codelesslib.CodelessCommands CodelessCommands}). */
    protected Object origin;
    /** The command text (provided by the user or created by the {@link #packCommand() command object}). */
    protected String command;
    /** The used AT command prefix. */
    protected String prefix;
    /** The response text received for this command (one string per line). */
    protected ArrayList<String> response = new ArrayList<>();
    /** Pattern matcher used for parsing the command text. */
    protected Matcher matcher;
    /** <code>true</code> if the command is received from the peer device, <code>false</code> if it is sent to it. */
    protected boolean inbound;
    /** <code>true</code> if the command is parsed from text, <code>false</code> if it is created by the library. */
    protected boolean parsed;
    /** <code>true</code> if the command is invalid (parsing failed, wrong arguments). */
    protected boolean invalid;
    /** <code>true</code> if the peer device responded with an invalid command error. */
    protected boolean peerInvalid;
    /** <code>true</code> if the command is complete. */
    protected boolean complete;
    /** The error message (if the sent or received command failed). */
    protected String error;
    /** The error code (if the sent or received command failed). */
    protected int errorCode;

    /**
     * Creates a CodelessCommand object without arguments.
     * @param manager the associated manager
     */
    public CodelessCommand(CodelessManager manager) {
        this.manager = manager;
    }

    /**
     * Creates a CodelessCommand object from text.
     * @param manager   the associated manager
     * @param command   the command text
     * @param parse     <code>true</code> to parse the command text
     */
    public CodelessCommand(CodelessManager manager, String command, boolean parse) {
        this(manager);
        this.command = command;
        if (parse)
            parseCommand(command);
    }

    /** Returns the associated manger. */
    public CodelessManager getManager() {
        return manager;
    }

    /** Returns the associated {@link CodelessScript script}, if the command is part of one. */
    public CodelessScript getScript() {
        return script;
    }

    /**
     * Indicates that the command is part of a {@link CodelessScript script}.
     * @param script the command script
     */
    public void setScript(CodelessScript script) {
        this.script = script;
    }

    /** Returns the object that created the command. */
    public Object getOrigin() {
        return origin;
    }

    /** Sets the object that created the command. */
    public void setOrigin(Object origin) {
        this.origin = origin;
    }

    /** Sets the object that created the command. */
    public CodelessCommand origin(Object origin) {
        this.origin = origin;
        return this;
    }

    /** Returns the command text. */
    public String getCommand() {
        return command;
    }

    /** Returns the command prefix. */
    public String getPrefix() {
        return prefix;
    }

    /** Sets the command prefix. */
    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    /** Checks if the command prefix is set. */
    public boolean hasPrefix() {
        return prefix != null;
    }

    /** Returns the response text received for this command (one string per line). */
    public ArrayList<String> getResponse() {
        return response;
    }

    /** Marks the command as received from the peer device. */
    public void setInbound() {
        inbound = true;
    }

    /** Checks if the command is received from the peer device. */
    public boolean isInbound() {
        return inbound;
    }

    /** Checks if the command is parsed from text or created by the library. */
    public boolean isParsed() {
        return parsed;
    }

    /** Checks if the command is valid. */
    public boolean isValid() {
        return !invalid;
    }

    /** Marks the command as invalid for the peer device. */
    public void setPeerInvalid() {
        peerInvalid = true;
    }

    /** Checks if the command is invalid for the peer device. */
    public boolean isPeerInvalid() {
        return peerInvalid;
    }

    /** Completes the command. */
    public void setComplete() {
        complete = true;
    }

    /** Checks if the command is complete. */
    public boolean isComplete() {
        return complete;
    }

    /** Checks if the command has failed. */
    public boolean failed() {
        return error != null;
    }

    /** Returns the error response for a failed command. */
    public String getError() {
        return error;
    }

    /** Returns the error code for a failed command. */
    public int getErrorCode() {
        return errorCode;
    }

    /** Returns the command log tag. */
    protected String getTag() {
        return TAG;
    }

    /** Returns the command text identifier (without the AT command prefix). */
    public abstract String getID();

    /** Returns the command name. */
    public abstract String getName();

    /** Returns the {@link CodelessProfile.CommandID command ID}. */
    public abstract CodelessProfile.CommandID getCommandID();

    /**
     * Returns the command pattern (regular expression, used for {@link #parseCommand(String) parsing}).
     * <p>
     * During parsing, the library will try to match the command text with this pattern.
     * <p>
     * The pattern starts with the command {@link #getID() text identifier} and can
     * contain capturing groups for the command arguments, which can be used to extract
     * them using the {@link #matcher}.
     */
    public Pattern getPattern() {
        return null;
    }

    /** Creates the command text to be sent to the peer device. */
    public String packCommand() {
        return command = !hasArguments() ? getID() : getID() + "=" + getArguments();
    }

    /** Checks if the command has arguments (used by {@link #packCommand() packCommand}). */
    protected boolean hasArguments() {
        return false;
    }

    /** Returns the text for the command's arguments (used by {@link #packCommand() packCommand}). */
    protected String getArguments() {
        return null;
    }

    /**
     * Checks if the command wants to parse each received response line immediately.
     * <p>
     * Otherwise the whole response will be parsed when the command is complete.
     * Used for command processing.
     */
    public boolean parsePartialResponse() {
        return false;
    }

    /**
     * Parses the response text.
     * <p> Called on each response line before the success or error response.
     * @param response the response text
     */
    public void parseResponse(String response) {
        this.response.add(response);
        if (CodelessLibLog.COMMAND)
            Log.d(getTag(), "Response: " + response);
    }

    /**
     * Returns the size of the {@link #response} array.
     * <p> Can be used to get the current response line in {@link #parseResponse(String) parseResponse}.
     */
    protected int responseLine() {
        return response.size();
    }

    /** Called on command success (for sent commands). */
    public void onSuccess() {
        if (CodelessLibLog.COMMAND)
            Log.d(getTag(), "Command succeeded");
        complete = true;
        EventBus.getDefault().post(new CodelessEvent.CommandSuccess(this));
        if (script != null)
            script.onSuccess(this);
    }

    /**
     * Called on command failure (for sent commands).
     * @param msg the error message
     */
    public void onError(String msg) {
        if (CodelessLibLog.COMMAND)
            Log.d(getTag(), "Command failed: " + msg);
        if (error == null)
            error = msg;
        complete = true;
        EventBus.getDefault().post(new CodelessEvent.CommandError(this, msg));
        if (script != null)
            script.onError(this);
    }

    /**
     * Sets the error code and message for a failed command.
     * @param code      the error code
     * @param message   the error message
     */
    public void setErrorCode(int code, String message) {
        errorCode = code;
        error = message;
    }

    /**
     * Parses the specified command text and initializes the command object.
     * <p>
     * The parsing uses methods that are overridden by subclasses to provide the required behavior.
     * First it checks if arguments are {@link #requiresArguments() required} but missing.
     * Then it checks if the number of arguments is {@link #checkArgumentsCount() correct}.
     * After that, it uses the command {@link #getPattern() pattern} to match the command text.
     * If the matching is successful, it {@link #parseArguments() parses} the arguments.
     * @param command the command text
     * @return <code>null</code> if the command was parsed successfully, otherwise the parse error message
     */
    public String parseCommand(String command) {
        if (CodelessLibLog.COMMAND)
            Log.d(getTag(), "Parse command: " + command);
        this.command = command;
        parsed = true;

        Pattern pattern = getPattern();
        if (pattern == null) {
            Log.e(getTag(), "No command pattern");
            invalid = true;
            return error = CodelessProfile.INVALID_COMMAND;
        }

        if (requiresArguments() && !CodelessProfile.hasArguments(command)) {
            if (CodelessLibLog.COMMAND)
                Log.d(getTag(), "No arguments");
            invalid = true;
            return error = CodelessProfile.NO_ARGUMENTS;
        }

        if (!checkArgumentsCount()) {
            if (CodelessLibLog.COMMAND)
                Log.d(getTag(), "Wrong number of arguments");
            invalid = true;
            return error = CodelessProfile.WRONG_NUMBER_OF_ARGUMENTS;
        }

        matcher = pattern.matcher(command);
        if (!matcher.matches()) {
            if (CodelessLibLog.COMMAND)
                Log.d(getTag(), "Command pattern match failed");
            invalid = true;
            return error = CodelessProfile.INVALID_ARGUMENTS;
        }

        String msg = parseArguments();
        if (msg != null) {
            if (CodelessLibLog.COMMAND)
                Log.d(getTag(), "Invalid arguments: " + msg);
            error = msg;
            invalid = true;
        }
        return msg;
    }

    /** Checks if the command requires arguments (used for {@link #parseCommand(String) parsing}). */
    protected boolean requiresArguments() {
        return false;
    }

    /** Checks if the number of arguments is correct (used for {@link #parseCommand(String) parsing}). */
    protected boolean checkArgumentsCount() {
        return true;
    }

    /**
     * Parses the command text arguments (used for {@link #parseCommand(String) parsing}).
     * <p>
     * The {@link #matcher} can be used to extract the arguments from the parsed text,
     * by using capturing groups defined in the command {@link #getPattern() pattern}.
     * @return <code>null</code> if the arguments were parsed successfully, otherwise the parse error message
     */
    protected String parseArguments() {
        return null;
    }

    /**
     * Called when a {@link com.diasemi.codelesslib.CodelessLibConfig#supportedCommands supported} command is received from the peer device.
     * <p>
     * Subclasses of supported commands can override this to implement the required behavior.
     * The command implementation is responsible for sending a proper response to the peer device.
     * <p>
     * The default behavior is to send a success response.
     * @see #sendSuccess(String)
     * @see #sendError(String)
     */
    public void processInbound() {
        sendSuccess();
    }

    /** Completes the command with a success response (no response message). */
    public void sendSuccess() {
        complete = true;
        manager.sendSuccess();
    }

    /**
     * Completes the command with a success response.
     * @param response the response message
     */
    public void sendSuccess(String response) {
        complete = true;
        manager.sendSuccess(response);
    }

    /**
     * Completes the command with an error response.
     * @param msg the error message
     */
    public void sendError(String msg) {
        complete = true;
        error = msg;
        manager.sendError(CodelessProfile.ERROR_PREFIX + msg);
    }

    /**
     * Sends a response message to the peer device.
     * @param response  the response message
     * @param more      <code>true</code> to add more response messages later, <code>false</code> to complete successfully
     * @see CodelessManager#sendResponse(String)
     */
    public void sendResponse(String response, boolean more) {
        manager.sendResponse(response);
        if (!more) {
            sendSuccess();
        }
    }

    /**
     * Decodes a number argument from a capturing group in {@link #matcher}.
     * @param group the capturing group index
     */
    protected Integer decodeNumberArgument(int group) {
        try {
            return Integer.decode(matcher.group(group));
        } catch (NumberFormatException e) {
            if (CodelessLibLog.COMMAND)
                Log.d(getTag(), "Invalid number argument: " + e.getMessage());
            return null;
        }
    }

    @NonNull
    @Override
    public String toString() {
        return "[" + getName() + "]";
    }
}
