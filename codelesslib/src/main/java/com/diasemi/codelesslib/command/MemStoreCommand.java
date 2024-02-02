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
import com.diasemi.codelesslib.CodelessLibConfig;
import com.diasemi.codelesslib.CodelessLibLog;
import com.diasemi.codelesslib.CodelessManager;
import com.diasemi.codelesslib.CodelessProfile;
import com.diasemi.codelesslib.CodelessProfile.CommandID;

import org.greenrobot.eventbus.EventBus;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * <code>AT+MEM</code> command implementation.
 * @see CodelessEvent.MemoryTextContent
 * @see <a href="https://lpccs-docs.renesas.com/UM-140-DA145x-CodeLess/index.html">AT commands documentation</a>
 */
public class MemStoreCommand extends CodelessCommand {
    public static final String TAG = "MemStoreCommand";

    public static final String COMMAND = "MEM";
    public static final String NAME = CodelessProfile.PREFIX_LOCAL + COMMAND;
    public static final CommandID ID = CommandID.MEM;

    public static final String PATTERN_STRING = "^MEM=(\\d+)(?:,(.*))?$"; // <index> <text>
    public static final Pattern PATTERN = Pattern.compile(PATTERN_STRING);

    /** The memory slot index argument (0-3). */
    private int memIndex;
    /** The stored text argument/response. */
    private String text;

    /**
     * Creates an <code>AT+MEM</code> command.
     * @param manager   the associated manager
     * @param memIndex  the memory slot index argument (0-3)
     */
    public MemStoreCommand(CodelessManager manager, int memIndex) {
        this(manager);
        setMemIndex(memIndex);
    }

    /**
     * Creates an <code>AT+MEM</code> command.
     * @param manager   the associated manager
     * @param memIndex  the memory slot index argument (0-3)
     * @param text      the stored text argument
     */
    public MemStoreCommand(CodelessManager manager, int memIndex, String text) {
        this(manager, memIndex);
        this.text = text;
    }

    /**
     * Default constructor.
     * @see CodelessCommand#CodelessCommand(CodelessManager)
     */
    public MemStoreCommand(CodelessManager manager) {
        super(manager);
    }

    /**
     * Default constructor.
     * @see CodelessCommand#CodelessCommand(CodelessManager, String, boolean)
     */
    public MemStoreCommand(CodelessManager manager, String command, boolean parse) {
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
        String arguments = String.format(Locale.US, "%d", memIndex);
        if (text != null)
            arguments += "," + text;
        return arguments;
    }

    @Override
    public void parseResponse(String response) {
        super.parseResponse(response);
        if (responseLine() == 1) {
            text = response;
            if (CodelessLibLog.COMMAND)
                Log.d(TAG, "Memory index: " + memIndex + " contains: " + text);
        }
    }

    @Override
    public void onSuccess() {
        super.onSuccess();
        if (isValid())
            EventBus.getDefault().post(new CodelessEvent.MemoryTextContent(this));
    }

    @Override
    protected boolean requiresArguments() {
        return true;
    }

    @Override
    protected boolean checkArgumentsCount() {
        int count = CodelessProfile.countArguments(command, ",");
        return count == 1 || count == 2;
    }

    @Override
    protected String parseArguments() {
        Integer num = decodeNumberArgument(1);
        if (num == null || CodelessLibConfig.CHECK_MEM_INDEX && (num < CodelessLibConfig.MEM_INDEX_MIN || num > CodelessLibConfig.MEM_INDEX_MAX))
            return "Invalid memory index";
        memIndex = num;

        if (CodelessProfile.countArguments(command, ",") == 1)
            return null;
        text = matcher.group(2);
        if (CodelessLibConfig.CHECK_MEM_CONTENT_SIZE && text.length() > CodelessLibConfig.MEM_MAX_CHAR_COUNT)
            return "Text exceeds max character number";

        return null;
    }

    /** Returns the memory slot index argument (0-3). */
    public int getMemIndex() {
        return memIndex;
    }

    /** Sets the memory slot index argument (0-3). */
    public void setMemIndex(int memIndex) {
        this.memIndex = memIndex;
        if (CodelessLibConfig.CHECK_MEM_INDEX) {
            if (memIndex < CodelessLibConfig.MEM_INDEX_MIN || memIndex > CodelessLibConfig.MEM_INDEX_MAX)
                invalid = true;
        }
    }

    /** Returns the stored text argument/response. */
    public String getText() {
        return text;
    }

    /** Sets the stored text argument. */
    public void setText(String text) {
        this.text = text;
        if (CodelessLibConfig.CHECK_MEM_CONTENT_SIZE && text.length() > CodelessLibConfig.MEM_MAX_CHAR_COUNT)
            invalid = true;
    }
}
