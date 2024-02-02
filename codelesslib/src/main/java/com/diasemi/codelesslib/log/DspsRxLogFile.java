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

import android.util.Log;

import com.diasemi.codelesslib.CodelessLibConfig;
import com.diasemi.codelesslib.CodelessManager;
import com.diasemi.codelesslib.dsps.DspsFileReceive;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * DSPS log file.
 * <p>
 * Used by the library to log the binary DSPS data that are received from the peer device,
 * if log is {@link CodelessLibConfig#DSPS_RX_LOG enabled}.
 * Also used to create the output files for DSPS file receive operations.
 * @see CodelessManager
 */
public class DspsRxLogFile extends LogFileBase {
    private static final String TAG = "DspsRxLogFile";

    private OutputStream output;

    /**
     * Creates a DSPS log file.
     * @param manager the associated manager
     */
    public DspsRxLogFile(CodelessManager manager) {
        super(manager, CodelessLibConfig.DSPS_RX_LOG_FILE_PREFIX);
    }

    /**
     * Creates the output file for a DSPS file receive operation.
     * @param dspsFileReceive the DSPS file receive operation
     */
    public DspsRxLogFile(DspsFileReceive dspsFileReceive) {
        super(dspsFileReceive);
    }

    @Override
    protected String getTag() {
        return TAG;
    }

    /** Opens the log file for writing. */
    protected boolean create() {
        try {
            if (!CodelessLibConfig.SCOPED_STORAGE) {
                output = new FileOutputStream(file);
            } else {
                if (createDocumentFile(name))
                    output = context.getContentResolver().openOutputStream(documentFile.getUri(), WRITE_MODE);
            }
        } catch (IOException | SecurityException e) {
            Log.e(TAG, "Failed to create file: " + name, e);
            closed = true;
        }
        return !closed;
    }

    /**
     * Appends some binary data to the log file.
     * @param data the binary data to append
     */
    public void log(byte[] data) {
        if (closed)
            return;
        if (output == null && !create())
            return;
        try {
            output.write(data);
            if (CodelessLibConfig.DSPS_RX_LOG_FLUSH)
                output.flush();
        } catch (IOException e) {
            Log.e(TAG, "Write failed: " + name, e);
            closed = true;
        }
    }

    /** Closes the log file. */
    public void close() {
        try {
            if (output != null)
                output.close();
        } catch (IOException e) {
            Log.e(TAG, "Failed to close file: " + name, e);
        }
        closed = true;
    }
}
