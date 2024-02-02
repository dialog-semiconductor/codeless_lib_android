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
import com.diasemi.codelesslib.CodelessManager;
import com.diasemi.codelesslib.CodelessProfile;
import com.diasemi.codelesslib.CodelessProfile.Command;
import com.diasemi.codelesslib.CodelessProfile.CommandID;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * <code>AT+CHGBNDP</code> command implementation.
 * @see CodelessEvent.BondingEntryPersistenceTableStatus
 * @see CodelessEvent.BondingEntryPersistenceStatusSet
 * @see <a href="https://lpccs-docs.renesas.com/UM-140-DA145x-CodeLess/index.html">AT commands documentation</a>
 */
public class BondingEntryStatusCommand extends CodelessCommand {
    public static final String TAG = "BondingEntryStatusCommand";

    public static final String COMMAND = "CHGBNDP";
    public static final String NAME = CodelessProfile.PREFIX_LOCAL + COMMAND;
    public static final CommandID ID = CommandID.CHGBNDP;

    public static final String PATTERN_STRING = "^CHGBNDP(?:=(0x[0-9a-fA-F]+|\\d+),(\\d))?$"; // <index> <status>
    public static final Pattern PATTERN = Pattern.compile(PATTERN_STRING);

    /** The bonding entry index argument. */
    private int index;
    /** The bonding entry persistence status argument. */
    private boolean persistent;
    /** The bonding entries persistence status response. */
    private ArrayList<Boolean> tablePersistenceStatus;
    /** <code>true</code> if the command has arguments, <code>false</code> for no arguments. */
    private boolean hasArguments;

    /**
     * Creates an <code>AT+CHGBNDP</code> command.
     * @param manager       the associated manager
     * @param index         the bonding entry index argument (1-5, 0xFF: all entries)
     * @param persistent    the bonding entry persistence status argument (<code>true</code> for enabled, <code>false</code> for disabled)
     */
    public BondingEntryStatusCommand(CodelessManager manager, int index, boolean persistent) {
        super(manager);
        setIndex(index);
        this.persistent = persistent;
        hasArguments = true;
    }

    /**
     * Default constructor.
     * @see CodelessCommand#CodelessCommand(CodelessManager)
     */
    public BondingEntryStatusCommand(CodelessManager manager) {
        super(manager);
    }

    /**
     * Default constructor.
     * @see CodelessCommand#CodelessCommand(CodelessManager, String, boolean)
     */
    public BondingEntryStatusCommand(CodelessManager manager, String command, boolean parse) {
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
        return hasArguments ? String.format(Locale.US, "%d,%d", index, (persistent ? Command.BONDING_ENTRY_PERSISTENT : Command.BONDING_ENTRY_NON_PERSISTENT)) : null;
    }

    @Override
    public void parseResponse(String response) {
        super.parseResponse(response);
        try {
            if (tablePersistenceStatus == null)
                tablePersistenceStatus = new ArrayList<>();
            int index;
            Boolean status = null;
            int commaPos = response.indexOf(",");
            index = Integer.parseInt(response.substring(0, commaPos));
            if (CodelessLibConfig.CHECK_BONDING_DATABASE_INDEX && (index < CodelessLibConfig.BONDING_DATABASE_INDEX_MIN || index > CodelessLibConfig.BONDING_DATABASE_INDEX_MAX))
                invalid = true;
            String statusStr = response.substring(commaPos + 1);
            if (!statusStr.equals("<empty>") && Integer.parseInt(statusStr) != Command.BONDING_ENTRY_NON_PERSISTENT && Integer.parseInt(statusStr) != Command.BONDING_ENTRY_PERSISTENT)
                invalid = true;
            if (!statusStr.equals("<empty>"))
                status = Integer.parseInt(statusStr) != Command.BONDING_ENTRY_NON_PERSISTENT;
            tablePersistenceStatus.add(status);
            if (!invalid)
                Log.d(TAG, "Bonding persistence status: " + index + ", " + (status != null ? (status ? "persistent" : "non-persistent") : "empty"));
        } catch (IndexOutOfBoundsException | NumberFormatException e) {
            invalid = true;
        }
        if (invalid)
            Log.e(TAG, "Received invalid bonding entry persistence status: " + response);
    }

    @Override
    public void onSuccess() {
        super.onSuccess();
        if (isValid())
            EventBus.getDefault().post(hasArguments ? new CodelessEvent.BondingEntryPersistenceStatusSet(this) : new CodelessEvent.BondingEntryPersistenceTableStatus(this));
    }

    @Override
    protected boolean checkArgumentsCount() {
        int count = CodelessProfile.countArguments(command, ",");
        return count == 0 || count == 2;
    }

    @Override
    protected String parseArguments() {
        if (!CodelessProfile.hasArguments(command))
            return null;
        hasArguments = true;

        Integer num = decodeNumberArgument(1);
        if (num == null || CodelessLibConfig.CHECK_BONDING_DATABASE_INDEX && (num < CodelessLibConfig.BONDING_DATABASE_INDEX_MIN || num > CodelessLibConfig.BONDING_DATABASE_INDEX_MAX) && num != CodelessLibConfig.BONDING_DATABASE_ALL_VALUES)
            return "Invalid bonding database index";
        index = num;

        num = decodeNumberArgument(2);
        if (num == null || num != Command.BONDING_ENTRY_NON_PERSISTENT && num != Command.BONDING_ENTRY_PERSISTENT)
            return "Invalid bonding entry persistent status";
        persistent = num == Command.BONDING_ENTRY_PERSISTENT;

        return null;
    }

    /** Returns the bonding entry index argument. */
    public int getIndex() {
        return index;
    }

    /** Sets the bonding entry index argument (1-5, 0xFF: all entries). */
    public void setIndex(int index) {
        this.index = index;
        if (CodelessLibConfig.CHECK_BONDING_DATABASE_INDEX) {
            if ((index < CodelessLibConfig.BONDING_DATABASE_INDEX_MIN || index > CodelessLibConfig.BONDING_DATABASE_INDEX_MAX) && index != CodelessLibConfig.BONDING_DATABASE_ALL_VALUES)
                invalid = true;
        }
    }

    /** Returns the bonding entry persistence status argument. */
    public boolean persistent() {
        return persistent;
    }

    /** Sets the bonding entry persistence status argument (<code>true</code> for enabled, <code>false</code> for disabled). */
    public void setPersistent(boolean persistent) {
        this.persistent = persistent;
    }

    /**
     * Returns the bonding entries persistence status response.
     * <p> One Boolean value per bonding entry: <code>true</code> if persistence is enabled, <code>false</code> if it is disabled, <code>null</code> if entry is unused.
     */
    public ArrayList<Boolean> getTablePersistenceStatus() {
        return tablePersistenceStatus;
    }
}
