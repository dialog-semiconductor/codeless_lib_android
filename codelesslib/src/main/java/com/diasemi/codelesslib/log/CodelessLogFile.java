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
import com.diasemi.codelesslib.CodelessProfile;

import java.io.IOException;
import java.io.PrintWriter;

/**
 * CodeLess log file.
 * <p>
 * Used by the library to log the CodeLess communication between the devices,
 * if log is {@link CodelessLibConfig#CODELESS_LOG enabled}.
 * @see CodelessManager
 */
public class CodelessLogFile extends LogFileBase {
    private static final String TAG = "CodelessLogFile";

    private PrintWriter writer;

    /**
     * Creates a CodeLess log file.
     * @param manager the associated manager
     */
    public CodelessLogFile(CodelessManager manager) {
        super(manager, CodelessLibConfig.CODELESS_LOG_FILE_PREFIX);
    }

    @Override
    protected String getTag() {
        return TAG;
    }

    /** Opens the log file for writing. */
    protected boolean create() {
        try {
            if (!CodelessLibConfig.SCOPED_STORAGE) {
                writer = new PrintWriter(file);
            } else {
                if (createDocumentFile(name))
                    writer = new PrintWriter(context.getContentResolver().openOutputStream(documentFile.getUri(), WRITE_MODE));
            }
        } catch (IOException | SecurityException e) {
            Log.e(TAG, "Failed to create file: " + name, e);
            closed = true;
        }
        return !closed;
    }

    /**
     * Appends a line to the log file.
     * @param line the line to append
     */
    public void log(String line) {
        if (closed)
            return;
        if (writer == null && !create())
            return;
        writer.println(line);
        if (CodelessLibConfig.CODELESS_LOG_FLUSH)
            writer.flush();
    }

    /**
     * Logs a {@link CodelessProfile.Line} using a different prefix for incoming and outgoing messages.
     * @param line the line to log
     */
    public void log(CodelessProfile.Line line) {
        log((line.getType().isOutbound() ? CodelessLibConfig.CODELESS_LOG_PREFIX_OUTBOUND : CodelessLibConfig.CODELESS_LOG_PREFIX_INBOUND) + line.getText());
    }

    /**
     * Logs some text.
     * @param text the text to log
     */
    public void logText(String text) {
        log(CodelessLibConfig.CODELESS_LOG_PREFIX_TEXT + text);
    }

    /** Closes the log file. */
    public void close() {
        if (writer != null)
            writer.close();
        closed = true;
    }
}
