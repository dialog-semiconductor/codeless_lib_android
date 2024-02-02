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
import com.diasemi.codelesslib.CodelessProfile.BondingEntry;
import com.diasemi.codelesslib.CodelessProfile.CommandID;

import org.greenrobot.eventbus.EventBus;

import java.util.Locale;
import java.util.regex.Pattern;

import static com.diasemi.codelesslib.CodelessUtil.hex;
import static com.diasemi.codelesslib.CodelessUtil.hex2bytes;
import static com.diasemi.codelesslib.CodelessUtil.hexArray;

/**
 * <code>AT+IEBNDE</code> command implementation.
 * @see CodelessEvent.BondingEntryEvent
 * @see CodelessProfile.BondingEntry
 * @see <a href="https://lpccs-docs.renesas.com/UM-140-DA145x-CodeLess/index.html">AT commands documentation</a>
 */
public class BondingEntryTransferCommand extends CodelessCommand {
    public static final String TAG = "BondingEntryTransferCommand";

    public static final String COMMAND = "IEBNDE";
    public static final String NAME = CodelessProfile.PREFIX_LOCAL + COMMAND;
    public static final CommandID ID = CommandID.IEBNDE;

    public static final String PATTERN_STRING = "^IEBNDE=(\\d+)(?:,([0-9a-fA-F]{54};[0-9a-fA-F]{50};[0-9a-fA-F]{32};[0-9a-fA-F]{2};[0-9a-fA-F]{8}))?$"; // <index> <entry>
    public static final Pattern PATTERN = Pattern.compile(PATTERN_STRING);

    protected static final String ENTRY_ARGUMENT_PATTERN_STRING = "^[0-9a-fA-F]{54};[0-9a-fA-F]{50};[0-9a-fA-F]{32};[0-9a-fA-F]{2};[0-9a-fA-F]{8}$";
    protected static final Pattern ENTRY_ARGUMENT_PATTERN = Pattern.compile(ENTRY_ARGUMENT_PATTERN_STRING);

    /**
     * Checks if a bonding entry configuration argument/response has the correct format.
     * @param data the bonding entry configuration data to check (packed hex data)
     */
    public static boolean validData(String data) {
        return ENTRY_ARGUMENT_PATTERN.matcher(data).matches();
    }

    /** The bonding entry index argument. */
    private int index;
    /** The bonding entry configuration argument/response (packed hex data). */
    private String entry;
    /** The bonding entry configuration argument/response (unpacked). */
    private BondingEntry bondingEntry;

    /**
     * Creates an <code>AT+IEBNDE</code> command.
     * @param manager   the associated manager
     * @param index     the bonding entry index argument (1-5)
     */
    public BondingEntryTransferCommand(CodelessManager manager, int index) {
        super(manager);
        setIndex(index);
    }

    /**
     * Creates an <code>AT+IEBNDE</code> command.
     * @param manager       the associated manager
     * @param index         the bonding entry index argument (1-5)
     * @param bondingEntry  the bonding entry configuration argument (unpacked)
     */
    public BondingEntryTransferCommand(CodelessManager manager, int index, BondingEntry bondingEntry) {
        super(manager);
        setIndex(index);
        this.bondingEntry = bondingEntry;
        packEntry(bondingEntry);
    }

    /**
     * Creates an <code>AT+IEBNDE</code> command.
     * @param manager   the associated manager
     * @param index     the bonding entry index argument (1-5)
     * @param entry     the bonding entry configuration argument (packed hex data)
     */
    public BondingEntryTransferCommand(CodelessManager manager, int index, String entry) {
        super(manager);
        setIndex(index);
        this.entry = entry;
        parseEntry(entry);
    }

    /**
     * Default constructor.
     * @see CodelessCommand#CodelessCommand(CodelessManager)
     */
    public BondingEntryTransferCommand(CodelessManager manager) {
        super(manager);
    }

    /**
     * Default constructor.
     * @see CodelessCommand#CodelessCommand(CodelessManager, String, boolean)
     */
    public BondingEntryTransferCommand(CodelessManager manager, String command, boolean parse) {
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
        String arguments = String.format(Locale.US, "%d", index);
        if (entry != null)
            arguments += "," + entry;
        return arguments;
    }

    @Override
    public void parseResponse(String response) {
        super.parseResponse(response);
        if (responseLine() == 1) {
            if (validData(response)) {
                entry = response;
                parseEntry(response);
            } else {
                invalid = true;
            }
            if (invalid)
                Log.e(TAG, "Received invalid bonding entry: " + response);
            else if (CodelessLibLog.COMMAND)
                Log.d(TAG, String.format(Locale.US, "Bonding entry: LTK:%s EDIV:%04X(%d) Rand:%s Key size:%02X(%d) CSRK:%s Bluetooth address:%s Address type:%02X(%d) Authentication level:%02X(%d) Bonding database slot:%02X(%d) IRK:%s Persistence status:%02X(%d) Timestamp:%s",
                        hexArray(bondingEntry.ltk), bondingEntry.ediv, bondingEntry.ediv, hexArray(bondingEntry.rand), bondingEntry.keySize, bondingEntry.keySize, hexArray(bondingEntry.csrk), hexArray(bondingEntry.bluetoothAddress), bondingEntry.addressType, bondingEntry.addressType,
                        bondingEntry.authenticationLevel, bondingEntry.authenticationLevel, bondingEntry.bondingDatabaseSlot, bondingEntry.bondingDatabaseSlot, hexArray(bondingEntry.irk), bondingEntry.persistenceStatus, bondingEntry.persistenceStatus, hexArray(bondingEntry.timestamp)));
        }
    }

    @Override
    public void onSuccess() {
        super.onSuccess();
        if (isValid())
            EventBus.getDefault().post(new CodelessEvent.BondingEntryEvent(this));
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
        if (num == null || CodelessLibConfig.CHECK_BONDING_DATABASE_INDEX && (num < CodelessLibConfig.BONDING_DATABASE_INDEX_MIN || num > CodelessLibConfig.BONDING_DATABASE_INDEX_MAX))
            return "Invalid bonding database index";
        index = num;

        if (CodelessProfile.countArguments(command, ",") == 1)
            return null;
        String entry = matcher.group(2);
        if (validData(entry)) {
            this.entry = entry;
            parseEntry(entry);
        } else {
            return "Invalid database entry";
        }

        return null;
    }

    /**
     * Parses the bonding entry configuration argument/response and stores it to {@link #bondingEntry}.
     * @param entry the bonding entry configuration argument/response (packed hex data)
     */
    private void parseEntry(String entry) {
        bondingEntry = new BondingEntry();
        try {
            bondingEntry.ltk = hex2bytes(entry.substring(0, 32));
            bondingEntry.ediv = Integer.parseInt(entry.substring(32, 36), 16);
            bondingEntry.rand = hex2bytes(entry.substring(36, 52));
            bondingEntry.keySize = Integer.parseInt(entry.substring(52, 54), 16);
            bondingEntry.csrk = hex2bytes(entry.substring(55, 87));
            bondingEntry.bluetoothAddress = hex2bytes(entry.substring(87, 99));
            bondingEntry.addressType = Integer.parseInt(entry.substring(99, 101), 16);
            bondingEntry.authenticationLevel = Integer.parseInt(entry.substring(101, 103), 16);
            bondingEntry.bondingDatabaseSlot = Integer.parseInt(entry.substring(103, 105), 16);
            bondingEntry.irk = hex2bytes(entry.substring(106, 138));
            bondingEntry.persistenceStatus = Integer.parseInt(entry.substring(139, 141), 16);
            bondingEntry.timestamp = hex2bytes(entry.substring(142, 150));
        } catch (IndexOutOfBoundsException | NumberFormatException e) {
            invalid = true;
        }
    }

    /**
     * Packs the bonding entry configuration argument and stores it to {@link #entry}.
     * @param bondingEntry the bonding entry configuration argument (unpacked)
     */
    private void packEntry(BondingEntry bondingEntry) {
        StringBuilder entry = new StringBuilder();
        entry.append(hex(bondingEntry.ltk));
        entry.append(String.format(Locale.US, "%04X", bondingEntry.ediv));
        entry.append(hex(bondingEntry.rand));
        entry.append(String.format(Locale.US, "%02X", bondingEntry.keySize));
        entry.append(";");
        entry.append(hex(bondingEntry.csrk));
        entry.append(hex(bondingEntry.bluetoothAddress));
        entry.append(String.format(Locale.US, "%02X", bondingEntry.addressType));
        entry.append(String.format(Locale.US, "%02X", bondingEntry.authenticationLevel));
        entry.append(String.format(Locale.US, "%02X", bondingEntry.bondingDatabaseSlot));
        entry.append(";");
        entry.append(hex(bondingEntry.irk));
        entry.append(";");
        entry.append(String.format(Locale.US, "%02X", bondingEntry.persistenceStatus));
        entry.append(";");
        entry.append(hex(bondingEntry.timestamp));
        this.entry = entry.toString();
    }

    /** Returns the bonding entry index argument. */
    public int getIndex() {
        return index;
    }

    /** Sets the bonding entry index argument (1-5). */
    public void setIndex(int index) {
        this.index = index;
        if (CodelessLibConfig.CHECK_BONDING_DATABASE_INDEX) {
            if (index < CodelessLibConfig.BONDING_DATABASE_INDEX_MIN || index > CodelessLibConfig.BONDING_DATABASE_INDEX_MAX)
                invalid = true;
        }
    }

    /** Returns the bonding entry configuration argument/response (packed hex data). */
    public String getEntry() {
        return entry;
    }

    /** Sets the bonding entry configuration argument (packed hex data). */
    public void setEntry(String entry) {
        this.entry = entry;
        parseEntry(entry);
    }

    /** Returns the bonding entry configuration argument/response (unpacked). */
    public BondingEntry getBondingEntry() {
        return bondingEntry;
    }

    /** Sets the bonding entry configuration argument (unpacked). */
    public void setBondingEntry(BondingEntry bondingEntry) {
        this.bondingEntry = bondingEntry;
        packEntry(bondingEntry);
    }
}
