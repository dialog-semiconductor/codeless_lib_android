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

package com.diasemi.codelesslib.log;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import com.diasemi.codelesslib.CodelessLibConfig;
import com.diasemi.codelesslib.CodelessManager;
import com.diasemi.codelesslib.dsps.DspsFileReceive;

import java.io.File;
import java.util.Date;

import androidx.documentfile.provider.DocumentFile;

/**
 * Base class for log files created by the library.
 * <p>
 * {@link CodelessLibConfig#SCOPED_STORAGE SCOPED_STORAGE} configures whether scoped storage or the {@link File} API is used.
 * @see CodelessManager
 */
public abstract class LogFileBase {
    private static final String TAG = "LogFileBase";

    /** The mode used when creating the log file. */
    public static final String WRITE_MODE = Build.VERSION.SDK_INT < 29 ? "w" : "rwt";

    /** The application context. */
    protected Context context;
    /** The file name. */
    protected String name;
    /** The associated {@link File} object, if {@link CodelessLibConfig#SCOPED_STORAGE scoped storage} is disabled. */
    protected File file;
    /** The associated {@link DocumentFile} object, if {@link CodelessLibConfig#SCOPED_STORAGE scoped storage} is enabled. */
    protected DocumentFile documentFile;
    /** <code>true</code> if the file has been closed. */
    protected boolean closed;

    /**
     * Creates a log file.
     * @param manager   the associated manager
     * @param prefix    the file name prefix
     */
    public LogFileBase(CodelessManager manager, String prefix) {
        context = manager.getContext();
        name = prefix + CodelessLibConfig.LOG_FILE_DATE.format(new Date());
        if (CodelessLibConfig.LOG_FILE_ADDRESS_SUFFIX)
            name += "_" + manager.getDevice().getAddress().replaceAll(":", "");
        name += CodelessLibConfig.LOG_FILE_EXTENSION;
        initialize(CodelessLibConfig.LOG_FILE_PATH, CodelessLibConfig.LOG_FILE_PATH_NAME);
    }

    /**
     * Creates a log file for a DSPS file receive operation.
     * @param dspsFileReceive the DSPS file receive operation
     */
    public LogFileBase(DspsFileReceive dspsFileReceive) {
        context = dspsFileReceive.getManager().getContext();
        name = dspsFileReceive.getName();
        initialize(CodelessLibConfig.DSPS_RX_FILE_PATH, CodelessLibConfig.DSPS_RX_FILE_PATH_NAME);
    }

    /**
     * Initializes the log file path.
     * @param filePath the log file path, used if {@link CodelessLibConfig#SCOPED_STORAGE scoped storage} is disabled
     * @param pathName the log file path name, relative to {@link CodelessManager#getFilePath()},
     *                 used if {@link CodelessLibConfig#SCOPED_STORAGE scoped storage} is enabled.
     */
    private void initialize(String filePath, String pathName) {
        if (!CodelessLibConfig.SCOPED_STORAGE) {
            File path = new File(filePath);
            file = new File(path, name);
            createPath(path);
        } else {
            documentFile = CodelessManager.getFilePath();
            if (documentFile == null) {
                Log.e(getTag(), "No file path selected");
                closed = true;
            } else {
                createPath(pathName);
            }
        }
    }

    /**
     * Creates all directories in a path.
     * @param path the path to create
     */
    private void createPath(File path) {
        if (Build.VERSION.SDK_INT >= 23 && context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            Log.e(getTag(), "Missing storage permission");
            closed = true;
        } else {
            try {
                if (!path.exists() && !path.mkdirs())
                    closed = true;
            } catch (SecurityException e) {
                Log.e(getTag(), "Failed to create log path: " + path.getAbsolutePath());
                closed = true;
            }
        }
    }

    /**
     * Creates all directories in a path and sets {@link #documentFile} to the created path.
     * @param path the path to create
     */
    private void createPath(String path) {
        for (String folder : path.split("/")) {
            DocumentFile subFolder = documentFile.findFile(folder);
            documentFile = subFolder != null ? subFolder : documentFile.createDirectory(folder);
            if (documentFile == null || !documentFile.isDirectory()) {
                Log.e(getTag(), "Failed to create log path: " + path);
                closed = true;
                break;
            }
        }
    }

    /**
     * Creates a file in the path set in {@link #documentFile} and sets {@link #documentFile} to the created file.
     * @param name the file name to create
     * @return <code>true</code> if the file was created successfully
     */
    protected boolean createDocumentFile(String name) {
        DocumentFile file = documentFile.findFile(name);
        documentFile = file != null ? file : documentFile.createFile("*/*", name);
        if (documentFile == null || !documentFile.isFile()) {
            Log.e(TAG, "Failed to create file: " + name);
            closed = true;
            return false;
        }
        return true;
    }

    /** Returns the log tag used for log messages. */
    protected String getTag() {
        return TAG;
    }

    /** Returns the file name. */
    public String getName() {
        return name;
    }

    /** Returns the associated File object, if available. */
    public File getFile() {
        return file;
    }

    /** Checks if the file has been closed. */
    public boolean isClosed() {
        return closed;
    }
}
