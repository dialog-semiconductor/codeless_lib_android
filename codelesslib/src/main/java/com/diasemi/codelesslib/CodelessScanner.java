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
import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;

import com.diasemi.codelesslib.CodelessProfile.Uuid;
import com.diasemi.codelesslib.misc.RuntimePermissionChecker;

import org.greenrobot.eventbus.EventBus;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import androidx.appcompat.app.AlertDialog;

/**
 * Provides Bluetooth scan functionality and advertising data parsing.
 * <h2>Usage</h2>
 * Create a CodelessScanner object from your UI code and register for the relevant scan events.
 * Use the {@link #startScanning} and {@link #stopScanning} methods to start and stop scanning.
 * A {@link CodelessEvent.ScanResult ScanResult} event is generated on each advertising event,
 * containing the found device and parsed advertising data.
 * <p>
 * After a device is found, you can create a CodelessManager object for the device and {@link CodelessManager#connect() connect} to it.
 * <p>
 * In order to scan for Bluetooth devices, the app must be granted the required runtime permissions.
 * These are: ACCESS_COARSE_LOCATION for Android 6-9, ACCESS_FINE_LOCATION for Android 10 and above,
 * BLUETOOTH_SCAN for Android 12 and above.
 * <p>
 * Also, depending on the device configuration, the Location Services may have to be enabled.
 * From Android 12, it is possible to scan without the need of location permissions and services,
 * but some scan results may be filtered out by the system and not sent to the app.
 * @see CodelessManager
 * @see CodelessEvent
 * @see <a href="https://developer.android.com/guide/topics/connectivity/bluetooth/permissions">Bluetooth permissions</a>
 */
public class CodelessScanner {
    private final static String TAG = "CodelessScanner";

    private Context context;
    private BluetoothAdapter bluetoothAdapter;
    private ScannerApi scannerApi;
    private boolean scanning = false;
    private Handler handler;

    /**
     * Creates a CodelessScanner object for the current context.
     * @param context the application context
     */
    public CodelessScanner(Context context) {
        this.context = context.getApplicationContext();
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        scannerApi = Build.VERSION.SDK_INT < 21 ? scannerApi19 : scannerApi21;
        handler = new Handler(Looper.getMainLooper());
    }

    /** Dialog Semiconductor manufacturer ID. */
    public static final int DIALOG_MANUFACTURER_ID = 0x00D2;
    /** Apple manufacturer ID. */
    public static final int APPLE_MANUFACTURER_ID = 0x004C;
    /** Microsoft manufacturer ID. */
    public static final int MICROSOFT_MANUFACTURER_ID = 0x0006;

    /** Parsed advertising data. */
    public static class AdvData {

        /** The raw advertising data byte array. */
        public byte[] raw;
        /** The advertised device name. */
        public String name;
        /** The discoverable flag value. */
        public boolean discoverable;
        /** The limited discoverable flag value. */
        public boolean limitedDiscoverable;
        /** List of advertised services. */
        public ArrayList<UUID> services = new ArrayList<>();
        /** Manufacturer specific data (mapped by manufacturer ID). */
        public HashMap<Integer, byte[]> manufacturer = new HashMap<>();

        /** <code>true</code> if CodeLess service is advertised, <code>false</code> otherwise. */
        public boolean codeless;
        /** <code>true</code> if DSPS service is advertised, <code>false</code> otherwise. */
        public boolean dsps;
        /** <code>true</code> if SUOTA service is advertised, <code>false</code> otherwise. */
        public boolean suota;
        /** <code>true</code> if Dialog IoT-Sensors service is advertised, <code>false</code> otherwise. */
        public boolean iot;
        /** <code>true</code> if Dialog Wearable service is advertised, <code>false</code> otherwise. */
        public boolean wearable;
        /** <code>true</code> if one of the Mesh services is advertised, <code>false</code> otherwise. */
        public boolean mesh;
        /** <code>true</code> if the proximity profile services are advertised, <code>false</code> otherwise. */
        public boolean proximity;

        /** <code>true</code> if the advertising data define an iBeacon. */
        public boolean iBeacon;
        /** <code>true</code> if the advertising data define an iBeacon, using Dialog's manufacturer ID. */
        public boolean dialogBeacon;
        /** The iBeacon UUID. */
        public UUID beaconUuid;
        /** The iBeacon major number. */
        public int beaconMajor;
        /** The iBeacon minor number. */
        public int beaconMinor;
        /**
         * <code>true</code> if the advertising data define an Eddystone beacon.
         * <p> NOTE: Checking for Eddystone beacons is not implemented.
         */
        public boolean eddystone;
        /** <code>true</code> if the advertising data define a Microsoft beacon. */
        public boolean microsoft;

        /** Checks if the advertising data contain known services other than Codeless, DSPS, SUOTA. */
        public boolean other() {
            return iot || wearable || mesh || proximity;
        }

        /** Checks if the advertising data define a beacon. */
        public boolean beacon() {
            return iBeacon || dialogBeacon || eddystone || microsoft;
        }

        /** Checks if the advertising data do not contain any of the known services. */
        public boolean unknown() {
            return !codeless && !dsps && !suota && !other() && !beacon();
        }
    }

    /**
     * Parses the raw advertising data to an {@link AdvData AdvData} object.
     * @param data the raw advertising data
     * @return the parsed advertising data
     */
    private AdvData parseAdvertisingData(byte[] data) {
        AdvData advData = new AdvData();
        advData.raw = data;

        ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        while (buffer.remaining() > 2) {
            int length = buffer.get() & 0xff;
            if (length == 0)
                break;

            int type = buffer.get() & 0xff;
            --length;

            switch (type) {
                case 0x01: // Flags
                    if (length == 0 || buffer.remaining() == 0)
                        break;
                    byte flags = buffer.get();
                    length -= 1;
                    advData.discoverable = (flags & 0x02) != 0;
                    advData.limitedDiscoverable = (flags & 0x01) != 0;
                    break;

                case 0x02: // Partial list of 16-bit UUIDs
                case 0x03: // Complete list of 16-bit UUIDs
                    while (length >= 2 && buffer.remaining() >= 2) {
                        advData.services.add(UUID.fromString(String.format("%08x-0000-1000-8000-00805f9b34fb", buffer.getShort() & 0xffff)));
                        length -= 2;
                    }
                    break;

                case 0x04: // Partial list of 32-bit UUIDs
                case 0x05: // Complete list of 32-bit UUIDs
                    while (length >= 4 && buffer.remaining() >= 4) {
                        advData.services.add(UUID.fromString(String.format("%08x-0000-1000-8000-00805f9b34fb", buffer.getInt() & 0xffffffffL)));
                        length -= 4;
                    }
                    break;

                case 0x06: // Partial list of 128-bit UUIDs
                case 0x07: // Complete list of 128-bit UUIDs
                    while (length >= 16 && buffer.remaining() >= 16) {
                        long lsb = buffer.getLong();
                        long msb = buffer.getLong();
                        advData.services.add(new UUID(msb, lsb));
                        length -= 16;
                    }
                    break;

                case 0x08: // Shortened Local Name
                    if (advData.name != null)
                        break;
                    // fall through
                case 0x09: // Complete Local Name
                    if (length > buffer.remaining())
                        break;
                    byte[] name = new byte[length];
                    buffer.get(name);
                    length = 0;
                    advData.name = new String(name, StandardCharsets.UTF_8);
                    break;

                case 0xff: // Manufacturer Specific Data
                    if (length >= 2 && buffer.remaining() >= 2) {
                        int manufacturer = buffer.getShort() & 0xffff;
                        length -= 2;
                        byte[] manufacturerData = new byte[0];
                        if (length <= buffer.remaining()) {
                            manufacturerData = new byte[length];
                            length = 0;
                            buffer.get(manufacturerData);
                        }
                        advData.manufacturer.put(manufacturer, manufacturerData);
                    }
                    break;
            }

            if (length > buffer.remaining())
                break;
            buffer.position(buffer.position() + length);
        }

        advData.codeless = advData.services.contains(Uuid.CODELESS_SERVICE_UUID);
        advData.dsps = advData.services.contains(Uuid.DSPS_SERVICE_UUID);
        advData.suota = advData.services.contains(Uuid.SUOTA_SERVICE_UUID);
        advData.iot = advData.services.contains(Uuid.IOT_SERVICE_UUID);
        advData.wearable = advData.services.contains(Uuid.WEARABLES_580_SERVICE_UUID) || advData.services.contains(Uuid.WEARABLES_680_SERVICE_UUID);
        advData.mesh = advData.services.contains(Uuid.MESH_PROVISIONING_SERVICE_UUID) || advData.services.contains(Uuid.MESH_PROXY_SERVICE_UUID);
        advData.proximity = advData.services.contains(Uuid.IMMEDIATE_ALERT_SERVICE_UUID) && advData.services.contains(Uuid.LINK_LOSS_SERVICE_UUID);

        // Check for iBeacon
        int manufacturerId = DIALOG_MANUFACTURER_ID;
        byte[] manufacturerData = advData.manufacturer.get(manufacturerId);
        if (manufacturerData == null) {
            manufacturerId = APPLE_MANUFACTURER_ID;
            manufacturerData = advData.manufacturer.get(manufacturerId);
        }
        if (manufacturerData != null && manufacturerData.length == 23) {
            ByteBuffer manufacturerDataBuffer = ByteBuffer.wrap(manufacturerData).order(ByteOrder.BIG_ENDIAN);
            // Check subtype/length
            if (manufacturerDataBuffer.get() == 2 && manufacturerDataBuffer.get() == 21) {
                advData.dialogBeacon = manufacturerId == DIALOG_MANUFACTURER_ID;
                advData.iBeacon = manufacturerId == APPLE_MANUFACTURER_ID;
                advData.beaconUuid = new UUID(manufacturerDataBuffer.getLong(), manufacturerDataBuffer.getLong());
                advData.beaconMajor = manufacturerDataBuffer.getShort() & 0xffff;
                advData.beaconMinor = manufacturerDataBuffer.getShort() & 0xffff;
            }
        }

        // Check for Microsoft beacon
        manufacturerData = advData.manufacturer.get(MICROSOFT_MANUFACTURER_ID);
        if (manufacturerData != null && manufacturerData.length == 27) {
            advData.microsoft = true;
        }

        return advData;
    }

    /** Checks if a Bluetooth scan is currently active. */
    public boolean isScanning() {
        return scanning;
    }

    /**
     * Starts a Bluetooth scan with no set duration.
     * <p> The scan will continue until {@link #stopScanning()} is called.
     * @see #startScanning(int, Activity, int, RuntimePermissionChecker)
     */
    public void startScanning() {
        startScanning(0, null, -1, null);
    }

    /**
     * Starts a Bluetooth scan with the specified duration.
     * <p> The scan will stop automatically.
     * @param duration the scan duration (ms)
     * @see #startScanning(int, Activity, int, RuntimePermissionChecker)
     */
    public void startScanning(int duration) {
        startScanning(duration, null, -1, null);
    }

    /**
     * Starts a Bluetooth scan with no set duration.
     * <p> The scan will continue until {@link #stopScanning()} is called.
     * @see #startScanning(int, Activity, int, RuntimePermissionChecker)
     */
    public void startScanning(Activity activity, int bluetoothRequestCode, RuntimePermissionChecker permissionChecker) {
        startScanning(0, activity, bluetoothRequestCode, permissionChecker);
    }

    /**
     * Starts a Bluetooth scan with the specified duration, checking if scan requirements are fulfilled.
     * <p>
     * If there are missing requirements, this method will try to fulfill them, by issuing requests to the system.
     * The requests that can be issued depend on the provided arguments.
     * After a permission request is accepted by the user, a {@link CodelessEvent.ScanRestart ScanRestart} event is generated.
     * If a Bluetooth enable request is issued, the activity should implement <code>onActivityResult</code> to get the result and restart the scan.
     * <p>
     * A {@link CodelessEvent.ScanStart ScanStart} event is generated when scanning has started.
     * <p>
     * A {@link CodelessEvent.ScanResult ScanResult} event is generated on each advertising event.
     * @param duration              the scan duration (ms). Set to 0 for continuous scanning.
     * @param activity              the activity that requested the scan. Used to request enabling Bluetooth and
     *                              Location Services, if either of them is disabled. Set to <code>null</code> to disable the requests.
     * @param bluetoothRequestCode  the request code used for the Bluetooth enable request. Set to -1 to disable the request.
     * @param permissionChecker     the {@link RuntimePermissionChecker permission checker} to request for permissions.
     *                              Set to <code>null</code> to disable the permission request.
     */
    public void startScanning(int duration, Activity activity, int bluetoothRequestCode, RuntimePermissionChecker permissionChecker) {
        handler.removeCallbacks(scanTimer);
        if (!checkScanRequirements(activity, bluetoothRequestCode, permissionChecker))
            return;
        if (scanning)
            return;
        scanning = true;
        Log.d(TAG, "Start scanning");
        scannerApi.startScanning();
        if (duration > 0)
            handler.postDelayed(scanTimer, duration);
        EventBus.getDefault().post(new CodelessEvent.ScanStart(this));
    }

    /**
     * Stops the active Bluetooth scan.
     * <p> A {@link CodelessEvent.ScanStop ScanStop} event is generated when scanning has stopped.
     */
    public void stopScanning() {
        handler.removeCallbacks(scanTimer);
        if (!scanning)
            return;
        scanning = false;
        Log.d(TAG, "Stop scanning");
        scannerApi.stopScanning();
        EventBus.getDefault().post(new CodelessEvent.ScanStop(this));
    }

    /** Timer to stop the Bluetooth scan automatically after the specified duration. */
    private Runnable scanTimer = new Runnable() {
        @Override
        public void run() {
            stopScanning();
        }
    };

    /**
     * Called for each advertising event.
     * <p> Parsed the advertising data and generates a {@link CodelessEvent.ScanResult ScanResult} event.
     * @param device        the found device
     * @param rssi          the advertising event RSSI
     * @param scanRecord    the raw advertising data
     */
    private void onScanResult(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
        EventBus.getDefault().post(new CodelessEvent.ScanResult(this, device, parseAdvertisingData(scanRecord), rssi));
    }

    /** Scan API abstraction. */
    private interface ScannerApi {
        void startScanning();
        void stopScanning();
    }

    /** Uses the old Android scan API for versions prior to Android 5. */
    private ScannerApi scannerApi19 = new ScannerApi() {

        BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
            @Override
            public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
                onScanResult(device, rssi, scanRecord);
            }
        };

        @SuppressLint("MissingPermission")
        @Override
        public void startScanning() {
            bluetoothAdapter.startLeScan(mLeScanCallback);
        }

        @SuppressLint("MissingPermission")
        @Override
        public void stopScanning() {
            bluetoothAdapter.stopLeScan(mLeScanCallback);
        }
    };

    /** Uses the new Android scan API for versions Android 5 and above. */
    @TargetApi(21)
    private ScannerApi scannerApi21 = new ScannerApi() {

        BluetoothLeScanner scanner;
        ScanCallback callback;
        ScanSettings settings;

        @SuppressLint("MissingPermission")
        @Override
        public void startScanning() {
            if (scanner == null) {
                scanner = bluetoothAdapter.getBluetoothLeScanner();
                settings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).setReportDelay(0).build();
                callback = new ScanCallback() {
                    @Override
                    public void onScanResult(int callbackType, ScanResult result) {
                        CodelessScanner.this.onScanResult(result.getDevice(), result.getRssi(), result.getScanRecord().getBytes());
                    }

                    @Override
                    public void onBatchScanResults(List<ScanResult> results) {
                        for (ScanResult result : results)
                            CodelessScanner.this.onScanResult(result.getDevice(), result.getRssi(), result.getScanRecord().getBytes());
                    }
                };
            }
            scanner.startScan(null, settings, callback);
        }

        @SuppressLint("MissingPermission")
        @Override
        public void stopScanning() {
            if (scanner != null && bluetoothAdapter.isEnabled())
                scanner.stopScan(callback);
        }
    };

    /**
     * Checks if the scan requirements are fulfilled.
     * <p>
     * If there are missing requirements, this method will try to fulfill them, by issuing requests to the system.
     * The requests that can be issued depend on the provided arguments.
     * After a permission request is accepted by the user, a {@link CodelessEvent.ScanRestart ScanRestart} event is generated.
     * If a Bluetooth enable request is issued, the activity should implement <code>onActivityResult</code> to get the result and restart the scan.
     * @param activity              the activity that requested the scan. Used to request enabling Bluetooth and
     *                              Location Services, if either of them is disabled. Set to <code>null</code> to disable the requests.
     * @param bluetoothRequestCode  the request code used for the Bluetooth enable request. Set to -1 to disable the request.
     * @param permissionChecker     the {@link RuntimePermissionChecker permission checker} to request for permissions.
     *                              Set to <code>null</code> to disable the permission request.
     */
    public boolean checkScanRequirements(Activity activity, int bluetoothRequestCode, RuntimePermissionChecker permissionChecker) {
        if (!bluetoothAdapter.isEnabled()) {
            if (bluetoothRequestCode >= 0 && activity != null) {
                if (!CodelessUtil.checkConnectPermission(context)) {
                    if (permissionChecker != null)
                        permissionChecker.checkPermission(Manifest.permission.BLUETOOTH_CONNECT, null, (requestCode, permissions, denied) -> {
                            if (denied == null)
                                EventBus.getDefault().post(new CodelessEvent.ScanRestart(CodelessScanner.this));
                        });
                    return false;
                }
                if (bluetoothRequestCode > 0)
                    activity.startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), bluetoothRequestCode);
                else
                    activity.startActivity(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));
            }
            return false;
        }
        if (!checkPermissions()) {
            if (permissionChecker != null) {
                permissionChecker.checkPermission(SCAN_LOCATION_PERMISSION, R.string.codeless_location_permission_rationale, (requestCode, permissions, denied) -> {
                    if (denied == null)
                        EventBus.getDefault().post(new CodelessEvent.ScanRestart(CodelessScanner.this));
                });
                if (Build.VERSION.SDK_INT >= 31)
                    permissionChecker.checkPermission(Manifest.permission.BLUETOOTH_SCAN, null, (requestCode, permissions, denied) -> {
                        if (denied == null)
                            EventBus.getDefault().post(new CodelessEvent.ScanRestart(CodelessScanner.this));
                    });
            }
            return false;
        }
        return checkLocationServices(activity);
    }

    /**
     * The location permission required for scanning.
     * <p> Prior to Android 10, ACCESS_COARSE_LOCATION permission was sufficient. Later versions require the ACCESS_FINE_LOCATION permission.
     */
    public static final String SCAN_LOCATION_PERMISSION = Build.VERSION.SDK_INT < 29 ?  Manifest.permission.ACCESS_COARSE_LOCATION : Manifest.permission.ACCESS_FINE_LOCATION;

    /** Checks if the required permissions for scanning have been granted. */
    public boolean checkPermissions() {
        return (Build.VERSION.SDK_INT < 23 || context.checkSelfPermission(SCAN_LOCATION_PERMISSION) == PackageManager.PERMISSION_GRANTED)
                && (Build.VERSION.SDK_INT < 31 || context.checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED);
    }

    private Boolean locationServicesRequired;
    private boolean locationServicesSkipCheck;

    /**
     * Checks if the Location Services are enabled, if this is required for scanning.
     * @see #checkLocationServices(Activity)
     */
    public boolean checkLocationServices() {
        return checkLocationServices(null);
    }

    /**
     * Checks if the Location Services are enabled, if this is required for scanning.
     * <p> If the Locations Services are disabled but required, a request to enable them is issued.
     * @param activity used for the Location Services enable request, if they are disabled. Set to <code>null</code> to disable the request.
     */
    public boolean checkLocationServices(final Activity activity) {
        if (Build.VERSION.SDK_INT < 23 || locationServicesSkipCheck)
            return true;
        // Check if location services are required by reading the setting from Bluetooth app.
        if (locationServicesRequired == null) {
            locationServicesRequired = true;
            try {
                Resources res = context.getPackageManager().getResourcesForApplication("com.android.bluetooth");
                int id = res.getIdentifier("strict_location_check", "bool", "com.android.bluetooth");
                if (id == 0)
                    throw new Resources.NotFoundException();
                locationServicesRequired = res.getBoolean(id);
            } catch (PackageManager.NameNotFoundException | Resources.NotFoundException e) {
                Log.e(TAG, "Failed to read location services requirement setting");
            }
            Log.d(TAG, "Location services requirement setting: " + locationServicesRequired);
        }
        if (!locationServicesRequired)
            return true;
        // Check location services setting. Prompt the user to enable them.
        if (Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE, Settings.Secure.LOCATION_MODE_OFF) != Settings.Secure.LOCATION_MODE_OFF)
            return true;
        Log.d(TAG, "Location services disabled");
        if (activity != null) {
            new AlertDialog.Builder(activity)
                    .setTitle(R.string.codeless_no_location_services_title)
                    .setMessage(R.string.codeless_no_location_services_msg)
                    .setPositiveButton(R.string.codeless_enable_location_services, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            activity.startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                        }
                    })
                    .setNegativeButton(R.string.codeless_no_location_services_scan, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            locationServicesSkipCheck = true;
                            EventBus.getDefault().post(new CodelessEvent.ScanRestart(CodelessScanner.this));
                        }
                    })
                    .show();
        }
        return false;
    }
}
