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

package com.diasemi.codelesslib;

import android.util.Log;

import com.diasemi.codelesslib.CodelessProfile.CommandID;
import com.diasemi.codelesslib.command.CodelessCommand;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;

import androidx.annotation.NonNull;

/**
 * CodeLess commands scripting functionality.
 * <h2>Usage</h2>
 * You can create a script from a single string or a list of strings, with one command per line.
 * The single string script may contain empty lines, which are ignored. The script text is parsed
 * to a list of {@link CodelessCommand} objects. After creating the script, you can use {@link #hasInvalid()}
 * to check if the script contains invalid commands, or {@link #hasCustom()} to check if the script
 * contains unidentified commands.
 * <p>
 * The script commands are executed in sequence when {@link #start()} is called.
 * When the scripts starts, a {@link CodelessEvent.ScriptStartEvent ScriptStartEvent} is generated.
 * For each script command that is complete, a {@link CodelessEvent.ScriptCommandEvent ScriptCommandEvent} is generated.
 * When the script is complete, a {@link CodelessEvent.ScriptEndEvent ScriptEndEvent} is generated.
 * By default, the script will stop if a command fails. Use {@link #setStopOnError(boolean)} to modify this behavior.
 * <p>
 * For example, a script that uses two timers to toggle an output pin:
 * <blockquote><pre>
 * String text = "AT+IOCFG=10,4\n" +
 *               "AT+CMDSTORE=0,AT+IO=10,0;ATZ\n" +
 *               "AT+CMDSTORE=1,AT+IO=10,1;AT+TMRSTART=0,0,200\n" +
 *               "AT+TMRSTART=1,1,1";
 * CodelessScript script = new CodelessScript(manager, text);
 * script.start();</pre></blockquote>
 * @see CodelessManager
 * @see CodelessEvent
 */
public class CodelessScript {
    public static final String TAG = "CodelessScript";

    private static int nextScriptId;

    private int id = nextScriptId++;
    private String name;
    private CodelessManager manager;
    private ArrayList<String> script = new ArrayList<>();
    private ArrayList<CodelessCommand> commands = new ArrayList<>();
    private int current;
    private boolean stopOnError = true;
    private boolean invalid;
    private boolean custom;
    private boolean started;
    private boolean stopped;
    private boolean complete;

    /**
     * Creates a CodelessScript with no commands.
     * @param manager the manager used to run the script
     */
    public CodelessScript(CodelessManager manager) {
        this.manager = manager;
    }

    /**
     * Creates a CodelessScript.
     * @param manager   the manager used to run the script
     * @param text      the script text
     */
    public CodelessScript(CodelessManager manager, String text) {
        this(manager);
        setScript(text);
    }

    /**
     * Creates a CodelessScript.
     * @param manager   the manager used to run the script
     * @param script    the script text (one command per line)
     */
    public CodelessScript(CodelessManager manager, ArrayList<String> script) {
        this(manager);
        setScript(script);
    }

    /**
     * Creates a named CodelessScript with no commands.
     * @param name      the script name
     * @param manager   the manager used to run the script
     */
    public CodelessScript(String name, CodelessManager manager) {
        this(manager);
        this.name = name;
    }

    /**
     * Creates a named CodelessScript.
     * @param name      the script name
     * @param manager   the manager used to run the script
     * @param script    the script text
     */
    public CodelessScript(String name, CodelessManager manager, String script) {
        this(manager, script);
        this.name = name;
    }

    /**
     * Creates a named CodelessScript.
     * @param name      the script name
     * @param manager   the manager used to run the script
     * @param script    the script text (one command per line)
     */
    public CodelessScript(String name, CodelessManager manager, ArrayList<String> script) {
        this(manager, script);
        this.name = name;
    }

    /** Initializes the script by parsing the script text to a list of {@link CodelessCommand} objects. */
    private void initScript() {
        commands = new ArrayList<>(script.size());
        for (String text : script) {
            CodelessCommand command = manager.parseTextCommand(text);
            command.setScript(this);
            commands.add(command);
            if (!command.isValid())
                invalid = true;
            if (command.getCommandID() == CommandID.CUSTOM)
                custom = true;
        }
    }

    /**
     * Starts the script.
     * <p> A {@link CodelessEvent.ScriptStartEvent ScriptStartEvent} is generated.
     */
    public void start() {
        if (started)
            return;
        started = true;
        if (CodelessLibLog.SCRIPT)
            Log.d(TAG, "Script start: " + this);
        EventBus.getDefault().post(new CodelessEvent.ScriptStartEvent(this));
        current = -1;
        sendNextCommand();
    }

    /** Stops the script. */
    public void stop() {
        if (CodelessLibLog.SCRIPT)
            Log.d(TAG, "Script stopped: " + this);
        stopped = true;
        complete = true;
    }

    /**
     * Called when a script command completes successfully.
     * <p> A {@link CodelessEvent.ScriptCommandEvent ScriptCommandEvent} is generated.
     * <p> The script continues its execution with the next command.
     * @param command the command that succeeded
     */
    public void onSuccess(CodelessCommand command) {
        if (CodelessLibLog.SCRIPT)
            Log.d(TAG, "Script command success: " + this + " " + command);
        EventBus.getDefault().post(new CodelessEvent.ScriptCommandEvent(this, command));
        sendNextCommand();
    }

    /**
     * Called when a script command fails.
     * <p> A {@link CodelessEvent.ScriptCommandEvent ScriptCommandEvent} is generated.
     * <p>
     * The script can be configured to stop if a command fails, or ignore the error and
     * continue its execution with the next command.
     * @param command the command that failed
     */
    public void onError(CodelessCommand command) {
        if (CodelessLibLog.SCRIPT)
            Log.d(TAG, "Script command error: " + this + " " + command + " " + command.getError());
        EventBus.getDefault().post(new CodelessEvent.ScriptCommandEvent(this, command));
        if (!stopOnError) {
            sendNextCommand();
        } else {
            stop();
            EventBus.getDefault().post(new CodelessEvent.ScriptEndEvent(this, true));
        }
    }

    /**
     * Continues the script execution with the next command.
     * <p> If there are no commands left, a {@link CodelessEvent.ScriptEndEvent ScriptEndEvent} is generated.
     */
    private void sendNextCommand() {
        if (stopped)
            EventBus.getDefault().post(new CodelessEvent.ScriptEndEvent(this, false));
        if (complete)
            return;
        current++;
        if (current < commands.size()) {
            CodelessCommand command = getCurrentCommand();
            if (CodelessLibLog.SCRIPT)
                Log.d(TAG, "Script command: " + this + "[" + (current +  1) + "] " + command);
            manager.sendCommand(command);
        } else {
            complete = true;
            if (CodelessLibLog.SCRIPT)
                Log.d(TAG, "Script end: " + this);
            EventBus.getDefault().post(new CodelessEvent.ScriptEndEvent(this, false));
        }
    }

    /** Returns the script ID (unique per app session). */
    public int getId() {
        return id;
    }

    /**
     * Sets the script unique ID.
     * @param id the script ID
     */
    public void setId(int id) {
        this.id = id;
    }

    /** Returns the script name. */
    public String getName() {
        return name;
    }

    /**
     * Sets the script name.
     * @param name the script name
     */
    public void setName(String name) {
        this.name = name;
    }

    /** Returns the associated {@link CodelessManager manager}. */
    public CodelessManager getManager() {
        return manager;
    }

    /** Returns the script text. */
    public ArrayList<String> getScript() {
        return script;
    }

    /**
     * Sets the script text and parses it to a list of {@link CodelessCommand} objects.
     * @param script the script text (one command per line)
     */
    public void setScript(ArrayList<String> script) {
        if (started)
            return;
        this.script = script;
        initScript();
    }

    /**
     * Sets the script text and parses it to a list of {@link CodelessCommand} objects.
     * @param text the script text
     * @see #setScript(ArrayList)
     */
    public void setScript(String text) {
        if (started)
            return;
        script = new ArrayList<>();
        for (String line : text.split("\n")) {
            line = line.trim();
            if (!line.isEmpty())
                script.add(line);
        }
        initScript();
    }

    /** Returns the whole script text as one string. */
    public String getText() {
        if (script.isEmpty())
            return "";
        StringBuilder text = new StringBuilder();
        for (String command : script)
            text.append(command).append("\n");
        return text.toString();
    }

    /** Returns the parsed script commands as a list of {@link CodelessCommand} objects. */
    public ArrayList<CodelessCommand> getCommands() {
        return commands;
    }

    /**
     * Sets the script commands to a list of prepared {@link CodelessCommand} objects.
     * <p> The corresponding script text is also created.
     * @param commands the script commands
     */
    public void setCommands(ArrayList<CodelessCommand> commands) {
        if (started)
            return;
        this.commands = commands;
        script = new ArrayList<>(commands.size());
        for (CodelessCommand command : commands) {
            if (!command.isParsed())
                command.packCommand();
            String text = command.getCommand();
            if (!command.isValid())
                invalid = true;
            if (command.getCommandID() == CommandID.CUSTOM)
                custom = true;
            else if (command.hasPrefix())
                text = command.getPrefix() + text;
            script.add(text);
        }
    }

    /** Returns the current command index (0-based). */
    public int getCurrent() {
        return current;
    }

    /** Returns the current {@link CodelessCommand command} object. */
    public CodelessCommand getCurrentCommand() {
        return commands.get(current);
    }

    /** Returns the current command text. */
    public String getCurrentCommandText() {
        return script.get(current);
    }

    /**
     * Returns the command index for the specified {@link CodelessCommand command} object.
     * @param command the command to search
     */
    public int getCommandIndex(CodelessCommand command) {
        return commands.indexOf(command);
    }

    /**
     * Sets the current command index, if the script is not running.
     * @param current the command index (0-based). This is the first command that will be executed when the script is started.
     */
    public void setCurrent(int current) {
        if (!started)
            this.current = current;
    }

    /** Returns the stop on error configuration. */
    public boolean stopOnError() {
        return stopOnError;
    }

    /**
     * Configures the stop on error behavior.
     * @param stopOnError <code>true</code> to stop the script if a command fails, <code>false</code> to continue execution
     */
    public void setStopOnError(boolean stopOnError) {
        this.stopOnError = stopOnError;
    }

    /** Checks if the script contains invalid commands. */
    public boolean hasInvalid() {
        return invalid;
    }

    /** Checks if the script contains unidentified commands. */
    public boolean hasCustom() {
        return custom;
    }

    /** Checks if the script has started. */
    public boolean isStarted() {
        return started;
    }

    /** Checks if the script was stopped by the user or due to an error. */
    public boolean isStopped() {
        return stopped;
    }

    /** Checks if the script is complete. */
    public boolean isComplete() {
        return complete;
    }

    @NonNull
    @Override
    public String toString() {
        return "[" + (name != null ? name : "Script " + id) + "]";
    }
}
