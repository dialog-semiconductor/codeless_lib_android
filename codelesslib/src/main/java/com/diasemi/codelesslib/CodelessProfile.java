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

import com.diasemi.codelesslib.command.AdcReadCommand;
import com.diasemi.codelesslib.command.AdvertisingDataCommand;
import com.diasemi.codelesslib.command.AdvertisingResponseCommand;
import com.diasemi.codelesslib.command.AdvertisingStartCommand;
import com.diasemi.codelesslib.command.AdvertisingStopCommand;
import com.diasemi.codelesslib.command.BasicCommand;
import com.diasemi.codelesslib.command.BatteryLevelCommand;
import com.diasemi.codelesslib.command.BaudRateCommand;
import com.diasemi.codelesslib.command.BinEscCommand;
import com.diasemi.codelesslib.command.BinExitAckCommand;
import com.diasemi.codelesslib.command.BinExitCommand;
import com.diasemi.codelesslib.command.BinRequestAckCommand;
import com.diasemi.codelesslib.command.BinRequestCommand;
import com.diasemi.codelesslib.command.BinResumeCommand;
import com.diasemi.codelesslib.command.BluetoothAddressCommand;
import com.diasemi.codelesslib.command.BondingEntryClearCommand;
import com.diasemi.codelesslib.command.BondingEntryStatusCommand;
import com.diasemi.codelesslib.command.BondingEntryTransferCommand;
import com.diasemi.codelesslib.command.BroadcasterRoleSetCommand;
import com.diasemi.codelesslib.command.CentralRoleSetCommand;
import com.diasemi.codelesslib.command.CmdGetCommand;
import com.diasemi.codelesslib.command.CmdPlayCommand;
import com.diasemi.codelesslib.command.CmdStoreCommand;
import com.diasemi.codelesslib.command.CodelessCommand;
import com.diasemi.codelesslib.command.ConnectionParametersCommand;
import com.diasemi.codelesslib.command.CursorCommand;
import com.diasemi.codelesslib.command.CustomCommand;
import com.diasemi.codelesslib.command.DataLengthEnableCommand;
import com.diasemi.codelesslib.command.DeviceInformationCommand;
import com.diasemi.codelesslib.command.DeviceSleepCommand;
import com.diasemi.codelesslib.command.ErrorReportingCommand;
import com.diasemi.codelesslib.command.EventConfigCommand;
import com.diasemi.codelesslib.command.EventHandlerCommand;
import com.diasemi.codelesslib.command.FlowControlCommand;
import com.diasemi.codelesslib.command.GapConnectCommand;
import com.diasemi.codelesslib.command.GapDisconnectCommand;
import com.diasemi.codelesslib.command.GapScanCommand;
import com.diasemi.codelesslib.command.GapStatusCommand;
import com.diasemi.codelesslib.command.HeartbeatCommand;
import com.diasemi.codelesslib.command.HostSleepCommand;
import com.diasemi.codelesslib.command.I2cConfigCommand;
import com.diasemi.codelesslib.command.I2cReadCommand;
import com.diasemi.codelesslib.command.I2cScanCommand;
import com.diasemi.codelesslib.command.I2cWriteCommand;
import com.diasemi.codelesslib.command.IoConfigCommand;
import com.diasemi.codelesslib.command.IoStatusCommand;
import com.diasemi.codelesslib.command.MaxMtuCommand;
import com.diasemi.codelesslib.command.MemStoreCommand;
import com.diasemi.codelesslib.command.PeripheralRoleSetCommand;
import com.diasemi.codelesslib.command.PinCodeCommand;
import com.diasemi.codelesslib.command.PowerLevelConfigCommand;
import com.diasemi.codelesslib.command.PulseGenerationCommand;
import com.diasemi.codelesslib.command.RandomNumberCommand;
import com.diasemi.codelesslib.command.ResetCommand;
import com.diasemi.codelesslib.command.ResetIoConfigCommand;
import com.diasemi.codelesslib.command.RssiCommand;
import com.diasemi.codelesslib.command.SecurityModeCommand;
import com.diasemi.codelesslib.command.SpiConfigCommand;
import com.diasemi.codelesslib.command.SpiReadCommand;
import com.diasemi.codelesslib.command.SpiTransferCommand;
import com.diasemi.codelesslib.command.SpiWriteCommand;
import com.diasemi.codelesslib.command.TimerStartCommand;
import com.diasemi.codelesslib.command.TimerStopCommand;
import com.diasemi.codelesslib.command.UartEchoCommand;
import com.diasemi.codelesslib.command.UartPrintCommand;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Contains definitions of static values used by the CodeLess and DSPS protocols, as well as helper classes and methods.
 * @see CodelessManager
 * @see CodelessScanner
 * @see <a href="https://lpccs-docs.renesas.com/UM-140-DA145x-CodeLess/index.html">CodeLess User Manual</a>
 */
public class CodelessProfile {
    private final static String TAG = "CodelessProfile";

    /** UUID values for GATT services, characteristics and descriptors that are used by the library. */
    public static class Uuid {
        public static final UUID CLIENT_CONFIG_DESCRIPTOR = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

        // Codeless
        public static final UUID CODELESS_SERVICE_UUID = UUID.fromString("866d3b04-e674-40dc-9c05-b7f91bec6e83");
        public static final UUID CODELESS_INBOUND_COMMAND_UUID = UUID.fromString("914f8fb9-e8cd-411d-b7d1-14594de45425");
        public static final UUID CODELESS_OUTBOUND_COMMAND_UUID = UUID.fromString("3bb535aa-50b2-4fbe-aa09-6b06dc59a404");
        public static final UUID CODELESS_FLOW_CONTROL_UUID = UUID.fromString("e2048b39-d4f9-4a45-9f25-1856c10d5639");

        // DSPS
        public static final UUID DSPS_SERVICE_UUID = UUID.fromString("0783b03e-8535-b5a0-7140-a304d2495cb7");
        public static final UUID DSPS_SERVER_TX_UUID = UUID.fromString("0783b03e-8535-b5a0-7140-a304d2495cb8");
        public static final UUID DSPS_SERVER_RX_UUID = UUID.fromString("0783b03e-8535-b5a0-7140-a304d2495cba");
        public static final UUID DSPS_FLOW_CONTROL_UUID = UUID.fromString("0783b03e-8535-b5a0-7140-a304d2495cb9");

        // Other
        public static final UUID SUOTA_SERVICE_UUID = UUID.fromString("0000fef5-0000-1000-8000-00805f9b34fb");
        public static final UUID IOT_SERVICE_UUID = UUID.fromString("2ea78970-7d44-44bb-b097-26183f402400");
        public static final UUID WEARABLES_580_SERVICE_UUID = UUID.fromString("00002800-0000-1000-8000-00805f9b34fb");
        public static final UUID WEARABLES_680_SERVICE_UUID = UUID.fromString("00002ea7-0000-1000-8000-00805f9b34fb");
        public static final UUID MESH_PROVISIONING_SERVICE_UUID = UUID.fromString("00001827-0000-1000-8000-00805f9b34fb");
        public static final UUID MESH_PROXY_SERVICE_UUID = UUID.fromString("00001828-0000-1000-8000-00805f9b34fb");
        public static final UUID IMMEDIATE_ALERT_SERVICE_UUID = UUID.fromString("00001802-0000-1000-8000-00805f9b34fb");
        public static final UUID LINK_LOSS_SERVICE_UUID = UUID.fromString("00001803-0000-1000-8000-00805f9b34fb");

        // Device information service
        public static final UUID DEVICE_INFORMATION_SERVICE = UUID.fromString("0000180a-0000-1000-8000-00805f9b34fb");
        public static final UUID MANUFACTURER_NAME_STRING = UUID.fromString("00002A29-0000-1000-8000-00805f9b34fb");
        public static final UUID MODEL_NUMBER_STRING = UUID.fromString("00002A24-0000-1000-8000-00805f9b34fb");
        public static final UUID SERIAL_NUMBER_STRING = UUID.fromString("00002A25-0000-1000-8000-00805f9b34fb");
        public static final UUID HARDWARE_REVISION_STRING = UUID.fromString("00002A27-0000-1000-8000-00805f9b34fb");
        public static final UUID FIRMWARE_REVISION_STRING = UUID.fromString("00002A26-0000-1000-8000-00805f9b34fb");
        public static final UUID SOFTWARE_REVISION_STRING = UUID.fromString("00002A28-0000-1000-8000-00805f9b34fb");
        public static final UUID SYSTEM_ID = UUID.fromString("00002A23-0000-1000-8000-00805f9b34fb");
        public static final UUID IEEE_11073 = UUID.fromString("00002A2A-0000-1000-8000-00805f9b34fb");
        public static final UUID PNP_ID = UUID.fromString("00002A50-0000-1000-8000-00805f9b34fb");

        // GAP
        public static final UUID GAP_SERVICE = UUID.fromString("00001800-0000-1000-8000-00805f9b34fb");
        public static final UUID GAP_DEVICE_NAME = UUID.fromString("00002A00-0000-1000-8000-00805f9b34fb");
    }

    /** The default MTU value of the connection. */
    public static final int MTU_DEFAULT = 23;

    // DSPS flow control
    /** Value used to set the DSPS TX/RX flow to on. */
    public static final int DSPS_XON = 0x01;
    /** Value used to set the DSPS TX/RX flow to off. */
    public static final int DSPS_XOFF = 0x02;

    // Codeless flow control
    /**
     * Value notified by the peer device, through the {@link Uuid#CODELESS_FLOW_CONTROL_UUID flow control} characteristic, when there are CodeLess data ready to be received.
     * <p> After receiving this notification, the library reads the {@link Uuid#CODELESS_OUTBOUND_COMMAND_UUID outbound} characteristic to get the data.
     */
    public static final int CODELESS_DATA_PENDING = 0x01;

    /** AT command prefix. */
    public static final String PREFIX = "AT";
    /** Local AT command prefix. */
    public static final String PREFIX_LOCAL = PREFIX + "+";
    /**
     * Remote AT command prefix.
     * <p>
     * The library always uses the remote prefix to send commands to the peer device,
     * except for unidentified commands, which are sent verbatim, and mode commands,
     * which always use the local prefix.
     */
    public static final String PREFIX_REMOTE = PREFIX + "r";
    /** AT command prefix pattern. */
    public static final String PREFIX_PATTERN_STRING = "^" + PREFIX + "(?:\\+|r\\+?)?";
    public static final Pattern PREFIX_PATTERN = Pattern.compile("(" + PREFIX_PATTERN_STRING + ").*"); // <prefix>
    /** AT command pattern. */
    public static final String COMMAND_PATTERN_STRING = PREFIX_PATTERN_STRING + "([^=]*)=?.*"; // <command>
    public static final Pattern COMMAND_PATTERN = Pattern.compile(COMMAND_PATTERN_STRING);
    /** AT command with arguments prefix pattern. */
    public static final String COMMAND_WITH_ARGUMENTS_PREFIX_PATTERN_STRING = "^(?:" + PREFIX_PATTERN_STRING + ")?([^=]*)="; // <command>
    public static final Pattern COMMAND_WITH_ARGUMENTS_PREFIX_PATTERN = Pattern.compile(COMMAND_WITH_ARGUMENTS_PREFIX_PATTERN_STRING);
    /** AT command with arguments pattern. */
    public static final String COMMAND_WITH_ARGUMENTS_PATTERN_STRING = COMMAND_WITH_ARGUMENTS_PREFIX_PATTERN_STRING + ".*";
    public static final Pattern COMMAND_WITH_ARGUMENTS_PATTERN = Pattern.compile(COMMAND_WITH_ARGUMENTS_PATTERN_STRING);

    /**
     * Checks if a command string starts with the AT prefix.
     * @param command the command string to check
     */
    public static boolean hasPrefix(String command) {
        return PREFIX_PATTERN.matcher(command).matches();
    }

    /**
     * Gets the AT command prefix from a command string.
     * @param command the command string
     */
    public static String getPrefix(String command) {
        Matcher matcher = PREFIX_PATTERN.matcher(command);
        return matcher.matches() ? matcher.group(1) : null;
    }

    /**
     * Checks if a command string is a valid AT command.
     * @param command the command string to check
     */
    public static boolean isCommand(String command) {
        return COMMAND_PATTERN.matcher(command).matches();
    }

    /**
     * Gets the AT command text identifier from a command string.
     * @param command the command string
     */
    public static String getCommand(String command) {
        Matcher matcher = COMMAND_PATTERN.matcher(command);
        return matcher.matches() ? matcher.group(1) : null;
    }

    /**
     * Removes the AT prefix from a command string.
     * @param command the command string
     */
    public static String removeCommandPrefix(String command) {
        return command.replaceFirst(CodelessProfile.PREFIX_PATTERN_STRING, "");
    }

    /**
     * Checks if a command string contains arguments.
     * @param command the command string to check
     */
    public static boolean hasArguments(String command) {
        return COMMAND_WITH_ARGUMENTS_PATTERN.matcher(command).matches();
    }

    /**
     * Gets the number of arguments contained in a command string.
     * @param command   the command string to check
     * @param split     the delimiter used to separate the arguments
     */
    public static int countArguments(String command, String split) {
        return !hasArguments(command) ? 0 : command.replaceFirst(COMMAND_WITH_ARGUMENTS_PREFIX_PATTERN_STRING, "").split(split, -1).length;
    }

    /** CodeLess command success response. */
    public static final String OK = "OK";
    /** CodeLess command error response. */
    public static final String ERROR = "ERROR";
    /** Error message prefix for sending an error response to the peer device. */
    public static final String ERROR_PREFIX = "ERROR: ";
    /** Error message for invalid command. */
    public static final String INVALID_COMMAND = "Invalid command";
    /** Error message for unsupported command. */
    public static final String COMMAND_NOT_SUPPORTED = "Command not supported";
    /** Error message for missing arguments. */
    public static final String NO_ARGUMENTS = "No arguments";
    /** Error message for wrong number of arguments. */
    public static final String WRONG_NUMBER_OF_ARGUMENTS = "Wrong number of arguments";
    /** Error message for invalid arguments. */
    public static final String INVALID_ARGUMENTS = "Invalid arguments";
    /** Error message for GATT operation error (local). */
    public static final String GATT_OPERATION_ERROR = "Gatt operation error";
    /** Error message pattern, when receiving an error response from the peer device. */
    public static final String ERROR_MESSAGE_PATTERN_STRING = "^(?:ERROR|INVALID COMMAND|EC\\d{1,8}:).*";
    public static final Pattern ERROR_MESSAGE_PATTERN = Pattern.compile(ERROR_MESSAGE_PATTERN_STRING);
    /** Error message for invalid command received from peer device. */
    public static final String PEER_INVALID_COMMAND = "INVALID COMMAND";
    /** Error code/message pattern received from peer device. */
    public static final String ERROR_CODE_PATTERN_STRING = "^EC(\\d{1,8}):\\s*(.*)"; // <code> <message>
    public static final Pattern ERROR_CODE_PATTERN = Pattern.compile(ERROR_CODE_PATTERN_STRING);

    /**
     * Checks if a command response indicates success.
     * @param response the response to check
     */
    public static boolean isSuccess(String response) {
        return response.equals(OK);
    }

    /**
     * Checks if a command response indicates failure.
     * @param response the response to check
     */
    public static boolean isError(String response) {
        return response.equals(ERROR);
    }

    /**
     * Checks if a command response contains an error message.
     * @param response the response to check
     */
    public static boolean isErrorMessage(String response) {
        return ERROR_MESSAGE_PATTERN.matcher(response).matches();
    }

    /**
     * Checks if an error message indicates an invalid command.
     * @param error the error message to check
     */
    public static boolean isPeerInvalidCommand(String error) {
        return error.startsWith(PEER_INVALID_COMMAND);
    }

    /** Error code and message received as response to a command that failed. */
    public static class ErrorCodeMessage {
        /** The error code of the failure. */
        public int code;
        /** The error message describing the failure. */
        public String message;

        public ErrorCodeMessage(int code, String message) {
            this.code = code;
            this.message = message;
        }
    }

    /**
     * Checks if an error message contains an error code/message pattern.
     * @param error the error message to check
     */
    public static boolean isErrorCodeMessage(String error) {
        return ERROR_CODE_PATTERN.matcher(error).matches();
    }

    /**
     * Parses an error code/message response.
     * @param error the error message to parse
     */
    public static ErrorCodeMessage parseErrorCodeMessage(String error) {
        Matcher matcher = ERROR_CODE_PATTERN.matcher(error);
        return matcher.matches() ? new ErrorCodeMessage(Integer.parseInt(matcher.group(1)), matcher.group(2)) : null;
    }

    /** The type of a CodeLess communication {@link Line line}. */
    public enum LineType {
        InboundCommand,
        InboundResponse,
        InboundOK,
        InboundError,
        InboundEmpty,
        OutboundCommand,
        OutboundResponse,
        OutboundOK,
        OutboundError,
        OutboundEmpty;

        /** Checks if the line is received from the peer device. */
        public boolean isInbound() {
            return this == InboundCommand || this == InboundResponse || this == InboundOK || this == InboundError || this == InboundEmpty;
        }

        /** Checks if the line is sent to the peer device. */
        public boolean isOutbound() {
            return this == OutboundCommand || this == OutboundResponse || this == OutboundOK || this == OutboundError || this == OutboundEmpty;
        }

        /** Checks if the line is a command. */
        public boolean isCommand() {
            return this == InboundCommand || this == OutboundCommand;
        }

        /** Checks if the line is a response. */
        public boolean isResponse() {
            return this == InboundResponse || this == OutboundResponse;
        }

        /** Checks if the line represents command success. */
        public boolean isOK() {
            return this == InboundOK || this == OutboundOK;
        }

        /** Checks if the line contains a command error. */
        public boolean isError() {
            return this == InboundError || this == OutboundError;
        }

        /** Checks if the line is empty. */
        public boolean isEmpty() {
            return this == InboundEmpty || this == OutboundEmpty;
        }
    }

    /**
     * Information about a CodeLess communication line.
     * <p> May be used to distinguish between incoming and outgoing messages, commands and responses.
     * @see CodelessEvent.CodelessLine
     */
    public static class Line {

        private String text;
        private LineType type;

        public Line(String text, LineType type) {
            this.text = text;
            this.type = type;
        }

        public Line(LineType type) {
            this.text = "";
            this.type = type;
        }

        /** Returns the communication text. */
        public String getText() {
            return text;
        }

        /** Returns the line type. */
        public LineType getType() {
            return type;
        }
    }

    /**
     * General Purpose Input Output pin.
     * <p> Used by various CodeLess commands to select or configure the peer device IO pins.
     */
    public static class GPIO {

        /** Indicates that the configuration option is not set. */
        public static final int INVALID = -1;

        /** The IO port number. */
        public int port = INVALID;
        /** The IO pin number. */
        public int pin = INVALID;

        /** The IO pin state. */
        public int state = INVALID;
        /** The IO pin {@link Command#GPIO_FUNCTION_UNDEFINED functionality}. */
        public int function = INVALID;
        /** The IO pin level. */
        public int level = INVALID;

        public GPIO() {
        }

        public GPIO(int port, int pin) {
            this.port = port;
            this.pin = pin;
        }

        public GPIO(int port, int pin, int function) {
            this(port, pin);
            this.function = function;
        }

        public GPIO(int port, int pin, int function, int level) {
            this(port, pin, function);
            this.level = level;
        }

        public GPIO(int pack) {
            setGpio(pack);
        }

        public GPIO(GPIO gpio) {
            this(gpio.port, gpio.pin, gpio.function, gpio.level);
            state = gpio.state;
        }

        public GPIO(GPIO gpio, int function) {
            this(gpio.port, gpio.pin, function);
        }

        public GPIO(GPIO gpio, int function, int level) {
            this(gpio.port, gpio.pin, function, level);
        }

        /**
         * Updates the IO pin configuration options, copying them from the specified GPIO.
         * @param gpio the GPIO to copy. Only valid configuration options are copied.
         */
        public void update(GPIO gpio) {
            if (!equals(gpio))
                return;
            if (gpio.validFunction()) {
                if (function != gpio.function) {
                    level = INVALID;
                    state = INVALID;
                }
                function = gpio.function;
            }
            if (gpio.validLevel())
                level = gpio.level;
            if (gpio.validState())
                state = gpio.state;
        }

        /** Returns the IO pin as a new object, with no other configuration options set. */
        public GPIO pin() {
            return new GPIO(port, pin);
        }

        /** Checks if the IO pin is valid. */
        public boolean validGpio() {
            return port != INVALID && pin != INVALID;
        }

        /**
         * Returns the IO port/pin packed to an <code>int</code> value.
         * @see Command#gpioPack(int, int) gpioPack(port, pin)
         */
        public int getGpio() {
            return Command.gpioPack(port, pin);
        }

        /**
         * Sets the IO port/pin by unpacking an <code>int</code> value.
         * @param pack the packed port/pin value
         * @see Command#gpioGetPort(int) gpioGetPort(pack)
         * @see Command#gpioGetPin(int) gpioGetPin(pack)
         */
        public void setGpio(int pack) {
            port = Command.gpioGetPort(pack);
            pin = Command.gpioGetPin(pack);
        }

        /**
         * Sets the IO port/pin.
         * @param port  the IO port number
         * @param pin   the IO pin number
         */
        public void setGpio(int port, int pin) {
            this.port = port;
            this.pin = pin;
        }

        /** Checks if the IO pin state is valid. */
        public boolean validState() {
            return state != INVALID;
        }

        /** Checks if the IO pin state is binary low. */
        public boolean isLow() {
            return state == Command.PIN_STATUS_LOW;
        }

        /** Checks if the IO pin state is binary high. */
        public boolean isHigh() {
            return state == Command.PIN_STATUS_HIGH;
        }

        /** Checks if the IO pin state is binary. */
        public boolean isBinary() {
            return isLow() || isHigh();
        }

        /** Sets the IO pin state to binary low. */
        public void setLow() {
            state = Command.PIN_STATUS_LOW;
        }

        /** Sets the IO pin state to binary high. */
        public void setHigh() {
            state = Command.PIN_STATUS_HIGH;
        }

        /**
         * Sets the IO pin binary state.
         * @param status <code>true</code> for high, <code>false</code> for low
         */
        public void setStatus(boolean status) {
            state = status ? Command.PIN_STATUS_HIGH : Command.PIN_STATUS_LOW;
        }

        /** Checks if the IO pin functionality is valid. */
        public boolean validFunction() {
            return function != INVALID;
        }

        /** Checks if the IO pin is a binary input pin. */
        public boolean isInput() {
            return function == Command.GPIO_FUNCTION_INPUT || function == Command.GPIO_FUNCTION_INPUT_PULL_UP || function == Command.GPIO_FUNCTION_INPUT_PULL_DOWN;
        }

        /** Checks if the IO pin is a binary output pin. */
        public boolean isOutput() {
            return function == Command.GPIO_FUNCTION_OUTPUT;
        }

        /** Checks if the IO pin is an analog input pin. */
        public boolean isAnalog() {
            return function == Command.GPIO_FUNCTION_ANALOG_INPUT || function == Command.GPIO_FUNCTION_ANALOG_INPUT_ATTENUATION;
        }

        /** Checks if the IO pin is used for PWM pulse generation. */
        public boolean isPwm() {
            return function == Command.GPIO_FUNCTION_PWM || function == Command.GPIO_FUNCTION_PWM1
                    || function == Command.GPIO_FUNCTION_PWM2 || function == Command.GPIO_FUNCTION_PWM3;
        }

        /** Checks if the IO pin is used for I2C operation. */
        public boolean isI2c() {
            return function == Command.GPIO_FUNCTION_I2C_CLK || function == Command.GPIO_FUNCTION_I2C_SDA;
        }

        /** Checks if the IO pin is used for SPI operation. */
        public boolean isSpi() {
            return function == Command.GPIO_FUNCTION_SPI_CLK || function == Command.GPIO_FUNCTION_SPI_CS
                    || function == Command.GPIO_FUNCTION_SPI_MISO || function == Command.GPIO_FUNCTION_SPI_MOSI;
        }

        /** Checks if the IO pin is used for UART operation. */
        public boolean isUart() {
            return function == Command.GPIO_FUNCTION_UART_CTS || function == Command.GPIO_FUNCTION_UART_RTS
                    || function == Command.GPIO_FUNCTION_UART_RX || function == Command.GPIO_FUNCTION_UART_TX
                    || function == Command.GPIO_FUNCTION_UART2_CTS || function == Command.GPIO_FUNCTION_UART2_RTS
                    || function == Command.GPIO_FUNCTION_UART2_RX || function == Command.GPIO_FUNCTION_UART2_TX;
        }

        /** Checks if the IO pin level is valid. */
        public boolean validLevel() {
            return level != INVALID;
        }

        /**
         * Creates a copy of a GPIO configuration list.
         * @param config the GPIO configuration list to copy
         */
        public static ArrayList<GPIO> copyConfig(ArrayList<GPIO> config) {
            ArrayList<GPIO> copy = new ArrayList<>(config.size());
            for (GPIO gpio : config)
                copy.add(new GPIO(gpio));
            return copy;
        }

        /**
         * Updates a GPIO configuration list, by copying configuration options from another one.
         * <p> If the lists contain different pins, a copy is created. Only valid configuration options are copied.
         * @param config the GPIO configuration list to update
         * @param update the GPIO configuration list to copy
         * @return the updated GPIO configuration list
         */
        public static ArrayList<GPIO> updateConfig(ArrayList<GPIO> config, ArrayList<GPIO> update) {
            if (config == null || !Arrays.equals(config.toArray(), update.toArray()))
                return copyConfig(update);
            for (int i = 0; i < config.size(); i++)
                config.get(i).update(update.get(i));
            return config;
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            if (obj instanceof GPIO) {
                GPIO gpio = (GPIO) obj;
                return port == gpio.port && pin == gpio.pin;
            } else if (obj instanceof Integer) {
                return getGpio() == (Integer) obj;
            }
            return super.equals(obj);
        }

        /** Returns a text representation of the IO port/pin that can be used as an identifier. */
        public String name() {
            return "P" + port + "_" + pin;
        }

        @NonNull
        @Override
        public String toString() {
            return name() + (validFunction() ? "(" + function + ")" : "");
        }
    }

    /** Information about the activation status of one of the predefined events. */
    public static class EventConfig {
        /** The event type (1: initialization, 2: connection, 3: disconnection, 4: wakeup). */
        public int type;
        /** <code>true</code> if the event is activated, <code>false</code> if it is deactivated. */
        public boolean status;

        public EventConfig() {
        }

        public EventConfig(int type, boolean status){
            this.type = type;
            this.status = status;
        }
    }

    /** Information about a device found during a scan performed by the peer device. */
    public static class GapScannedDevice {
        /** The Bluetooth address of the found device. */
        public String address;
        /** The type of the Bluetooth address (public, random). */
        public int addressType;
        /** The type of the advertising packet (advertising, scan response). */
        public int type;
        /** The RSSI of the advertising event. */
        public int rssi;
    }

    /** Information about the event handler for one of the predefined events. */
    public static class EventHandler {
        /** The event type (1: connection, 2: disconnection, 3: wakeup). */
        public int event;
        /** The commands to be executed when the event occurs. */
        public ArrayList<CodelessCommand> commands;
    }

    /** Bonding database entry configuration. */
    public static class BondingEntry {
        /** The Long Term Key (LTK). */
        public byte[] ltk;
        /** The Encrypted Diversifier (EDIV). */
        public int ediv;
        /** The random number (RAND). */
        public byte[] rand;
        /** The key size. */
        public int keySize;
        /** The Connection Signature Resolving Key (CSRK). */
        public byte[] csrk;
        /** The peer Bluetooth address. */
        public byte[] bluetoothAddress;
        /** The peer Bluetooth address type. */
        public int addressType;
        /** The authentication level. */
        public int authenticationLevel;
        /** The bonding database slot. */
        public int bondingDatabaseSlot;
        /** The Identity Resolving Key (IRK). */
        public byte[] irk;
        /** The entry persistence status. */
        public int persistenceStatus;
        /** The entry timestamp. */
        public byte[] timestamp;
    }

    /** Contains definitions for static values used in various CodeLess commands. */
    public static class Command {

        // ATE
        public static final int UART_ECHO_OFF = 0;
        public static final int UART_ECHO_ON = 1;

        // ATF
        public static final int ERROR_REPORTING_OFF = 0;
        public static final int ERROR_REPORTING_ON = 1;

        // Flow control
        public static final int DISABLE_UART_FLOW_CONTROL = 0;
        public static final int ENABLE_UART_FLOW_CONTROL = 1;

        // Sleep
        public static final int AWAKE_DEVICE = 0;
        public static final int PUT_DEVICE_IN_SLEEP = 1;

        // BINESC
        public static final int BINESC_TIME_PRIOR_DEFAULT = 1000;
        public static final int BINESC_TIME_AFTER_DEFAULT = 1000;

        // GPIO
        public static final int GPIO_FUNCTION_UNDEFINED = 0;
        public static final int GPIO_FUNCTION_INPUT = 1;
        public static final int GPIO_FUNCTION_INPUT_PULL_UP = 2;
        public static final int GPIO_FUNCTION_INPUT_PULL_DOWN = 3;
        public static final int GPIO_FUNCTION_OUTPUT = 4;
        public static final int GPIO_FUNCTION_ANALOG_INPUT = 5;
        public static final int GPIO_FUNCTION_ANALOG_INPUT_ATTENUATION = 6;
        public static final int GPIO_FUNCTION_I2C_CLK = 7;
        public static final int GPIO_FUNCTION_I2C_SDA = 8;
        public static final int GPIO_FUNCTION_CONNECTION_INDICATOR_HIGH = 9;
        public static final int GPIO_FUNCTION_CONNECTION_INDICATOR_LOW = 10;
        public static final int GPIO_FUNCTION_UART_TX = 11;
        public static final int GPIO_FUNCTION_UART_RX = 12;
        public static final int GPIO_FUNCTION_UART_CTS = 13;
        public static final int GPIO_FUNCTION_UART_RTS = 14;
        public static final int GPIO_FUNCTION_UART2_TX = 15; // Reserved
        public static final int GPIO_FUNCTION_UART2_RX = 16; // Reserved
        public static final int GPIO_FUNCTION_UART2_CTS = 17; // Reserved
        public static final int GPIO_FUNCTION_UART2_RTS = 18; // Reserved
        public static final int GPIO_FUNCTION_SPI_CLK = 19;
        public static final int GPIO_FUNCTION_SPI_CS = 20;
        public static final int GPIO_FUNCTION_SPI_MOSI = 21;
        public static final int GPIO_FUNCTION_SPI_MISO = 22;
        public static final int GPIO_FUNCTION_PWM1 = 23; // Reserved
        public static final int GPIO_FUNCTION_PWM = 24;
        public static final int GPIO_FUNCTION_PWM2 = 25; // Reserved
        public static final int GPIO_FUNCTION_PWM3 = 26; // Reserved
        public static final int GPIO_FUNCTION_HEARTBEAT = 27;
        public static final int GPIO_FUNCTION_NOT_AVAILABLE = 28;

        public static final int PIN_STATUS_LOW = 0;
        public static final int PIN_STATUS_HIGH = 1;

        /**
         * Checks if value represents a binary pin state.
         * @param state the value to check
         */
        public static boolean isBinaryState(int state) {
            return state == PIN_STATUS_HIGH || state == PIN_STATUS_LOW;
        }

        /**
         * Packs a GPIO port/pin to an <code>int</code> value.
         * @param port  the port number
         * @param pin   the pin number
         * @return the packed value (10 x port + pin)
         */
        public static int gpioPack(int port, int pin) {
            return port * 10 + pin;
        }

        /**
         * Gets the GPIO port number from a packed value.
         * @param pack the packed port/pin value
         * @return the port number
         * @see #gpioPack(int, int)
         */
        public static int gpioGetPort(int pack) {
            return pack / 10;
        }

        /**
         * Gets the GPIO pin number from a packed value.
         * @param pack the packed port/pin value
         * @return the pin number
         * @see #gpioPack(int, int)
         */
        public static int gpioGetPin(int pack) {
            return pack % 10;
        }

        // GAP
        public static final int GAP_ROLE_PERIPHERAL = 0;
        public static final int GAP_ROLE_CENTRAL = 1;
        public static final int GAP_STATUS_DISCONNECTED = 0;
        public static final int GAP_STATUS_CONNECTED = 1;
        public static final String GAP_ADDRESS_TYPE_PUBLIC_STRING = "P";
        public static final String GAP_ADDRESS_TYPE_RANDOM_STRING = "R";
        public static final int GAP_ADDRESS_TYPE_PUBLIC = 0;
        public static final int GAP_ADDRESS_TYPE_RANDOM = 1;
        public static final String GAP_SCAN_TYPE_ADV_STRING = "ADV";
        public static final String GAP_SCAN_TYPE_RSP_STRING = "RSP";
        public static final int GAP_SCAN_TYPE_ADV = 0;
        public static final int GAP_SCAN_TYPE_RSP = 1;

        // Connection Parameters
        public static final int CONNECTION_INTERVAL_MIN = 6;
        public static final int CONNECTION_INTERVAL_MAX = 3200;
        public static final int SLAVE_LATENCY_MIN = 0;
        public static final int SLAVE_LATENCY_MAX = 500;
        public static final int SUPERVISION_TIMEOUT_MIN = 10;
        public static final int SUPERVISION_TIMEOUT_MAX = 3200;

        public static final int PARAMETER_UPDATE_DISABLE = 0;
        public static final int PARAMETER_UPDATE_ON_CONNECTION = 1;
        public static final int PARAMETER_UPDATE_NOW_ONLY = 2;
        public static final int PARAMETER_UPDATE_NOW_SAVE = 3;
        public static final int PARAMETER_UPDATE_ACTION_MIN = PARAMETER_UPDATE_DISABLE;
        public static final int PARAMETER_UPDATE_ACTION_MAX = PARAMETER_UPDATE_NOW_SAVE;

        // MTU
        public static final int MTU_MIN = 23;
        public static final int MTU_MAX = 512;

        // DLE
        public static final int DLE_DISABLED = 0;
        public static final int DLE_ENABLED = 1;
        public static final int DLE_PACKET_LENGTH_MIN = 27;
        public static final int DLE_PACKET_LENGTH_MAX = 251;
        public static final int DLE_PACKET_LENGTH_DEFAULT = 251;

        // SPI
        public static final int SPI_CLOCK_VALUE_2_MHZ = 0;
        public static final int SPI_CLOCK_VALUE_4_MHZ = 1;
        public static final int SPI_CLOCK_VALUE_8_MHZ = 2;

        public static final int SPI_MODE_0 = 0;
        public static final int SPI_MODE_1 = 1;
        public static final int SPI_MODE_2 = 2;
        public static final int SPI_MODE_3 = 3;

        // Baud rate
        public static final int BAUD_RATE_2400 = 2400;
        public static final int BAUD_RATE_4800 = 4800;
        public static final int BAUD_RATE_9600 = 9600;
        public static final int BAUD_RATE_19200 = 19200;
        public static final int BAUD_RATE_38400 = 38400;
        public static final int BAUD_RATE_57600 = 57600;
        public static final int BAUD_RATE_115200 = 115200;
        public static final int BAUD_RATE_230400 = 230400;

        // Output power level
        public static final int OUTPUT_POWER_LEVEL_MINUS_19_POINT_5_DBM = 1;
        public static final int OUTPUT_POWER_LEVEL_MINUS_13_POINT_5_DBM = 2;
        public static final int OUTPUT_POWER_LEVEL_MINUS_10_DBM = 3;
        public static final int OUTPUT_POWER_LEVEL_MINUS_7_DBM = 4;
        public static final int OUTPUT_POWER_LEVEL_MINUS_5_DBM = 5;
        public static final int OUTPUT_POWER_LEVEL_MINUS_3_POINT_5_DBM = 6;
        public static final int OUTPUT_POWER_LEVEL_MINUS_2_DBM = 7;
        public static final int OUTPUT_POWER_LEVEL_MINUS_1_DBM = 8;
        public static final int OUTPUT_POWER_LEVEL_0_DBM = 9;
        public static final int OUTPUT_POWER_LEVEL_1_DBM = 10;
        public static final int OUTPUT_POWER_LEVEL_1_POINT_5_DBM = 11;
        public static final int OUTPUT_POWER_LEVEL_2_POINT_5_DBM = 12;

        public static final String OUTPUT_POWER_LEVEL_NOT_SUPPORTED = "NOT SUPPORTED";

        // Event configuration
        public static final int DEACTIVATE_EVENT = 0;
        public static final int ACTIVATE_EVENT = 1;

        public static final int INITIALIZATION_EVENT = 1;
        public static final int CONNECTION_EVENT = 2;
        public static final int DISCONNECTION_EVENT = 3;
        public static final int WAKEUP_EVENT = 4;

        // Bonding entry persistence status
        public static final int BONDING_ENTRY_NON_PERSISTENT = 0;
        public static final int BONDING_ENTRY_PERSISTENT = 1;

        // Event handler configuration
        public static final int CONNECTION_EVENT_HANDLER = 1;
        public static final int DISCONNECTION_EVENT_HANDLER = 2;
        public static final int WAKEUP_EVENT_HANDLER = 3;

        // Heartbeat
        public static final int HEARTBEAT_DISABLED = 0;
        public static final int HEARTBEAT_ENABLED = 1;

        // Host sleep
        public static final int HOST_SLEEP_MODE_0 = 0;
        public static final int HOST_SLEEP_MODE_1 = 1;

        // Security mode
        /** LE secure connections pairing. */
        public static final int SECURITY_MODE_0 = 0;
        /** Legacy pairing with MITM protection. */
        public static final int SECURITY_MODE_1 = 1;
        /** Legacy pairing without MITM protection (Just Works). */
        public static final int SECURITY_MODE_2 = 2;
        /** No security. */
        public static final int SECURITY_MODE_3 = 3;

        /**
         * Map each command text identifier to a {@link CodelessCommand} subclass.
         * <p> Used for command parsing.
         */
        public static final HashMap<String, Class<? extends CodelessCommand>> commandMap = new HashMap<>();
        static {
            commandMap.put(BasicCommand.COMMAND, BasicCommand.class);
            commandMap.put(DeviceInformationCommand.COMMAND, DeviceInformationCommand.class);
            commandMap.put(UartEchoCommand.COMMAND, UartEchoCommand.class);
            commandMap.put(ResetIoConfigCommand.COMMAND, ResetIoConfigCommand.class);
            commandMap.put(ErrorReportingCommand.COMMAND, ErrorReportingCommand.class);
            commandMap.put(ResetCommand.COMMAND, ResetCommand.class);
            commandMap.put(BinRequestCommand.COMMAND, BinRequestCommand.class);
            commandMap.put(BinRequestAckCommand.COMMAND, BinRequestAckCommand.class);
            commandMap.put(BinExitCommand.COMMAND, BinExitCommand.class);
            commandMap.put(BinExitAckCommand.COMMAND, BinExitAckCommand.class);
            commandMap.put(BinResumeCommand.COMMAND, BinResumeCommand.class);
            commandMap.put(BinEscCommand.COMMAND, BinEscCommand.class);
            commandMap.put(TimerStartCommand.COMMAND, TimerStartCommand.class);
            commandMap.put(TimerStopCommand.COMMAND, TimerStopCommand.class);
            commandMap.put(CursorCommand.COMMAND, CursorCommand.class);
            commandMap.put(RandomNumberCommand.COMMAND, RandomNumberCommand.class);
            commandMap.put(BatteryLevelCommand.COMMAND, BatteryLevelCommand.class);
            commandMap.put(BluetoothAddressCommand.COMMAND, BluetoothAddressCommand.class);
            commandMap.put(RssiCommand.COMMAND, RssiCommand.class);
            commandMap.put(DeviceSleepCommand.COMMAND, DeviceSleepCommand.class);
            commandMap.put(IoConfigCommand.COMMAND, IoConfigCommand.class);
            commandMap.put(IoStatusCommand.COMMAND, IoStatusCommand.class);
            commandMap.put(AdcReadCommand.COMMAND, AdcReadCommand.class);
            commandMap.put(I2cScanCommand.COMMAND, I2cScanCommand.class);
            commandMap.put(I2cConfigCommand.COMMAND, I2cConfigCommand.class);
            commandMap.put(I2cReadCommand.COMMAND, I2cReadCommand.class);
            commandMap.put(I2cWriteCommand.COMMAND, I2cWriteCommand.class);
            commandMap.put(UartPrintCommand.COMMAND, UartPrintCommand.class);
            commandMap.put(MemStoreCommand.COMMAND, MemStoreCommand.class);
            commandMap.put(PinCodeCommand.COMMAND, PinCodeCommand.class);
            commandMap.put(CmdStoreCommand.COMMAND, CmdStoreCommand.class);
            commandMap.put(CmdPlayCommand.COMMAND, CmdPlayCommand.class);
            commandMap.put(CmdGetCommand.COMMAND, CmdGetCommand.class);
            commandMap.put(AdvertisingStopCommand.COMMAND, AdvertisingStopCommand.class);
            commandMap.put(AdvertisingStartCommand.COMMAND, AdvertisingStartCommand.class);
            commandMap.put(AdvertisingDataCommand.COMMAND, AdvertisingDataCommand.class);
            commandMap.put(AdvertisingResponseCommand.COMMAND, AdvertisingResponseCommand.class);
            commandMap.put(CentralRoleSetCommand.COMMAND, CentralRoleSetCommand.class);
            commandMap.put(PeripheralRoleSetCommand.COMMAND, PeripheralRoleSetCommand.class);
            commandMap.put(BroadcasterRoleSetCommand.COMMAND, BroadcasterRoleSetCommand.class);
            commandMap.put(GapStatusCommand.COMMAND, GapStatusCommand.class);
            commandMap.put(GapScanCommand.COMMAND, GapScanCommand.class);
            commandMap.put(GapConnectCommand.COMMAND, GapConnectCommand.class);
            commandMap.put(GapDisconnectCommand.COMMAND, GapDisconnectCommand.class);
            commandMap.put(ConnectionParametersCommand.COMMAND, ConnectionParametersCommand.class);
            commandMap.put(MaxMtuCommand.COMMAND, MaxMtuCommand.class);
            commandMap.put(DataLengthEnableCommand.COMMAND, DataLengthEnableCommand.class);
            commandMap.put(SpiConfigCommand.COMMAND, SpiConfigCommand.class);
            commandMap.put(SpiWriteCommand.COMMAND, SpiWriteCommand.class);
            commandMap.put(SpiReadCommand.COMMAND, SpiReadCommand.class);
            commandMap.put(SpiTransferCommand.COMMAND, SpiTransferCommand.class);
            commandMap.put(BaudRateCommand.COMMAND, BaudRateCommand.class);
            commandMap.put(PowerLevelConfigCommand.COMMAND, PowerLevelConfigCommand.class);
            commandMap.put(PulseGenerationCommand.COMMAND, PulseGenerationCommand.class);
            commandMap.put(EventConfigCommand.COMMAND, EventConfigCommand.class);
            commandMap.put(BondingEntryClearCommand.COMMAND, BondingEntryClearCommand.class);
            commandMap.put(BondingEntryStatusCommand.COMMAND, BondingEntryStatusCommand.class);
            commandMap.put(BondingEntryTransferCommand.COMMAND, BondingEntryTransferCommand.class);
            commandMap.put(EventHandlerCommand.COMMAND, EventHandlerCommand.class);
            commandMap.put(HeartbeatCommand.COMMAND, HeartbeatCommand.class);
            commandMap.put(HostSleepCommand.COMMAND, HostSleepCommand.class);
            commandMap.put(SecurityModeCommand.COMMAND, SecurityModeCommand.class);
            commandMap.put(FlowControlCommand.COMMAND, FlowControlCommand.class);
        }

        /** Commands that can change the operation mode. */
        public static final HashSet<CommandID> modeCommands = new HashSet<>();
        static {
            modeCommands.add(CommandID.BINREQ);
            modeCommands.add(CommandID.BINREQACK);
            modeCommands.add(CommandID.BINREQEXIT);
            modeCommands.add(CommandID.BINREQEXITACK);
        }

        /**
         * Checks if the specified command is a mode command.
         * @param command the command to check
         */
        public static boolean isModeCommand(CodelessCommand command) {
            return modeCommands.contains(command.getCommandID());
        }
    }

    /**
     * Creates a {@link CodelessCommand} subclass object from the specified command text.
     * @param manager       the associated manager
     * @param commandClass  the CodelessCommand subclass type
     * @param command       the command text to parse
     * @return the created command object
     */
    public static CodelessCommand createCommand(CodelessManager manager, Class<? extends CodelessCommand> commandClass, String command) {
        try {
            return commandClass.getConstructor(CodelessManager.class, String.class, boolean.class).newInstance(manager, command, true);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            Log.e(TAG, "Failed to create " + commandClass.getSimpleName() + " object: " + e.getCause());
            return new CustomCommand(manager, PREFIX + command, true);
        }
    }

    /**
     * Enumeration of CodeLess command identifiers.
     * <p>
     * Each value is the same as the corresponding command text identifier, except for
     * single character commands, like <code>ATI</code>, where the prefix is also present,
     * and <code>CUSTOM</code>, which is used for unidentified commands.
     * <p>
     * Used for quick referencing or checking of the command identifier.
     */
    public enum CommandID {
        AT,
        ATI,
        ATE,
        ATZ,
        ATF,
        ATR,
        BINREQ,
        BINREQACK,
        BINREQEXIT,
        BINREQEXITACK,
        BINRESUME,
        BINESC,
        TMRSTART,
        TMRSTOP,
        CURSOR,
        RANDOM,
        BATT,
        BDADDR,
        RSSI,
        FLOWCONTROL,
        SLEEP,
        IOCFG,
        IO,
        ADC,
        I2CSCAN,
        I2CCFG,
        I2CREAD,
        I2CWRITE,
        PRINT,
        MEM,
        PIN,
        CMDSTORE,
        CMDPLAY,
        CMD,
        ADVSTOP,
        ADVSTART,
        ADVDATA,
        ADVRESP,
        CENTRAL,
        PERIPHERAL,
        BROADCASTER,
        GAPSTATUS,
        GAPSCAN,
        GAPCONNECT,
        GAPDISCONNECT,
        CONPAR,
        MAXMTU,
        DLEEN,
        HOSTSLP,
        SPICFG,
        SPIWR,
        SPIRD,
        SPITR,
        BAUD,
        PWRLVL,
        PWM,
        EVENT,
        CLRBNDE,
        CHGBNDP,
        IEBNDE,
        HNDL,
        SEC,
        HRTBT,

        CUSTOM
    }
}
