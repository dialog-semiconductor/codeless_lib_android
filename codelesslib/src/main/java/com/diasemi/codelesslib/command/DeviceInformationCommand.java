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

package com.diasemi.codelesslib.command;

import android.content.pm.PackageManager;
import android.util.Log;

import com.diasemi.codelesslib.CodelessEvent;
import com.diasemi.codelesslib.CodelessLibConfig;
import com.diasemi.codelesslib.CodelessLibLog;
import com.diasemi.codelesslib.CodelessManager;
import com.diasemi.codelesslib.CodelessProfile;
import com.diasemi.codelesslib.CodelessProfile.CommandID;

import org.greenrobot.eventbus.EventBus;

import java.util.regex.Pattern;

/**
 * <code>ATI</code> command implementation with incoming command support.
 * <p> When incoming, it responds with the app version string.
 * @see CodelessEvent.DeviceInformation
 * @see CodelessLibConfig#CODELESS_LIB_INFO
 * @see <a href="https://lpccs-docs.renesas.com/UM-140-DA145x-CodeLess/index.html">AT commands documentation</a>
 */
public class DeviceInformationCommand extends CodelessCommand {
    public static final String TAG = "DeviceInformationCommand";

    public static final String COMMAND = "I";
    public static final String NAME = CodelessProfile.PREFIX + COMMAND;
    public static final CommandID ID = CommandID.ATI;

    public static final String PATTERN_STRING = "^I$";
    public static final Pattern PATTERN = Pattern.compile(PATTERN_STRING);

    /** The information text response. */
    private String info;

    /**
     * Default constructor.
     * @see CodelessCommand#CodelessCommand(CodelessManager)
     */
    public DeviceInformationCommand(CodelessManager manager) {
        super(manager);
    }

    /**
     * Default constructor.
     * @see CodelessCommand#CodelessCommand(CodelessManager, String, boolean)
     */
    public DeviceInformationCommand(CodelessManager manager, String command, boolean parse) {
        super(manager, command, parse);
    }

    @Override
    protected String getTag() {
        return TAG;
    }

    @Override
    public String getID() {
        return COMMAND;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public CommandID getCommandID() {
        return ID;
    }

    @Override
    public Pattern getPattern() {
        return PATTERN;
    }

    @Override
    public void parseResponse(String response) {
        super.parseResponse(response);
        if (responseLine() == 1) {
            info = response;
            if (CodelessLibLog.COMMAND)
                Log.d(TAG, "Device info: " + info);
        }
    }

    @Override
    public void onSuccess() {
        super.onSuccess();
        EventBus.getDefault().post(new CodelessEvent.DeviceInformation(this));
    }

    @Override
    public void processInbound() {
        if (info == null) {
            if (CodelessLibConfig.CODELESS_LIB_INFO != null) {
                info = CodelessLibConfig.CODELESS_LIB_INFO;
            } else {
                info = "CodeLess Android";
                try {
                    info += " " + manager.getContext().getPackageManager().getPackageInfo(manager.getContext().getPackageName(), 0).versionName;
                } catch (PackageManager.NameNotFoundException e) {}
            }
        }
        if (CodelessLibLog.COMMAND)
            Log.d(TAG, "Send device info: " + info);
        sendSuccess(info);
    }

    /** Returns the information text response. */
    public String getInfo() {
        return info;
    }

    /** Sets the information text response. */
    public void setInfo(String info) {
        this.info = info;
    }
}
