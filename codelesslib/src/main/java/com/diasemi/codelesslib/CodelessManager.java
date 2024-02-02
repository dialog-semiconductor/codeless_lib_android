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

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothStatusCodes;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import com.diasemi.codelesslib.CodelessLibConfig.PREF;
import com.diasemi.codelesslib.CodelessProfile.LineType;
import com.diasemi.codelesslib.CodelessProfile.Uuid;
import com.diasemi.codelesslib.command.BinExitAckCommand;
import com.diasemi.codelesslib.command.BinExitCommand;
import com.diasemi.codelesslib.command.BinRequestAckCommand;
import com.diasemi.codelesslib.command.BinRequestCommand;
import com.diasemi.codelesslib.command.CodelessCommand;
import com.diasemi.codelesslib.command.CustomCommand;
import com.diasemi.codelesslib.dsps.DspsFileReceive;
import com.diasemi.codelesslib.dsps.DspsFileSend;
import com.diasemi.codelesslib.dsps.DspsPeriodicSend;
import com.diasemi.codelesslib.log.CodelessLogFile;
import com.diasemi.codelesslib.log.DspsRxLogFile;
import com.diasemi.codelesslib.misc.RuntimePermissionChecker;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.UUID;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;

import static com.diasemi.codelesslib.CodelessEvent.ERROR_GATT_OPERATION;
import static com.diasemi.codelesslib.CodelessEvent.ERROR_INIT_SERVICES;
import static com.diasemi.codelesslib.CodelessEvent.ERROR_INVALID_COMMAND;
import static com.diasemi.codelesslib.CodelessEvent.ERROR_INVALID_PREFIX;
import static com.diasemi.codelesslib.CodelessEvent.ERROR_NOT_READY;

/**
 * Manages the connection and communication with the peer CodeLess/DSPS device.
 * <h2>Usage</h2>
 * Create a CodelessManager object by providing the {@link BluetoothDevice} you want to interact with.
 * The device can be obtained from a Bluetooth scan using {@link CodelessScanner}.
 * Use {@link #connect()} to connect to the device and {@link #disconnect()} to end the connection.
 * After connection, the library will automatically start a service discovery, request a MTU change
 * (if {@link CodelessLibConfig#REQUEST_MTU} is enabled) and enable all the required notifications.
 * After that, the library is ready for bidirectional communication with the peer device using
 * CodeLess commands and/or DSPS binary data, depending on the supported services.
 * <p>
 * This class provides methods and functionality that allow the app to send CodeLess commands, receive commands and
 * respond to them, as well as send and receive binary data using the DSPS protocol.
 * For example, see: {@link #getState() getState}, {@link #isReady() isReady}, {@link #getCommandFactory() getCommandFactory},
 * {@link #sendCommand(CodelessCommand) sendCommand}, {@link #setMode(boolean) setMode}, {@link #sendDspsData(byte[], int) sendDspsData},
 * {@link #sendFile(Uri, int, int) sendFile}, {@link #sendPattern(Uri, int, int) sendPattern}, {@link #isDspsTxFlowOn() isDspsTxFlowOn}.
 * See {@link CodelessCommand} on how to implement incoming commands.
 * <p>
 * The library generates several events to inform the app about specific actions or results.
 * For example: {@link CodelessEvent.Connection Connection}, {@link CodelessEvent.Ready Ready}, {@link CodelessEvent.Mode Mode},
 * {@link CodelessEvent.Error Error}, {@link CodelessEvent.CommandSuccess CommandSuccess}, {@link CodelessEvent.CommandError CommandError},
 * {@link CodelessEvent.InboundCommand InboundCommand}, {@link CodelessEvent.HostCommand HostCommand},
 * {@link CodelessEvent.DspsRxData DspsRxData}, {@link CodelessEvent.DspsTxFlowControl DspsTxFlowControl}.
 * Each command may generate additional events.
 * <p>
 * The library automatically handles mode switching between command (CodeLess) and binary (DSPS) mode, by implementing the mode
 * commands as described in the CodeLess specification. If {@link CodelessLibConfig#HOST_BINARY_REQUEST} is enabled, see
 * {@link #acceptBinaryModeRequest()} on how to handle a peer request to switch to binary mode.
 * <p>
 * Before trying to connect to a device, the app must be granted the required runtime permissions.
 * These include the storage permissions to read and write scripts and log files, if enabled in the configuration and
 * {@link CodelessLibConfig#SCOPED_STORAGE} is disabled. On Android 12 and above, the BLUETOOTH_CONNECT permission is required.
 * Use {@link #checkPermissions(Context, RuntimePermissionChecker, RuntimePermissionChecker.PermissionRequestCallback) checkPermissions()}
 * to check for and request the required permissions.
 * <p>
 * If {@link CodelessLibConfig#SCOPED_STORAGE} is disabled, files are saved in subfolders of a specific
 * {@link CodelessLibConfig#FILE_PATH path} in the device's external storage. Otherwise, files are saved
 * in subfolders of a user selected path URI. Use {@link #checkStorage(Context) checkStorage} to initialize the
 * previously selected path and {@link #setFilePathUri(Context, Uri) setFilePathUri} to select a path.
 * @see CodelessScanner
 * @see CodelessCommands
 * @see CodelessEvent
 * @see <a href="https://lpccs-docs.renesas.com/UM-140-DA145x-CodeLess/index.html">CodeLess User Manual</a>
 */
public class CodelessManager {
    private final static String TAG = "CodelessManager";

    // State
    /** The device is disconnected. */
    public static final int DISCONNECTED = 0;
    /** Connection in progress. */
    public static final int CONNECTING = 1;
    /** The device is connected. */
    public static final int CONNECTED = 2;
    /** Service discovery in progress. */
    public static final int SERVICE_DISCOVERY = 3;
    /** The device is ready for operation. */
    public static final int READY = 4;

    /** Indicates that the speed value hasn't been set yet. */
    public static final int SPEED_INVALID = -1;

    /**
     * Path URI where the library saves/loads files, if {@link CodelessLibConfig#SCOPED_STORAGE} is enabled.
     * @see #setFilePathUri(Context, Uri)
     */
    private static Uri FILE_PATH_URI;
    /**
     * Path where the library saves/loads files, if {@link CodelessLibConfig#SCOPED_STORAGE} is enabled.
     * <p> Initialized from {@link #FILE_PATH_URI}.
     */
    private static DocumentFile FILE_PATH;

    private Context context;
    private BluetoothDevice device;
    private BluetoothGatt gatt;
    private int state = DISCONNECTED;
    private int mtu = CodelessProfile.MTU_DEFAULT;
    private Handler handler;
    private LinkedList<GattOperation> gattQueue = new LinkedList<>();
    private GattOperation gattOperationPending;
    private boolean commandMode;
    private boolean binaryRequestPending;
    private boolean binaryExitRequestPending;
    // Codeless
    private CodelessCommands commandFactory;
    private ArrayDeque<CodelessCommand> commandQueue = new ArrayDeque<>();
    private CodelessCommand commandPending;
    private CodelessCommand commandInbound;
    private int inboundPending;
    private int outboundResponseLines;
    private ArrayList<String> parsePending = new ArrayList<>();
    private CodelessLogFile codelessLogFile;
    // DSPS
    private int dspsChunkSize = CodelessLibConfig.DEFAULT_DSPS_CHUNK_SIZE;
    private boolean dspsRxFlowOn = CodelessLibConfig.DEFAULT_DSPS_RX_FLOW_CONTROL;
    private boolean dspsTxFlowOn = CodelessLibConfig.DEFAULT_DSPS_TX_FLOW_CONTROL;
    private boolean dspsEcho;
    private ArrayList<GattOperation> dspsPending = new ArrayList<>();
    private ArrayList<DspsPeriodicSend> dspsPeriodic = new ArrayList<>();
    private ArrayList<DspsFileSend> dspsFiles = new ArrayList<>();
    private DspsFileReceive dspsFileReceive;
    private DspsRxLogFile dspsRxLogFile;
    private Handler dspsLogHandler;
    private Handler dspsStatsHandler;
    private long dspsLastInterval;
    private int dspsRxBytesInterval;
    private int dspsRxSpeed = SPEED_INVALID;
    // Service database
    private boolean servicesDiscovered;
    private boolean codelessSupport;
    private boolean dspsSupport;
    private BluetoothGattService codelessService;
    private BluetoothGattCharacteristic codelessInbound;
    private BluetoothGattCharacteristic codelessOutbound;
    private BluetoothGattCharacteristic codelessFlowControl;
    private BluetoothGattService dspsService;
    private BluetoothGattCharacteristic dspsServerTx;
    private BluetoothGattCharacteristic dspsServerRx;
    private BluetoothGattCharacteristic dspsFlowControl;
    private BluetoothGattService deviceInfoService;
    private ArrayList<BluetoothGattCharacteristic> pendingEnableNotifications;
    private String logPrefix;

    /**
     * Creates a CodelessManager to manage the connection with the specified device.
     * @param context   the application context
     * @param device    the device to connect to
     */
    public CodelessManager(Context context, BluetoothDevice device) {
        this.context = context.getApplicationContext();
        this.device = device;
        commandFactory = new CodelessCommands(this);
        logPrefix = "[" + device.getAddress() + "] ";
    }

    /** Returns the associated application context. */
    public Context getContext() {
        return context;
    }

    /** Returns the associated device. */
    public BluetoothDevice getDevice() {
        return device;
    }

    /** Returns the connection {@link #DISCONNECTED state}. */
    public int getState() {
        return state;
    }

    /** Returns the connection MTU. */
    public int getMtu() {
        return mtu;
    }

    /**
     * Sets the connection MTU.
     * <p> WARNING: This does not initiate a MTU request, it just sets the local value.
     * @see #requestMtu(int)
     */
    public void setMtu(int mtu) {
        this.mtu = mtu;
    }

    /**
     * Initiates a MTU request.
     * @param mtu the MTU value
     */
    public void requestMtu(int mtu) {
        if (Build.VERSION.SDK_INT < 21)
            return;
        enqueueGattOperation(new GattOperation(mtu));
    }

    /** Returns the background handler. */
    public Handler getHandler() {
        return handler;
    }

    /** Returns the log prefix, used for log messages. */
    public String getLogPrefix() {
        return logPrefix;
    }

    /**
     * Checks if the required permissions are granted and requests them if not.
     * <p>
     * If {@link CodelessLibConfig#SCOPED_STORAGE} is disabled, READ_EXTERNAL_STORAGE permissions is required,
     * plus WRITE_EXTERNAL_STORAGE permissions if log files are enabled.
     * <p>
     * From Android 12, BLUETOOTH_CONNECT permission is required.
     * @param context           the application context
     * @param permissionChecker the permission checker used to check/requests permissions
     * @param callback          the permission request callback
     * @return <code>true</code> if all the required permissions are granted
     */
    public static boolean checkPermissions(Context context, RuntimePermissionChecker permissionChecker, RuntimePermissionChecker.PermissionRequestCallback callback) {
        if (Build.VERSION.SDK_INT < 23)
            return true;
        String[] permissions = new String[0];
        int rationale = 0;
        if (!CodelessLibConfig.SCOPED_STORAGE) {
            if (CodelessLibConfig.CODELESS_LOG || CodelessLibConfig.DSPS_RX_LOG) {
                permissions = new String[] { Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE };
                rationale = R.string.codeless_storage_permission_rationale;
            } else {
                permissions = new String[] { Manifest.permission.READ_EXTERNAL_STORAGE };
                rationale = R.string.codeless_storage_permission_rationale_no_log;
            }
        }
        if (!CodelessUtil.checkConnectPermission(context)) {
            permissions = Arrays.copyOf(permissions, permissions.length + 1);
            permissions[permissions.length - 1] = Manifest.permission.BLUETOOTH_CONNECT;
        }
        return permissions.length == 0 || permissionChecker.checkPermissions(permissions, rationale != 0 ? context.getString(rationale) : null, callback);
    }

    /**
     * Checks if the file path URI is configured and accessible, if {@link CodelessLibConfig#SCOPED_STORAGE} is enabled.
     * @param context the application context
     */
    public static boolean checkStorage(Context context) {
        if (!CodelessLibConfig.SCOPED_STORAGE)
            return true;
        if (isFilePathValid())
            return true;
        if (FILE_PATH_URI == null) {
            SharedPreferences preferences = context.getSharedPreferences(PREF.NAME, Context.MODE_PRIVATE);
            String uri = preferences.getString(PREF.filePathUri, null);
            if (uri != null)
                FILE_PATH_URI = Uri.parse(uri);
        }
        if (FILE_PATH_URI != null)
            FILE_PATH = DocumentFile.fromTreeUri(context.getApplicationContext(), FILE_PATH_URI);
        return isFilePathValid();
    }

    /** Returns the configured file path URI. */
    public static Uri getFilePathUri() {
        return FILE_PATH_URI;
    }

    /**
     * Sets the file path URI that will be used to save and load files, if {@link CodelessLibConfig#SCOPED_STORAGE} is enabled.
     * <p> Files include CodeLess/DSPS logs, received files, scripts.
     * @param context       the application context
     * @param filePathUri   the selected file path URI
     */
    public static void setFilePathUri(Context context, Uri filePathUri) {
        if (FILE_PATH_URI != null) {
            try {
                context.getContentResolver().releasePersistableUriPermission(FILE_PATH_URI, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            } catch (Exception e) {
                Log.e(TAG, "Failed to release URI permission: " + FILE_PATH_URI);
            }
        }
        FILE_PATH_URI = filePathUri;
        context.getSharedPreferences(PREF.NAME, Context.MODE_PRIVATE).edit().putString(PREF.filePathUri, FILE_PATH_URI.toString()).apply();
        FILE_PATH = DocumentFile.fromTreeUri(context.getApplicationContext(), FILE_PATH_URI);
        try {
            context.getContentResolver().takePersistableUriPermission(FILE_PATH_URI, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        } catch (Exception e) {
            Log.e(TAG, "Failed to persist URI permission: " + FILE_PATH_URI);
        }
    }

    /** Checks if the file path URI is accessible. */
    public static boolean isFilePathValid() {
        return FILE_PATH != null && FILE_PATH.exists() && FILE_PATH.isDirectory() && FILE_PATH.canRead() && FILE_PATH.canWrite();
    }

    /** Returns the configured file path (initialized from the configured path URI). */
    public static DocumentFile getFilePath() {
        return FILE_PATH;
    }

    /** Connects to the peer device. */
    synchronized public void connect() {
        Log.d(TAG, logPrefix + "Connect");
        if (state != DISCONNECTED)
            return;
        if (!CodelessUtil.checkConnectPermission(context)) {
            Log.e(TAG, logPrefix + "Missing BLUETOOTH_CONNECT permission");
            return;
        }
        state = CONNECTING;
        EventBus.getDefault().post(new CodelessEvent.Connection(this));
        if (CodelessLibConfig.BLUETOOTH_STATE_MONITOR)
            context.registerReceiver(bluetoothStateReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
        if (Build.VERSION.SDK_INT < 23) {
            gatt = device.connectGatt(context, false, gattCallback);
        } else {
            gatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE);
        }
    }

    /** Disconnects from the peer device. */
    @SuppressLint("MissingPermission")
    synchronized public void disconnect() {
        Log.d(TAG, logPrefix + "Disconnect");
        if (gatt == null)
            return;
        gatt.disconnect();
        if (state == CONNECTING) {
            state = DISCONNECTED;
            gatt.close();
            gatt = null;
            if (CodelessLibConfig.BLUETOOTH_STATE_MONITOR)
                context.unregisterReceiver(bluetoothStateReceiver);
            EventBus.getDefault().post(new CodelessEvent.Connection(this));
        }
    }

    /** Checks if the device is connected. */
    public boolean isConnected() {
        return state >= CONNECTED;
    }

    /** Checks if the connection is in progress. */
    public boolean isConnecting() {
        return state == CONNECTING;
    }

    /** Checks if the device is disconnected. */
    public boolean isDisconnected() {
        return state == DISCONNECTED;
    }

    /** Checks if the service discovery is complete. */
    public boolean servicesDiscovered() {
        return servicesDiscovered;
    }

    /** Checks if the peer device supports CodeLess. */
    public boolean codelessSupport() {
        return codelessSupport;
    }

    /** Checks if the peer device supports DSPS. */
    public boolean dspsSupport() {
        return dspsSupport;
    }

    /**
     * Checks if the peer device is ready for Codeless/DSPS operations.
     * <p> The device becomes ready after the service discovery is complete and the required notifications are enabled.
     */
    public boolean isReady() {
        return state == READY;
    }

    /**
     * Checks if the device is ready.
     * <p> If not, a {@link CodelessEvent.Error Error} event is generated.
     */
    private boolean checkReady() {
        if (!isReady()) {
            Log.e(TAG, logPrefix + "Device not ready. Operation not allowed.");
            EventBus.getDefault().post(new CodelessEvent.Error(this, ERROR_NOT_READY));
            return false;
        } else {
            return true;
        }
    }

    /** Checks if the device has the GAP name characteristic. */
    public boolean hasDeviceName() {
        return gatt.getService(Uuid.GAP_SERVICE) != null && gatt.getService(Uuid.GAP_SERVICE).getCharacteristic(Uuid.GAP_DEVICE_NAME) != null;
    }

    /** Reads the GAP name characteristic. */
    public void readDeviceName() {
        if (!hasDeviceName()) {
            Log.e(TAG, logPrefix + "Device name not available");
            return;
        }
        readCharacteristic(gatt.getService(Uuid.GAP_SERVICE).getCharacteristic(Uuid.GAP_DEVICE_NAME));
    }

    /**
     * Called when the GAP name characteristic is read successfully.
     * <p> A {@link CodelessEvent.DeviceName DeviceName} event is generated.
     */
    private void onDeviceNameRead(byte[] value) {
        String name = new String(value, StandardCharsets.UTF_8);
        Log.d(TAG, logPrefix + "Device name : " + name);
        EventBus.getDefault().post(new CodelessEvent.DeviceName(this, name));
    }

    /**
     * Checks if the device has one of the device information service characteristics.
     * @param uuid the UUID of the characteristic to check, or <code>null</code> to just check for the service
     */
    public boolean hasDeviceInfo(UUID uuid) {
        return deviceInfoService != null && (uuid == null || deviceInfoService.getCharacteristic(uuid) != null);
    }

    /**
     * Reads one of the device information service characteristics.
     * @param uuid the UUID of the characteristic to read
     */
    public void readDeviceInfo(UUID uuid) {
        if (!hasDeviceInfo(uuid)) {
            Log.e(TAG, logPrefix + "Device information not available: " + uuid);
            return;
        }
        readCharacteristic(deviceInfoService.getCharacteristic(uuid));
    }

    /**
     * Called when a device information service characteristic is read successfully.
     * <p> A {@link CodelessEvent.DeviceInfo DeviceInfo} event is generated.
     */
    private void onDeviceInfoRead(BluetoothGattCharacteristic characteristic, byte[] value) {
        UUID uuid = characteristic.getUuid();
        String info = new String(value, StandardCharsets.UTF_8);
        Log.d(TAG, logPrefix + "Device information ["+ uuid + "]: " + info);
        EventBus.getDefault().post(new CodelessEvent.DeviceInfo(this, uuid, value, info));
    }

    /**
     * Reads the connection RSSI.
     * <p> On success, a {@link CodelessEvent.Rssi} event is generated.
     */
    @SuppressLint("MissingPermission")
    public void getRssi() {
        if (isConnected())
            gatt.readRemoteRssi();
    }

    /**
     * Called after the service discovery is complete and the required notifications are enabled.
     * <p> A {@link CodelessEvent.Ready Ready} event is generated.
     */
    private void onDeviceReady() {
        Log.d(TAG, logPrefix + "Device ready");
        state = READY;
        if (codelessSupport) {
            commandMode = CodelessLibConfig.START_IN_COMMAND_MODE;
        }
        if (dspsSupport) {
            if (CodelessLibConfig.SET_FLOW_CONTROL_ON_CONNECTION)
                setDspsRxFlowOn(dspsRxFlowOn);
            if (CodelessLibConfig.DSPS_STATS) {
                if (!commandMode) {
                    dspsRxBytesInterval = 0;
                    dspsLastInterval = new Date().getTime();
                    dspsStatsHandler.postDelayed(dspsUpdateStats, CodelessLibConfig.DSPS_STATS_INTERVAL);
                }
            }
        }
        EventBus.getDefault().post(new CodelessEvent.Ready(this));
    }

    /** Checks if the device is in command (CodeLess) mode. */
    public boolean commandMode() {
        return commandMode;
    }

    /** Checks if the device is in binary (DSPS) mode. */
    public boolean binaryMode() {
        return !commandMode;
    }

    /**
     * Checks if a binary operation can be performed in the current mode.
     * @param outbound <code>true</code> for outgoing data, <code>false</code> for incoming data
     */
    private boolean checkBinaryMode(boolean outbound) {
        if (outbound) {
            if (!dspsSupport) {
                Log.e(TAG, logPrefix + "DSPS not supported");
                EventBus.getDefault().post(new CodelessEvent.Error(this, CodelessEvent.ERROR_OPERATION_NOT_ALLOWED));
                return false;
            }
            if (!CodelessLibConfig.ALLOW_OUTBOUND_BINARY_IN_COMMAND_MODE) {
                if (commandMode) {
                    Log.e(TAG, logPrefix + "Binary data not allowed in command mode");
                    EventBus.getDefault().post(new CodelessEvent.Error(this, CodelessEvent.ERROR_OPERATION_NOT_ALLOWED));
                    return false;
                }
            }
        } else {
            if (!CodelessLibConfig.ALLOW_INBOUND_BINARY_IN_COMMAND_MODE) {
                if (commandMode) {
                    Log.e(TAG, logPrefix + "Received binary data in command mode");
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Checks if a command operation can be performed in the current mode.
     * <p> {@link CodelessProfile.Command#modeCommands Mode commands} are allowed in binary mode.
     * @param outbound  <code>true</code> for outgoing data, <code>false</code> for incoming data
     * @param command   the corresponding command, if available
     */
    private boolean checkCommandMode(boolean outbound, CodelessCommand command) {
        if (outbound) {
            if (!codelessSupport) {
                Log.e(TAG, logPrefix + "Codeless not supported");
                EventBus.getDefault().post(new CodelessEvent.Error(this, CodelessEvent.ERROR_OPERATION_NOT_ALLOWED));
                return false;
            }
            if (!CodelessLibConfig.ALLOW_OUTBOUND_COMMAND_IN_BINARY_MODE) {
                if (!commandMode && (command == null || !CodelessProfile.Command.isModeCommand(command))) {
                    Log.e(TAG, logPrefix + "Commands not allowed in binary mode");
                    EventBus.getDefault().post(new CodelessEvent.Error(this, CodelessEvent.ERROR_OPERATION_NOT_ALLOWED));
                    return false;
                }
            }
        } else {
            if (!CodelessLibConfig.ALLOW_INBOUND_COMMAND_IN_BINARY_MODE) {
                if (!commandMode && (command == null || !CodelessProfile.Command.isModeCommand(command))) {
                    Log.e(TAG, logPrefix + "Received command in binary mode");
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Sets the operation mode.
     * <p>
     * If the mode needs to change, the appropriate mode command is sent to change the mode.
     * After the mode command transaction is complete, the mode will change and
     * a {@link CodelessEvent.Mode Mode} event will be generated.
     * @param command <code>true</code> for command mode, <code>false</code> for binary mode
     */
    public void setMode(boolean command) {
        if (commandMode == command)
            return;
        Log.d(TAG, logPrefix + "Change to "+ (command ? "command": "binary") + " mode");
        if (!command) {
            sendCommand(CodelessLibConfig.MODE_CHANGE_SEND_BINARY_REQUEST ? new BinRequestCommand(this) : new BinRequestAckCommand(this));
        } else {
            sendCommand(new BinExitCommand(this));
        }
    }

    /**
     * Accepts the binary mode request that was sent by the peer device.
     * <p>
     * If <code>AT+BINREQ</code> is received and {@link CodelessLibConfig#HOST_BINARY_REQUEST} is enabled,
     * a {@link CodelessEvent.BinaryModeRequest BinaryModeRequest} event is generated.
     * The app should call this function to accept the mode change.
     * The library responds with <code>AT+BINREQACK</code> and enters binary mode.
     */
    public void acceptBinaryModeRequest() {
        if (!binaryRequestPending) {
            Log.e(TAG, logPrefix + "No binary mode request pending");
            return;
        }
        binaryRequestPending = false;
        Log.d(TAG, logPrefix + "Binary mode request accepted");
        sendCommand(new BinRequestAckCommand(this));
    }

    /**
     * Accepts the binary mode exit request that was sent by the peer device.
     * <p> NOTE: This is deprecated. The library responds automatically with <code>AT+BINREQEXITACK</code> and exits binary mode.
     */
    public void acceptBinaryModeExitRequest() {
        if (!binaryExitRequestPending) {
            Log.e(TAG, logPrefix + "No binary mode exit request pending");
            return;
        }
        binaryExitRequestPending = false;
        Log.d(TAG, logPrefix + "Binary mode exit request accepted");
        sendCommand(new BinExitAckCommand(this));
    }

    /** Actions performed when switching from command to binary mode. */
    synchronized private void enterBinaryMode() {
        if (!commandMode)
            return;
        Log.d(TAG, logPrefix + "Enter binary mode");
        commandMode = false;
        EventBus.getDefault().post(new CodelessEvent.Mode(this, commandMode));

        // Remove pending commands
        Iterator<CodelessCommand> i = commandQueue.iterator();
        while (i.hasNext()) {
            if (!CodelessProfile.Command.isModeCommand(i.next()))
                i.remove();
        }

        if (CodelessLibConfig.CODELESS_LOG)
            codelessLogFile.log("=========== BINARY MODE ==========");

        if (CodelessLibConfig.DSPS_STATS) {
            dspsRxBytesInterval = 0;
            dspsLastInterval = new Date().getTime();
            dspsStatsHandler.postDelayed(dspsUpdateStats, CodelessLibConfig.DSPS_STATS_INTERVAL);
        }
        resumeDspsOperations();
    }

    /** Actions performed when switching from binary to command mode. */
    synchronized private void exitBinaryMode() {
        if (commandMode)
            return;
        Log.d(TAG, logPrefix + "Exit binary mode");
        commandMode = true;
        EventBus.getDefault().post(new CodelessEvent.Mode(this, commandMode));

        if (CodelessLibConfig.DSPS_STATS) {
            dspsStatsHandler.removeCallbacks(dspsUpdateStats);
        }
        pauseDspsOperations(false);

        if (CodelessLibConfig.CODELESS_LOG)
            codelessLogFile.log("=========== COMMAND MODE ==========");
    }

    /** Called when <code>AT+BINREQ</code> is sent successfully. */
    public void onBinRequestSent() {
    }

    /**
     * Called when <code>AT+BINREQ</code> is received.
     * <p>
     * If a mode switch is needed and {@link CodelessLibConfig#HOST_BINARY_REQUEST} is enabled,
     * a {@link CodelessEvent.BinaryModeRequest BinaryModeRequest} event is generated,
     * otherwise the library responds automatically with <code>AT+BINREQACK</code>.
     * <p>
     * The app should call {@link #acceptBinaryModeRequest()} to accept the request.
     */
    public void onBinRequestReceived() {
        if (!commandMode) {
            Log.d(TAG, logPrefix + "Already in binary mode");
            sendCommand(new BinRequestAckCommand(this));
            return;
        }
        if (CodelessLibConfig.HOST_BINARY_REQUEST) {
            Log.d(TAG, logPrefix + "Pass binary mode request to host");
            binaryRequestPending = true;
            EventBus.getDefault().post(new CodelessEvent.BinaryModeRequest(this));
        } else {
            sendCommand(new BinRequestAckCommand(this));
        }
    }

    /**
     * Called when <code>AT+BINREQACK</code> is sent successfully.
     * <p> The library switches to binary mode (if needed).
     */
    public void onBinAckSent() {
        enterBinaryMode();
    }

    /**
     * Called when <code>AT+BINREQACK</code> is received.
     * <p> The library switches to binary mode (if needed).
     */
    public void onBinAckReceived() {
        enterBinaryMode();
    }

    /**
     * Called when <code>AT+BINREQEXIT</code> is sent successfully.
     * <p> The library switches to command mode (if needed).
     */
    public void onBinExitSent() {
        exitBinaryMode();
    }

    /**
     * Called when <code>AT+BINREQEXIT</code> is received.
     * <p> The library responds with <code>AT+BINREQEXITACK</code> and switches to command mode (if needed).
     */
    public void onBinExitReceived() {
        if (commandMode) {
            Log.d(TAG, logPrefix + "Already in command mode");
            sendCommand(new BinExitAckCommand(this));
            return;
        }
        exitBinaryMode();
        sendCommand(new BinExitAckCommand(this));
    }

    /** Called when <code>AT+BINREQEXITACK</code> is sent successfully. */
    public void onBinExitAckSent() {
    }

    /** Called when <code>AT+BINREQEXITACK</code> is received. */
    public void onBinExitAckReceived() {
    }

    /** Checks if an outgoing command is pending (sent and waiting for response). */
    public boolean isCommandPending() {
        return commandPending != null;
    }

    /** Returns the pending outgoing command. */
    public CodelessCommand getCommandPending() {
        return commandPending;
    }

    /**
     * Checks if there are incoming CodeLess data that must be read.
     * <p> The library handles this automatically.
     */
    public boolean isInboundPending() {
        return inboundPending > 0;
    }

    /**
     * Returns the number of CodeLess data that must be read.
     * <p>
     * This number is increased for every CodeLess Flow Control notification.
     * The library reads the CodeLess Outbound characteristic to get the incoming data.
     */
    public int getInboundPending() {
        return inboundPending;
    }

    /** Returns the command creation helper object. */
    public CodelessCommands getCommandFactory() {
        return commandFactory;
    }

    /**
     * Sends a text command to the peer device.
     * <p> The command is parsed to a {@link CodelessCommand} subclass object.
     * @param line the text command
     */
    public void sendCommand(String line) {
        if (!line.trim().isEmpty())
            sendCommand(parseTextCommand(line));
    }

    /**
     * Sends a series of text commands to the peer device.
     * @param script the command script (one command per line)
     * @see CodelessScript
     */
    public void sendCommandScript(Collection<String> script) {
        ArrayList<CodelessCommand> commands = new ArrayList<>();
        for (String line : script) {
            if (!line.trim().isEmpty())
                commands.add(parseTextCommand(line));
        }
        sendCommands(commands);
    }

    /**
     * Parses a text command to a {@link CodelessCommand} subclass object.
     * @param line the text command
     * @return the command subclass object
     */
    public CodelessCommand parseTextCommand(String line) {
        line = line.trim();
        if (CodelessLibConfig.AUTO_ADD_PREFIX && !CodelessProfile.hasPrefix(line))
            line = CodelessProfile.PREFIX + line;

        if (CodelessLibLog.CODELESS)
            Log.d(TAG, logPrefix + "Text command: " + line);

        if (CodelessLibConfig.CODELESS_LOG)
            codelessLogFile.logText(line);

        CodelessCommand command;
        String id = CodelessProfile.getCommand(line);
        if (id == null) {
            command = new CustomCommand(this, line, true);
        } else {
            Class<? extends CodelessCommand> commandClass = CodelessProfile.Command.commandMap.get(id);
            if (commandClass == null) {
                command = new CustomCommand(this, line, true);
            } else {
                String prefix = CodelessProfile.getPrefix(line);
                line = CodelessProfile.removeCommandPrefix(line);
                command = CodelessProfile.createCommand(this, commandClass, line);
                command.setPrefix(prefix);
            }
        }

        if (CodelessLibLog.CODELESS)
            Log.d(TAG, logPrefix + "Text command identified: " + command + (command.isValid() ? "" : " (invalid)"));
        return command;
    }

    /**
     * Sends a command to the peer device.
     * @param command the command to send
     */
    public void sendCommand(CodelessCommand command) {
        if (!checkReady() || !checkCommandMode(true, command))
            return;
        enqueueCommand(command);
    }

    /**
     * Sends a series of commands to the peer device.
     * @param commands the commands to send
     * @see CodelessScript
     */
    public void sendCommands(Collection<CodelessCommand> commands) {
        if (!checkReady() || !checkCommandMode(true, null))
            return;
        enqueueCommands(commands);
    }

    /**
     * Enqueues a command to be sent.
     * @param command the command to send
     */
    synchronized private void enqueueCommand(CodelessCommand command) {
        if (commandPending != null || commandInbound != null || inboundPending > 0) {
            commandQueue.add(command);
        } else {
            commandPending = command;
            executeCommand(command);
        }
    }

    /**
     * Enqueues a series of commands to be sent.
     * @param commands the commands to send
     */
    synchronized private void enqueueCommands(Collection<CodelessCommand> commands) {
        commandQueue.addAll(commands);
        if (commandPending == null) {
            dequeueCommand();
        }
    }

    /** Dequeues and sends the next command from the command queue. */
    synchronized private void dequeueCommand() {
        if (commandQueue.isEmpty() || commandInbound != null || inboundPending > 0)
            return;
        commandPending = commandQueue.poll();
        executeCommand(commandPending);
    }

    /**
     * Actions performed when the pending outgoing command is complete.
     * @param dequeue <code>true</code> to dequeue and send the next command
     */
    synchronized private void commandComplete(boolean dequeue) {
        if (CodelessLibLog.CODELESS)
            Log.d(TAG, logPrefix + "Command complete: " + commandPending);
        parsePending.clear();
        commandPending = null;
        if (dequeue)
            dequeueCommand();
    }

    /** Actions performed when the pending incoming command is complete. */
    synchronized private void inboundCommandComplete() {
        if (CodelessLibLog.CODELESS)
            Log.d(TAG, logPrefix + "Inbound command complete: " + commandInbound);
        if (CodelessLibConfig.SINGLE_WRITE_RESPONSE)
            parsePending.clear();
        else
            outboundResponseLines = 0;
        commandInbound = null;
        dequeueCommand();
    }

    /** Sends a command to the peer device. */
    private void executeCommand(CodelessCommand command) {
        if (CodelessLibLog.CODELESS)
            Log.d(TAG, logPrefix + "Send codeless command: " + command);
        if (!checkReady()) {
            command.setComplete();
            commandComplete(true);
            return;
        }

        if (!command.isParsed())
            command.packCommand();
        String text = command.getCommand();
        if (command.getCommandID() != CodelessProfile.CommandID.CUSTOM) {
            String prefix = !CodelessProfile.Command.isModeCommand(command) ? CodelessProfile.PREFIX_REMOTE : CodelessProfile.PREFIX_LOCAL;
            text = prefix + text.replaceFirst(CodelessProfile.PREFIX_PATTERN_STRING, "");
        } else if (CodelessLibConfig.DISALLOW_INVALID_PREFIX && !CodelessProfile.hasPrefix(text)) {
            if (CodelessLibLog.CODELESS)
                Log.d(TAG, logPrefix + "Invalid prefix: " + text);
            EventBus.getDefault().post(new CodelessEvent.Error(this, ERROR_INVALID_PREFIX));
            command.setComplete();
            commandComplete(true);
            return;
        }

        if (CodelessLibConfig.DISALLOW_INVALID_COMMAND && !command.isParsed() && !command.isValid()) {
            Log.e(TAG, logPrefix + "Invalid command: " + text);
            EventBus.getDefault().post(new CodelessEvent.Error(this, ERROR_INVALID_COMMAND));
            command.setComplete();
            commandComplete(true);
            return;
        }

        if (CodelessLibConfig.DISALLOW_INVALID_PARSED_COMMAND && command.isParsed() && !command.isValid()) {
            if (CodelessLibLog.CODELESS)
                Log.d(TAG, logPrefix + "Invalid command: " + text);
            EventBus.getDefault().post(new CodelessEvent.Error(this, ERROR_INVALID_COMMAND));
            command.setComplete();
            commandComplete(true);
            return;
        }

        if (CodelessLibLog.CODELESS)
            Log.d(TAG, logPrefix + "Codeless command text: " + text);
        sendText(text, LineType.OutboundCommand);
    }

    /**
     * Completes the specified outgoing command, if it is currently pending.
     * @param command the command to complete
     */
    public void completePendingCommand(CodelessCommand command) {
        if (commandPending != command) {
            Log.e(TAG, logPrefix + "Not current pending command: " + command);
            return;
        }
        commandPending.setComplete();
        commandComplete(true);
    }

    /**
     * CodeLess communication processing.
     * @param line the communication text
     * @param type the communication type
     */
    private void processCodelessLine(String line, LineType type) {
        CodelessProfile.Line codelessLine = new CodelessProfile.Line(line, type);
        if (CodelessLibConfig.CODELESS_LOG)
            codelessLogFile.log(codelessLine);
        if (CodelessLibConfig.LINE_EVENTS)
            EventBus.getDefault().post(new CodelessEvent.CodelessLine(this, codelessLine));
    }

    /**
     * Sends a success response to the peer device.
     * <p> Use this to respond to a supported incoming command.
     * @see CodelessCommand#sendSuccess()
     */
    public void sendSuccess() {
        if (commandInbound == null) {
            Log.e(TAG, logPrefix + "No inbound command pending");
            return;
        }
        if (CodelessLibLog.CODELESS)
            Log.d(TAG, logPrefix + "Send success: " + commandInbound);
        if (CodelessLibConfig.SINGLE_WRITE_RESPONSE) {
            sendText(createSingleWriteResponse(true, null), LineType.OutboundResponse);
        } else {
            sendText(!CodelessLibConfig.EMPTY_LINE_BEFORE_OK || outboundResponseLines > 0 ? CodelessProfile.OK : "\n" + CodelessProfile.OK, LineType.OutboundOK);
        }
        if (commandInbound.isComplete())
            inboundCommandComplete();
    }

    /**
     * Sends a success response to the peer device, prepended with the specified response message.
     * <p> Use this to respond to a supported incoming command.
     * @param response the response message
     * @see CodelessCommand#sendSuccess(String)
     */
    public void sendSuccess(String response) {
        if (commandInbound == null) {
            Log.e(TAG, logPrefix + "No inbound command pending");
            return;
        }
        if (CodelessLibConfig.SINGLE_WRITE_RESPONSE) {
            if (CodelessLibLog.CODELESS) {
                Log.d(TAG, logPrefix + "Send response: " + commandInbound + " " + response);
                Log.d(TAG, logPrefix + "Send success: " + commandInbound);
            }
            sendText(response + "\n" + CodelessProfile.OK, LineType.OutboundResponse);
            if (commandInbound.isComplete())
                inboundCommandComplete();
        } else {
            sendResponse(response);
            sendSuccess();
        }
    }

    /**
     * Sends an error response to the peer device, prepended with the specified error message.
     * <p> Use this to respond to a supported incoming command with an error.
     * @param error the error message
     * @see CodelessCommand#sendError(String)
     */
    public void sendError(String error) {
        if (commandInbound == null) {
            Log.e(TAG, logPrefix + "No inbound command pending");
            return;
        }
        if (CodelessLibLog.CODELESS)
            Log.d(TAG, logPrefix + "Send error: " + commandInbound + " " + error);
        if (CodelessLibConfig.SINGLE_WRITE_RESPONSE) {
            sendText(createSingleWriteResponse(false, error), LineType.OutboundError);
        } else {
            sendText(!CodelessLibConfig.EMPTY_LINE_BEFORE_ERROR || outboundResponseLines > 0 ? error : "\n" + error, LineType.OutboundError);
        }
        if (commandInbound.isComplete())
            inboundCommandComplete();
    }

    /**
     * Sends a response message to the peer device.
     * <p>
     * Use this to respond to a supported incoming command with a message.
     * The command is still pending after a call to this method.
     * You can add more response messages or complete the command.
     * <p>
     * If {@link CodelessLibConfig#SINGLE_WRITE_RESPONSE} is enabled the response is not sent immediately.
     * It will be sent along with the success or error response.
     * @param response the response message
     * @see #sendSuccess(String)
     * @see #sendError(String)
     */
    public void sendResponse(String response) {
        if (commandInbound == null) {
            Log.e(TAG, logPrefix + "No inbound command pending");
            return;
        }
        if (CodelessLibLog.CODELESS)
            Log.d(TAG, logPrefix + "Send response: " + commandInbound + " " + response);
        if (CodelessLibConfig.SINGLE_WRITE_RESPONSE) {
            parsePending.add(response);
        } else {
            sendText(response, LineType.OutboundResponse);
        }
    }

    /**
     * Completes the specified incoming command, if it is currently pending.
     * @param command the command to complete
     * @see #sendSuccess(String)
     * @see #sendError(String)
     */
    public void completeInboundCommand(CodelessCommand command) {
        if (commandInbound != command) {
            Log.e(TAG, logPrefix + "Not current inbound command: " + command);
            return;
        }
        commandInbound.setComplete();
        inboundCommandComplete();
    }

    /**
     * Sends an error message to the peer device, if the parsing of the incoming command failed.
     * @param error the error message
     */
    private void sendParseError(String error) {
        if (CodelessLibLog.CODELESS)
            Log.d(TAG, logPrefix + "Send error: " + error);
        error = CodelessProfile.ERROR_PREFIX + error;
        if (CodelessLibConfig.SINGLE_WRITE_RESPONSE) {
            sendText(error + "\n" + CodelessProfile.ERROR, LineType.OutboundError);
        } else {
            sendText(error, LineType.OutboundError);
            sendText(CodelessProfile.ERROR, LineType.OutboundError);
            outboundResponseLines = 0;
        }
    }

    /**
     * Creates the combined response text for a single write operation.
     * <p> Used if {@link CodelessLibConfig#SINGLE_WRITE_RESPONSE} is enabled.
     **/
    private String createSingleWriteResponse(boolean success, String message) {
        StringBuilder text = new StringBuilder();
        for (String line : parsePending) {
            text.append(line).append("\n");
        }
        parsePending.clear();
        if (message != null)
            text.append(message).append("\n");
        if (success) {
            if (CodelessLibConfig.EMPTY_LINE_BEFORE_OK && text.length() == 0)
                text.append("\n");
            text.append(CodelessProfile.OK);
        } else {
            if (CodelessLibConfig.EMPTY_LINE_BEFORE_ERROR && text.length() == 0)
                text.append("\n");
            text.append(CodelessProfile.ERROR);
        }
        return text.toString();
    }

    /** Sends the specified text to the peer device, by writing to the CodeLess Inbound characteristic. */
    private void sendText(String text, LineType type) {
        if (CodelessLibConfig.CODELESS_LOG || CodelessLibConfig.LINE_EVENTS) {
            for (String line : text.split("\n", -1)) {
                LineType lineType = type;
                if (line.isEmpty())
                    lineType = LineType.OutboundEmpty;
                else if (CodelessProfile.isSuccess(line))
                    lineType = LineType.OutboundOK;
                processCodelessLine(line, lineType);
            }
        }

        if (!CodelessLibConfig.SINGLE_WRITE_RESPONSE && type != LineType.OutboundCommand)
            outboundResponseLines++;

        if (!text.endsWith("\n") && CodelessLibConfig.APPEND_END_OF_LINE && (CodelessLibConfig.END_OF_LINE_AFTER_COMMAND || type != LineType.OutboundCommand))
            text += "\n";
        if (!CodelessLibConfig.END_OF_LINE.equals("\n"))
            text = text.replace("\n", CodelessLibConfig.END_OF_LINE);

        byte[] data = text.getBytes(CodelessLibConfig.CHARSET);
        if (CodelessLibConfig.TRAILING_ZERO || !CodelessLibConfig.APPEND_END_OF_LINE || type == LineType.OutboundCommand && !CodelessLibConfig.END_OF_LINE_AFTER_COMMAND)
            data = Arrays.copyOf(data, data.length + 1);
        writeCharacteristic(codelessInbound, data);
    }

    /** Parses the text that was received from the peer device as response to the pending outgoing command. */
    private void parseCommandResponse(String line) {
        if (line.isEmpty()) {
            if (parsePending.isEmpty()) {
                if (CodelessLibConfig.CODELESS_LOG || CodelessLibConfig.LINE_EVENTS)
                    processCodelessLine(line, LineType.InboundEmpty);
            } else {
                parsePending.add(line);
            }
            return;
        }
        if (CodelessProfile.isSuccess(line)) {
            if (CodelessLibLog.CODELESS)
                Log.d(TAG, logPrefix + "Received OK");
            for (String response : parsePending) {
                if (response.isEmpty()) {
                    if (CodelessLibConfig.CODELESS_LOG || CodelessLibConfig.LINE_EVENTS)
                        processCodelessLine(response, LineType.InboundEmpty);
                    continue;
                }
                if (CodelessLibConfig.CODELESS_LOG || CodelessLibConfig.LINE_EVENTS)
                    processCodelessLine(response, LineType.InboundResponse);
                commandPending.parseResponse(response);
            }
            parsePending.clear();
            if (CodelessLibConfig.CODELESS_LOG || CodelessLibConfig.LINE_EVENTS)
                processCodelessLine(line, LineType.InboundOK);
            commandPending.onSuccess();
        } else if (CodelessProfile.isError(line)) {
            if (CodelessLibLog.CODELESS)
                Log.d(TAG, logPrefix + "Received ERROR");
            StringBuilder error = new StringBuilder();
            for (String msg : parsePending) {
                if (msg.isEmpty()) {
                    if (CodelessLibConfig.CODELESS_LOG || CodelessLibConfig.LINE_EVENTS)
                        processCodelessLine(msg, LineType.InboundEmpty);
                    continue;
                }
                if (CodelessLibConfig.CODELESS_LOG || CodelessLibConfig.LINE_EVENTS)
                    processCodelessLine(msg, LineType.InboundError);
                if (CodelessProfile.isPeerInvalidCommand(msg))
                    commandPending.setPeerInvalid();
                if (CodelessProfile.isErrorCodeMessage(msg)) {
                    CodelessProfile.ErrorCodeMessage ec = CodelessProfile.parseErrorCodeMessage(msg);
                    commandPending.setErrorCode(ec.code, ec.message);
                }
                if (error.length() > 0)
                    error.append("\n");
                error.append(msg);
            }
            parsePending.clear();
            if (CodelessLibConfig.CODELESS_LOG || CodelessLibConfig.LINE_EVENTS)
                processCodelessLine(line, LineType.InboundError);
            commandPending.onError(error.length() > 0 ? error.toString() : line);
        } else if (CodelessProfile.isErrorMessage(line)) {
            if (CodelessLibLog.CODELESS)
                Log.d(TAG, logPrefix + "Received potential error: " + line);
            parsePending.add(line);
        } else {
            if (CodelessLibLog.CODELESS)
                Log.d(TAG, logPrefix + "Received response: " + line);
            if (parsePending.isEmpty() && commandPending.parsePartialResponse()) {
                if (CodelessLibConfig.CODELESS_LOG || CodelessLibConfig.LINE_EVENTS)
                    processCodelessLine(line, LineType.InboundResponse);
                commandPending.parseResponse(line);
            } else {
                parsePending.add(line);
            }
        }
        if (commandPending != null && commandPending.isComplete())
            commandComplete(false);
    }

    /** Parses the text that was received from the peer device as an incoming command. */
    private void parseInboundCommand(String line) {
        if (CodelessLibLog.CODELESS)
            Log.d(TAG, logPrefix + "Received command: " + line);
        if (CodelessLibConfig.CODELESS_LOG || CodelessLibConfig.LINE_EVENTS)
            processCodelessLine(line, LineType.InboundCommand);

        if (commandInbound != null) {
            if (CodelessLibLog.CODELESS)
                Log.d(TAG, logPrefix + "Inbound command in progress. Ignore inbound data.");
            return;
        }

        CodelessCommand hostCommand = null;
        String id = CodelessProfile.getCommand(line);
        if (id == null) {
            if (CodelessLibConfig.HOST_INVALID_COMMANDS) {
                hostCommand = new CustomCommand(this, line, true);
            } else {
                sendParseError(CodelessProfile.INVALID_COMMAND);
            }
        } else {
            Class<? extends CodelessCommand> commandClass = CodelessProfile.Command.commandMap.get(id);
            if (commandClass == null) {
                if (CodelessLibConfig.HOST_UNSUPPORTED_COMMANDS) {
                    hostCommand = new CustomCommand(this, line, true);
                } else {
                    sendParseError(CodelessProfile.COMMAND_NOT_SUPPORTED);
                }
            } else {
                line = CodelessProfile.removeCommandPrefix(line);
                CodelessCommand command = CodelessProfile.createCommand(this, commandClass, line);
                if (CodelessLibConfig.hostCommands.contains(command.getCommandID())) {
                    hostCommand = command;
                } else if (CodelessLibConfig.supportedCommands.contains(command.getCommandID())) {
                    if (!checkCommandMode(false, command))
                        return;
                    if (CodelessLibLog.CODELESS)
                        Log.d(TAG, logPrefix + "Library command: " + command);
                    commandInbound = command;
                    commandInbound.setInbound();
                    EventBus.getDefault().post(new CodelessEvent.InboundCommand(commandInbound));
                    if (!command.isValid()) {
                        if (CodelessLibLog.CODELESS)
                            Log.d(TAG, logPrefix + "Invalid command: " + command + " " + command.getError());
                        commandInbound.setComplete();
                        sendError(CodelessProfile.ERROR_PREFIX + commandInbound.getError());
                    } else {
                        commandInbound.processInbound();
                    }
                } else {
                    sendParseError(CodelessProfile.COMMAND_NOT_SUPPORTED);
                }
            }
        }

        if (hostCommand != null) {
            if (!checkCommandMode(false, hostCommand))
                return;
            if (CodelessLibLog.CODELESS)
                Log.d(TAG, logPrefix + "Host command: " + hostCommand);
            commandInbound = hostCommand;
            commandInbound.setInbound();
            EventBus.getDefault().post(new CodelessEvent.HostCommand(commandInbound));
        }
    }

    /**
     * Actions performed when a Codeless Flow Control characteristic notification is received.
     * <p> The CodeLess Outbound characteristic is read to get the incoming data.
     */
    private void onCodelessFlowControl(byte[] data) {
        if (data.length > 0 && data[0] == CodelessProfile.CODELESS_DATA_PENDING) {
            inboundPending++;
            if (CodelessLibLog.CODELESS)
                Log.d(TAG, logPrefix + "Pending codeless inbound data: " + inboundPending);
            readCharacteristic(codelessOutbound);
        } else {
            Log.e(TAG, logPrefix + "Invalid codeless flow control value: " + CodelessUtil.hexArrayLog(data));
        }
    }

    /**
     * Actions performed when the Codeless Outbound characteristic is read successfully.
     * <p> The incoming data may be an incoming command or a response to an outgoing command.
     */
    private void onCodelessInbound(byte[] data) {
        if (CodelessLibLog.CODELESS)
            Log.d(TAG, logPrefix + "Codeless inbound data: " + CodelessUtil.hexArrayLog(data));

        // Remove trailing zero
        if (data.length > 0 && data[data.length - 1] == 0)
            data = Arrays.copyOf(data, data.length - 1);

        if (data.length == 0) {
            if (CodelessLibLog.CODELESS)
                Log.d(TAG, logPrefix + "Received empty buffer");
        }

        String inbound = new String(data, CodelessLibConfig.CHARSET);
        inbound = inbound.replace("\r\n", "\n");
        inbound = inbound.replace("\n\r", "\n");
        inbound = inbound.replace("\r", "\n");
        if (inbound.endsWith("\n"))
            inbound = inbound.substring(0, inbound.length() - 1);
        String[] lines = inbound.split("\n", -1);

        synchronized (this) {
            inboundPending--;

            for (String line : lines) {
                line = line.trim();
                if (commandPending != null) {
                    parseCommandResponse(line);
                } else {
                    if (line.isEmpty()) {
                        if (CodelessLibConfig.CODELESS_LOG || CodelessLibConfig.LINE_EVENTS)
                            processCodelessLine(line, LineType.InboundEmpty);
                        continue;
                    }
                    parseInboundCommand(line);
                }
            }

            if (commandPending == null || commandPending.isComplete())
                dequeueCommand();
        }
    }

    /** Returns the DSPS chunk size. */
    public int getDspsChunkSize() {
        return dspsChunkSize;
    }

    /**
     * Sets the DSPS chunk size.
     * <p> WARNING: The chunk size must not exceed the value (MTU - 3), otherwise chunks will be truncated when sent.
     * @param dspsChunkSize the chunk size
     */
    public void setDspsChunkSize(int dspsChunkSize) {
        this.dspsChunkSize = dspsChunkSize;
    }

    /**
     * Returns the DSPS echo configuration.
     * <p> If echo is enabled, all incoming binary data are sent back to the peer device.
     */
    public boolean getDspsEcho() {
        return dspsEcho;
    }

    /**
     * Sets the DSPS echo configuration.
     * @param dspsEcho <code>true</code> to enable echo, <code>false</code> to disabled it
     */
    public void setDspsEcho(boolean dspsEcho) {
        this.dspsEcho = dspsEcho;
    }

    /**
     * Sends text data to the peer device.
     * @param data the text data to send
     * @see #sendDspsData(byte[], int)
     */
    public void sendBinaryData(String data) {
        sendDspsData(data);
    }

    /**
     * Sends binary data to the peer device.
     * @param hex the binary data to send as a hex string
     * @see #sendDspsData(byte[], int)
     */
    public void sendHexData(String hex) {
        sendDspsHexData(hex);
    }

    /**
     * Sends binary data to the peer device.
     * @param data the binary data to send
     * @see #sendDspsData(byte[], int)
     */
    public void sendBinaryData(byte[] data) {
        sendDspsData(data, dspsChunkSize);
    }

    /**
     * Sends binary data to the peer device.
     * @param data      the binary data to send
     * @param chunkSize the chunk size to use when splitting the data
     * @see #sendDspsData(byte[], int)
     */
    public void sendBinaryData(byte[] data, int chunkSize) {
        sendDspsData(data, chunkSize);
    }

    /**
     * Sends text data to the peer device.
     * @param data the text data to send
     * @see #sendDspsData(byte[], int)
     */
    public void sendDspsData(String data) {
        if (CodelessLibLog.DSPS_DATA)
            Log.d(TAG, logPrefix + "DSPS TX text: " + data);
        sendDspsData(data.getBytes(CodelessLibConfig.CHARSET));
    }

    /**
     * Sends binary data to the peer device.
     * @param hex the binary data to send as a hex string
     * @see #sendDspsData(byte[], int)
     */
    public void sendDspsHexData(String hex) {
        if (CodelessLibLog.DSPS_DATA)
            Log.d(TAG, logPrefix + "DSPS TX hex: " + hex);
        byte[] data = CodelessUtil.hex2bytes(hex);
        if (data != null)
            sendDspsData(data);
        else
            Log.e(TAG, logPrefix + "Invalid hex data: " + hex);
    }

    /**
     * Sends binary data to the peer device.
     * @param data the binary data to send
     * @see #sendDspsData(byte[], int)
     */
    public void sendDspsData(byte[] data) {
        sendDspsData(data, dspsChunkSize);
    }

    /**
     * Sends binary data to the peer device.
     * <p>
     * If the data size is less than the chunk size, the data are sent in one write operation.
     * Otherwise they are split into chunks which are enqueued to be sent in multiple writes.
     * When TX flow control is off, the data are kept in a buffer to be sent when flow control
     * is set to on by the peer device.
     * <p>
     * WARNING: The chunk size must not exceed the value (MTU - 3), otherwise chunks will be truncated when sent.
     * @param data      the binary data to send
     * @param chunkSize the chunk size to use when splitting the data
     */
    public void sendDspsData(byte[] data, int chunkSize) {
        if (!checkReady() || !checkBinaryMode(true))
            return;
        if (CodelessLibLog.DSPS_DATA)
            Log.d(TAG, logPrefix + "DSPS TX data: " + CodelessUtil.hexArrayLog(data));
        if (chunkSize > dspsChunkSize)
            chunkSize = dspsChunkSize;
        if (data.length <= chunkSize) {
            if (dspsTxFlowOn) {
                enqueueGattOperation(new DspsChunkOperation(data));
            } else if (dspsPending.size() <= CodelessLibConfig.DSPS_PENDING_MAX_SIZE) {
                synchronized (this) {
                    dspsPending.add(new DspsChunkOperation(data));
                }
            } else {
                if (CodelessLibLog.DSPS)
                    Log.d(TAG, "DSPS TX data dropped (flow off, queue full)");
            }
        } else {
            ArrayList<GattOperation> chunks = new ArrayList<>();
            for (int i = 0; i < data.length; i += chunkSize) {
                chunks.add(new DspsChunkOperation(Arrays.copyOfRange(data, i, Math.min(i + chunkSize, data.length))));
            }
            if (dspsTxFlowOn) {
                enqueueGattOperations(chunks);
            } else if (dspsPending.size() <= CodelessLibConfig.DSPS_PENDING_MAX_SIZE) {
                synchronized (this) {
                    dspsPending.addAll(chunks);
                }
            } else {
                if (CodelessLibLog.DSPS)
                    Log.d(TAG, "DSPS TX data dropped (flow off, queue full)");
            }
        }
    }

    /**
     * Actions performed when binary (DSPS) data are received from the peer device.
     * <p> A {@link CodelessEvent.DspsRxData DspsRxData} event is generated.
     * @param data the received binary data
     */
    private void onDspsData(byte[] data) {
        if (CodelessLibLog.DSPS_DATA)
            Log.d(TAG, logPrefix + "DSPS RX data: " + CodelessUtil.hexArrayLog(data));
        if (!checkBinaryMode(false))
            return;
        if (dspsEcho)
            sendDspsData(data);
        if (dspsFileReceive != null)
            dspsFileReceive.onDspsData(data);
        if (CodelessLibConfig.DSPS_RX_LOG && (dspsFileReceive == null || CodelessLibConfig.DSPS_RX_FILE_LOG_DATA)) {
            if (CodelessLibConfig.DSPS_LOG_HANDLER)
                dspsLogHandler.post(() -> dspsRxLogFile.log(data));
            else
                dspsRxLogFile.log(data);
        }
        if (CodelessLibConfig.DSPS_STATS) {
            synchronized (this) {
                dspsRxBytesInterval += data.length;
            }
        }
        EventBus.getDefault().post(new CodelessEvent.DspsRxData(this, data));
    }

    /** Checks if the DSPS RX flow control in on. */
    public boolean isDspsRxFlowOn() {
        return dspsRxFlowOn;
    }

    /**
     * Sets the DSPS RX flow control configuration.
     * <p>
     * The appropriate value is written to the DSPS Flow Control characteristic.
     * A {@link CodelessEvent.DspsRxFlowControl DspsRxFlowControl} event is generated.
     * @param on <code>true</code> to set RX flow to on (allow incoming data), <code>false</code> to set it to off
     */
    public void setDspsRxFlowOn(boolean on) {
        dspsRxFlowOn = on;
        if (CodelessLibLog.DSPS)
            Log.d(TAG, logPrefix + "DSPS RX flow control: " + (dspsRxFlowOn ? "ON" : "OFF"));
        byte[] data = new byte[] { dspsRxFlowOn ? (byte) CodelessProfile.DSPS_XON : (byte) CodelessProfile.DSPS_XOFF };
        writeCharacteristic(dspsFlowControl, data, false);
        EventBus.getDefault().post(new CodelessEvent.DspsRxFlowControl(this, dspsRxFlowOn));
    }

    /**
     * Checks if the DSPS TX flow control in on.
     * <p>
     * When TX flow control is off, the library stops sending binary data to the peer device.
     * Any active file and periodic send operations are paused. Outgoing binary data that are
     * sent by the app at this time are kept in a buffer. When the peer device notifies that
     * it can receive data, by setting the TX flow control to on, all active operations are
     * resumed and any pending data in the buffer are sent.
     * <p>
     * NOTE: Any binary data that have already been passed to the Android BLE stack when TX
     * flow control is set to off will be sent. The library cannot control this behavior.
     */
    public boolean isDspsTxFlowOn() {
        return dspsTxFlowOn;
    }

    /**
     * Actions performed when a DSPS Flow Control characteristic notification is received.
     * <p> A {@link CodelessEvent.DspsTxFlowControl DspsTxFlowControl} event is generated.
     * @param data the notification data
     */
    synchronized private void onDspsFlowControl(byte[] data) {
        int value = data.length > 0 ? data[0] : Integer.MIN_VALUE;
        boolean prev = dspsTxFlowOn;
        switch (value) {
            case CodelessProfile.DSPS_XON:
                dspsTxFlowOn = true;
                break;
            case CodelessProfile.DSPS_XOFF:
                dspsTxFlowOn = false;
                break;
            default:
                Log.d(TAG, logPrefix + "Invalid DSPS TX flow control value: " + value);
                return;
        }

        if (prev == dspsTxFlowOn)
            return;

        if (CodelessLibLog.DSPS)
            Log.d(TAG, logPrefix + "DSPS TX flow control: " + (dspsTxFlowOn ? "ON" : "OFF"));
        EventBus.getDefault().post(new CodelessEvent.DspsTxFlowControl(this, dspsTxFlowOn));

        if (dspsTxFlowOn) {
            resumeDspsOperations();
        } else {
            pauseDspsOperations(true);
        }
    }

    /**
     * Creates and starts a DSPS file send operation.
     * @param file      the file to send
     * @param chunkSize the chunk size to use when splitting the file
     * @param period    the chunks enqueueing period (ms).
     *                  Set to 0 to enqueue all chunks (may be slower for large files).
     * @return the DSPS file send operation
     */
    public DspsFileSend sendFile(File file, int chunkSize, int period) {
        DspsFileSend operation = new DspsFileSend(this, file, chunkSize, period);
        if (operation.isLoaded())
            operation.start();
        return operation;
    }

    /**
     * Creates and starts a DSPS file send operation, using the current chunk size.
     * @param file      the file to send
     * @param period    the chunks enqueueing period (ms).
     *                  Set to 0 to enqueue all chunks (may be slower for large files).
     * @return the DSPS file send operation
     */
    public DspsFileSend sendFile(File file, int period) {
        return sendFile(file, dspsChunkSize, period);
    }

    /**
     * Creates and starts a DSPS file send operation, using the current chunk size.
     * <p> All chunks are enqueued at once (may be slower for large files).
     * @param file the file to send
     * @return the DSPS file send operation
     */
    public DspsFileSend sendFile(File file) {
        return sendFile(file, dspsChunkSize, 0);
    }

    /**
     * Creates and starts a DSPS file send operation.
     * @param uri       the file to send
     * @param chunkSize the chunk size to use when splitting the file
     * @param period    the chunks enqueueing period (ms).
     *                  Set to 0 to enqueue all chunks (may be slower for large files).
     * @return the DSPS file send operation
     */
    public DspsFileSend sendFile(Uri uri, int chunkSize, int period) {
        DspsFileSend operation = new DspsFileSend(this, uri, chunkSize, period);
        if (operation.isLoaded())
            operation.start();
        return operation;
    }

    /**
     * Creates and starts a DSPS file send operation, using the current chunk size.
     * @param uri       the file to send
     * @param period    the chunks enqueueing period (ms).
     *                  Set to 0 to enqueue all chunks (may be slower for large files).
     * @return the DSPS file send operation
     */
    public DspsFileSend sendFile(Uri uri, int period) {
        return sendFile(uri, dspsChunkSize, period);
    }

    /**
     * Creates and starts a DSPS file send operation, using the current chunk size.
     * <p> All chunks are enqueued at once (may be slower for large files).
     * @param uri the file to send
     * @return the DSPS file send operation
     */
    public DspsFileSend sendFile(Uri uri) {
        return sendFile(uri, dspsChunkSize, 0);
    }

    /**
     * Starts or resumes a DSPS file send operation.
     * <p> WARNING: For internal use only. Use one of the {@link #sendFile(Uri, int, int) sendFile} methods instead.
     */
    // INTERNAL
    synchronized public void start(DspsFileSend operation, boolean resume) {
        if (!checkReady() || !checkBinaryMode(true))
            return;
        if (!resume)
            dspsFiles.add(operation);
        if (!dspsTxFlowOn)
            return;
        if (operation.getPeriod() > 0) {
            handler.postDelayed(operation.getRunnable(), resume ? operation.getPeriod() : 0);
        } else {
            if (CodelessLibLog.DSPS_FILE_CHUNK)
                Log.d(TAG, logPrefix + "Queue all file chunks: " + operation);
            ArrayList<GattOperation> chunks = new ArrayList<>();
            for (int i = resume ? operation.getChunk() : 0; i < operation.getTotalChunks(); i++) {
                chunks.add(new DspsFileChunkOperation(operation, operation.getChunks()[i], i + 1));
            }
            enqueueGattOperations(chunks);
        }
    }

    /**
     * Stops a DSPS file send operation.
     * <p> WARNING: For internal use only. Use {@link DspsFileSend#stop()} instead.
     */
    // INTERNAL
    synchronized public void stop(final DspsFileSend operation) {
        dspsFiles.remove(operation);
        handler.postAtFrontOfQueue(new Runnable() {
            @Override
            public void run() {
                handler.removeCallbacks(operation.getRunnable());
                removePendingDspsFileChunkOperations(operation);
            }
        });
    }

    /**
     * Enqueues the next file chunk of a DSPS file send operation.
     * <p> WARNING: For internal use only.
     */
    // INTERNAL
    public void sendData(DspsFileSend operation) {
        enqueueGattOperation(new DspsFileChunkOperation(operation, operation.getCurrentChunk(), operation.getChunk() + 1));
    }

    /**
     * Creates and starts a DSPS periodic pattern send operation.
     * @param file      the file containing the pattern prefix
     * @param chunkSize the pattern packet size
     * @param period    the packet enqueueing period (ms)
     * @return the DSPS periodic send operation
     */
    public DspsPeriodicSend sendPattern(File file, int chunkSize, int period) {
        DspsPeriodicSend operation = new DspsPeriodicSend(this, file, chunkSize, period);
        if (operation.isLoaded())
            operation.start();
        return operation;
    }

    /**
     * Creates and starts a DSPS periodic pattern send operation, using the current chunk size.
     * @param file      the file containing the pattern prefix
     * @param period    the packet enqueueing period (ms)
     * @return the DSPS periodic send operation
     */
    public DspsPeriodicSend sendPattern(File file, int period) {
        return sendPattern(file, dspsChunkSize, period);
    }

    /**
     * Creates and starts a DSPS periodic pattern send operation, using the current chunk size.
     * @param file the file containing the pattern prefix
     * @return the DSPS periodic send operation
     */
    public DspsPeriodicSend sendPattern(File file) {
        return sendPattern(file, dspsChunkSize, 0);
    }

    /**
     * Creates and starts a DSPS periodic pattern send operation.
     * @param uri       the file URI containing the pattern prefix
     * @param chunkSize the pattern packet size
     * @param period    the packet enqueueing period (ms)
     * @return the DSPS periodic send operation
     */
    public DspsPeriodicSend sendPattern(Uri uri, int chunkSize, int period) {
        DspsPeriodicSend operation = new DspsPeriodicSend(this, uri, chunkSize, period);
        if (operation.isLoaded())
            operation.start();
        return operation;
    }

    /**
     * Creates and starts a DSPS periodic pattern send operation, using the current chunk size.
     * @param uri       the file URI containing the pattern prefix
     * @param period    the packet enqueueing period (ms)
     * @return the DSPS periodic send operation
     */
    public DspsPeriodicSend sendPattern(Uri uri, int period) {
        return sendPattern(uri, dspsChunkSize, period);
    }

    /**
     * Creates and starts a DSPS periodic pattern send operation, using the current chunk size.
     * @param uri the file URI containing the pattern prefix
     * @return the DSPS periodic send operation
     */
    public DspsPeriodicSend sendPattern(Uri uri) {
        return sendPattern(uri, dspsChunkSize, 0);
    }

    /**
     * Starts or resumes a DSPS periodic send operation.
     * <p> WARNING: For internal use only. Use one of the {@link #sendPattern(Uri, int, int) sendPattern} or {@link DspsPeriodicSend#start()} methods instead.
     */
    // INTERNAL
    synchronized public void start(DspsPeriodicSend operation) {
        if (!checkReady() || !checkBinaryMode(true))
            return;
        dspsPeriodic.add(operation);
        if (dspsTxFlowOn)
            handler.post(operation.getRunnable());
    }

    /**
     * Stops a DSPS periodic send operation.
     * <p> WARNING: For internal use only. Use {@link DspsPeriodicSend#stop()} instead.
     */
    // INTERNAL
    synchronized public void stop(final DspsPeriodicSend operation) {
        dspsPeriodic.remove(operation);
        handler.postAtFrontOfQueue(new Runnable() {
            @Override
            public void run() {
                handler.removeCallbacks(operation.getRunnable());
                removePendingDspsPeriodicChunkOperations(operation);
            }
        });
    }

    /**
     * Enqueues the next packet of a DSPS periodic send operation.
     * <p> WARNING: For internal use only.
     */
    // INTERNAL
    public void sendData(DspsPeriodicSend operation) {
        byte[] data = operation.getData();
        int chunkSize =  operation.getChunkSize();
        if (chunkSize > dspsChunkSize)
            chunkSize = dspsChunkSize;
        int totalChunks = data.length / chunkSize + (data.length % chunkSize != 0 ? 1 : 0);
        if (totalChunks == 1) {
            enqueueGattOperation(new DspsPeriodicChunkOperation(operation, operation.getCount(), operation.getData(), 1, 1));
        } else {
            ArrayList<GattOperation> chunks = new ArrayList<>();
            for (int i = 0; i < data.length; i += chunkSize) {
                chunks.add(new DspsPeriodicChunkOperation(operation, operation.getCount(), Arrays.copyOfRange(data, i, Math.min(i + chunkSize, data.length)), i / chunkSize + 1, totalChunks));
            }
            enqueueGattOperations(chunks);
        }
    }

    /**
     * Pauses any active DSPS send operations.
     * @param keepPending <code>true</code> to keep pending outgoing data in a buffer, <code>false</code> to discard them
     */
    private void pauseDspsOperations(boolean keepPending) {
        // Remove pending operations
        handler.removeCallbacks(resumeDspsOperations);
        handler.postAtFrontOfQueue(pauseDspsOperations);
        removePendingDspsChunkOperations(keepPending);
        if (!keepPending)
            dspsPending.clear();
    }

    private Runnable pauseDspsOperations = new Runnable() {
        @Override
        public void run() {
            synchronized (CodelessManager.this) {
                handler.removeCallbacks(pauseDspsOperations);
                for (DspsPeriodicSend operation : dspsPeriodic) {
                    handler.removeCallbacks(operation.getRunnable());
                    int count = removePendingDspsPeriodicChunkOperations(operation);
                    if (count > 0)
                        operation.setResumeCount(count);
                }
                for (DspsFileSend operation : dspsFiles) {
                    handler.removeCallbacks(operation.getRunnable());
                    int chunk = removePendingDspsFileChunkOperations(operation);
                    if (chunk > 0)
                        operation.setResumeChunk(chunk);
                }
            }
        }
    };

    /** Resumes any active DSPS send operations and sends any buffered data. */
    private void resumeDspsOperations() {
        // Send pending data
        enqueueGattOperations(dspsPending);
        dspsPending.clear();
        // Resume operations
        handler.post(resumeDspsOperations);
    }

    private Runnable resumeDspsOperations = new Runnable() {
        @Override
        public void run() {
            synchronized (CodelessManager.this) {
                for (DspsPeriodicSend operation : dspsPeriodic) {
                    handler.postDelayed(operation.getRunnable(), operation.getPeriod());
                }
                for (DspsFileSend operation : dspsFiles) {
                    start(operation, true);
                }
            }
        }
    };

    /**
     * Starts a DSPS file receive operation.
     * <p> WARNING: For internal use only. Use {@link #receiveFile()} instead.
     */
    // INTERNAL
    synchronized public void start(DspsFileReceive operation) {
        if (!checkReady() || !checkBinaryMode(true))
            return;
        if (dspsFileReceive != null)
            dspsFileReceive.stop();
        dspsFileReceive = operation;
    }

    /**
     * Stops a DSPS file receive operation.
     * <p> WARNING: For internal use only. Use {@link DspsFileReceive#stop()} instead.
     */
    // INTERNAL
    synchronized public void stop(DspsFileReceive operation) {
        if (dspsFileReceive == operation)
            dspsFileReceive = null;
    }

    /**
     * Creates and starts a DSPS file receive operation.
     * <p> Only a single file receive operation can be active.
     * @return the DSPS file receive operation
     */
    public DspsFileReceive receiveFile() {
        DspsFileReceive operation = new DspsFileReceive(this);
        operation.start();
        return operation;
    }

    /** Returns the active DSPS file receive operation, if available. */
    public DspsFileReceive getDspsFileReceive() {
        return dspsFileReceive;
    }

    /**
     * Returns the background handler used for writing data to the DSPS log file.
     * <p> Available if {@link CodelessLibConfig#DSPS_LOG_HANDLER} is enabled.
     */
    public Handler getDspsLogHandler() {
        return dspsLogHandler;
    }

    /**
     * Returns the background handler used for statistics calculations.
     * <p> Available if {@link CodelessLibConfig#DSPS_STATS statistics} are enabled.
     */
    public Handler getDspsStatsHandler() {
        return dspsStatsHandler;
    }

    /**
     * Returns the calculated current receive speed.
     * <p> Available only if {@link CodelessLibConfig#DSPS_STATS statistics} are enabled.
     */
    public int getDspsRxSpeed() {
        return dspsRxSpeed;
    }

    /**
     * Performs statistics calculations, called every {@link CodelessLibConfig#DSPS_STATS_INTERVAL}.
     * <p> A {@link CodelessEvent.DspsStats DspsStats} event is generated.
     */
    private Runnable dspsUpdateStats = new Runnable() {
        @Override
        public void run() {
            if (commandMode)
                return;
            synchronized (CodelessManager.this) {
                long now = new Date().getTime();
                if (now == dspsLastInterval)
                    now++;
                dspsRxSpeed = (int) (dspsRxBytesInterval * 1000L / (now - dspsLastInterval));
                dspsLastInterval = now;
                dspsRxBytesInterval = 0;
                dspsStatsHandler.postDelayed(this, CodelessLibConfig.DSPS_STATS_INTERVAL);
                EventBus.getDefault().post(new CodelessEvent.DspsStats(CodelessManager.this, null, dspsRxSpeed, SPEED_INVALID));
            }
        }
    };

    /**
     * BluetoothGatt {@link BluetoothGattCallback#onConnectionStateChange(BluetoothGatt, int, int) onConnectionStateChange} implementation.
     * <p>
     * A {@link CodelessEvent.Connection Connection} event is generated.
     * After connection, a service discovery is started automatically and a {@link CodelessEvent.ServiceDiscovery ServiceDiscovery} event is generated.
     */
    @SuppressLint("MissingPermission")
    synchronized private void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        this.gatt = gatt;
        if (newState == BluetoothProfile.STATE_CONNECTED) {
            Log.d(TAG, logPrefix + "Connected");
            state = CONNECTED;
            EventBus.getDefault().post(new CodelessEvent.Connection(this));
            Log.d(TAG, logPrefix + "Discover services");
            state = SERVICE_DISCOVERY;
            EventBus.getDefault().post(new CodelessEvent.ServiceDiscovery(this, false));
            gatt.discoverServices();
            initialize();
        } else {
            Log.d(TAG, logPrefix + "Disconnected: status=" + status);
            state = DISCONNECTED;
            gatt.close();
            this.gatt = null;
            if (CodelessLibConfig.BLUETOOTH_STATE_MONITOR)
                context.unregisterReceiver(bluetoothStateReceiver);
            reset();
            EventBus.getDefault().post(new CodelessEvent.Connection(this));
        }
    }

    /** Initializes the manager when the peer device is connected. */
    private void initialize() {
        HandlerThread handlerThread = new HandlerThread("CodelessManager");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());

        if (CodelessLibConfig.DSPS_STATS) {
            handlerThread = new HandlerThread("CodelessDspsStats");
            handlerThread.start();
            dspsStatsHandler = new Handler(handlerThread.getLooper());
        }

        if (CodelessLibConfig.DSPS_LOG_HANDLER) {
            handlerThread = new HandlerThread("CodelessDspsLog");
            handlerThread.start();
            dspsLogHandler = new Handler(handlerThread.getLooper());
        }

        if (CodelessLibConfig.CODELESS_LOG)
            codelessLogFile = new CodelessLogFile(this);
        if (CodelessLibConfig.DSPS_RX_LOG)
            dspsRxLogFile = new DspsRxLogFile(this);
    }

    /** Resets the manager when the peer device is disconnected. */
    private void reset() {
        mtu = CodelessProfile.MTU_DEFAULT;

        dspsPending.clear();
        for (DspsPeriodicSend operation : new ArrayList<>(dspsPeriodic))
            operation.stop();
        for (DspsFileSend operation : new ArrayList<>(dspsFiles))
            operation.stop();
        if (dspsFileReceive != null)
            dspsFileReceive.stop();

        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
            handler.getLooper().quit();
        }

        if (CodelessLibConfig.DSPS_STATS && dspsStatsHandler != null) {
            dspsStatsHandler.removeCallbacksAndMessages(null);
            dspsStatsHandler.getLooper().quit();
        }

        if (CodelessLibConfig.CODELESS_LOG && codelessLogFile != null)
            codelessLogFile.close();
        if (CodelessLibConfig.DSPS_RX_LOG && dspsRxLogFile != null) {
            if (CodelessLibConfig.DSPS_LOG_HANDLER)
                dspsLogHandler.post(() -> dspsRxLogFile.close());
            else
                dspsRxLogFile.close();
        }

        if (CodelessLibConfig.DSPS_LOG_HANDLER && dspsLogHandler != null)
            dspsLogHandler.getLooper().quitSafely();

        gattOperationPending = null;
        gattQueue.clear();

        commandMode = false;
        binaryRequestPending = false;
        binaryExitRequestPending = false;

        commandQueue.clear();
        commandPending = null;
        commandInbound = null;
        inboundPending = 0;
        outboundResponseLines = 0;
        parsePending.clear();

        dspsRxFlowOn = CodelessLibConfig.DEFAULT_DSPS_RX_FLOW_CONTROL;
        dspsTxFlowOn = CodelessLibConfig.DEFAULT_DSPS_TX_FLOW_CONTROL;

        servicesDiscovered = false;
        codelessSupport = false;
        dspsSupport = false;
        codelessService = null;
        codelessInbound = null;
        codelessOutbound = null;
        codelessFlowControl = null;
        dspsService = null;
        dspsServerTx = null;
        dspsServerRx = null;
        dspsFlowControl = null;
        deviceInfoService = null;
    }

    /**
     * BluetoothGatt {@link BluetoothGattCallback#onServicesDiscovered(BluetoothGatt, int) onServicesDiscovered} implementation.
     * <p>
     * A {@link CodelessEvent.ServiceDiscovery ServiceDiscovery} event is generated.
     * It initializes the CodeLess and DSPS services and enables notifications.
     * If {@link CodelessLibConfig#REQUEST_MTU} is enabled, a {@link CodelessLibConfig#MTU MTU} request is initiated.
     */
    @SuppressLint("MissingPermission")
    private void onServicesDiscovered(BluetoothGatt gatt, int status) {
        Log.d(TAG, logPrefix + "Services discovered: status=" + status);

        BluetoothGattDescriptor codelessFlowControlClientConfig = null;
        BluetoothGattDescriptor dspsServerTxClientConfig = null;
        BluetoothGattDescriptor dspsFlowControlClientConfig = null;
        pendingEnableNotifications = new ArrayList<>();

        deviceInfoService = gatt.getService(Uuid.DEVICE_INFORMATION_SERVICE);

        codelessService = gatt.getService(Uuid.CODELESS_SERVICE_UUID);
        Log.d(TAG, logPrefix + "Codeless service " + (codelessService != null ? "found" : "not found"));
        if (codelessService != null) {
            codelessInbound = codelessService.getCharacteristic(Uuid.CODELESS_INBOUND_COMMAND_UUID);
            if (codelessInbound == null)
                Log.e(TAG, logPrefix + "Missing codeless inbound characteristic " + Uuid.CODELESS_INBOUND_COMMAND_UUID);

            codelessOutbound = codelessService.getCharacteristic(Uuid.CODELESS_OUTBOUND_COMMAND_UUID);
            if (codelessOutbound == null)
                Log.e(TAG, logPrefix + "Missing codeless outbound characteristic " + Uuid.CODELESS_OUTBOUND_COMMAND_UUID);

            codelessFlowControl = codelessService.getCharacteristic(Uuid.CODELESS_FLOW_CONTROL_UUID);
            if (codelessFlowControl == null) {
                Log.e(TAG, logPrefix + "Missing codeless flow control characteristic " + Uuid.CODELESS_FLOW_CONTROL_UUID);
            } else {
                codelessFlowControlClientConfig = codelessFlowControl.getDescriptor(Uuid.CLIENT_CONFIG_DESCRIPTOR);
                if (codelessFlowControlClientConfig == null)
                    Log.e(TAG, logPrefix + "Missing codeless flow control characteristic client configuration");
            }
        }
        codelessSupport = codelessService != null && codelessInbound != null && codelessOutbound != null && codelessFlowControl != null && codelessFlowControlClientConfig != null;
        if (codelessSupport)
            pendingEnableNotifications.add(codelessFlowControl);

        dspsService = gatt.getService(Uuid.DSPS_SERVICE_UUID);
        Log.d(TAG, logPrefix + "DSPS service " + (dspsService != null ? "found" : "not found"));
        if (dspsService != null) {
            dspsServerTx = dspsService.getCharacteristic(Uuid.DSPS_SERVER_TX_UUID);
            if (dspsServerTx == null) {
                Log.e(TAG, logPrefix + "Missing DSPS server TX characteristic " + Uuid.DSPS_SERVER_TX_UUID);
            } else {
                dspsServerTxClientConfig = dspsServerTx.getDescriptor(Uuid.CLIENT_CONFIG_DESCRIPTOR);
                if (dspsServerTxClientConfig == null)
                    Log.e(TAG, logPrefix + "Missing DSPS server TX characteristic client configuration");
            }

            dspsServerRx = dspsService.getCharacteristic(Uuid.DSPS_SERVER_RX_UUID);
            if (dspsServerRx == null)
                Log.e(TAG, logPrefix + "Missing DSPS server RX characteristic " + Uuid.DSPS_SERVER_RX_UUID);

            dspsFlowControl = dspsService.getCharacteristic(Uuid.DSPS_FLOW_CONTROL_UUID);
            if (dspsFlowControl == null) {
                Log.e(TAG, logPrefix + "Missing DSPS flow control characteristic " + Uuid.DSPS_FLOW_CONTROL_UUID);
            } else {
                dspsFlowControlClientConfig = dspsFlowControl.getDescriptor(Uuid.CLIENT_CONFIG_DESCRIPTOR);
                if (dspsFlowControlClientConfig == null)
                    Log.e(TAG, logPrefix + "Missing DSPS flow control characteristic client configuration");
            }
        }
        dspsSupport = dspsService != null && dspsServerTx != null && dspsServerRx != null && dspsFlowControl != null && dspsServerTxClientConfig != null && dspsFlowControlClientConfig != null;
        if (dspsSupport) {
            pendingEnableNotifications.add(dspsServerTx);
            pendingEnableNotifications.add(dspsFlowControl);
        }

        servicesDiscovered = true;
        state = CONNECTED;
        EventBus.getDefault().post(new CodelessEvent.ServiceDiscovery(this, true));

        if (CodelessLibConfig.REQUEST_MTU && (codelessSupport || dspsSupport)) {
            if (CodelessLibConfig.MTU == CodelessProfile.MTU_DEFAULT)
                dspsChunkSize = CodelessProfile.MTU_DEFAULT - 3;
            else if (mtu == CodelessProfile.MTU_DEFAULT)
                requestMtu(CodelessLibConfig.MTU);
        }

        if (!pendingEnableNotifications.isEmpty()) {
            if (!CodelessLibConfig.DELAY_INIT_IF_BONDING || device.getBondState() != BluetoothDevice.BOND_BONDING) {
                for (BluetoothGattCharacteristic characteristic : pendingEnableNotifications) {
                    enableNotifications(characteristic);
                }
            } else {
                // Gatt operations may fail if pairing is in progress
                Log.d(TAG, logPrefix + "Pairing in progress");
                BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        if (!BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(intent.getAction()))
                            return;
                        if (!device.equals(intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)))
                            return;
                        int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, 0);
                        Log.d(TAG, logPrefix + "Pairing complete: " + state);
                        if (isConnected() && (state == BluetoothDevice.BOND_BONDED || state == BluetoothDevice.BOND_NONE)) {
                            for (BluetoothGattCharacteristic characteristic : pendingEnableNotifications) {
                                enableNotifications(characteristic);
                            }
                        }
                        context.unregisterReceiver(this);
                    }
                };
                context.registerReceiver(broadcastReceiver, new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED));
            }
        } else {
            pendingEnableNotifications = null;
        }
    }

    /** Enqueues a read characteristic operation in the GATT operation queue. */
    private void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        enqueueGattOperation(new GattOperation(characteristic));
    }

    /** Executes a read characteristic operation. */
    @SuppressLint("MissingPermission")
    private void executeReadCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (CodelessLibLog.GATT_OPERATION)
            Log.d(TAG, logPrefix + "Read characteristic: " + characteristic.getUuid());
        if (!gatt.readCharacteristic(characteristic)) {
            Log.e(TAG, logPrefix + "Error reading characteristic: " + characteristic.getUuid());
            EventBus.getDefault().post(new CodelessEvent.Error(this, ERROR_GATT_OPERATION));
            dequeueGattOperation();
        }
    }

    /** BluetoothGatt {@link BluetoothGattCallback#onCharacteristicRead(BluetoothGatt, BluetoothGattCharacteristic, int) onCharacteristicRead} implementation. */
    private void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        onCharacteristicRead(gatt, characteristic, characteristic.getValue(), status);
    }

    /** BluetoothGatt {@link BluetoothGattCallback#onCharacteristicRead(BluetoothGatt, BluetoothGattCharacteristic, byte[], int) onCharacteristicRead} implementation. */
    private void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, byte[] value, int status) {
        if (CodelessLibLog.GATT_OPERATION)
            Log.d(TAG, logPrefix + "onCharacteristicRead: " + status + " " + characteristic.getUuid() + " " + CodelessUtil.hexArrayLog(value));
        if (CodelessLibConfig.GATT_DEQUEUE_BEFORE_PROCESSING)
            dequeueGattOperation();

        if (status == BluetoothGatt.GATT_SUCCESS) {
            if (characteristic.equals(codelessOutbound)) {
                onCodelessInbound(value);
            } else if (characteristic.getService().equals(deviceInfoService)) {
                onDeviceInfoRead(characteristic, value);
            } else if (characteristic.getUuid().equals(Uuid.GAP_DEVICE_NAME)) {
                onDeviceNameRead(value);
            }
        } else {
            Log.e(TAG, logPrefix + "Failed to read characteristic: " + characteristic.getUuid());
            EventBus.getDefault().post(new CodelessEvent.Error(this, ERROR_GATT_OPERATION));
        }

        if (!CodelessLibConfig.GATT_DEQUEUE_BEFORE_PROCESSING)
            dequeueGattOperation();
    }

    /** Enqueues a write characteristic operation in the GATT operation queue. */
    private void writeCharacteristic(BluetoothGattCharacteristic characteristic, byte[] value) {
        enqueueGattOperation(new GattOperation(characteristic, value));
    }

    /**
     * Enqueues a write characteristic operation in the GATT operation queue.
     * @param response <code>true</code> for write with response, <code>false</code> for write command
     */
    private void writeCharacteristic(BluetoothGattCharacteristic characteristic, byte[] value, boolean response) {
        enqueueGattOperation(new GattOperation(characteristic, value, response));
    }

    /** Executes a write characteristic operation. */
    @SuppressLint("MissingPermission")
    private void executeWriteCharacteristic(BluetoothGattCharacteristic characteristic, byte[] value, boolean response) {
        if (CodelessLibLog.GATT_OPERATION)
            Log.d(TAG, logPrefix + "Write characteristic" + (!response ? " (no response): " : ": ") + characteristic.getUuid() + " " + CodelessUtil.hexArrayLog(value));
        int type = response ? BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT : BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE;
        characteristic.setWriteType(type);
        characteristic.setValue(value);
        boolean success;
        if (Build.VERSION.SDK_INT >= 33) {
            success = gatt.writeCharacteristic(characteristic, value, type) == BluetoothStatusCodes.SUCCESS;
        } else {
            success = gatt.writeCharacteristic(characteristic);
        }
        if (!success) {
            Log.e(TAG, logPrefix + "Error writing characteristic: " + characteristic.getUuid());
            EventBus.getDefault().post(new CodelessEvent.Error(this, ERROR_GATT_OPERATION));
            onCharacteristicWriteError(characteristic);
            dequeueGattOperation();
        }
    }

    /** BluetoothGatt {@link BluetoothGattCallback#onCharacteristicWrite(BluetoothGatt, BluetoothGattCharacteristic, int) onCharacteristicWrite} implementation. */
    private void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        if (CodelessLibLog.GATT_OPERATION)
            Log.d(TAG, logPrefix + "onCharacteristicWrite: " + status + " " + characteristic.getUuid());
        if (CodelessLibConfig.GATT_DEQUEUE_BEFORE_PROCESSING)
            dequeueGattOperation();

        if (status != BluetoothGatt.GATT_SUCCESS) {
            Log.e(TAG, logPrefix + "Failed to write characteristic: " + characteristic.getUuid());
            EventBus.getDefault().post(new CodelessEvent.Error(this, ERROR_GATT_OPERATION));
            onCharacteristicWriteError(characteristic);
        }

        if (!CodelessLibConfig.GATT_DEQUEUE_BEFORE_PROCESSING)
            dequeueGattOperation();
    }

    /** Actions performed when a write characteristic operation fails. */
    private void onCharacteristicWriteError(BluetoothGattCharacteristic characteristic) {
        if (characteristic.equals(codelessInbound)) {
            if (commandPending != null) {
                commandPending.onError(CodelessProfile.GATT_OPERATION_ERROR);
                commandComplete(true);
            } else if (commandInbound != null) {
                commandInbound.setComplete();
                inboundCommandComplete();
            }
        }
    }

    /**
     * BluetoothGatt {@link BluetoothGattCallback#onCharacteristicChanged(BluetoothGatt, BluetoothGattCharacteristic) onCharacteristicChanged} implementation.
     * <p> Actions performed when a characteristic notification is received.
     */
    private void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        onCharacteristicChanged(gatt, characteristic, characteristic.getValue());
    }

    /**
     * BluetoothGatt {@link BluetoothGattCallback#onCharacteristicChanged(BluetoothGatt, BluetoothGattCharacteristic, byte[] value) onCharacteristicChanged} implementation.
     * <p> Actions performed when a characteristic notification is received.
     */
    private void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, byte[] value) {
        if (CodelessLibLog.GATT_OPERATION)
            Log.d(TAG, logPrefix + "onCharacteristicChanged: " + characteristic.getUuid() + " " + CodelessUtil.hexArrayLog(value));
        if (characteristic.equals(codelessFlowControl)) {
            onCodelessFlowControl(value);
        } else if (characteristic.equals(dspsServerTx)) {
            onDspsData(value);
        } else if (characteristic.equals(dspsFlowControl)){
            onDspsFlowControl(value);
        }
    }

    /** Enqueues a read descriptor operation in the GATT operation queue. */
    private void readDescriptor(BluetoothGattDescriptor descriptor) {
        enqueueGattOperation(new GattOperation(descriptor));
    }

    /** Executes a read descriptor operation. */
    @SuppressLint("MissingPermission")
    private void executeReadDescriptor(BluetoothGattDescriptor descriptor) {
        BluetoothGattCharacteristic characteristic = descriptor.getCharacteristic();
        if (CodelessLibLog.GATT_OPERATION)
            Log.d(TAG, logPrefix + "Read descriptor: " + characteristic.getUuid() + " " + descriptor.getUuid());
        if (!gatt.readDescriptor(descriptor)) {
            Log.e(TAG, logPrefix + "Error reading descriptor: " + characteristic.getUuid() + " " + descriptor.getUuid());
            EventBus.getDefault().post(new CodelessEvent.Error(this, ERROR_GATT_OPERATION));
            dequeueGattOperation();
        }
    }

    /** BluetoothGatt {@link BluetoothGattCallback#onDescriptorRead(BluetoothGatt, BluetoothGattDescriptor, int) onDescriptorRead} implementation. */
    private void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        onDescriptorRead(gatt, descriptor, status, descriptor.getValue());
    }

    /** BluetoothGatt {@link BluetoothGattCallback#onDescriptorRead(BluetoothGatt, BluetoothGattDescriptor, int, byte[]) onDescriptorRead} implementation. */
    private void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status, byte[] value) {
        BluetoothGattCharacteristic characteristic = descriptor.getCharacteristic();
        if (CodelessLibLog.GATT_OPERATION)
            Log.d(TAG, logPrefix + "onDescriptorRead: " + status + " " + characteristic.getUuid() + " " + descriptor.getUuid() + " " + CodelessUtil.hexArrayLog(value));
        if (CodelessLibConfig.GATT_DEQUEUE_BEFORE_PROCESSING)
            dequeueGattOperation();

        if (status != BluetoothGatt.GATT_SUCCESS) {
            Log.e(TAG, logPrefix + "Failed to read descriptor: " + characteristic.getUuid() + " " + descriptor.getUuid());
            EventBus.getDefault().post(new CodelessEvent.Error(this, ERROR_GATT_OPERATION));
        }

        if (!CodelessLibConfig.GATT_DEQUEUE_BEFORE_PROCESSING)
            dequeueGattOperation();
    }

    /** Enqueues a write descriptor operation in the GATT operation queue. */
    private void writeDescriptor(BluetoothGattDescriptor descriptor, byte[] value) {
        enqueueGattOperation(new GattOperation(descriptor, value));
    }

    /** Executes a write descriptor operation. */
    @SuppressLint("MissingPermission")
    private void executeWriteDescriptor(BluetoothGattDescriptor descriptor, byte[] value) {
        BluetoothGattCharacteristic characteristic = descriptor.getCharacteristic();
        if (CodelessLibLog.GATT_OPERATION)
            Log.d(TAG, logPrefix + "Write descriptor: " + characteristic.getUuid() + " " + descriptor.getUuid() + " " + CodelessUtil.hexArrayLog(value));
        descriptor.setValue(value);
        boolean success;
        if (Build.VERSION.SDK_INT >= 33) {
            success = gatt.writeDescriptor(descriptor, value) == BluetoothStatusCodes.SUCCESS;
        } else {
            success = gatt.writeDescriptor(descriptor);
        }
        if (!success) {
            Log.e(TAG, logPrefix + "Error writing descriptor: " + characteristic.getUuid() + " " + descriptor.getUuid());
            EventBus.getDefault().post(new CodelessEvent.Error(this, ERROR_GATT_OPERATION));
            if (pendingEnableNotifications.contains(descriptor.getCharacteristic()))
                EventBus.getDefault().post(new CodelessEvent.Error(this, ERROR_INIT_SERVICES));
            dequeueGattOperation();
        }
    }

    /**
     * BluetoothGatt {@link BluetoothGattCallback#onDescriptorWrite(BluetoothGatt, BluetoothGattDescriptor, int) onDescriptorWrite} implementation.
     * <p> A {@link CodelessEvent.Ready Ready} event is generated after all required notifications are enabled.
     */
    private void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        BluetoothGattCharacteristic characteristic = descriptor.getCharacteristic();
        if (CodelessLibLog.GATT_OPERATION)
            Log.d(TAG, logPrefix + "onDescriptorWrite: " + status + " " + characteristic.getUuid() + " " + descriptor.getUuid());
        if (CodelessLibConfig.GATT_DEQUEUE_BEFORE_PROCESSING)
            dequeueGattOperation();

        if (status == BluetoothGatt.GATT_SUCCESS) {
            if (pendingEnableNotifications != null) {
                pendingEnableNotifications.remove(descriptor.getCharacteristic());
                if (pendingEnableNotifications.isEmpty()) {
                    pendingEnableNotifications = null;
                    onDeviceReady();
                }
            }
        } else {
            Log.e(TAG, logPrefix + "Failed to write descriptor:" + characteristic.getUuid() + " " + descriptor.getUuid());
            EventBus.getDefault().post(new CodelessEvent.Error(this, ERROR_GATT_OPERATION));
            if (pendingEnableNotifications.contains(descriptor.getCharacteristic()))
                EventBus.getDefault().post(new CodelessEvent.Error(this, ERROR_INIT_SERVICES));
        }

        if (!CodelessLibConfig.GATT_DEQUEUE_BEFORE_PROCESSING)
            dequeueGattOperation();
    }

    /** Initiates a write descriptor operation to enable notifications for a characteristic. */
    @SuppressLint("MissingPermission")
    private void enableNotifications(BluetoothGattCharacteristic characteristic) {
        BluetoothGattDescriptor ccc = characteristic.getDescriptor(Uuid.CLIENT_CONFIG_DESCRIPTOR);
        if (ccc == null) {
            Log.e(TAG, logPrefix + "Missing client configuration descriptor: " + characteristic.getUuid());
            return;
        }
        boolean notify = (characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0;
        byte[] value = notify ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE : BluetoothGattDescriptor.ENABLE_INDICATION_VALUE;
        Log.d(TAG, logPrefix + "Enable " + (notify ? "notifications" : "indications") + ": " + characteristic.getUuid());
        gatt.setCharacteristicNotification(characteristic, true);
        writeDescriptor(ccc, value);
    }

    /** Executes a MTU request operation. */
    @SuppressLint("MissingPermission")
    private void executeMtuRequest(int mtu) {
        if (Build.VERSION.SDK_INT >= 21) {
            Log.d(TAG, logPrefix + "MTU request: " + mtu);
            if (!gatt.requestMtu(mtu)) {
                Log.e(TAG, logPrefix + "MTU request error");
                dequeueGattOperation();
            }
        }
    }

    /**
     * BluetoothGatt {@link BluetoothGattCallback#onMtuChanged(BluetoothGatt, int, int) onMtuChanged} implementation.
     * <p> The {@link #setDspsChunkSize(int) DSPS chunk size} may be updated based on the MTU to avoid data loss.
     */
    private void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
        Log.d(TAG, logPrefix + "onMtuChanged: " + status);
        if (status == BluetoothGatt.GATT_SUCCESS) {
            Log.d(TAG, logPrefix + "MTU changed to " + mtu);
            this.mtu = mtu;
            if (CodelessLibConfig.DSPS_CHUNK_SIZE_INCREASE_TO_MTU || dspsChunkSize > mtu - 3)
                dspsChunkSize = mtu - 3;
        } else {
            Log.e(TAG, logPrefix + "Failed to change MTU");
        }
        if (gattOperationPending != null && gattOperationPending.getType() == GattOperation.Type.MtuRequest)
            dequeueGattOperation();
    }

    /**
     * BluetoothGatt {@link BluetoothGattCallback#onReadRemoteRssi(BluetoothGatt, int, int) onReadRemoteRssi} implementation.
     * <p> A {@link CodelessEvent.Rssi Rssi} event is generated.
     */
    private void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            if (CodelessLibLog.GATT_OPERATION)
                Log.d(TAG, logPrefix + "RSSI: " + rssi);
            EventBus.getDefault().post(new CodelessEvent.Rssi(this, rssi));
        } else {
            Log.e(TAG, logPrefix + "Failed to read RSSI");
        }
    }

    /** {@link BluetoothGattCallback} implementation. */
    private BluetoothGattCallback gattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            CodelessManager.this.onConnectionStateChange(gatt, status, newState);
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            CodelessManager.this.onServicesDiscovered(gatt, status);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            CodelessManager.this.onCharacteristicRead(gatt, characteristic, status);
        }

        @Override
        public void onCharacteristicRead(@NonNull BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic, @NonNull byte[] value, int status) {
            CodelessManager.this.onCharacteristicRead(gatt, characteristic, value, status);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            CodelessManager.this.onCharacteristicWrite(gatt, characteristic, status);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            CodelessManager.this.onCharacteristicChanged(gatt, characteristic);
        }

        @Override
        public void onCharacteristicChanged(@NonNull BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic, @NonNull byte[] value) {
            CodelessManager.this.onCharacteristicChanged(gatt, characteristic, value);
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            CodelessManager.this.onDescriptorRead(gatt, descriptor, status);
        }

        @Override
        public void onDescriptorRead(@NonNull BluetoothGatt gatt, @NonNull BluetoothGattDescriptor descriptor, int status, @NonNull byte[] value) {
            CodelessManager.this.onDescriptorRead(gatt, descriptor, status, value);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            CodelessManager.this.onDescriptorWrite(gatt, descriptor, status);
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            CodelessManager.this.onReadRemoteRssi(gatt, rssi, status);
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            CodelessManager.this.onMtuChanged(gatt, mtu, status);
        }
    };

    /** Broadcast receiver that handles Bluetooth state changes. */
    private BroadcastReceiver bluetoothStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
                int prev = intent.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_STATE, -1);
                Log.d(TAG, "Bluetooth state changed: " + prev + " -> " + state);
                if (state == BluetoothAdapter.STATE_TURNING_OFF)
                    disconnect();
            }
        }
    };

    /** GATT operation wrapper class, used for the GATT operation queue implementation. */
    private static class GattOperation {

        /** GATT operation type enumeration. */
        public enum Type {
            ReadCharacteristic,
            WriteCharacteristic,
            WriteCommand,
            ReadDescriptor,
            WriteDescriptor,
            MtuRequest
        }

        private Type type;
        private Object gattObject;
        private byte[] value;

        /** Read characteristic/descriptor operation. */
        public GattOperation(Object gattObject) {
            this.gattObject = gattObject;
            type = gattObject instanceof BluetoothGattCharacteristic ? Type.ReadCharacteristic : Type.ReadDescriptor;
        }

        /** Write characteristic/descriptor operation. */
        public GattOperation(Object gattObject, byte[] value) {
            this.gattObject = gattObject;
            type = gattObject instanceof BluetoothGattCharacteristic ? Type.WriteCharacteristic : Type.WriteDescriptor;
            this.value = value.clone();
        }

        /** Write with response or write command operation. */
        public GattOperation(BluetoothGattCharacteristic gattObject, byte[] value, boolean response) {
            this.gattObject = gattObject;
            type = response ? Type.WriteCharacteristic : Type.WriteCommand;
            this.value = value.clone();
        }

        /** Request MTU operation. */
        public GattOperation(int mtu) {
            type = Type.MtuRequest;
            value = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort((short)mtu).array();
        }

        /** Returns the operation type. */
        public Type getType() {
            return type;
        }

        /** Returns the associated GATT object. */
        public Object getGattObject() {
            return gattObject;
        }

        /** Returns the associated characteristic. */
        public BluetoothGattCharacteristic getCharacteristic() {
            return (BluetoothGattCharacteristic) gattObject;
        }

        /** Returns the associated descriptor. */
        public BluetoothGattDescriptor getDescriptor() {
            return (BluetoothGattDescriptor) gattObject;
        }

        /** Returns the value to be used by the operation. */
        public byte[] getValue() {
            return value;
        }

        /**
         * Checks if the operation is low priority.
         * <p> High priority operations are put before low priority ones in the queue.
         */
        public boolean lowPriority() {
            return false;
        }

        /** Called just before the operation is executed. */
        protected void onExecute() {
        }
    }

    /** Checks if a GATT operation is pending. */
    public boolean isGattOperationPending() {
        return gattOperationPending != null;
    }

    /** Returns the pending GATT operation. */
    public GattOperation getGattOperationPending() {
        return gattOperationPending;
    }

    /**
     * Enqueues a GATT operation in the GATT operation queue.
     * <p> If the queue is empty, the operation starts immediately.
     */
    synchronized private void enqueueGattOperation(GattOperation operation) {
        if (gatt == null)
            return;
        if (gattOperationPending != null) {
            if (!CodelessLibConfig.GATT_QUEUE_PRIORITY)
                gattQueue.add(operation);
            else
                enqueueGattOperationWithPriority(operation);
        } else {
            executeGattOperation(operation);
        }
    }

    /**
     * Enqueues a series of GATT operations in the GATT operation queue.
     * <p> If the queue is empty, the first operation starts immediately.
     */
    synchronized private void enqueueGattOperations(List<GattOperation> operations) {
        if (gatt == null || operations.isEmpty())
            return;
        if (!CodelessLibConfig.GATT_QUEUE_PRIORITY)
            gattQueue.addAll(operations);
        else
            enqueueGattOperationsWithPriority(operations);
        if (gattOperationPending == null) {
            dequeueGattOperation();
        }
    }

    /**
     * Enqueues a GATT operation in the GATT operation queue, taking {@link GattOperation#lowPriority() priority} into account.
     * <p> Used if {@link CodelessLibConfig#GATT_QUEUE_PRIORITY} is enabled.
     */
    private void enqueueGattOperationWithPriority(GattOperation operation) {
        if (gattQueue.isEmpty() || !gattQueue.peekLast().lowPriority() || operation.lowPriority()) {
            gattQueue.add(operation);
        } else if (gattQueue.peekFirst().lowPriority()) {
            gattQueue.addFirst(operation);
        } else {
            ListIterator<GattOperation> i = gattQueue.listIterator();
            while(i.hasNext()) {
                if (i.next().lowPriority()) {
                    i.previous();
                    i.add(operation);
                    break;
                }
            }
        }
    }

    /**
     * Enqueues a series of GATT operations in the GATT operation queue, taking {@link GattOperation#lowPriority() priority} into account.
     * <p> Used if {@link CodelessLibConfig#GATT_QUEUE_PRIORITY} is enabled.
     * <p> NOTE: All operations in the series must have the same priority.
     */
    private void enqueueGattOperationsWithPriority(List<GattOperation> operations) {
        if (gattQueue.isEmpty() || !gattQueue.peekLast().lowPriority() || operations.get(0).lowPriority()) {
            gattQueue.addAll(operations);
        } else {
            ListIterator<GattOperation> i = gattQueue.listIterator();
            while(i.hasNext()) {
                if (i.next().lowPriority()) {
                    i.previous();
                    for (GattOperation operation : operations) {
                        i.add(operation);
                    }
                    break;
                }
            }
        }
    }

    /** Executes the next GATT operation from the GATT operation queue. */
    synchronized private void dequeueGattOperation() {
        gattOperationPending = null;
        if (gattQueue.isEmpty())
            return;
        executeGattOperation(gattQueue.poll());
    }

    /** Executes a GATT operation. */
    private void executeGattOperation(GattOperation operation) {
        gattOperationPending = operation;
        operation.onExecute();
        switch (operation.getType()) {
            case ReadCharacteristic:
                executeReadCharacteristic(operation.getCharacteristic());
                break;
            case WriteCharacteristic:
            case WriteCommand:
                executeWriteCharacteristic(operation.getCharacteristic(), operation.getValue(), operation.getType() == GattOperation.Type.WriteCharacteristic);
                break;
            case ReadDescriptor:
                executeReadDescriptor(operation.getDescriptor());
                break;
            case WriteDescriptor:
                executeWriteDescriptor(operation.getDescriptor(), operation.getValue());
                break;
            case MtuRequest:
                executeMtuRequest(ByteBuffer.wrap(operation.getValue()).order(ByteOrder.LITTLE_ENDIAN).getShort() & 0xffff);
                break;
        }
    }

    /**
     * Base class for enqueued DSPS data send operations.
     * <p> Executes a write command operation on the DSPS Server RX characteristic.
     */
    private class DspsGattOperation extends GattOperation {

        public DspsGattOperation(byte[] data) {
            super(dspsServerRx, data, false);
        }
    }

    /**
     * Enqueued DSPS chunk send operation (not part of a file or periodic send operation).
     * <p>
     * These are operations with high priority. If {@link CodelessLibConfig#GATT_QUEUE_PRIORITY} is enabled and the user
     * tries to send some data while a file transfer is in progress, the data will be added at the front of the queue.
     */
    private class DspsChunkOperation extends DspsGattOperation {

        public DspsChunkOperation(byte[] data) {
            super(data);
        }

        @Override
        protected void onExecute() {
            if (CodelessLibLog.DSPS_CHUNK)
                Log.d(TAG, logPrefix + "Send DSPS chunk: " + CodelessUtil.hexArrayLog(getValue()));
        }
    }

    /**
     * Enqueued DSPS chunk send operation, part of a periodic send operation.
     * <p> These are operations with low priority.
     */
    private class DspsPeriodicChunkOperation extends DspsGattOperation {

        private DspsPeriodicSend operation;
        private int count;
        private int chunk;
        private int totalChunks;

        public DspsPeriodicChunkOperation(DspsPeriodicSend operation, int count, byte[] data, int chunk, int totalChunks) {
            super(data);
            this.operation = operation;
            this.count = count;
            this.chunk = chunk;
            this.totalChunks = totalChunks;
        }

        public DspsPeriodicSend getOperation() {
            return operation;
        }

        public int getCount() {
            return count;
        }

        public int getChunk() {
            return chunk;
        }

        public int getTotalChunks() {
            return totalChunks;
        }

        @Override
        public boolean lowPriority() {
            return true;
        }

        @Override
        protected void onExecute() {
            if (CodelessLibLog.DSPS_PERIODIC_CHUNK)
                Log.d(TAG, logPrefix + "Send periodic DSPS chunk: count " + count + " (" + chunk + " of " + totalChunks + ") " + CodelessUtil.hexArrayLog(getValue()));
            if (CodelessLibConfig.DSPS_STATS)
                operation.updateBytesSent(getValue().length);
            if (operation.isPattern()) {
                operation.setPatternSentCount((count - 1) % operation.getPatternMaxCount());
                EventBus.getDefault().post(new CodelessEvent.DspsPatternChunk(CodelessManager.this, operation, operation.getPatternSentCount()));
            }
        }
    }

    /**
     * Enqueued DSPS chunk send operation, part of a file send operation.
     * <p> These are operations with low priority.
     */
    private class DspsFileChunkOperation extends DspsGattOperation {

        private DspsFileSend operation;
        private int chunk;

        public DspsFileChunkOperation(DspsFileSend operation, byte[] data, int chunk) {
            super(data);
            this.operation = operation;
            this.chunk = chunk;
        }

        public DspsFileSend getOperation() {
            return operation;
        }

        public int getChunk() {
            return chunk;
        }

        @Override
        public boolean lowPriority() {
            return true;
        }

        @Override
        protected void onExecute() {
            if (CodelessLibLog.DSPS_FILE_CHUNK)
                Log.d(TAG, logPrefix + "Send file chunk: " + operation + " (" + chunk + " of " + operation.getTotalChunks() + ") " + CodelessUtil.hexArrayLog(getValue()));
            operation.setSentChunks(chunk);
            if (CodelessLibConfig.DSPS_STATS)
                operation.updateBytesSent(getValue().length);
            if (chunk == operation.getTotalChunks()) {
                if (CodelessLibLog.DSPS)
                    Log.d(TAG, logPrefix + "File sent: " + operation);
                operation.setComplete();
                dspsFiles.remove(operation);
            }
            EventBus.getDefault().post(new CodelessEvent.DspsFileChunk(CodelessManager.this, operation, chunk));
        }
    }

    /**
     * Removes any enqueued operations from the GATT operation queue that are not part of a file or periodic send operation.
     * @param keep <code>true</code> to keep enqueued outgoing data in a buffer, <code>false</code> to discard them
     */
    synchronized private void removePendingDspsChunkOperations(boolean keep) {
        Iterator<GattOperation> i = gattQueue.iterator();
        while (i.hasNext()) {
            GattOperation gattOperation = i.next();
            if (gattOperation instanceof DspsChunkOperation) {
                if (keep)
                    dspsPending.add(gattOperation);
                i.remove();
            }
        }
    }

    /**
     * Removes any enqueued operations from the GATT operation queue that are part of a periodic send operation.
     * @param operation the periodic send operation
     * @return the counter of the first enqueued operation (used to set the resume counter)
     */
    synchronized private int removePendingDspsPeriodicChunkOperations(DspsPeriodicSend operation) {
        int count = -1;
        Iterator<GattOperation> i = gattQueue.iterator();
        while (i.hasNext()) {
            GattOperation gattOperation = i.next();
            if (gattOperation instanceof DspsPeriodicChunkOperation && ((DspsPeriodicChunkOperation)gattOperation).getOperation() == operation) {
                DspsPeriodicChunkOperation periodicChunkOperation = (DspsPeriodicChunkOperation) gattOperation;
                if (count == -1 && periodicChunkOperation.getChunk() == 1)
                    count = periodicChunkOperation.getCount();
                i.remove();
            }
        }
        return count;
    }

    /**
     * Removes any enqueued operations from the GATT operation queue that are part of a file send operation.
     * @param operation the file send operation
     * @return the chunk number of the first enqueued operation (used to set the resume chunk)
     */
    synchronized private int removePendingDspsFileChunkOperations(DspsFileSend operation) {
        int chunk = -1;
        Iterator<GattOperation> i = gattQueue.iterator();
        while (i.hasNext()) {
            GattOperation gattOperation = i.next();
            if (gattOperation instanceof DspsFileChunkOperation && ((DspsFileChunkOperation)gattOperation).getOperation() == operation) {
                if (chunk == -1)
                    chunk = ((DspsFileChunkOperation)gattOperation).getChunk() - 1;
                i.remove();
            }
        }
        return chunk;
    }
}
