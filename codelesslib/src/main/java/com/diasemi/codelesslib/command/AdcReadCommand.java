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

import java.util.Arrays;
import java.util.regex.Pattern;

import static com.diasemi.codelesslib.CodelessLibConfig.ANALOG_INPUT_GPIO;
import static com.diasemi.codelesslib.CodelessLibConfig.CHECK_ANALOG_INPUT_GPIO;

/**
 * <code>AT+ADC</code> command implementation.
 * @see CodelessEvent.AnalogRead
 * @see <a href="https://lpccs-docs.renesas.com/UM-140-DA145x-CodeLess/index.html">AT commands documentation</a>
 */
public class AdcReadCommand extends CodelessCommand {
    public static final String TAG = "AdcReadCommand";

    public static final String COMMAND = "ADC";
    public static final String NAME = CodelessProfile.PREFIX_LOCAL + COMMAND;
    public static final CommandID ID = CommandID.ADC;

    public static final String PATTERN_STRING = "^ADC=(\\d+)$"; // <pin>
    public static final Pattern PATTERN = Pattern.compile(PATTERN_STRING);

    /** The analog input pin argument. */
    private GPIO gpio;

    /**
     * Creates an <code>AT+ADC</code> command.
     * @param manager   the associated manager
     * @param gpio      the analog input pin argument
     */
    public AdcReadCommand(CodelessManager manager, GPIO gpio) {
        this(manager);
        setGpio(gpio);
    }

    /**
     * Creates an <code>AT+ADC</code> command.
     * @param manager   the associated manager
     * @param port      the analog input port
     * @param pin       the analog input pin
     */
    public AdcReadCommand(CodelessManager manager, int port, int pin) {
        this(manager, new GPIO(port, pin));
    }

    /**
     * Default constructor.
     * @see CodelessCommand#CodelessCommand(CodelessManager)
     */
    public AdcReadCommand(CodelessManager manager) {
        super(manager);
    }

    /**
     * Default constructor.
     * @see CodelessCommand#CodelessCommand(CodelessManager, String, boolean)
     */
    public AdcReadCommand(CodelessManager manager, String command, boolean parse) {
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
        return Integer.toString(gpio.getGpio());
    }

    @Override
    public void parseResponse(String response) {
        super.parseResponse(response);
        if (responseLine() == 1) {
            try {
                gpio.state = Integer.parseInt(response);
                if (CodelessLibLog.COMMAND)
                    Log.d(TAG, "ADC: " + gpio.name() + " " + gpio.state);
            } catch (NumberFormatException e) {
                Log.e(TAG, "Received invalid ADC result: " + response);
                invalid = true;
            }
        }
    }

    @Override
    public void onSuccess() {
        super.onSuccess();
        if (isValid())
            EventBus.getDefault().post(new CodelessEvent.AnalogRead(this));
    }

    @Override
    protected boolean requiresArguments() {
        return true;
    }

    @Override
    protected boolean checkArgumentsCount() {
        return CodelessProfile.countArguments(command, ",") == 1;
    }

    @Override
    protected String parseArguments() {
        gpio = new GPIO();
        Integer num = decodeNumberArgument(1);
        if (num == null || CHECK_ANALOG_INPUT_GPIO && !Arrays.asList(ANALOG_INPUT_GPIO).contains(new GPIO(num)))
            return "Invalid ADC GPIO";
        gpio.setGpio(num);
        return null;
    }

    /** Returns the analog input pin argument. */
    public GPIO getGpio() {
        return gpio;
    }

    /** Sets the analog input pin argument. */
    public void setGpio(GPIO gpio) {
        this.gpio = gpio;
        gpio.state = GPIO.INVALID;
        if (CHECK_ANALOG_INPUT_GPIO && !Arrays.asList(ANALOG_INPUT_GPIO).contains(gpio))
            invalid = true;
    }

    /** Returns the analog input pin state response. */
    public int getState() {
        return gpio.state;
    }
}
