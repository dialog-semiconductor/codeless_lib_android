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

/** Configuration options that configure the log output produced by the library. */
public class CodelessLibLog {

    /** Log GATT operations. */
    public static final boolean GATT_OPERATION = true;
    /** Log runtime permissions checks. */
    public static final boolean PERMISSION = true;

    /** Log CodeLess operations. */
    public static final boolean CODELESS = true;
    /** Log command specific messages. */
    public static final boolean COMMAND = true;
    /** Log CodeLess script operations. */
    public static final boolean SCRIPT = true;

    /** Log DSPS operations. Data operations are configured separately. */
    public static final boolean DSPS = true;
    /** Log sent/received DSPS data. */
    public static final boolean DSPS_DATA = true;
    /** Log sending of DSPS data chunks. */
    public static final boolean DSPS_CHUNK = true;
    /** Log queueing/sending/receiving of DSPS file chunks. */
    public static final boolean DSPS_FILE_CHUNK = true;
    /** Log queueing/sending of DSPS periodic/pattern chunks. */
    public static final boolean DSPS_PERIODIC_CHUNK = true;

}
