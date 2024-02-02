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

import android.util.Log;

import com.diasemi.codelesslib.CodelessEvent;
import com.diasemi.codelesslib.CodelessLibLog;
import com.diasemi.codelesslib.CodelessManager;
import com.diasemi.codelesslib.CodelessProfile;
import com.diasemi.codelesslib.CodelessProfile.CommandID;

import org.greenrobot.eventbus.EventBus;

import java.util.Random;
import java.util.regex.Pattern;

/**
 * <code>AT+RANDOM</code> command implementation with incoming command support.
 * <p> When incoming, it responds with a random 32-bit hex number.
 * @see CodelessEvent.RandomNumber
 * @see <a href="https://lpccs-docs.renesas.com/UM-140-DA145x-CodeLess/index.html">AT commands documentation</a>
 */
public class RandomNumberCommand extends CodelessCommand {
    public static final String TAG = "RandomNumberCommand";

    public static final String COMMAND = "RANDOM";
    public static final String NAME = CodelessProfile.PREFIX_LOCAL + COMMAND;
    public static final CommandID ID = CommandID.RANDOM;

    public static final String PATTERN_STRING = "^RANDOM$";
    public static final Pattern PATTERN = Pattern.compile(PATTERN_STRING);

    private static final Random random = new Random();

    /** The random number response. */
    private long number = Long.MIN_VALUE;

    /**
     * Default constructor.
     * @see CodelessCommand#CodelessCommand(CodelessManager)
     */
    public RandomNumberCommand(CodelessManager manager) {
        super(manager);
    }

    /**
     * Default constructor.
     * @see CodelessCommand#CodelessCommand(CodelessManager, String, boolean)
     */
    public RandomNumberCommand(CodelessManager manager, String command, boolean parse) {
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
                number = Long.decode(response);
                if (CodelessLibLog.COMMAND)
                    Log.d(TAG, "Random number: " + number);
            } catch (NumberFormatException e) {
                Log.e(TAG, "Received invalid random number: " + response, e);
                invalid = true;
            }
        }
    }

    @Override
    public void onSuccess() {
        super.onSuccess();
        if (isValid())
            EventBus.getDefault().post(new CodelessEvent.RandomNumber(this));
    }

    @Override
    public void processInbound() {
        if (number == Long.MIN_VALUE)
            initRandomNumber();
        if (CodelessLibLog.COMMAND)
            Log.d(TAG, "Send random number: " + number);
        sendSuccess(String.format("0x%08X", number));
    }

    /** Returns the random number response. */
    public long getNumber() {
        return number;
    }

    /** Sets the random number response. */
    public void setNumber(long number) {
        this.number = number;
    }

    /** Initializes the random number response. */
    public void initRandomNumber() {
        number = random.nextLong() & 0xffffffffL;
    }
}