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
import com.diasemi.codelesslib.CodelessProfile.Command;
import com.diasemi.codelesslib.CodelessProfile.CommandID;
import com.diasemi.codelesslib.CodelessProfile.GPIO;

import org.greenrobot.eventbus.EventBus;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * <code>AT+IO</code> command implementation.
 * @see CodelessEvent.IoStatus
 * @see <a href="https://lpccs-docs.renesas.com/UM-140-DA145x-CodeLess/index.html">AT commands documentation</a>
 */
public class IoStatusCommand extends CodelessCommand {
    public static final String TAG = "IoStatusCommand";

    public static final String COMMAND = "IO";
    public static final String NAME = CodelessProfile.PREFIX_LOCAL + COMMAND;
    public static final CommandID ID = CommandID.IO;

    public static final String PATTERN_STRING = "^IO=(\\d+)(?:,(\\d))?$"; // <pin> <status>
    public static final Pattern PATTERN = Pattern.compile(PATTERN_STRING);

    /** The GPIO pin argument/response. */
    private GPIO gpio;

    /**
     * Creates an <code>AT+IO</code> command.
     * @param manager   the associated manager
     * @param gpio      the GPIO pin argument
     */
    public IoStatusCommand(CodelessManager manager, GPIO gpio) {
        this(manager);
        setGpio(gpio);
    }

    /**
     * Creates an <code>AT+IO</code> command.
     * @param manager   the associated manager
     * @param gpio      the GPIO output pin argument
     * @param status    the output pin status argument (<code>true</code> for high, <code>false</code> for low)
     */
    public IoStatusCommand(CodelessManager manager, GPIO gpio, boolean status) {
        this(manager, gpio);
        gpio.setStatus(status);
    }

    /**
     * Creates an <code>AT+IO</code> command.
     * @param manager   the associated manager
     * @param port      the GPIO port number
     * @param pin       the GPIO pin number
     */
    public IoStatusCommand(CodelessManager manager, int port, int pin) {
        this(manager, new GPIO(port, pin));
    }

    /**
     * Creates an <code>AT+IO</code> command.
     * @param manager   the associated manager
     * @param port      the GPIO port number
     * @param pin       the GPIO pin number
     * @param status    the output pin status argument (<code>true</code> for high, <code>false</code> for low)
     */
    public IoStatusCommand(CodelessManager manager, int port, int pin, boolean status) {
        this(manager, new GPIO(port, pin), status);
    }

    /**
     * Default constructor.
     * @see CodelessCommand#CodelessCommand(CodelessManager)
     */
    public IoStatusCommand(CodelessManager manager) {
        super(manager);
    }

    /**
     * Default constructor.
     * @see CodelessCommand#CodelessCommand(CodelessManager, String, boolean)
     */
    public IoStatusCommand(CodelessManager manager, String command, boolean parse) {
        super(manager, command, parse);
        if (gpio == null)
            gpio = new GPIO();
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
        String arguments = String.format(Locale.US, "%d", gpio.getGpio());
        if (gpio.validState())
            arguments += String.format(Locale.US, ",%d", gpio.state);
        return arguments;
    }

    @Override
    public void parseResponse(String response) {
        super.parseResponse(response);
        if (responseLine() == 1) {
            try {
                gpio.state = Integer.parseInt(response);
                if (!gpio.isBinary())
                    invalid = true;
            } catch (NumberFormatException e) {
                invalid = true;
            }
            if (invalid)
                Log.e(TAG, "Received invalid GPIO status: " + response);
            else if (CodelessLibLog.COMMAND)
                Log.d(TAG, "GPIO status: " + gpio.name() + (gpio.isHigh() ? " high" : " low"));
        }
    }

    @Override
    public void onSuccess() {
        super.onSuccess();
        if (isValid())
            EventBus.getDefault().post(new CodelessEvent.IoStatus(this));
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
        gpio = new GPIO();

        Integer num = decodeNumberArgument(1);
        if (num == null)
            return "Invalid GPIO";
        gpio.setGpio(num);

        if (CodelessProfile.countArguments(command, ",") == 2) {
            num = decodeNumberArgument(2);
            if (num == null || !Command.isBinaryState(num))
                return "Argument must be 0 or 1";
            gpio.state = num;
        }

        return null;
    }

    /** Returns the GPIO pin argument. */
    public GPIO getGpio() {
        return gpio;
    }

    /** Sets the GPIO pin argument. */
    public void setGpio(GPIO gpio) {
        this.gpio = gpio;
        if (gpio.validState() && !gpio.isBinary())
            invalid = true;
    }

    /** Returns the output/input pin status argument/response (<code>true</code> for high, <code>false</code> for low). */
    public boolean getStatus() {
        return gpio.isHigh();
    }

    /** Sets the output pin status argument (<code>true</code> for high, <code>false</code> for low). */
    public void setStatus(boolean status) {
        gpio.setStatus(status);
    }
}
