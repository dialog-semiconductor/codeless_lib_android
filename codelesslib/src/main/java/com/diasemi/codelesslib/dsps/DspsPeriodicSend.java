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

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import com.diasemi.codelesslib.CodelessEvent;
import com.diasemi.codelesslib.CodelessLibConfig;
import com.diasemi.codelesslib.CodelessLibLog;
import com.diasemi.codelesslib.CodelessManager;
import com.diasemi.codelesslib.CodelessUtil;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Date;

import androidx.documentfile.provider.DocumentFile;

import static com.diasemi.codelesslib.CodelessLibConfig.CHARSET;
import static com.diasemi.codelesslib.CodelessLibConfig.DSPS_PATTERN_DIGITS;
import static com.diasemi.codelesslib.CodelessLibConfig.DSPS_PATTERN_SUFFIX;
import static com.diasemi.codelesslib.CodelessManager.SPEED_INVALID;

/**
 * DSPS periodic send operation.
 * <h2>Usage</h2>
 * There are two types of periodic send operations.
 * <p>
 * For both types, if {@link CodelessLibConfig#DSPS_STATS statistics} are enabled, a {@link CodelessEvent.DspsStats DspsStats}
 * event is generated every {@link CodelessLibConfig#DSPS_STATS_INTERVAL}.
 * <h3>Data packet periodic send</h3>
 * In this type of  periodic send, the data to send are specified initially and remain the same for all packets.
 * Each packet may be split into chunks if its size exceeds the specified {@link #getChunkSize() chunk size}.
 * Every {@link #getPeriod() period}, a packet (with all its chunks) is enqueued to be sent to the peer device.
 * <p>
 * Use one of the constructors to initialize the operation. Use {@link #start()} to start the periodic operation,
 * which will run until {@link #stop()} is called.
 * <h3>Pattern packet periodic send</h3>
 * In this type of periodic send, the packet to send consists of a prefix, which is read from the start of the specified file,
 * and a number suffix which changes for each packet. The packet size is equal to the specified {@link #getChunkSize() chunk size}
 * (unless the file size is less than that). The number suffix has a constant length of {@link CodelessLibConfig#DSPS_PATTERN_DIGITS}.
 * {@link CodelessLibConfig#DSPS_PATTERN_SUFFIX} can be used to add extra data after the number (for example, a new line character).
 * The number suffix counts all numbers from 0 to the maximum allowed by its length and wraps around.
 * Every {@link #getPeriod() period}, a packet (single chunk) with the next suffix is enqueued to be sent to the peer device.
 * <p>
 * Use one of the {@link CodelessManager#sendPattern(Uri, int, int) sendPattern} methods to create and {@link #start() start} the operation,
 * which will run until {@link #stop()} is called. The pattern file is selected using the {@link File} API or a file {@link Uri} for
 * scoped storage. If the pattern fails to load, a {@link CodelessEvent.DspsPatternFileError DspsPatternFileError} event is generated.
 * A {@link CodelessEvent.DspsPatternChunk DspsPatternChunk} event is generated for each packet that is sent to the peer device.
 * <p>
 * For example, if the pattern file contains the text "abcdefgh" and 4 digits with end of line are used,
 * the pattern will be the following, with one packet sent per line:
 * <blockquote><pre>
 * abcdefgh0000
 * abcdefgh0001
 * abcdefgh0002
 * ...
 * abcdefgh9998
 * abcdefgh9999
 * abcdefgh0000
 * ...</pre></blockquote>
 * @see CodelessManager
 */
public class DspsPeriodicSend {
    private final static String TAG = "DspsPeriodicSend";

    private CodelessManager manager;
    private int period;
    private byte[] data;
    private int chunkSize;
    private boolean active;
    private int count;
    private boolean pattern;
    private int patternMaxCount;
    private int patternSentCount;
    private String patternFormat;
    private long startTime;
    private long endTime;
    private int bytesSent;
    private long lastInterval;
    private int bytesSentInterval;
    private int currentSpeed = SPEED_INVALID;

    /**
     * Creates a DSPS periodic send operation, which sends a data packet periodically to the peer device.
     * @param manager   the associated manager
     * @param period    the packet enqueueing period (ms)
     * @param data      the data packet
     * @param chunkSize the chunk size to use when splitting the packet
     */
    public DspsPeriodicSend(CodelessManager manager, int period, byte[] data, int chunkSize) {
        this.manager = manager;
        this.period = period;
        this.data = data;
        this.chunkSize = chunkSize;
    }

    /**
     * Creates a DSPS periodic send operation, which sends a data packet periodically to the peer device,
     * using the manager's chunk size.
     * @param manager   the associated manager
     * @param period    the packet enqueueing period (ms)
     * @param data      the data packet
     */
    public DspsPeriodicSend(CodelessManager manager, int period, byte[] data) {
        this(manager, period, data, manager.getDspsChunkSize());
    }

    /**
     * Creates a DSPS periodic send operation, which sends a text packet periodically to the peer device.
     * @param manager   the associated manager
     * @param period    the packet enqueueing period (ms)
     * @param text      the text packet
     * @param chunkSize the chunk size to use when splitting the packet
     */
    public DspsPeriodicSend(CodelessManager manager, int period, String text, int chunkSize) {
        this(manager, period, text.getBytes(CodelessLibConfig.CHARSET), chunkSize);
    }

    /**
     * Creates a DSPS periodic send operation, which sends a text packet periodically to the peer device,
     * using the manager's chunk size.
     * @param manager   the associated manager
     * @param period    the packet enqueueing period (ms)
     * @param text      the text packet
     */
    public DspsPeriodicSend(CodelessManager manager, int period, String text) {
        this(manager, period, text, manager.getDspsChunkSize());
    }

    private DspsPeriodicSend(CodelessManager manager, int chunkSize, int period) {
        this.manager = manager;
        this.chunkSize = Math.max(Math.min(chunkSize, manager.getDspsChunkSize()), DSPS_PATTERN_DIGITS + (DSPS_PATTERN_SUFFIX != null ? DSPS_PATTERN_SUFFIX.length : 0));
        this.period = period;
        pattern = true;
        patternMaxCount = (int) Math.pow(10, DSPS_PATTERN_DIGITS);
        patternFormat = "%0" + DSPS_PATTERN_DIGITS + "d";
    }

    /**
     * Creates a DSPS periodic send operation, which sends a pattern packet periodically to the peer device.
     * @param manager   the associated manager
     * @param file      the file containing the pattern prefix
     * @param chunkSize the pattern packet size
     * @param period    the packet enqueueing period (ms)
     */
    public DspsPeriodicSend(CodelessManager manager, File file, int chunkSize, int period) {
        this(manager, chunkSize, period);
        loadPattern(file, null);
    }

    /**
     * Creates a DSPS periodic send operation, which sends a pattern packet periodically to the peer device,
     * using the manager's chunk size.
     * @param manager   the associated manager
     * @param file      the file containing the pattern prefix
     * @param period    the packet enqueueing period (ms)
     */
    public DspsPeriodicSend(CodelessManager manager, File file, int period) {
        this(manager, file, manager.getDspsChunkSize(), period);
    }

    /**
     * Creates a DSPS periodic send operation, which sends a pattern packet periodically to the peer device.
     * @param manager   the associated manager
     * @param uri       the file URI containing the pattern prefix
     * @param chunkSize the pattern packet size
     * @param period    the packet enqueueing period (ms)
     */
    public DspsPeriodicSend(CodelessManager manager, Uri uri, int chunkSize, int period) {
        this(manager, chunkSize, period);
        loadPattern(null, uri);
    }

    /**
     * Creates a DSPS periodic send operation, which sends a pattern packet periodically to the peer device,
     * using the manager's chunk size.
     * @param manager   the associated manager
     * @param uri       the file URI containing the pattern prefix
     * @param period    the packet enqueueing period (ms)
     */
    public DspsPeriodicSend(CodelessManager manager, Uri uri, int period) {
        this(manager, uri, manager.getDspsChunkSize(), period);
    }

    /** Returns the associated manager. */
    public CodelessManager getManager() {
        return manager;
    }

    /** Returns the period of the periodic send operation (ms). */
    public int getPeriod() {
        return period;
    }

    /**
     * Returns the data packet that is sent periodically.
     * <p> When a pattern is used, this contains the last packet that was enqueued, which has the latest number suffix.
     */
    public byte[] getData() {
        return data;
    }

    /** Returns the chunk size. */
    public int getChunkSize() {
        return chunkSize;
    }

    /** Checks if the operation is active. */
    public boolean isActive() {
        return active;
    }

    /** Returns the counter of periodic packets that have been enqueued or sent. */
    public int getCount() {
        return count;
    }

    /**
     * Sets the counter from which the operation will resume.
     * <p> Used by the library to resume the operation after it was paused.
     * @param count the resume counter
     */
    public void setResumeCount(int count) {
        this.count = count - 1;
    }

    /** Checks if this is a pattern operation. */
    public boolean isPattern() {
        return pattern;
    }

    /**
     * Returns the maximum value of the pattern counter.
     * <p> The pattern counter will wrap around to 0 after the maximum is reached.
     */
    public int getPatternMaxCount() {
        return patternMaxCount;
    }

    /**
     * Returns the current pattern counter, which is used as the packet number suffix.
     * <p> Current pattern counter is the counter that was used for the last packet that was enqueued.
     */
    public int getPatternCount() {
        return (count - 1) % patternMaxCount;
    }

    /** Returns the pattern counter of the last sent packet. */
    public int getPatternSentCount() {
        return patternSentCount;
    }

    /**
     * Sets the pattern counter of the last sent packet.
     * <p> Called by the library when a pattern packet is sent to the peer device.
     * @param patternSentCount the pattern counter
     */
    public void setPatternSentCount(int patternSentCount) {
        this.patternSentCount = patternSentCount;
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
     * Returns the total number of sent bytes.
     * <p> Available only if {@link CodelessLibConfig#DSPS_STATS statistics} are enabled.
     */
    public int getBytesSent() {
        return bytesSent;
    }

    /**
     * Returns the calculated current speed.
     * <p> Available only if {@link CodelessLibConfig#DSPS_STATS statistics} are enabled.
     */
    public int getCurrentSpeed() {
        return currentSpeed;
    }

    /**
     * Returns the calculated average speed for the duration of the periodic send operation.
     * <p> Available only if {@link CodelessLibConfig#DSPS_STATS statistics} are enabled.
     */
    public int getAverageSpeed() {
        long elapsed = (!active ? endTime : new Date().getTime()) - startTime;
        if (elapsed == 0)
            elapsed = 1;
        return (int) (bytesSent * 1000L / elapsed);
    }

    /**
     * Updates the byte counters used in statistics calculations.
     * @param bytes the number of sent bytes
     */
    synchronized public void updateBytesSent(int bytes) {
        bytesSent += bytes;
        bytesSentInterval += bytes;
    }

    /**
     * Performs statistics calculations, called every {@link CodelessLibConfig#DSPS_STATS_INTERVAL}.
     * <p> A {@link CodelessEvent.DspsStats DspsStats} event is generated.
     */
    private Runnable updateStats = new Runnable() {
        @Override
        public void run() {
            if (!active)
                return;
            synchronized (DspsPeriodicSend.this) {
                long now = new Date().getTime();
                if (now == lastInterval)
                    now++;
                currentSpeed = (int) (bytesSentInterval * 1000L / (now - lastInterval));
                lastInterval = now;
                bytesSentInterval = 0;
                manager.getDspsStatsHandler().postDelayed(this, CodelessLibConfig.DSPS_STATS_INTERVAL);
                EventBus.getDefault().post(new CodelessEvent.DspsStats(manager, DspsPeriodicSend.this, currentSpeed, getAverageSpeed()));
            }
        }
    };

    /**
     * Starts the periodic send operation.
     * <p> The operation will continue until {@link #stop()} is called.
     * @see CodelessManager#sendPattern(File, int, int)
     * @see CodelessManager#sendPattern(Uri, int, int)
     */
    public void start() {
        if (active)
            return;
        active = true;
        if (CodelessLibLog.DSPS)
            Log.d(TAG, manager.getLogPrefix() + "Start periodic send" + (pattern ? " (pattern)" : "") + ": period=" + period + "ms " + CodelessUtil.hexArrayLog(data));
        count = 0;
        startTime = new Date().getTime();
        if (CodelessLibConfig.DSPS_STATS) {
            lastInterval = startTime;
            manager.getDspsStatsHandler().postDelayed(updateStats, CodelessLibConfig.DSPS_STATS_INTERVAL);
        }
        manager.start(this);
    }

    /** Stops the periodic send operation. */
    public void stop() {
        active = false;
        if (CodelessLibLog.DSPS)
            Log.d(TAG, manager.getLogPrefix() + "Stop periodic send" + (pattern ? " (pattern)" : "") + ": period=" + period + "ms " + CodelessUtil.hexArrayLog(data));
        endTime = new Date().getTime();
        if (CodelessLibConfig.DSPS_STATS) {
            manager.getDspsStatsHandler().removeCallbacks(updateStats);
        }
        manager.stop(this);
    }

    /** Returns the send packet runnable, which enqueues the next packet for sending. */
    public Runnable getRunnable() {
        return sendData;
    }

    /** Enqueues the next packet for sending, called every {@link #getPeriod() period}. */
    private Runnable sendData = new Runnable() {
        @Override
        public void run() {
            count++;
            if (pattern) {
                byte[] patternBytes = String.format(patternFormat, getPatternCount()).getBytes(CHARSET);
                System.arraycopy(patternBytes, 0, data, data.length - DSPS_PATTERN_DIGITS - (DSPS_PATTERN_SUFFIX != null ? DSPS_PATTERN_SUFFIX.length : 0), DSPS_PATTERN_DIGITS);
            }
            if (CodelessLibLog.DSPS_PERIODIC_CHUNK)
                Log.d(TAG, manager.getLogPrefix() + "Queue periodic data (" + count + "): " + CodelessUtil.hexArrayLog(data));
            manager.sendData(DspsPeriodicSend.this);
            manager.getHandler().postDelayed(sendData, period);
        }
    };

    /**
     * Loads the pattern prefix from the selected file.
     * <p> If the pattern fails to load, a {@link CodelessEvent.DspsPatternFileError DspsPatternFileError} event is generated.
     * @param file  the selected {@link File}
     * @param uri   the selected file {@link Uri}, if scoped storage is used
     */
    private void loadPattern(File file, Uri uri) {
        if (CodelessLibLog.DSPS)
            Log.d(TAG, "Load pattern: " + (file != null ? file.getAbsolutePath() : uri.toString()));

        if (Build.VERSION.SDK_INT >= 23 && file != null && manager.getContext().checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Missing storage permission");
            EventBus.getDefault().post(new CodelessEvent.DspsPatternFileError(manager, this, file, uri));
            return;
        }

        DocumentFile documentFile = file == null ? DocumentFile.fromSingleUri(manager.getContext(), uri) : null;
        InputStream inputStream = null;
        byte[] pattern;
        try {
            inputStream = file != null ? new FileInputStream(file) : manager.getContext().getContentResolver().openInputStream(uri);
            pattern = new byte[Math.min((int) (file != null ? file.length() : documentFile.length()), chunkSize - DSPS_PATTERN_DIGITS - (DSPS_PATTERN_SUFFIX != null ? DSPS_PATTERN_SUFFIX.length : 0))];
            inputStream.read(pattern);
        } catch (IOException | SecurityException e) {
            Log.e(TAG, "Failed to load pattern: " + (file != null ? file.getAbsolutePath() : uri.toString()), e);
            pattern = null;
        } finally {
            try {
                if (inputStream != null)
                    inputStream.close();
            } catch (IOException e) {
                Log.e(TAG, "Failed to close pattern file: " + (file != null ? file.getAbsolutePath() : uri.toString()), e);
            }
        }

        if (pattern == null) {
            EventBus.getDefault().post(new CodelessEvent.DspsPatternFileError(manager, this, file, uri));
            return;
        }

        data = Arrays.copyOf(pattern, pattern.length + DSPS_PATTERN_DIGITS + (DSPS_PATTERN_SUFFIX != null ? DSPS_PATTERN_SUFFIX.length : 0));
        chunkSize = data.length;
        if (DSPS_PATTERN_SUFFIX != null)
            System.arraycopy(DSPS_PATTERN_SUFFIX, 0, data, data.length - DSPS_PATTERN_SUFFIX.length, DSPS_PATTERN_SUFFIX.length);
    }

    /** Checks if the pattern is loaded properly. */
    public boolean isLoaded() {
        return data != null;
    }
}
