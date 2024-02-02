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
import com.diasemi.codelesslib.CodelessProfile.GPIO;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.regex.Pattern;

import static com.diasemi.codelesslib.CodelessLibConfig.CHECK_GPIO_FUNCTION;
import static com.diasemi.codelesslib.CodelessLibConfig.GPIO_FUNCTION_MAX;
import static com.diasemi.codelesslib.CodelessLibConfig.GPIO_FUNCTION_MIN;

/**
 * <code>AT+IOCFG</code> command implementation.
 * @see CodelessEvent.IoConfig
 * @see CodelessEvent.IoConfigSet
 * @see <a href="https://lpccs-docs.renesas.com/UM-140-DA145x-CodeLess/index.html">AT commands documentation</a>
 */
public class IoConfigCommand extends CodelessCommand {
    public static final String TAG = "IoConfigCommand";

    public static final String COMMAND = "IOCFG";
    public static final String NAME = CodelessProfile.PREFIX_LOCAL + COMMAND;
    public static final CommandID ID = CommandID.IOCFG;

    public static final String PATTERN_STRING = "^IOCFG(?:=(\\d+),(\\d+)(?:,(\\d+))?)?$"; // <pin> <function> <level>
    public static final Pattern PATTERN = Pattern.compile(PATTERN_STRING);

    /** The GPIO pin configuration argument. */
    private GPIO gpio;
    /** The GPIO pin configuration response. */
    private ArrayList<GPIO> configuration;
    /** <code>true</code> if the command has arguments, <code>false</code> for no arguments. */
    private boolean hasArguments;

    /**
     * Creates an <code>AT+IOCFG</code> command.
     * @param manager   the associated manager
     * @param gpio      the GPIO pin configuration argument
     */
    public IoConfigCommand(CodelessManager manager, GPIO gpio) {
        this(manager);
        this.gpio = gpio;
        setFunction(gpio.function);
        hasArguments = true;
    }

    /**
     * Creates an <code>AT+IOCFG</code> command.
     * @param manager   the associated manager
     * @param port      the GPIO port number
     * @param pin       the GPIO pin number
     * @param function  the GPIO {@link CodelessProfile.Command#GPIO_FUNCTION_UNDEFINED functionality} argument
     */
    public IoConfigCommand(CodelessManager manager, int port, int pin, int function) {
        this(manager, new GPIO(port, pin, function));
    }

    /**
     * Creates an <code>AT+IOCFG</code> command.
     * @param manager   the associated manager
     * @param port      the GPIO port number
     * @param pin       the GPIO pin number
     * @param function  the GPIO pin {@link CodelessProfile.Command#GPIO_FUNCTION_UNDEFINED functionality} argument
     * @param level     the GPIO pin level argument
     */
    public IoConfigCommand(CodelessManager manager, int port, int pin, int function, int level) {
        this(manager, new GPIO(port, pin, function, level));
    }

    /**
     * Default constructor.
     * @see CodelessCommand#CodelessCommand(CodelessManager)
     */
    public IoConfigCommand(CodelessManager manager) {
        super(manager);
    }

    /**
     * Default constructor.
     * @see CodelessCommand#CodelessCommand(CodelessManager, String, boolean)
     */
    public IoConfigCommand(CodelessManager manager, String command, boolean parse) {
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
        return hasArguments;
    }

    @Override
    protected String getArguments() {
        if (!hasArguments)
            return null;
        String arguments = String.format(Locale.US, "%d,%d", gpio.getGpio(), gpio.function);
        if (gpio.validLevel())
            arguments += String.format(Locale.US, ",%d", gpio.level);
        return arguments;
    }

    @Override
    public void parseResponse(String response) {
        super.parseResponse(response);
        if (responseLine() == 1) {
            configuration = new ArrayList<>();

            // Get GPIO function from response
            String[] gpioFunction = response.split(" ");
            int[] function = new int[gpioFunction.length];
            try {
                for (int i = 0; i < gpioFunction.length; i++) {
                    function[i] = Integer.parseInt(gpioFunction[i]);
                }
            } catch (NumberFormatException e) {
                Log.e(TAG, "Received invalid GPIO configuration: " + response);
                invalid = true;
                return;
            }

            // Find GPIO configuration (based on number of pins)
            GPIO[] gpioConfig = null;
            for (GPIO[] config : CodelessLibConfig.GPIO_CONFIGURATIONS) {
                if (function.length == config.length) {
                    gpioConfig = config;
                    break;
                }
            }
            if (gpioConfig == null) {
                Log.e(TAG, "Unknown GPIO configuration: " + response);
                invalid = true;
                return;
            }

            if (CodelessLibLog.COMMAND)
                Log.d(TAG, "Using GPIO configuration: " + Arrays.toString(gpioConfig));
            for (int i = 0; i < function.length; i++) {
                if (gpioConfig[i] != null)
                    configuration.add(new GPIO(gpioConfig[i], function[i]));
            }
            if (CodelessLibLog.COMMAND)
                Log.d(TAG, "GPIO configuration: " + configuration);
        }
    }

    @Override
    public void onSuccess() {
        super.onSuccess();
        if (isValid())
            EventBus.getDefault().post(!hasArguments ? new CodelessEvent.IoConfig(this) : new CodelessEvent.IoConfigSet(this));
    }

    @Override
    protected boolean checkArgumentsCount() {
        int count = CodelessProfile.countArguments(command, ",");
        return count == 0 || count == 2 || count == 3;
    }

    @Override
    protected String parseArguments() {
        gpio = new GPIO();

        int count = CodelessProfile.countArguments(command, ",");
        if (count == 0)
            return null;
        hasArguments = true;

        Integer num = decodeNumberArgument(1);
        if (num == null)
            return "Invalid GPIO";
        gpio.setGpio(num);

        num = decodeNumberArgument(2);
        if (num == null || CHECK_GPIO_FUNCTION && (num < GPIO_FUNCTION_MIN || num > GPIO_FUNCTION_MAX))
            return "Invalid GPIO function";
        gpio.function = num;

        if (count == 3) {
            num = decodeNumberArgument(3);
            if (num == null)
                return "Invalid level";
            gpio.level = num;
        }

        return null;
    }

    /** Returns the GPIO pin configuration argument. */
    public GPIO getGpio() {
        return gpio;
    }

    /** Sets the GPIO pin configuration argument. */
    public void setGpio(GPIO gpio) {
        this.gpio = gpio;
    }

    /** Sets the GPIO pin configuration argument. */
    public void setGpio(int pack) {
        gpio.setGpio(pack);
    }

    /** Sets the GPIO pin configuration argument. */
    public void setGpio(int port, int pin) {
        gpio.setGpio(port, pin);
    }

    /** Returns the port number of the GPIO pin configuration argument. */
    public int getPort() {
        return gpio.port;
    }

    /** Sets the port number of the GPIO pin configuration argument. */
    public void setPort(int port) {
        gpio.port = port;
    }

    /** Returns the pin number of the GPIO pin configuration argument. */
    public int getPin() {
        return gpio.pin;
    }

    /** Sets the pin number of the GPIO pin configuration argument. */
    public void setPin(int pin) {
        gpio.pin = pin;
    }

    /** Returns the GPIO {@link CodelessProfile.Command#GPIO_FUNCTION_UNDEFINED functionality} argument. */
    public int getFunction() {
        return gpio.function;
    }

    /** Sets the GPIO {@link CodelessProfile.Command#GPIO_FUNCTION_UNDEFINED functionality} argument. */
    public void setFunction(int function) {
        gpio.function = function;
        if (CHECK_GPIO_FUNCTION && (function < GPIO_FUNCTION_MIN || function > GPIO_FUNCTION_MAX))
            invalid = true;
    }

    /** Returns the GPIO pin level argument. */
    public int getLevel() {
        return gpio.level;
    }

    /** Sets the GPIO pin level argument. */
    public void setLevel(int level) {
        gpio.level = level;
    }

    /** Returns the GPIO pin configuration response. */
    public ArrayList<GPIO> getConfiguration() {
        return configuration;
    }
}
