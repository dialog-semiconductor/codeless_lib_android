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

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Build;
import android.util.Log;

import com.diasemi.codelesslib.CodelessEvent;
import com.diasemi.codelesslib.CodelessLibLog;
import com.diasemi.codelesslib.CodelessManager;
import com.diasemi.codelesslib.CodelessProfile;
import com.diasemi.codelesslib.CodelessProfile.CommandID;

import org.greenrobot.eventbus.EventBus;

import java.util.regex.Pattern;

/**
 * <code>AT+BATT</code> command implementation with incoming command support.
 * <p> When incoming, it responds with the host device battery level.
 * @see CodelessEvent.BatteryLevel
 * @see <a href="https://lpccs-docs.renesas.com/UM-140-DA145x-CodeLess/index.html">AT commands documentation</a>
 */
public class BatteryLevelCommand extends CodelessCommand {
    public static final String TAG = "BatteryLevelCommand";

    public static final String COMMAND = "BATT";
    public static final String NAME = CodelessProfile.PREFIX_LOCAL + COMMAND;
    public static final CommandID ID = CommandID.BATT;

    public static final String PATTERN_STRING = "^BATT$";
    public static final Pattern PATTERN = Pattern.compile(PATTERN_STRING);

    /** The battery level response. */
    private int level = -1;

    /**
     * Default constructor.
     * @see CodelessCommand#CodelessCommand(CodelessManager)
     */
    public BatteryLevelCommand(CodelessManager manager) {
        super(manager);
    }

    /**
     * Default constructor.
     * @see CodelessCommand#CodelessCommand(CodelessManager, String, boolean)
     */
    public BatteryLevelCommand(CodelessManager manager, String command, boolean parse) {
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
            try {
                level = Integer.parseInt(response);
                if (CodelessLibLog.COMMAND)
                    Log.d(TAG, "Battery level: " + level);
            } catch (NumberFormatException e) {
                Log.e(TAG, "Received invalid battery level: " + response, e);
                invalid = true;
            }
        }
    }

    @Override
    public void onSuccess() {
        super.onSuccess();
        if (isValid())
            EventBus.getDefault().post(new CodelessEvent.BatteryLevel(this));
    }

    @Override
    public void processInbound() {
        if (level == -1)
            level = getBatteryLevel();
        if (level != -1) {
            if (CodelessLibLog.COMMAND)
                Log.d(TAG, "Send battery level: " + level);
            sendSuccess(Integer.toString(level));
        } else {
            Log.e(TAG, "Failed to retrieve battery level");
            sendError("Battery level not available");
        }
    }

    /** Returns the host device battery level. */
    private int getBatteryLevel() {
        if (Build.VERSION.SDK_INT >= 21) {
            BatteryManager batteryManager = (BatteryManager) manager.getContext().getSystemService(Context.BATTERY_SERVICE);
            if (batteryManager != null) {
                int level = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
                if (level != Integer.MIN_VALUE)
                    return level;
            }
        }
        Intent status = manager.getContext().registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (status == null || !status.hasExtra(BatteryManager.EXTRA_LEVEL) || !status.hasExtra(BatteryManager.EXTRA_SCALE))
            return -1;
        int level = status.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = status.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        return 100 * level / scale;
    }

    /** Returns the battery level response. */
    public int getLevel() {
        return level;
    }

    /** Sets the battery level response. */
    public void setLevel(int level) {
        this.level = level;
    }
}
