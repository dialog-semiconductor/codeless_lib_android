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

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Date;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;

import static com.diasemi.codelesslib.CodelessManager.SPEED_INVALID;

/**
 * DSPS file send operation.
 * <h2>Usage</h2>
 * Use one of the {@link CodelessManager#sendFile(Uri, int, int) sendFile} methods to create and {@link #start() start}
 * a DSPS file send operation.
 * <p>
 * The file to send is selected using the {@link File} API or a file {@link Uri} for scoped storage.
 * The file is split into chunks based on the specified {@link #getChunkSize() chunk size}.
 * The chunk size must not exceed the value (MTU - 3), otherwise chunks will be truncated when sent.
 * The chunks are enqueued to be sent, one every the specified {@link #getPeriod() period}.
 * If the period is 0, all chunks are enqueued at once, which may be slower for large files.
 * <p>
 * If the file fails to load, a {@link CodelessEvent.DspsFileError DspsFileError} event is generated.
 * A {@link CodelessEvent.DspsFileChunk DspsFileChunk} event is generated for each chunk that is sent to the peer device.
 * Use {@link #stop()} to stop the operation. If {@link CodelessLibConfig#DSPS_STATS statistics} are enabled,
 * a {@link CodelessEvent.DspsStats DspsStats} event is generated every {@link CodelessLibConfig#DSPS_STATS_INTERVAL}.
 * @see CodelessManager
 */
public class DspsFileSend {
    private final static String TAG = "DspsFileSend";

    private CodelessManager manager;
    private File file;
    private Uri uri;
    private DocumentFile documentFile;
    private String fileName;
    private int chunkSize;
    private byte[][] chunks;
    private int chunk;
    private int sentChunks;
    private int totalChunks;
    private int period;
    private boolean started;
    private boolean complete;
    private long startTime;
    private long endTime;
    private int bytesSent;
    private long lastInterval;
    private int bytesSentInterval;
    private int currentSpeed = SPEED_INVALID;

    private DspsFileSend(CodelessManager manager, int chunkSize, int period) {
        this.manager = manager;
        this.chunkSize = Math.min(chunkSize, manager.getDspsChunkSize());
        this.period = period;
    }

    /**
     * Creates a DSPS file send operation.
     * @param manager   the associated manager
     * @param file      the file to send
     * @param chunkSize the chunk size to use when splitting the file
     * @param period    the chunks enqueueing period (ms).
     *                  Set to 0 to enqueue all chunks (may be slower for large files).
     */
    public DspsFileSend(CodelessManager manager, File file, int chunkSize, int period) {
        this(manager, chunkSize, period);
        this.file = file;
        fileName = file.getName();
        loadFile();
    }

    /**
     * Creates a DSPS file send operation, using the manager's chunk size.
     * @param manager   the associated manager
     * @param file      the file to send
     * @param period    the chunks enqueueing period (ms).
     *                  Set to 0 to enqueue all chunks (may be slower for large files).
     */
    public DspsFileSend(CodelessManager manager, File file, int period) {
        this(manager, file, manager.getDspsChunkSize(), period);
    }

    /**
     * Creates a DSPS file send operation, using the manager's chunk size.
     * <p> All chunks are enqueued at once (may be slower for large files).
     * @param manager   the associated manager
     * @param file      the file to send
     */
    public DspsFileSend(CodelessManager manager, File file) {
        this(manager, file, manager.getDspsChunkSize(), -1);
    }

    /**
     * Creates a DSPS file send operation.
     * @param manager   the associated manager
     * @param uri       the file to send
     * @param chunkSize the chunk size to use when splitting the file
     * @param period    the chunks enqueueing period (ms).
     *                  Set to 0 to enqueue all chunks (may be slower for large files).
     */
    public DspsFileSend(CodelessManager manager, Uri uri, int chunkSize, int period) {
        this(manager, chunkSize, period);
        this.uri = uri;
        documentFile = DocumentFile.fromSingleUri(manager.getContext(), uri);
        fileName = documentFile.getName();
        loadFile();
    }

    /**
     * Creates a DSPS file send operation, using the manager's chunk size.
     * @param manager   the associated manager
     * @param uri       the file to send
     * @param period    the chunks enqueueing period (ms).
     *                  Set to 0 to enqueue all chunks (may be slower for large files).
     */
    public DspsFileSend(CodelessManager manager, Uri uri, int period) {
        this(manager, uri, manager.getDspsChunkSize(), period);
    }

    /**
     * Creates a DSPS file send operation, using the manager's chunk size.
     * <p> All chunks are enqueued at once (may be slower for large files).
     * @param manager   the associated manager
     * @param uri       the file to send
     */
    public DspsFileSend(CodelessManager manager, Uri uri) {
        this(manager, uri, manager.getDspsChunkSize(), -1);
    }

    /** Returns the associated manager. */
    public CodelessManager getManager() {
        return manager;
    }

    /** Returns the {@link File} to send, if available. */
    public File getFile() {
        return file;
    }

    /** Returns the {@link Uri} of the file to send, if available. */
    public Uri getUri() {
        return uri;
    }

    /**
     * Returns the {@link DocumentFile} to send, if available.
     * <p> Initialized from the specified file URI.
     */
    public DocumentFile getDocumentFile() {
        return documentFile;
    }

    /** Returns the file name. */
    public String getFileName() {
        return fileName;
    }

    /** Returns the chunk size. */
    public int getChunkSize() {
        return chunkSize;
    }

    /** Returns the file chunks. */
    public byte[][] getChunks() {
        return chunks;
    }

    /** Returns the current chunk. */
    public byte[] getCurrentChunk() {
        return chunks[chunk];
    }

    /**
     * Returns the current chunk index (0-based).
     * <p> Current chunk is the last chunk that was enqueued.
     */
    public int getChunk() {
        return chunk;
    }

    /**
     * Sets the current chunk index (0-based).
     * @param chunk the chunk index
     */
    public void setChunk(int chunk) {
        this.chunk = chunk;
    }

    /**
     * Sets the chunk index (0-based) from which the operation will resume.
     * <p> Used by the library to resume the operation after it was paused.
     * @param chunk the resume chunk index
     */
    public void setResumeChunk(int chunk) {
        setChunk(period > 0 ? Math.min(chunk - 1, this.chunk) : chunk);
    }

    /** Returns the number of sent chunks. */
    public int getSentChunks() {
        return sentChunks;
    }

    /**
     * Sets the number of sent chunks.
     * <p> Called by the library when a chunk is sent to the peer device.
     * @param sentChunks the number of sent chunks
     */
    public void setSentChunks(int sentChunks) {
        this.sentChunks = sentChunks;
    }

    /** The total number of chunks. */
    public int getTotalChunks() {
        return totalChunks;
    }

    /** Returns the file send operation period (ms). */
    public int getPeriod() {
        return period;
    }

    /** Checks if the operation has started. */
    public boolean isStarted() {
        return started;
    }

    /** Checks if the operation is complete. */
    public boolean isComplete() {
        return complete;
    }

    /**
     * Completes the file send operation.
     * <p> Called by the library when the last chunk is sent to the peer device.
     */
    public void setComplete() {
        complete = true;
        endTime = new Date().getTime();
        if (CodelessLibConfig.DSPS_STATS) {
            synchronized (this) {
                manager.getDspsStatsHandler().removeCallbacks(updateStats);
                EventBus.getDefault().post(new CodelessEvent.DspsStats(manager, this, currentSpeed, getAverageSpeed()));
            }
        }
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
     * Returns the calculated average speed for the duration of the file send operation.
     * <p> Available only if {@link CodelessLibConfig#DSPS_STATS statistics} are enabled.
     */
    public int getAverageSpeed() {
        long elapsed = (complete ? endTime : new Date().getTime()) - startTime;
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
            if (complete)
                return;
            synchronized (DspsFileSend.this) {
                long now = new Date().getTime();
                if (now == lastInterval)
                    now++;
                currentSpeed = (int) (bytesSentInterval * 1000L / (now - lastInterval));
                lastInterval = now;
                bytesSentInterval = 0;
                manager.getDspsStatsHandler().postDelayed(this, CodelessLibConfig.DSPS_STATS_INTERVAL);
                EventBus.getDefault().post(new CodelessEvent.DspsStats(manager, DspsFileSend.this, currentSpeed, getAverageSpeed()));
            }
        }
    };

    /**
     * Loads the selected file and splits its data into chunks.
     * <p> If the file fails to load, a {@link CodelessEvent.DspsFileError DspsFileError} event is generated.
     */
    private void loadFile() {
        if (CodelessLibLog.DSPS)
            Log.d(TAG, "Load file: " + (file != null ? file.getAbsolutePath() : uri.toString()));

        if (Build.VERSION.SDK_INT >= 23 && file != null && manager.getContext().checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Missing storage permission");
            EventBus.getDefault().post(new CodelessEvent.DspsFileError(manager, this));
            return;
        }

        InputStream inputStream = null;
        byte[] data;
        try {
            inputStream = file != null ? new FileInputStream(file) : manager.getContext().getContentResolver().openInputStream(uri);
            data = new byte[(int) (file != null ? file.length() : documentFile.length())];
            inputStream.read(data);
        } catch (IOException | SecurityException e) {
            Log.e(TAG, "Failed to load file: " + (file != null ? file.getAbsolutePath() : uri.toString()), e);
            data = null;
        } finally {
            try {
                if (inputStream != null)
                    inputStream.close();
            } catch (IOException e) {
                Log.e(TAG, "Failed to close file: " + (file != null ? file.getAbsolutePath() : uri.toString()), e);
            }
        }

        if (data == null || data.length == 0) {
            EventBus.getDefault().post(new CodelessEvent.DspsFileError(manager, this));
            return;
        }

        totalChunks = data.length / chunkSize + (data.length % chunkSize != 0 ? 1 : 0);
        chunks = new byte[totalChunks][];
        for (int i = 0; i < data.length; i += chunkSize) {
            chunks[i / chunkSize] = Arrays.copyOfRange(data, i, Math.min(i + chunkSize, data.length));
        }
    }

    /** Checks if the file is loaded properly. */
    public boolean isLoaded() {
        return chunks != null;
    }

    /**
     * Starts the file send operation.
     * @see CodelessManager#sendFile(File, int, int)
     * @see CodelessManager#sendFile(Uri, int, int)
     */
    public void start() {
        if (started)
            return;
        started = true;
        if (CodelessLibLog.DSPS)
            Log.d(TAG, manager.getLogPrefix() + "Start file send: " + this);
        chunk = -1;
        startTime = new Date().getTime();
        if (CodelessLibConfig.DSPS_STATS) {
            lastInterval = startTime;
            manager.getDspsStatsHandler().postDelayed(updateStats, CodelessLibConfig.DSPS_STATS_INTERVAL);
        }
        manager.start(this, false);
    }

    /** Stops the file send operation. */
    public void stop() {
        if (CodelessLibLog.DSPS)
            Log.d(TAG, manager.getLogPrefix() + "Stop file send: " + this);
        endTime = new Date().getTime();
        if (CodelessLibConfig.DSPS_STATS) {
            manager.getDspsStatsHandler().removeCallbacks(updateStats);
        }
        manager.stop(this);
    }

    /** Returns the send chunk runnable, which enqueues the next chunk for sending. */
    public Runnable getRunnable() {
        return sendChunk;
    }

    /** Enqueues the next file chunk for sending, called every {@link #getPeriod() period}. */
    private Runnable sendChunk = new Runnable() {
        @Override
        public void run() {
            chunk++;
            if (CodelessLibLog.DSPS_FILE_CHUNK)
                Log.d(TAG, manager.getLogPrefix() + "Queue file chunk: " + DspsFileSend.this + " " + (chunk + 1) + " of " + totalChunks);
            manager.sendData(DspsFileSend.this);
            if (chunk < totalChunks - 1)
                manager.getHandler().postDelayed(sendChunk, period);
        }
    };

    @NonNull
    @Override
    public String toString() {
        return getFileName();
    }
}
