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

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.diasemi.codelesslib.CodelessProfile.Command.DLE_DISABLED;
import static com.diasemi.codelesslib.CodelessProfile.Command.DLE_ENABLED;
import static com.diasemi.codelesslib.CodelessProfile.Command.DLE_PACKET_LENGTH_DEFAULT;
import static com.diasemi.codelesslib.CodelessProfile.Command.DLE_PACKET_LENGTH_MAX;
import static com.diasemi.codelesslib.CodelessProfile.Command.DLE_PACKET_LENGTH_MIN;

/**
 * <code>AT+DLEEN</code> command implementation.
 * @see CodelessEvent.DataLengthEnable
 * @see <a href="https://lpccs-docs.renesas.com/UM-140-DA145x-CodeLess/index.html">AT commands documentation</a>
 */
public class DataLengthEnableCommand extends CodelessCommand {
    public static final String TAG = "DataLengthEnableCommand";

    public static final String COMMAND = "DLEEN";
    public static final String NAME = CodelessProfile.PREFIX_LOCAL + COMMAND;
    public static final CommandID ID = CommandID.DLEEN;

    public static final String PATTERN_STRING = "^DLEEN(?:=(\\d),(\\d+),(\\d+))?$"; // <enable> <tx> <rx>
    public static final Pattern PATTERN = Pattern.compile(PATTERN_STRING);

    private static final String RESPONSE_PATTERN_STRING = "^(\\d).(\\d+).(\\d+)$"; // <enable> <tx> <rx>
    private static final Pattern RESPONSE_PATTERN = Pattern.compile(RESPONSE_PATTERN_STRING);

    /** The DLE configuration argument/response (<code>true</code> for enabled, <code>false</code> for disabled). */
    private boolean enabled;
    /** The DLE TX packet length argument/response. */
    private int txPacketLength;
    /** The DLE RX packet length argument/response. */
    private int rxPacketLength;
    /** <code>true</code> if the command has arguments, <code>false</code> for no arguments. */
    private boolean hasArguments;

    /**
     * Creates an <code>AT+DLEEN</code> command.
     * @param manager           the associated manager
     * @param enabled           the DLE configuration argument (<code>true</code> for enabled, <code>false</code> for disabled)
     * @param txPacketLength    the DLE TX packet length argument
     * @param rxPacketLength    the DLE RX packet length argument
     */
    public DataLengthEnableCommand(CodelessManager manager, boolean enabled, int txPacketLength, int rxPacketLength) {
        this(manager);
        this.enabled = enabled;
        setTxPacketLength(txPacketLength);
        setRxPacketLength(rxPacketLength);
        hasArguments = true;
    }

    /**
     * Creates an <code>AT+DLEEN</code> command.
     * @param manager   the associated manager
     * @param enabled   the DLE configuration argument (<code>true</code> for enabled, <code>false</code> for disabled)
     */
    public DataLengthEnableCommand(CodelessManager manager, boolean enabled) {
        this(manager, enabled, DLE_PACKET_LENGTH_DEFAULT, DLE_PACKET_LENGTH_DEFAULT);
    }

    /**
     * Default constructor.
     * @see CodelessCommand#CodelessCommand(CodelessManager)
     */
    public DataLengthEnableCommand(CodelessManager manager) {
        super(manager);
    }

    /**
     * Default constructor.
     * @see CodelessCommand#CodelessCommand(CodelessManager, String, boolean)
     */
    public DataLengthEnableCommand(CodelessManager manager, String command, boolean parse) {
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
        return hasArguments ? String.format(Locale.US, "%d,%d,%d", enabled ? DLE_ENABLED : DLE_DISABLED, txPacketLength, rxPacketLength) : null;
    }

    @Override
    public void parseResponse(String response) {
        super.parseResponse(response);
        if (responseLine() == 1) {
            Matcher matcher = RESPONSE_PATTERN.matcher(response);
            if (matcher.matches()) {
                try {
                    int num = Integer.parseInt(matcher.group(1));
                    if (num != DLE_DISABLED && num != DLE_ENABLED)
                        invalid = true;
                    enabled = num != DLE_DISABLED;
                    setTxPacketLength(Integer.parseInt(matcher.group(2)));
                    setRxPacketLength(Integer.parseInt(matcher.group(3)));
                } catch (NumberFormatException e) {
                    invalid = true;
                }
            } else {
                invalid = true;
            }
            if (invalid)
                Log.e(TAG, "Received invalid DLE parameters: " + response);
            else if (CodelessLibLog.COMMAND)
                Log.d(TAG, "DLE: " + (enabled ? "enabled" : "disabled") + " tx=" + txPacketLength + " rx=" + rxPacketLength);
        }
    }

    @Override
    public void onSuccess() {
        super.onSuccess();
        if (isValid())
            EventBus.getDefault().post(new CodelessEvent.DataLengthEnable(this));
    }

    @Override
    protected boolean checkArgumentsCount() {
        int count = CodelessProfile.countArguments(command, ",");
        return count == 0 || count == 3;
    }

    @Override
    protected String parseArguments() {
        if (!CodelessProfile.hasArguments(command))
            return null;
        hasArguments = true;

        Integer num = decodeNumberArgument(1);
        if (num == null || num != DLE_DISABLED && num != DLE_ENABLED)
            return "Enable must be 0 or 1";
        enabled = num != DLE_DISABLED;

        num = decodeNumberArgument(2);
        if (num == null || num < DLE_PACKET_LENGTH_MIN || num > DLE_PACKET_LENGTH_MAX)
            return "Invalid TX packet length";
        txPacketLength = num;

        num = decodeNumberArgument(3);
        if (num == null || num < DLE_PACKET_LENGTH_MIN || num > DLE_PACKET_LENGTH_MAX)
            return "Invalid RX packet length";
        rxPacketLength = num;

        return null;
    }

    /** Returns the DLE configuration argument/response (<code>true</code> for enabled, <code>false</code> for disabled). */
    public boolean enabled() {
        return enabled;
    }

    /** Sets the DLE configuration argument (<code>true</code> for enabled, <code>false</code> for disabled). */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /** Returns the DLE TX packet length argument/response. */
    public int getTxPacketLength() {
        return txPacketLength;
    }

    /** Sets the DLE TX packet length argument. */
    public void setTxPacketLength(int txPacketLength) {
        this.txPacketLength = txPacketLength;
        if (txPacketLength < DLE_PACKET_LENGTH_MIN || txPacketLength > DLE_PACKET_LENGTH_MAX)
            invalid = true;
    }

    /** Returns the DLE RX packet length argument/response. */
    public int getRxPacketLength() {
        return rxPacketLength;
    }

    /** Sets the DLE RX packet length argument. */
    public void setRxPacketLength(int rxPacketLength) {
        this.rxPacketLength = rxPacketLength;
        if (rxPacketLength < DLE_PACKET_LENGTH_MIN || rxPacketLength > DLE_PACKET_LENGTH_MAX)
            invalid = true;
    }
}
