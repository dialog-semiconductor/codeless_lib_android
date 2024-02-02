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

import android.os.Build;
import android.os.Environment;

import com.diasemi.codelesslib.CodelessProfile.Command;
import com.diasemi.codelesslib.CodelessProfile.CommandID;
import com.diasemi.codelesslib.CodelessProfile.GPIO;
import com.diasemi.codelesslib.command.CodelessCommand;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.Locale;
import java.util.regex.Pattern;

/** Configuration options that configure the library behavior. */
public class CodelessLibConfig {

    /** ATI command response (if <code>null</code>, the app version is used). */
    public static final String CODELESS_LIB_INFO = null;

    /**
     * Base folder for library files (scripts, logs) on the device's external storage (if {@link #SCOPED_STORAGE} is disabled).
     * <p> If {@link #SCOPED_STORAGE} is enabled, files are located in a {@link CodelessManager#checkStorage(android.content.Context) user selected folder}.
     */
    public static final String FILE_PATH = "Renesas Electronics/SmartConsole";
    /** Full path for {@link #FILE_PATH base folder}. */
    public static final String FILE_PATH_FULL = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + FILE_PATH + "/";
    /** Subfolder for CodeLess and DSPS log files. */
    public static final String LOG_FILE_PATH_NAME = "log";
    /** Full path for {@link #LOG_FILE_PATH_NAME log folder}. */
    public static final String LOG_FILE_PATH = FILE_PATH_FULL + LOG_FILE_PATH_NAME;
    /** Date format used when creating log file names. */
    public static final SimpleDateFormat LOG_FILE_DATE = new SimpleDateFormat("yyyy-MM-dd'_'HH.mm.ss", Locale.US);
    /** Append the device address to the log file name. */
    public static final boolean LOG_FILE_ADDRESS_SUFFIX = true;
    /** Log file extension. */
    public static final String LOG_FILE_EXTENSION = ".txt";
    /** Enable CodeLess communication log file. */
    public static final boolean CODELESS_LOG = true;
    /** Flush the CodeLess log file on each write. */
    public static final boolean CODELESS_LOG_FLUSH = true;
    /** Prefix used for the CodeLess log file name. */
    public static final String CODELESS_LOG_FILE_PREFIX = "Codeless_";
    /** Prefix used for CodeLess log entries for user input. */
    public static final String CODELESS_LOG_PREFIX_TEXT = "";
    /** Prefix used for CodeLess log entries for outgoing messages. */
    public static final String CODELESS_LOG_PREFIX_OUTBOUND = ">> ";
    /** Prefix used for CodeLess log entries for incoming messages. */
    public static final String CODELESS_LOG_PREFIX_INBOUND = "<< ";
    /** Enable DSPS received data log file. */
    public static final boolean DSPS_RX_LOG = true;
    /** Flush the DSPS received data log file on each write. */
    public static final boolean DSPS_RX_LOG_FLUSH = true;
    /** Prefix used for the DSPS received data log file name. */
    public static final String DSPS_RX_LOG_FILE_PREFIX = "DSPS_RX_";
    /** Enable scoped storage for file access (mandatory from Android 11). */
    public static final boolean SCOPED_STORAGE = Build.VERSION.SDK_INT >= 30;
    /** Use background thread for DSPS file operations. */
    public static final boolean DSPS_LOG_HANDLER = true;

    /**
     * Enable priority for DSPS send data GATT operations.
     * <p>
     * High priority operations are put before low priority ones in the queue.
     * File and periodic send operations are low priority, while other DSPS operations are high priority.
     */
    public static final boolean GATT_QUEUE_PRIORITY = true;
    /** Execute the next GATT operation in the queue before processing the results of the previous one. */
    public static final boolean GATT_DEQUEUE_BEFORE_PROCESSING = true;
    /** Monitor Bluetooth state and perform required actions. */
    public static final boolean BLUETOOTH_STATE_MONITOR = true;
    /** If pairing is in progress, delay initialization operations until pairing is complete. */
    public static final boolean DELAY_INIT_IF_BONDING = true;

    // WARNING: Modifying these may cause parse failure on peer device.
    /** Used character set for conversion between text and bytes. */
    public static final Charset CHARSET = StandardCharsets.US_ASCII;
    /** End of line characters used when sending text. */
    public static final String END_OF_LINE = "\r\n";
    /** Append an end of line character to the sent text (if not already there, does not apply to sent commands). */
    public static final boolean APPEND_END_OF_LINE = true;
    /** Append an end of line character to the sent command text (if not already there). */
    public static final boolean END_OF_LINE_AFTER_COMMAND = false;
    /** Add an empty line before a success response, if there is no response message. */
    public static final boolean EMPTY_LINE_BEFORE_OK = true;
    /** Add an empty line before an error response, if there is no response message. */
    public static final boolean EMPTY_LINE_BEFORE_ERROR = true;
    /** Append a null byte to the sent text. */
    public static final boolean TRAILING_ZERO = true;
    /** Use single write operation to send response (merge lines). */
    public static final boolean SINGLE_WRITE_RESPONSE = true;

    /** Do not send invalid commands which are parsed from text (for example, user input). */
    public static final boolean DISALLOW_INVALID_PARSED_COMMAND = false;
    /** Do not send invalid commands which are not parsed from text (for example, commands created with {@link CodelessCommands}). */
    public static final boolean DISALLOW_INVALID_COMMAND = true;
    /** Do not send commands which do not have a valid AT command prefix. */
    public static final boolean DISALLOW_INVALID_PREFIX = true;
    /** Automatically add the AT command prefix (if missing). */
    public static final boolean AUTO_ADD_PREFIX = true;

    /** Enable {@link CodelessEvent.CodelessLine CodelessLine} events. */
    public static final boolean LINE_EVENTS = true;

    /** Start in command mode operation, if the peer device supports CodeLess. */
    public static final boolean START_IN_COMMAND_MODE = true;
    /**
     * Enable {@link CodelessEvent.BinaryModeRequest BinaryModeRequest} event when the peer CodeLess device sends the <code>AT+BINREQ</code> command.
     * <p>
     * The app should call {@link CodelessManager#acceptBinaryModeRequest()}, if the request is accepted.
     * If disabled, the library will automatically respond with <code>AT+BINREQACK</code>, entering binary mode.
     */
    public static final boolean HOST_BINARY_REQUEST = true;
    /**
     * Send the <code>AT+BINREQ</code> command to the peer device to request switching to binary mode.
     * <p> If disabled, the library will send the <code>AT+BINREQACK</code> command to force the switch.
     */
    public static final boolean MODE_CHANGE_SEND_BINARY_REQUEST = true;
    /** Allow incoming binary data in command mode. */
    public static final boolean ALLOW_INBOUND_BINARY_IN_COMMAND_MODE = false;
    /** Allow outgoing binary data in command mode. */
    public static final boolean ALLOW_OUTBOUND_BINARY_IN_COMMAND_MODE = false;
    /** Allow incoming commands in binary mode (mode commands are always allowed). */
    public static final boolean ALLOW_INBOUND_COMMAND_IN_BINARY_MODE = false;
    /** Allow outgoing commands in binary mode (mode commands are always allowed). */
    public static final boolean ALLOW_OUTBOUND_COMMAND_IN_BINARY_MODE = false;

    /** Initiate a MTU request on connection. */
    public static final boolean REQUEST_MTU = true;
    /** The requested MTU value. */
    public static final int MTU = 517;

    /**
     * The initial DSPS chunk size.
     * <p> WARNING: The chunk size must not exceed the value (MTU - 3), otherwise chunks will be truncated when sent.
     */
    public static final int DEFAULT_DSPS_CHUNK_SIZE = 128;
    /** Increase the DSPS chunk size to the maximum allowed value after the MTU exchange. */
    public static final boolean DSPS_CHUNK_SIZE_INCREASE_TO_MTU = true;
    /** Maximum buffer size for pending binary data operations when TX flow control is off. */
    public static final int DSPS_PENDING_MAX_SIZE = 1000;
    /** The initial DSPS RX flow control configuration (<code>true</code> for on, <code>false</code> for off). */
    public static final boolean DEFAULT_DSPS_RX_FLOW_CONTROL = true;
    /**
     * The initial DSPS TX flow control configuration (<code>true</code> for on, <code>false</code> for off).
     * <p>
     * If set to on, the library will be able to send data immediately after connection. Otherwise, it will wait for the
     * peer device to set the flow control to on by sending a notification through the DSPS Flow Control characteristic.
     */
    public static final boolean DEFAULT_DSPS_TX_FLOW_CONTROL = true;
    /** Configure the RX flow control on connection by writing the appropriate value to the DSPS Flow Control characteristic. */
    public static final boolean SET_FLOW_CONTROL_ON_CONNECTION = true;

    /** Length of the number suffix for pattern {@link com.diasemi.codelesslib.dsps.DspsPeriodicSend DspsPeriodicSend} operations. */
    public static final int DSPS_PATTERN_DIGITS = 4;
    /** Bytes added after the number suffix for pattern {@link com.diasemi.codelesslib.dsps.DspsPeriodicSend DspsPeriodicSend} operations. */
    public static final byte[] DSPS_PATTERN_SUFFIX = new byte[] { 0x0a };

    /** Subfolder for DSPS receive file operations. */
    public static final String DSPS_RX_FILE_PATH_NAME = "files";
    /** Full path for {@link #DSPS_RX_FILE_PATH_NAME received file folder}. */
    public static final String DSPS_RX_FILE_PATH = FILE_PATH_FULL + DSPS_RX_FILE_PATH_NAME;
    /** Log receive file operation data to the DSPS RX log file (if {@link #DSPS_RX_LOG enabled}). */
    public static final boolean DSPS_RX_FILE_LOG_DATA = false;
    /** Received file header pattern, used to detect the file header, if a receive file operation is active. */
    public static final String DSPS_RX_FILE_HEADER_PATTERN_STRING = "(?s)(.{0,100})Name:\\s*(\\S{1,100})\\s*Size:\\s*(\\d{1,9})\\s*(?:CRC:\\s*([0-9a-f]{8})\\s*)?(?:\\x00|END\\s*)(.*)"; // <ignored> <name> <size> <crc> <data>
    public static final Pattern DSPS_RX_FILE_HEADER_PATTERN = Pattern.compile(DSPS_RX_FILE_HEADER_PATTERN_STRING, Pattern.CASE_INSENSITIVE);

    /** Enable DSPS statistics calculation. */
    public static final boolean DSPS_STATS = true;
    /** DSPS statistics update interval (ms). */
    public static final int DSPS_STATS_INTERVAL = 1000; // ms

    /** Library preference keys. */
    public static final class PREF {
        /** Preferences file name. */
        public static final String NAME = "codeless-lib-preferences";
        /** Preference key for the selected file path URI. */
        public static final String filePathUri = "filePathUri";
    }

    /** Check the timer index value in command arguments. */
    public static final boolean CHECK_TIMER_INDEX = true;
    /** Minimum timer index value. */
    public static final int TIMER_INDEX_MIN = 0;
    /** Maximum timer index value. */
    public static final int TIMER_INDEX_MAX = 3;

    /** Check the command slot index value in timer command arguments. */
    public static final boolean CHECK_COMMAND_INDEX = true;
    /** Minimum command slot index value. */
    public static final int COMMAND_INDEX_MIN = 0;
    /** Maximum command slot index value. */
    public static final int COMMAND_INDEX_MAX = 3;

    /** Check the GPIO function value in command arguments. */
    public static final boolean CHECK_GPIO_FUNCTION = true;
    /** Minimum GPIO function value. */
    public static final int GPIO_FUNCTION_MIN = Command.GPIO_FUNCTION_UNDEFINED;
    /** Maximum GPIO function value. */
    public static final int GPIO_FUNCTION_MAX = Command.GPIO_FUNCTION_NOT_AVAILABLE;

    /** Check if the selected GPIO pin in command arguments supports analog input. */
    public static final boolean CHECK_ANALOG_INPUT_GPIO = true;
    /** GPIO pins that support analog input. */
    public static final GPIO[] ANALOG_INPUT_GPIO = {
            new GPIO(0, 0), new GPIO(0, 1), new GPIO(0, 2), new GPIO(0, 3),
    };

    /** Check the memory slot index value in command arguments. */
    public static final boolean CHECK_MEM_INDEX = true;
    /** Minimum memory slot index value. */
    public static final int MEM_INDEX_MIN = 0;
    /** Maximum memory slot index value. */
    public static final int MEM_INDEX_MAX = 3;

    /** Check the memory content size in command arguments. */
    public static final boolean CHECK_MEM_CONTENT_SIZE = true;
    /** Maximum memory content size. */
    public static final int MEM_MAX_CHAR_COUNT = 100;

    /** Check the command slot index value in command arguments. */
    public static final boolean CHECK_COMMAND_STORE_INDEX = true;
    /** Minimum command slot index value. */
    public static final int COMMAND_STORE_INDEX_MIN = 0;
    /** Maximum command slot index value. */
    public static final int COMMAND_STORE_INDEX_MAX = 3;

    /** Check the advertising internal value in command arguments. */
    public static final boolean CHECK_ADVERTISING_INTERVAL = true;
    /** Minimum advertising internal value (ms). */
    public static final int ADVERTISING_INTERVAL_MIN = 100; // ms
    /** Maximum advertising internal value (ms). */
    public static final int ADVERTISING_INTERVAL_MAX = 3000; // ms

    /** Check the SPI word size value in command arguments. */
    public static final boolean CHECK_SPI_WORD_SIZE = true;
    /** Supported SPI word size (bits). */
    public static final int SPI_WORD_SIZE = 8; // bits

    /** Check the hex string size in SPI command arguments. */
    public static final boolean CHECK_SPI_HEX_STRING_WRITE = true;
    /** Minimum SPI hex string size. */
    public static final int SPI_HEX_STRING_CHAR_SIZE_MIN = 2;
    /** Maximum SPI hex string size. */
    public static final int SPI_HEX_STRING_CHAR_SIZE_MAX = 64;

    /** Check the read size in SPI command arguments. */
    public static final boolean CHECK_SPI_READ_SIZE = true;
    /** Maximum SPI read size. */
    public static final int SPI_MAX_BYTE_READ_SIZE = 64;

    /** Check the PWM frequency value in command arguments. */
    public static final boolean CHECK_PWM_FREQUENCY = true;
    /** Minimum PWM frequency value. */
    public static final int PWM_FREQUENCY_MIN = 1000;
    /** Maximum PWM frequency value. */
    public static final int PWM_FREQUENCY_MAX = 500000;

    /** Check the PWM duty cycle value in command arguments. */
    public static final boolean CHECK_PWM_DUTY_CYCLE = true;
    /** Minimum PWM duty cycle value. */
    public static final int PWM_DUTY_CYCLE_MIN = 0;
    /** Maximum PWM duty cycle value. */
    public static final int PWM_DUTY_CYCLE_MAX = 100;

    /** Check the PWM duration value in command arguments. */
    public static final boolean CHECK_PWM_DURATION = true;
    /** Minimum PWM duration value. */
    public static final int PWM_DURATION_MIN = 100;
    /** Maximum PWM duration value. */
    public static final int PWM_DURATION_MAX = 10000;

    /** Check the bonding entry index value in command arguments. */
    public static final boolean CHECK_BONDING_DATABASE_INDEX = true;
    /** Minimum bonding entry index value. */
    public static final int BONDING_DATABASE_INDEX_MIN = 1;
    /** Maximum bonding entry index value. */
    public static final int BONDING_DATABASE_INDEX_MAX = 5;
    /** Bonding entry index value that selects all entries. */
    public static final int BONDING_DATABASE_ALL_VALUES = 0xff;

    // GPIO configurations
    /** DA14585 GPIO pin configuration. */
    public static final GPIO[] GPIO_LIST_585 = {
            // Port 0, Pin 0-7, 8-9 not used
            new GPIO(0, 0), new GPIO(0, 1), new GPIO(0, 2), new GPIO(0, 3),
            new GPIO(0, 4), new GPIO(0, 5), new GPIO(0, 6), new GPIO(0, 7),
            null, null,
            // Port 1, Pin 0-5, 6-9 not used
            new GPIO(1, 0), new GPIO(1, 1), new GPIO(1, 2), new GPIO(1, 3),
            new GPIO(1, 4), new GPIO(1, 5),
            null, null, null, null,
            // Port 2, Pin 0-9
            new GPIO(2, 0), new GPIO(2, 1), new GPIO(2, 2), new GPIO(2, 3),
            new GPIO(2, 4), new GPIO(2, 5), new GPIO(2, 6), new GPIO(2, 7),
            new GPIO(2, 8), new GPIO(2, 9),
            // Port 3, Pin 0, 1-6 not used
            new GPIO(3, 0),
            null, null, null, null, null, null
    };
    /** DA14531 GPIO pin configuration. */
    public static final GPIO[] GPIO_LIST_531 = {
            // Port 0, Pin 0-11
            new GPIO(0, 0), new GPIO(0, 1), new GPIO(0, 2), new GPIO(0, 3),
            new GPIO(0, 4), new GPIO(0, 5), new GPIO(0, 6), new GPIO(0, 7),
            new GPIO(0, 8), new GPIO(0, 9), new GPIO(0, 10), new GPIO(0, 11)
    };
    /** Supported GPIO configurations. */
    public static final GPIO[][] GPIO_CONFIGURATIONS = {
            GPIO_LIST_585,
            GPIO_LIST_531,
    };

    /**
     * Commands to be processed by the library.
     * <p> The library provides a default implementation with an appropriate response for each command.
     * @see CodelessCommand#processInbound()
     */
    public static final HashSet<CommandID> supportedCommands = new HashSet<>();
    static {
        supportedCommands.add(CommandID.AT);
        supportedCommands.add(CommandID.ATI);
        supportedCommands.add(CommandID.BINREQ);
        supportedCommands.add(CommandID.BINREQACK);
        supportedCommands.add(CommandID.BINREQEXIT);
        supportedCommands.add(CommandID.BINREQEXITACK);
        supportedCommands.add(CommandID.RANDOM);
        supportedCommands.add(CommandID.BATT);
        supportedCommands.add(CommandID.BDADDR);
        supportedCommands.add(CommandID.GAPSTATUS);
        supportedCommands.add(CommandID.PRINT);
    }

    /**
     * Commands to be sent to the app for processing.
     * <p>
     * Add here the commands that you want to be processed by the app.
     * The app is responsible for sending a proper response.
     * Use {@link CodelessManager#sendResponse(String) sendResponse}, {@link CodelessManager#sendSuccess(String) sendSuccess},
     * {@link CodelessManager#sendError(String) sendError} to send the response to the peer device.
     */
    public static final HashSet<CommandID> hostCommands = new HashSet<>();
    static {
    }

    /**
     * Send unsupported commands to the app for processing.
     * <p> Otherwise, an error response is sent by the library.
     * <p> If <code>true</code>, the app is responsible for sending a proper response.
     */
    public static final boolean HOST_UNSUPPORTED_COMMANDS = false;
    /**
     * Send invalid commands to the app for processing.
     * <p> Otherwise, an error response is sent by the library.
     * <p> If <code>true</code>, the app is responsible for sending a proper response.
     */
    public static final boolean HOST_INVALID_COMMANDS = false;
}
