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
import com.diasemi.codelesslib.CodelessProfile.GPIO;

import org.greenrobot.eventbus.EventBus;

import java.util.Locale;
import java.util.regex.Pattern;

import static com.diasemi.codelesslib.CodelessProfile.Command.DISABLE_UART_FLOW_CONTROL;
import static com.diasemi.codelesslib.CodelessProfile.Command.ENABLE_UART_FLOW_CONTROL;
import static com.diasemi.codelesslib.CodelessProfile.Command.GPIO_FUNCTION_UART_CTS;
import static com.diasemi.codelesslib.CodelessProfile.Command.GPIO_FUNCTION_UART_RTS;

/**
 * <code>AT+FLOWCONTROL</code> command implementation.
 * @see CodelessEvent.FlowControl
 * @see <a href="https://lpccs-docs.renesas.com/UM-140-DA145x-CodeLess/index.html">AT commands documentation</a>
 */
public class FlowControlCommand extends CodelessCommand {
    public static final String TAG = "FlowControlCommand";

    public static final String COMMAND = "FLOWCONTROL";
    public static final String NAME = CodelessProfile.PREFIX_LOCAL + COMMAND;
    public static final CommandID ID = CommandID.FLOWCONTROL;

    public static final String PATTERN_STRING = "^FLOWCONTROL(?:=(\\d),(\\d+),(\\d+))?$"; // <fc_mode> <rts_pin> <cts_pin>
    public static final Pattern PATTERN = Pattern.compile(PATTERN_STRING);

    /** The flow control mode argument/response. */
    private int mode;
    /** The RTS pin argument/response. */
    private GPIO rtsGpio;
    /** The CTS pin argument/response. */
    private GPIO ctsGpio;
    /** <code>true</code> if the command has arguments, <code>false</code> for no arguments. */
    private boolean hasArguments;

    /**
     * Creates an <code>AT+FLOWCONTROL</code> command.
     * @param manager   the associated manager
     * @param enabled   the flow control mode argument (<code>true</code> to enable, <code>false</code> to disable)
     * @param rtsGpio   the RTS pin argument
     * @param ctsGpio   the CTS pin argument
     */
    public FlowControlCommand(CodelessManager manager, boolean enabled, GPIO rtsGpio, GPIO ctsGpio) {
        this(manager, enabled ? ENABLE_UART_FLOW_CONTROL : DISABLE_UART_FLOW_CONTROL, rtsGpio, ctsGpio);
    }

    /**
     * Creates an <code>AT+FLOWCONTROL</code> command.
     * @param manager   the associated manager
     * @param mode      the flow control mode argument
     * @param rtsGpio   the RTS pin argument
     * @param ctsGpio   the CTS pin argument
     */
    public FlowControlCommand(CodelessManager manager, int mode, GPIO rtsGpio, GPIO ctsGpio) {
        this(manager);
        setMode(mode);
        setRtsGpio(rtsGpio);
        setCtsGpio(ctsGpio);
        hasArguments = true;
    }

    /**
     * Default constructor.
     * @see CodelessCommand#CodelessCommand(CodelessManager)
     */
    public FlowControlCommand(CodelessManager manager) {
        super(manager);
    }

    /**
     * Default constructor.
     * @see CodelessCommand#CodelessCommand(CodelessManager, String, boolean)
     */
    public FlowControlCommand(CodelessManager manager, String command, boolean parse) {
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
        return hasArguments ? String.format(Locale.US, "%d,%d,%d", mode, rtsGpio.getGpio(), ctsGpio.getGpio()) : null;
    }

    @Override
    public void parseResponse(String response) {
        super.parseResponse(response);
        if (responseLine() == 1) {
            try {
                String[] values = response.split(" ");
                mode = Integer.parseInt(values[0]);
                if (mode != DISABLE_UART_FLOW_CONTROL && mode != ENABLE_UART_FLOW_CONTROL)
                    invalid = true;

                rtsGpio = new GPIO();
                rtsGpio.setGpio(Integer.parseInt(values[1]));
                rtsGpio.function = GPIO_FUNCTION_UART_RTS;

                ctsGpio = new GPIO();
                ctsGpio.setGpio(Integer.parseInt(values[2]));
                ctsGpio.function = GPIO_FUNCTION_UART_CTS;
            } catch (IndexOutOfBoundsException | NumberFormatException e) {
                invalid = true;
            }
            if (invalid)
                Log.e(TAG, "Received invalid flow control response: " + response);
            else if (CodelessLibLog.COMMAND)
                Log.d(TAG, "Flow control: " + (mode == ENABLE_UART_FLOW_CONTROL ? "Enabled" : "Disabled") + " RTS=" + rtsGpio.name() + " CTS=" + ctsGpio.name());
        }
    }

    @Override
    public void onSuccess() {
        super.onSuccess();
        if (isValid())
            EventBus.getDefault().post(new CodelessEvent.FlowControl(this));
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
        if (num == null || mode != DISABLE_UART_FLOW_CONTROL && mode != ENABLE_UART_FLOW_CONTROL)
            return "Invalid mode";
        mode = num;

        num = decodeNumberArgument(2);
        if (num == null)
            return "Invalid RTS GPIO";
        rtsGpio = new GPIO();
        rtsGpio.setGpio(num);
        rtsGpio.function = GPIO_FUNCTION_UART_RTS;

        num = decodeNumberArgument(3);
        if (num == null)
            return "Invalid CTS GPIO";
        ctsGpio = new GPIO();
        ctsGpio.setGpio(num);
        ctsGpio.function = GPIO_FUNCTION_UART_CTS;

        return null;
    }

    /** Returns the flow control mode argument/response. */
    public int getMode() {
        return mode;
    }

    /** Sets the flow control mode argument. */
    public void setMode(int mode) {
        this.mode = mode;
        if (mode != DISABLE_UART_FLOW_CONTROL && mode != ENABLE_UART_FLOW_CONTROL)
            invalid = true;
    }

    /** Checks if flow control is enabled, based on the flow control mode argument/response. */
    public boolean isEnabled() {
        return mode != DISABLE_UART_FLOW_CONTROL;
    }

    /** Sets the flow control mode argument (<code>true</code> to enable, <code>false</code> to disable). */
    public void setEnabled(boolean enabled) {
        mode = enabled ? ENABLE_UART_FLOW_CONTROL : DISABLE_UART_FLOW_CONTROL;
    }

    /** Returns the RTS pin argument/response. */
    public GPIO getRtsGpio() {
        return rtsGpio;
    }

    /** Sets the RTS pin argument. */
    public void setRtsGpio(GPIO rtsGpio) {
        this.rtsGpio = rtsGpio;
        if (rtsGpio.function != GPIO_FUNCTION_UART_RTS)
            invalid = true;
    }

    /** Returns the CTS pin argument/response. */
    public GPIO getCtsGpio() {
        return ctsGpio;
    }

    /** Sets the CTS pin argument. */
    public void setCtsGpio(GPIO ctsGpio) {
        this.ctsGpio = ctsGpio;
        if (ctsGpio.function != GPIO_FUNCTION_UART_CTS)
            invalid = true;
    }
}
