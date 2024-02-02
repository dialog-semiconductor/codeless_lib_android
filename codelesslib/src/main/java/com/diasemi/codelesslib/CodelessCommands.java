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

import com.diasemi.codelesslib.CodelessProfile.BondingEntry;
import com.diasemi.codelesslib.CodelessProfile.GPIO;
import com.diasemi.codelesslib.command.AdcReadCommand;
import com.diasemi.codelesslib.command.AdvertisingDataCommand;
import com.diasemi.codelesslib.command.AdvertisingResponseCommand;
import com.diasemi.codelesslib.command.BasicCommand;
import com.diasemi.codelesslib.command.BatteryLevelCommand;
import com.diasemi.codelesslib.command.BaudRateCommand;
import com.diasemi.codelesslib.command.BinExitAckCommand;
import com.diasemi.codelesslib.command.BinExitCommand;
import com.diasemi.codelesslib.command.BinRequestAckCommand;
import com.diasemi.codelesslib.command.BinRequestCommand;
import com.diasemi.codelesslib.command.BluetoothAddressCommand;
import com.diasemi.codelesslib.command.BondingEntryClearCommand;
import com.diasemi.codelesslib.command.BondingEntryStatusCommand;
import com.diasemi.codelesslib.command.BondingEntryTransferCommand;
import com.diasemi.codelesslib.command.CmdGetCommand;
import com.diasemi.codelesslib.command.CmdPlayCommand;
import com.diasemi.codelesslib.command.CmdStoreCommand;
import com.diasemi.codelesslib.command.CodelessCommand;
import com.diasemi.codelesslib.command.ConnectionParametersCommand;
import com.diasemi.codelesslib.command.CursorCommand;
import com.diasemi.codelesslib.command.DataLengthEnableCommand;
import com.diasemi.codelesslib.command.DeviceInformationCommand;
import com.diasemi.codelesslib.command.DeviceSleepCommand;
import com.diasemi.codelesslib.command.ErrorReportingCommand;
import com.diasemi.codelesslib.command.EventConfigCommand;
import com.diasemi.codelesslib.command.EventHandlerCommand;
import com.diasemi.codelesslib.command.FlowControlCommand;
import com.diasemi.codelesslib.command.HeartbeatCommand;
import com.diasemi.codelesslib.command.HostSleepCommand;
import com.diasemi.codelesslib.command.I2cConfigCommand;
import com.diasemi.codelesslib.command.I2cReadCommand;
import com.diasemi.codelesslib.command.I2cScanCommand;
import com.diasemi.codelesslib.command.I2cWriteCommand;
import com.diasemi.codelesslib.command.IoConfigCommand;
import com.diasemi.codelesslib.command.IoStatusCommand;
import com.diasemi.codelesslib.command.MaxMtuCommand;
import com.diasemi.codelesslib.command.MemStoreCommand;
import com.diasemi.codelesslib.command.PinCodeCommand;
import com.diasemi.codelesslib.command.PowerLevelConfigCommand;
import com.diasemi.codelesslib.command.PulseGenerationCommand;
import com.diasemi.codelesslib.command.RandomNumberCommand;
import com.diasemi.codelesslib.command.ResetCommand;
import com.diasemi.codelesslib.command.ResetIoConfigCommand;
import com.diasemi.codelesslib.command.RssiCommand;
import com.diasemi.codelesslib.command.SecurityModeCommand;
import com.diasemi.codelesslib.command.SpiConfigCommand;
import com.diasemi.codelesslib.command.SpiReadCommand;
import com.diasemi.codelesslib.command.SpiTransferCommand;
import com.diasemi.codelesslib.command.SpiWriteCommand;
import com.diasemi.codelesslib.command.TimerStartCommand;
import com.diasemi.codelesslib.command.TimerStopCommand;
import com.diasemi.codelesslib.command.UartEchoCommand;
import com.diasemi.codelesslib.command.UartPrintCommand;

/**
 * Contains helper methods for sending various CodeLess AT commands to the peer device.
 * <p>
 * Each method creates the relevant {@link CodelessCommand command} subclass object,
 * sends the command to the peer device, and returns the command object. If the command
 * completes successfully, a {@link CodelessEvent command specific event} may be generated.
 * @see CodelessManager#getCommandFactory()
 * @see <a href="https://lpccs-docs.renesas.com/UM-140-DA145x-CodeLess/index.html">AT commands documentation</a>
 */
@SuppressWarnings("UnusedReturnValue")
public class CodelessCommands {

    /** The {@link CodelessManager manager} associated with the peer device. */
    private CodelessManager manager;

    /**
     * @param manager the {@link CodelessManager manager} associated with the peer device
     */
    public CodelessCommands(CodelessManager manager) {
        this.manager = manager;
    }

    /** Returns the {@link CodelessManager manager} associated with the peer device. */
    public CodelessManager getManager() {
        return manager;
    }

    /** Sends the AT command represented by the provided command object to the peer device. */
    private <T extends CodelessCommand> T sendCommand(T command) {
        command.setOrigin(this);
        manager.sendCommand(command);
        return command;
    }

    /**
     * Sends the <code>AT+</code> command.
     * <p> On success, a {@link CodelessEvent.Ping Ping} event is generated.
     */
    public BasicCommand ping() {
        return sendCommand(new BasicCommand(manager));
    }

    /**
     * Sends the <code>AT+I</code> command to get the peer device information.
     * <p> On success, a {@link CodelessEvent.DeviceInformation DeviceInformation} event is generated.
     */
    public DeviceInformationCommand getDeviceInfo() {
        return sendCommand(new DeviceInformationCommand(manager));
    }

    /** Sends the <code>AT+R</code> command to reset the peer device. */
    public ResetCommand resetDevice() {
        return sendCommand(new ResetCommand(manager));
    }

    /**
     * Sends the <code>AT+BDADDR</code> command to get the Bluetooth address of the peer device.
     * <p> On success, a {@link CodelessEvent.BluetoothAddress BluetoothAddress} event is generated.
     */
    public BluetoothAddressCommand getBluetoothAddress() {
        return sendCommand(new BluetoothAddressCommand(manager));
    }

    /**
     * Sends the <code>AT+RSSI</code> command to get the connection RSSI measured by the peer device.
     * <p> On success, a {@link CodelessEvent.PeerRssi PeerRssi} event is generated.
     */
    public RssiCommand getPeerRssi() {
        return sendCommand(new RssiCommand(manager));
    }

    /**
     * Sends the <code>AT+BATT</code> command to get the battery level of the peer device.
     * <p> On success, a {@link CodelessEvent.BatteryLevel BatteryLevel} event is generated.
     */
    public BatteryLevelCommand getBatteryLevel() {
        return sendCommand(new BatteryLevelCommand(manager));
    }

    /**
     * Sends the <code>AT+RANDOM</code> command to get a random value from the peer device.
     * <p> On success, a {@link CodelessEvent.RandomNumber RandomNumber} event is generated.
     */
    public RandomNumberCommand getRandomNumber() {
        return sendCommand(new RandomNumberCommand(manager));
    }

    /**
     * Sends the <code>AT+BINREQ</code> command to request switching to binary (DSPS) mode.
     * <p> On success, {@link CodelessManager#onBinRequestSent()} is called.
     */
    public BinRequestCommand requestBinaryMode() {
        return sendCommand(new BinRequestCommand(manager));
    }

    /**
     * Sends the <code>AT+BINREQACK</code> command to accept the peer request to switch to binary (DSPS) mode.
     * <p> On success, {@link CodelessManager#onBinAckSent()} is called.
     */
    public BinRequestAckCommand sendBinaryRequestAck() {
        return sendCommand(new BinRequestAckCommand(manager));
    }

    /**
     * Sends the <code>AT+BINREQEXIT</code> command to request switching to command (CodeLess) mode.
     * <p> On success, {@link CodelessManager#onBinExitSent()} is called.
     */
    public BinExitCommand sendBinaryExit() {
        return sendCommand(new BinExitCommand(manager));
    }

    /**
     * Sends the <code>AT+BINREQEXITACK</code> command to accept the peer request to switch to command (CodeLess) mode.
     * <p> On success, {@link CodelessManager#onBinExitAckSent()} is called.
     */
    public BinExitAckCommand sendBinaryExitAck() {
        return sendCommand(new BinExitAckCommand(manager));
    }

    /**
     * Sends the <code>AT+CONPAR</code> command to get the current connection parameters.
     * <p> On success, a {@link CodelessEvent.ConnectionParameters ConnectionParameters} event is generated.
     */
    public ConnectionParametersCommand getConnectionParameters() {
        return sendCommand(new ConnectionParametersCommand(manager));
    }

    /**
     * Sends the <code>AT+CONPAR</code> command to set the connection parameters.
     * <p> On success, a {@link CodelessEvent.ConnectionParameters ConnectionParameters} event is generated.
     * @param connectionInterval    the connection interval in multiples of 1.25 ms
     * @param slaveLatency          the slave latency
     * @param supervisionTimeout    the supervision timeout in multiples of 10 ms
     * @param action                specify how to apply the new connection parameters
     */
    public ConnectionParametersCommand setConnectionParameters(int connectionInterval, int slaveLatency, int supervisionTimeout, int action) {
        return sendCommand(new ConnectionParametersCommand(manager, connectionInterval, slaveLatency, supervisionTimeout, action));
    }

    /**
     * Sends the <code>AT+MAXMTU</code> command to get the current maximum MTU.
     * <p> On success, a {@link CodelessEvent.MaxMtu MaxMtu} event is generated.
     */
    public MaxMtuCommand getMaxMtu() {
        return sendCommand(new MaxMtuCommand(manager));
    }

    /**
     * Sends the <code>AT+MAXMTU</code> command to set the maximum MTU.
     * <p> On success, a {@link CodelessEvent.MaxMtu MaxMtu} event is generated.
     * @param mtu the MTU value
     */
    public MaxMtuCommand setMaxMtu(int mtu) {
        return sendCommand(new MaxMtuCommand(manager, mtu));
    }

    /**
     * Sends the <code>AT+DLEEN</code> command to get the DLE feature configuration.
     * <p> On success, a {@link CodelessEvent.DataLengthEnable DataLengthEnable} event is generated.
     */
    public DataLengthEnableCommand getDataLength() {
        return sendCommand(new DataLengthEnableCommand(manager));
    }

    /**
     * Sends the <code>AT+DLEEN</code> command to set the DLE feature configuration.
     * <p> On success, a {@link CodelessEvent.DataLengthEnable DataLengthEnable} event is generated.
     * @param enabled           enable/disable the DLE feature
     * @param txPacketLength    the DLE TX packet length
     * @param rxPacketLength    the DLE RX packet length
     */
    public DataLengthEnableCommand setDataLength(boolean enabled, int txPacketLength, int rxPacketLength) {
        return sendCommand(new DataLengthEnableCommand(manager, enabled, txPacketLength, rxPacketLength));
    }

    /**
     * Sends the <code>AT+DLEEN</code> command to enable/disable the DLE feature.
     * <p> Default values are used for TX/RX packet length.
     * @param enabled enable/disable the DLE feature
     * @see #setDataLength(boolean, int, int)
     */
    public DataLengthEnableCommand setDataLengthEnabled(boolean enabled) {
        return sendCommand(new DataLengthEnableCommand(manager, enabled));
    }

    /**
     * Sends the <code>AT+DLEEN</code> command to enable the DLE feature.
     * @see #setDataLengthEnabled(boolean)
     */
    public DataLengthEnableCommand enableDataLength() {
        return setDataLengthEnabled(true);
    }

    /**
     * Sends the <code>AT+DLEEN</code> command to disable the DLE feature.
     * @see #setDataLengthEnabled(boolean)
     */
    public DataLengthEnableCommand disableDataLength() {
        return setDataLengthEnabled(false);
    }

    /**
     * Sends the <code>AT+ADVDATA</code> command to get the advertising data configuration.
     * <p> On success, an {@link CodelessEvent.AdvertisingData AdvertisingData} event is generated.
     */
    public AdvertisingDataCommand getAdvertisingData() {
        return sendCommand(new AdvertisingDataCommand(manager));
    }

    /**
     * Sends the <code>AT+ADVDATA</code> command to set the advertising data configuration.
     * <p> On success, an {@link CodelessEvent.AdvertisingData AdvertisingData} event is generated.
     * @param data the advertising data byte array
     */
    public AdvertisingDataCommand setAdvertisingData(byte[] data) {
        return sendCommand(new AdvertisingDataCommand(manager, data));
    }

    /**
     * Sends the <code>AT+ADVRESP</code> command to get the scan response data configuration.
     * <p> On success, a {@link CodelessEvent.ScanResponseData ScanResponseData} event is generated.
     */
    public AdvertisingResponseCommand getScanResponseData() {
        return sendCommand(new AdvertisingResponseCommand(manager));
    }

    /**
     * Sends the <code>AT+ADVRESP</code> command to set the scan response data configuration.
     * <p> On success, a {@link CodelessEvent.ScanResponseData ScanResponseData} event is generated.
     * @param data the scan response data byte array
     */
    public AdvertisingResponseCommand setScanResponseData(byte[] data) {
        return sendCommand(new AdvertisingResponseCommand(manager, data));
    }

    /**
     * Sends the <code>AT+IOCFG</code> command to get the IO pin configuration.
     * <p> On success, an {@link CodelessEvent.IoConfig IoConfig} event is generated.
     */
    public IoConfigCommand readIoConfig() {
        return sendCommand(new IoConfigCommand(manager));
    }

    /**
     * Sends the <code>AT+Z</code> command to reset the IO pin configuration to the default values.
     * <p> On success, the app should {@link #readIoConfig() read the new configuration}.
     */
    public ResetIoConfigCommand resetIoConfig() {
        return sendCommand(new ResetIoConfigCommand(manager));
    }

    /**
     * Sends the <code>AT+IOCFG</code> command to set the functionality of an IO pin.
     * <p> On success, an {@link CodelessEvent.IoConfigSet IoConfigSet} event is generated.
     * @param gpio {@link CodelessProfile.GPIO GPIO} that specifies the selected pin and IO functionality, and, optionally, the initial level
     */
    public IoConfigCommand setIoConfig(GPIO gpio) {
        return sendCommand(new IoConfigCommand(manager, gpio));
    }

    /**
     * Sends the <code>AT+IO</code> command to read the binary status of an input pin.
     * <p> On success, an {@link CodelessEvent.IoStatus IoStatus} event is generated.
     * @param gpio {@link CodelessProfile.GPIO GPIO} that selects the input pin
     */
    public IoStatusCommand readInput(GPIO gpio) {
        return sendCommand(new IoStatusCommand(manager, gpio));
    }

    /**
     * Sends the <code>AT+IO</code> command to set the status of an output pin.
     * <p> On success, an {@link CodelessEvent.IoStatus IoStatus} event is generated.
     * @param gpio      {@link CodelessProfile.GPIO GPIO} that selects the output pin
     * @param status    <code>true</code> for high, <code>false</code> for low.
     */
    public IoStatusCommand setOutput(GPIO gpio, boolean status) {
        return sendCommand(new IoStatusCommand(manager, gpio, status));
    }

    /**
     * Sends the <code>AT+IO</code> command to set the status of an output pin to low.
     * @param gpio {@link CodelessProfile.GPIO GPIO} that selects the output pin
     * @see #setOutput(GPIO, boolean)
     */
    public IoStatusCommand setOutputLow(GPIO gpio) {
        return setOutput(gpio, false);
    }

    /**
     * Sends the <code>AT+IO</code> command to set the status of an output pin to high.
     * @param gpio {@link CodelessProfile.GPIO GPIO} that selects the output pin
     * @see #setOutput(GPIO, boolean)
     */
    public IoStatusCommand setOutputHigh(GPIO gpio) {
        return setOutput(gpio, true);
    }

    /**
     * Sends the <code>AT+ADC</code> command to read the analog state of an input pin.
     * <p> On success, an {@link CodelessEvent.AnalogRead AnalogRead} event is generated.
     * @param gpio {@link CodelessProfile.GPIO GPIO} that selects the input pin
     */
    public AdcReadCommand readAnalogInput(GPIO gpio) {
        return sendCommand(new AdcReadCommand(manager, gpio));
    }

    /**
     * Sends the <code>AT+PWM</code> command to get the PWM configuration.
     * <p> On success, an {@link CodelessEvent.PwmStatus PwmStatus} event is generated.
     */
    public PulseGenerationCommand getPwm() {
        return sendCommand(new PulseGenerationCommand(manager));
    }

    /**
     * Sends the <code>AT+PWM</code> command to generate a PWM pulse with the specified configuration.
     * <p> On success, a {@link CodelessEvent.PwmStart PwmStart} event is generated.
     * @param frequency     the frequency of the pulse in Hz
     * @param dutyCycle     the duty cycle of the pulse
     * @param duration      the duration of the pulse in ms
     */
    public PulseGenerationCommand setPwm(int frequency, int dutyCycle, int duration) {
        return sendCommand(new PulseGenerationCommand(manager, frequency, dutyCycle, duration));
    }

    /**
     * Sends the <code>AT+I2CCFG</code> command to configure the I2C bus.
     * <p> On success, an {@link CodelessEvent.I2cConfig I2cConfig} event is generated.
     * @param addressSize   the I2C address bit-count
     * @param bitrate       the I2C bus bitrate in KHz
     * @param registerSize  the I2C register bit-count
     */
    public I2cConfigCommand setI2cConfig(int addressSize, int bitrate, int registerSize) {
        return sendCommand(new I2cConfigCommand(manager, addressSize, bitrate, registerSize));
    }

    /**
     * Sends the <code>AT+I2CSCAN</code> command to scan the I2C bus for devices.
     * <p> On success, an {@link CodelessEvent.I2cScan I2cScan} event is generated.
     */
    public I2cScanCommand i2cScan() {
        return sendCommand(new I2cScanCommand(manager));
    }

    /**
     * Sends the <code>AT+I2CREAD</code> command to read the value of an I2C register.
     * <p> On success, an {@link CodelessEvent.I2cRead I2cRead} event is generated.
     * @param address   the I2C address
     * @param register  the register to read
     */
    public I2cReadCommand i2cRead(int address, int register) {
        return sendCommand(new I2cReadCommand(manager, address, register));
    }

    /**
     * Sends the <code>AT+I2CREAD</code> command to read one or more bytes starting from the specified I2C register.
     * <p> On success, an {@link CodelessEvent.I2cRead I2cRead} event is generated.
     * @param address   the I2C address
     * @param register  the register to read
     * @param count     the number of bytes to read
     */
    public I2cReadCommand i2cRead(int address, int register, int count) {
        return sendCommand(new I2cReadCommand(manager, address, register, count));
    }

    /**
     * Sends the <code>AT+I2CWRITE</code> command to write a byte value to an I2C register.
     * @param address   the I2C address
     * @param register  the register to write
     * @param value     the value to write
     */
    public I2cWriteCommand i2cWrite(int address, int register, int value) {
        return sendCommand(new I2cWriteCommand(manager, address, register, value));
    }

    /**
     * Sends the <code>AT+SPICFG</code> command to get the SPI configuration.
     * <p> On success, a {@link CodelessEvent.SpiConfig SpiConfig} event is generated.
     */
    public SpiConfigCommand readSpiConfig() {
        return sendCommand(new SpiConfigCommand(manager));
    }

    /**
     * Sends the <code>AT+SPICFG</code> command to set the SPI configuration.
     * <p> On success, a {@link CodelessEvent.SpiConfig SpiConfig} event is generated.
     * @param speed     the SPI clock value (0: 2 MHz, 1: 4 MHz, 2: 8 MHz)
     * @param mode      the SPI mode (clock polarity and phase)
     * @param size      the SPI word bit-count
     */
    public SpiConfigCommand setSpiConfig(int speed, int mode, int size) {
        return sendCommand(new SpiConfigCommand(manager, speed, mode, size));
    }

    /**
     * Sends the <code>AT+SPIWR</code> command to write a byte array value to the attached SPI device.
     * @param hexString the byte array value to write as a hex string
     */
    public SpiWriteCommand spiWrite(String hexString) {
        return sendCommand(new SpiWriteCommand(manager, hexString));
    }

    /**
     * Sends the <code>AT+SPIRD</code> command to read one or more bytes from the attached SPI device.
     * <p> On success, a {@link CodelessEvent.SpiRead SpiRead} event is generated.
     * @param count the number of bytes to read
     */
    public SpiReadCommand spiRead(int count) {
        return sendCommand(new SpiReadCommand(manager, count));
    }

    /**
     * Sends the <code>AT+SPITR</code> command to write a byte array value to the attached SPI device while reading the response.
     * <p> On success, a {@link CodelessEvent.SpiTransfer SpiTransfer} event is generated.
     * @param hexString the byte array value to write as a hex string
     */
    public SpiTransferCommand spiTransfer(String hexString) {
        return sendCommand(new SpiTransferCommand(manager, hexString));
    }

    /**
     * Sends the <code>AT+PRINT</code> command to print some text to the UART of the peer device.
     * @param text the text to print
     */
    public UartPrintCommand print(String text) {
        return sendCommand(new UartPrintCommand(manager, text));
    }

    /**
     * Sends the <code>AT+MEM</code> command to store text data in a memory slot.
     * <p> On success, a {@link CodelessEvent.MemoryTextContent MemoryTextContent} event is generated.
     * @param index     the memory slot index (0-3)
     * @param content   the text to store
     */
    public MemStoreCommand setMemContent(int index, String content) {
        return sendCommand(new MemStoreCommand(manager, index, content));
    }

    /**
     * Sends the <code>AT+MEM</code> command to get the text data stored in a memory slot.
     * <p> On success, a {@link CodelessEvent.MemoryTextContent MemoryTextContent} event is generated.
     * @param index the memory slot index (0-3)
     */
    public MemStoreCommand getMemContent(int index) {
        return sendCommand(new MemStoreCommand(manager, index));
    }

    /**
     * Sends the <code>AT+RANDOM</code> command to get a random value from the peer device.
     * <p> On success, a {@link CodelessEvent.RandomNumber RandomNumber} event is generated.
     */
    public RandomNumberCommand getRandom() {
        return sendCommand(new RandomNumberCommand(manager));
    }

    /**
     * Sends the <code>AT+CMD</code> command to get the list of the stored commands in a command slot.
     * <p> On success, a {@link CodelessEvent.StoredCommands StoredCommands} event is generated.
     * @param index the command slot index (0-3)
     */
    public CmdGetCommand getStoredCommands(int index) {
        return sendCommand(new CmdGetCommand(manager, index));
    }

    /**
     * Sends the <code>AT+CMDSTORE</code> command to store a list of commands in a command slot.
     * @param index             the command slot index (0-3)
     * @param commandString     the commands to store (semicolon separated)
     */
    public CmdStoreCommand storeCommands(int index, String commandString) {
        return sendCommand(new CmdStoreCommand(manager, index, commandString));
    }

    /**
     * Sends the <code>AT+CMDPLAY</code> command to execute the list of the stored commands in a command slot.
     * @param index the command slot index to execute (0-3)
     */
    public CmdPlayCommand playCommands(int index) {
        return sendCommand(new CmdPlayCommand(manager, index));
    }

    /**
     * Sends the <code>AT+TMRSTART</code> command to start a timer that will trigger the execution of a list of stored commands.
     * @param timerIndex    the timer index to start (0-3)
     * @param commandIndex  the command slot index to execute when the timer expires (0-3)
     * @param delay         the timer delay in multiples of 10 ms
     */
    public TimerStartCommand startTimer(int timerIndex, int commandIndex, int delay) {
        return sendCommand(new TimerStartCommand(manager, timerIndex, commandIndex, delay));
    }

    /**
     * Sends the <code>AT+TMRSTOP</code> command to stop a timer if it is still running.
     * @param timerIndex the timer index to stop (0-3)
     */
    public TimerStopCommand stopTimer(int timerIndex) {
        return sendCommand(new TimerStopCommand(manager, timerIndex));
    }

    /**
     * Sends the <code>AT+EVENT</code> command to activate or deactivate one of the predefined events.
     * <p> On success, an {@link CodelessEvent.EventStatus EventStatus} event is generated.
     * @param eventType     the event type (1: initialization, 2: connection, 3: disconnection, 4: wakeup)
     * @param status        <code>true</code> to activate the event, <code>false</code> to deactivate it
     */
    public EventConfigCommand setEventConfig(int eventType, boolean status) {
        return sendCommand(new EventConfigCommand(manager, eventType, status));
    }

    /**
     * Sends the <code>AT+EVENT</code> command to get the activation status of the predefined events.
     * <p> On success, an {@link CodelessEvent.EventStatusTable EventStatusTable} event is generated.
     */
    public EventConfigCommand getEventConfigTable() {
        return sendCommand(new EventConfigCommand(manager));
    }

    /**
     * Sends the <code>AT+HNDL</code> command to set the commands to be executed on one of the predefined events.
     * <p> On success, an {@link CodelessEvent.EventCommands EventCommands} event is generated.
     * @param eventType         the event type (1: connection, 2: disconnection, 3: wakeup)
     * @param commandString     the commands to be executed (semicolon separated)
     */
    public EventHandlerCommand setEventHandler(int eventType, String commandString) {
        return sendCommand(new EventHandlerCommand(manager, eventType, commandString));
    }

    /**
     * Sends the <code>AT+HNDL</code> command to get the commands to be executed on each of the predefined events.
     * <p> On success, an {@link CodelessEvent.EventCommandsTable EventCommandsTable} event is generated.
     */
    public EventHandlerCommand getEventHandlers() {
        return sendCommand(new EventHandlerCommand(manager));
    }

    /**
     * Sends the <code>AT+BAUD</code> command to get the UART baud rate.
     * <p> On success, a {@link CodelessEvent.BaudRate BaudRate} event is generated.
     */
    public BaudRateCommand getBaudRate() {
        return sendCommand(new BaudRateCommand(manager));
    }

    /**
     * Sends the <code>AT+BAUD</code> command to set the UART baud rate.
     * <p> On success, a {@link CodelessEvent.BaudRate BaudRate} event is generated.
     * @param baudRate the UART baud rate
     */
    public BaudRateCommand setBaudRate(int baudRate) {
        return sendCommand(new BaudRateCommand(manager, baudRate));
    }

    /**
     * Sends the <code>AT+E</code> command to get the UART echo state.
     * <p> On success, a {@link CodelessEvent.UartEcho UartEcho} event is generated.
     */
    public UartEchoCommand getUartEcho() {
        return sendCommand(new UartEchoCommand(manager));
    }

    /**
     * Sends the <code>AT+E</code> command to set the UART echo state.
     * <p> On success, a {@link CodelessEvent.UartEcho UartEcho} event is generated.
     * @param echo <code>true</code> for UART echo on, <code>false</code> for off
     */
    public UartEchoCommand setUartEcho(boolean echo) {
        return sendCommand(new UartEchoCommand(manager, echo));
    }

    /**
     * Sends the <code>AT+HRTBT</code> command to get the heartbeat signal status.
     * <p> On success, a {@link CodelessEvent.Heartbeat Heartbeat} event is generated.
     */
    public HeartbeatCommand getHeartbeatStatus() {
        return sendCommand(new HeartbeatCommand(manager));
    }

    /**
     * Sends the <code>AT+HRTBT</code> command to enable or disable the heartbeat signal.
     * <p> On success, a {@link CodelessEvent.Heartbeat Heartbeat} event is generated.
     * @param enable <code>true</code> to enable the heartbeat signal, <code>false</code> to disable it
     */
    public HeartbeatCommand setHeartbeatStatus(boolean enable) {
        return sendCommand(new HeartbeatCommand(manager, enable));
    }

    /**
     * Sends the <code>AT+F</code> command to enable or disable error reporting.
     * @param enable <code>true</code> to enable error reporting, <code>false</code> to disable it
     */
    public ErrorReportingCommand setErrorReporting(boolean enable) {
        return sendCommand(new ErrorReportingCommand(manager, enable));
    }

    /** Sends the <code>AT+CURSOR</code> command to place a time cursor in a SmartSnippets power profiler plot. */
    public CursorCommand timeCursor() {
        return sendCommand(new CursorCommand(manager));
    }

    /** Sends the <code>AT+SLEEP</code> command to instruct the peer device controller to enter sleep mode. */
    public DeviceSleepCommand sleep() {
        return sendCommand(new DeviceSleepCommand(manager, true));
    }

    /** Sends the <code>AT+SLEEP</code> command to instruct the peer device controller to disable sleep mode. */
    public DeviceSleepCommand awake() {
        return sendCommand(new DeviceSleepCommand(manager, false));
    }

    /**
     * Sends the <code>AT+HOSTSLP</code> command to get the peer device host sleep configuration.
     * <p> On success, a {@link CodelessEvent.HostSleep HostSleep} event is generated.
     */
    public HostSleepCommand getHostSleepStatus() {
        return sendCommand(new HostSleepCommand(manager));
    }

    /**
     * Sends the <code>AT+HOSTSLP</code> command to set the peer device host sleep configuration.
     * <p> On success, a {@link CodelessEvent.HostSleep HostSleep} event is generated.
     * @param hostSleepMode         the host sleep mode to use
     * @param wakeupByte            the byte value to use in order to wake up the host
     * @param wakeupRetryInterval   the interval between wakeup retries (ms)
     * @param wakeupRetryTimes      the number of wakeup retries
     */
    public HostSleepCommand setHostSleepStatus(int hostSleepMode, int wakeupByte, int wakeupRetryInterval, int wakeupRetryTimes) {
        return sendCommand(new HostSleepCommand(manager, hostSleepMode, wakeupByte, wakeupRetryInterval, wakeupRetryTimes));
    }

    /**
     * Sends the <code>AT+PWRLVL</code> command to get the peer device Bluetooth output power level.
     * <p> On success, a {@link CodelessEvent.PowerLevel PowerLevel} event is generated.
     */
    public PowerLevelConfigCommand getPowerLevel() {
        return sendCommand(new PowerLevelConfigCommand(manager));
    }

    /**
     * Sends the <code>AT+PWRLVL</code> command to set the peer device Bluetooth output power level.
     * <p> On success, a {@link CodelessEvent.PowerLevel PowerLevel} event is generated.
     * @param powerLevel the Bluetooth output power level {@link CodelessProfile.Command#OUTPUT_POWER_LEVEL_MINUS_19_POINT_5_DBM index}
     */
    public PowerLevelConfigCommand setPowerLevel(int powerLevel) {
        return sendCommand(new PowerLevelConfigCommand(manager, powerLevel));
    }

    /**
     * Sends the <code>AT+SEC</code> command to get the security mode configuration.
     * <p> On success, a {@link CodelessEvent.SecurityMode SecurityMode} event is generated.
     */
    public SecurityModeCommand getSecurityMode() {
        return sendCommand(new SecurityModeCommand(manager));
    }

    /**
     * Sends the <code>AT+SEC</code> command to set the security mode configuration.
     * <p> On success, a {@link CodelessEvent.SecurityMode SecurityMode} event is generated.
     * @param mode the security {@link CodelessProfile.Command#SECURITY_MODE_0 mode} to use
     */
    public SecurityModeCommand setSecurityMode(int mode) {
        return sendCommand(new SecurityModeCommand(manager, mode));
    }

    /**
     * Sends the <code>AT+PIN</code> command to get the pin code for the pairing process.
     * <p> On success, a {@link CodelessEvent.PinCode PinCode} event is generated.
     */
    public PinCodeCommand getPinCode() {
        return sendCommand(new PinCodeCommand(manager));
    }

    /**
     * Sends the <code>AT+PIN</code> command to set the pin code for the pairing process.
     * <p> On success, a {@link CodelessEvent.PinCode PinCode} event is generated.
     * @param code the pin code (six-digit)
     */
    public PinCodeCommand setPinCode(int code) {
        return sendCommand(new PinCodeCommand(manager, code));
    }

    /**
     * Sends the <code>AT+FLOWCONTROL</code> command to get the UART hardware flow control configuration.
     * <p> On success, a {@link CodelessEvent.FlowControl FlowControl} event is generated.
     */
    public FlowControlCommand getFlowControl() {
        return sendCommand(new FlowControlCommand(manager));
    }

    /**
     * Sends the <code>AT+FLOWCONTROL</code> command to set the UART hardware flow control configuration.
     * <p> On success, a {@link CodelessEvent.FlowControl FlowControl} event is generated.
     * @param enabled   <code>true</code> to enable UART RTS/CTS flow control, <code>false</code> to disable it
     * @param rts       {@link CodelessProfile.GPIO GPIO} that selects the pin for the RTS signal
     * @param cts       {@link CodelessProfile.GPIO GPIO} that selects the pin for the CTS signal
     */
    public FlowControlCommand setFlowControl(boolean enabled, GPIO rts, GPIO cts) {
        return sendCommand(new FlowControlCommand(manager, enabled, rts, cts));
    }

    /**
     * Sends the <code>AT+CLRBNDE</code> command to clear an entry from the bonding database.
     * <p> On success, a {@link CodelessEvent.BondingEntryClear BondingEntryClear} event is generated.
     * @param index the bonding entry to clear (1-5, 0xFF: all entries)
     */
    public BondingEntryClearCommand clearBondingDatabaseEntry(int index) {
        return sendCommand(new BondingEntryClearCommand(manager, index));
    }

    /**
     * Sends the <code>AT+CLRBNDE</code> command to clear the whole bonding database.
     * <p> On success, a {@link CodelessEvent.BondingEntryClear BondingEntryClear} event is generated.
     */
    public BondingEntryClearCommand clearBondingDatabase() {
        return clearBondingDatabaseEntry(CodelessLibConfig.BONDING_DATABASE_ALL_VALUES);
    }

    /**
     * Sends the <code>AT+CHGBNDP</code> command to get the persistence status of all entries in the bonding database.
     * <p> On success, a {@link CodelessEvent.BondingEntryPersistenceTableStatus BondingEntryPersistenceTableStatus} event is generated.
     */
    public BondingEntryStatusCommand getBondingDatabasePersistenceStatus() {
        return sendCommand(new BondingEntryStatusCommand(manager));
    }

    /**
     * Sends the <code>AT+CHGBNDP</code> command to set the persistence status of an entry in the bonding database.
     * <p> On success, a {@link CodelessEvent.BondingEntryPersistenceStatusSet BondingEntryPersistenceStatusSet} event is generated.
     * @param index         the bonding entry to clear (1-5, 0xFF: all entries)
     * @param persistent    <code>true</code> to enable persistence, <code>false</code> to disable it
     */
    public BondingEntryStatusCommand setBondingEntryPersistenceStatus(int index, boolean persistent) {
        return sendCommand(new BondingEntryStatusCommand(manager, index, persistent));
    }

    /**
     * Sends the <code>AT+IEBNDE</code> command to get a bonding entry configuration.
     * <p> On success, a {@link CodelessEvent.BondingEntryEvent BondingEntryEvent} is generated.
     * @param index the bonding entry to get (1-5)
     */
    public BondingEntryTransferCommand getBondingDatabase(int index) {
        return sendCommand(new BondingEntryTransferCommand(manager, index));
    }

    /**
     * Sends the <code>AT+IEBNDE</code> command to set a bonding entry configuration.
     * <p> On success, a {@link CodelessEvent.BondingEntryEvent BondingEntryEvent} is generated.
     * @param index the bonding entry to set (1-5)
     * @param entry the bonding entry {@link BondingEntry configuration}
     */
    public BondingEntryTransferCommand setBondingDatabase(int index, BondingEntry entry) {
        return sendCommand(new BondingEntryTransferCommand(manager, index, entry));
    }
}
