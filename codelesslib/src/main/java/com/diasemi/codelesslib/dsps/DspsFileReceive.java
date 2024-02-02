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

package com.diasemi.codelesslib.dsps;

import android.util.Log;

import com.diasemi.codelesslib.CodelessEvent;
import com.diasemi.codelesslib.CodelessLibConfig;
import com.diasemi.codelesslib.CodelessLibLog;
import com.diasemi.codelesslib.CodelessManager;
import com.diasemi.codelesslib.log.DspsRxLogFile;

import org.greenrobot.eventbus.EventBus;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.zip.CRC32;

import static com.diasemi.codelesslib.CodelessManager.SPEED_INVALID;

/**
 * DSPS file receive operation.
 * <h2>Usage</h2>
 * Use the {@link CodelessManager#receiveFile() receiveFile} method to create and {@link #start() start}
 * a DSPS file receive operation. Only a single file receive operation can be active.
 * <p>
 * After the operation is started, it constantly checks the received data for the
 * following {@link CodelessLibConfig#DSPS_RX_FILE_HEADER_PATTERN_STRING file header}:
 * <blockquote><pre>
 * Name: &lt;file_name&gt; (no whitespace)
 * Size: &lt;n&gt; (bytes)
 * CRC: &lt;hex&gt; (CRC-32, optional)
 * END (header end mark)
 * ... &lt;n&gt; bytes of data ...</pre></blockquote>
 * When the header is detected, the {@link DspsRxLogFile output file} with the specified name is created in
 * the configured output path. After that, and until the file size specified in the header is reached, all
 * incoming data are saved to the output file. A {@link CodelessEvent.DspsRxFileData DspsRxFileData} event
 * is generated for each received data packet.
 * <p>
 * After all the data are received, if the header contained a CRC value, the file data CRC is validated and
 * a {@link CodelessEvent.DspsRxFileCrc DspsRxFileCrc} event is generated.
 * <p>
 * NOTE: A single null byte may also be used as the header end mark. The file data start immediately after.
 * @see CodelessManager
 */
public class DspsFileReceive {
    private final static String TAG = "DspsFileReceive";

    private CodelessManager manager;
    private byte[] header;
    private String name;
    private int size;
    private long crc = -1;
    private DspsRxLogFile file;
    private int bytesReceived;
    private CRC32 crc32;
    private boolean started;
    private boolean complete;
    private long startTime;
    private long endTime;
    private long lastInterval;
    private int bytesReceivedInterval;
    private int currentSpeed = SPEED_INVALID;

    /**
     * Creates a DSPS file receive operation.
     * @param manager the associated manager
     */
    public DspsFileReceive(CodelessManager manager) {
        this.manager = manager;
    }

    /** Returns the associated manager. */
    public CodelessManager getManager() {
        return manager;
    }

    /** Returns the file name. */
    public String getName() {
        return name;
    }

    /** Returns the file size. */
    public int getSize() {
        return size;
    }

    /** Returns the file data CRC, if it is set. */
    public long getCrc() {
        return crc;
    }

    /** Checks if a CRC is set for the file data. */
    public boolean hasCrc() {
        return crc != -1;
    }

    /** Checks if the file data CRC validation succeeded. */
    public boolean crcOk() {
        return crc32 != null && crc32.getValue() == crc;
    }

    /** Returns the log file where the received data are saved. */
    public DspsRxLogFile getFile() {
        return file;
    }

    /** Returns the number of received bytes. */
    public int getBytesReceived() {
        return bytesReceived;
    }

    /** Checks if the operation has started. */
    public boolean isStarted() {
        return started;
    }

    /** Checks if the operation is complete. */
    public boolean isComplete() {
        return complete;
    }

    /** Returns the operation start time. */
    public long getStartTime() {
        return startTime;
    }

    /** Returns the operation end time. */
    public long getEndTime() {
        return endTime;
    }

    /**
     * Returns the calculated current speed.
     * <p> Available only if {@link CodelessLibConfig#DSPS_STATS statistics} are enabled.
     */
    public int getCurrentSpeed() {
        return currentSpeed;
    }

    /**
     * Returns the calculated average speed for the duration of the file receive operation.
     * <p> Available only if {@link CodelessLibConfig#DSPS_STATS statistics} are enabled.
     */
    public int getAverageSpeed() {
        long elapsed = (complete ? endTime : new Date().getTime()) - startTime;
        if (elapsed == 0)
            elapsed = 1;
        return (int) (bytesReceived * 1000L / elapsed);
    }

    /**
     * Performs statistics calculations, called every {@link CodelessLibConfig#DSPS_STATS_INTERVAL}.
     * <p> A {@link CodelessEvent.DspsStats DspsStats} event is generated.
     */
    private Runnable updateStats = new Runnable() {
        @Override
        public void run() {
            if (complete)
                return;
            synchronized (DspsFileReceive.this) {
                long now = new Date().getTime();
                if (now == lastInterval)
                    now++;
                currentSpeed = (int) (bytesReceivedInterval * 1000L / (now - lastInterval));
                lastInterval = now;
                bytesReceivedInterval = 0;
                manager.getDspsStatsHandler().postDelayed(this, CodelessLibConfig.DSPS_STATS_INTERVAL);
                EventBus.getDefault().post(new CodelessEvent.DspsStats(manager, DspsFileReceive.this, currentSpeed, getAverageSpeed()));
            }
        }
    };

    /**
     * Starts the file receive operation.
     * @see CodelessManager#receiveFile()
     */
    public void start() {
        if (started)
            return;
        started = true;
        if (CodelessLibLog.DSPS)
            Log.d(TAG, manager.getLogPrefix() + "Start file receive");
        manager.start(this);
    }

    /** Stops the file receive operation. */
    public void stop() {
        if (CodelessLibLog.DSPS)
            Log.d(TAG, manager.getLogPrefix() + "Stop file receive");
        endTime = new Date().getTime();
        if (file != null) {
            if (CodelessLibConfig.DSPS_LOG_HANDLER)
                manager.getDspsLogHandler().post(() -> file.close());
            else
                file.close();
        }
        if (CodelessLibConfig.DSPS_STATS) {
            manager.getDspsStatsHandler().removeCallbacks(updateStats);
        }
        manager.stop(this);
    }

    /**
     * Called by the library when binary data are received from the peer device, if a file receive operation is active.
     * <p>
     * It checks the received data for the file header. If the file header is detected, the {@link DspsRxLogFile output file} is
     * created and all received data after that are saved to the file, until the file size specified in the header is reached.
     * A {@link CodelessEvent.DspsRxFileData DspsRxFileData} event is generated for each received data packet.
     * After all the data are received, if the header contained a CRC value, the file data CRC is validated and
     * a {@link CodelessEvent.DspsRxFileCrc DspsRxFileCrc} event is generated.
     * <p>
     * The file header has the following {@link CodelessLibConfig#DSPS_RX_FILE_HEADER_PATTERN_STRING format}:
     * <blockquote><pre>
     * Name: &lt;file_name&gt; (no whitespace)
     * Size: &lt;n&gt; (bytes)
     * CRC: &lt;hex&gt; (CRC-32, optional)
     * END (header end mark)
     * ... &lt;n&gt; bytes of data ...</pre></blockquote>
     * @param data the received data
     */
    public void onDspsData(byte[] data) {
        if (!started)
            return;

        // Check for header
        if (file == null) {
            if (header == null) {
                header = data;
            } else {
                header = Arrays.copyOf(header, header.length + data.length);
                System.arraycopy(data, 0, header, header.length - data.length, data.length);
            }
            String headerText = new String(header, StandardCharsets.US_ASCII);
            Matcher matcher = CodelessLibConfig.DSPS_RX_FILE_HEADER_PATTERN.matcher(headerText);
            if (matcher.matches()) {
                name = matcher.group(2);
                try {
                    size = Integer.decode(matcher.group(3));
                    if (matcher.group(4) != null)
                        crc = Long.parseLong(matcher.group(4), 16);
                } catch (NumberFormatException e) {
                    Log.e(TAG, "File header parse failure", e);
                }
                int start = matcher.end(1);
                int end = matcher.start(5);

                data = Arrays.copyOfRange(header, end, header.length);
                header = Arrays.copyOfRange(header, start, end);

                if (CodelessLibLog.DSPS)
                    Log.d(TAG, manager.getLogPrefix() + "File receive: " + name + " size=" + size + " crc=" + (crc != -1 ? Long.toHexString(crc).toUpperCase() : "N/A"));
                startTime = new Date().getTime();
                if (CodelessLibConfig.DSPS_STATS) {
                    lastInterval = startTime;
                    manager.getDspsStatsHandler().postDelayed(updateStats, CodelessLibConfig.DSPS_STATS_INTERVAL);
                }

                file = new DspsRxLogFile(this);
                if (crc != -1)
                    crc32 = new CRC32();
                EventBus.getDefault().post(new CodelessEvent.DspsRxFileData(manager, this, size, bytesReceived));
            } else if (!matcher.hitEnd()) {
                header = null;
            }
        }

        if (file == null || data.length == 0)
            return;

        // Write data to file
        if (data.length > size - bytesReceived)
            data = Arrays.copyOf(data, size - bytesReceived);
        synchronized (this) {
            bytesReceived += data.length;
            bytesReceivedInterval += data.length;
        }

        if (CodelessLibLog.DSPS_FILE_CHUNK)
            Log.d(TAG, manager.getLogPrefix() + "File receive: " + name + " " + bytesReceived + " of " + size);
        if (CodelessLibConfig.DSPS_LOG_HANDLER) {
            byte[] logData = data;
            manager.getDspsLogHandler().post(() -> file.log(logData));
        } else {
            file.log(data);
        }
        if (crc != -1)
            crc32.update(data);

        if (bytesReceived == size) {
            if (CodelessLibLog.DSPS)
                Log.d(TAG, manager.getLogPrefix() + "File received: " + name);
            complete = true;
            endTime = new Date().getTime();
            if (CodelessLibConfig.DSPS_STATS) {
                synchronized (this) {
                    manager.getDspsStatsHandler().removeCallbacks(updateStats);
                    EventBus.getDefault().post(new CodelessEvent.DspsStats(manager, this, currentSpeed, getAverageSpeed()));
                }
            }
            if (CodelessLibConfig.DSPS_LOG_HANDLER)
                manager.getDspsLogHandler().post(() -> file.close());
            else
                file.close();
            manager.stop(this);
        }

        EventBus.getDefault().post(new CodelessEvent.DspsRxFileData(manager, this, size, bytesReceived));
        if (complete && crc != -1) {
            boolean ok = crc == crc32.getValue();
            Log.d(TAG, manager.getLogPrefix() + "Received file CRC " + (ok ? "OK" : "error") + ": " + name);
            EventBus.getDefault().post(new CodelessEvent.DspsRxFileCrc(manager, this, ok));
        }
    }
}
