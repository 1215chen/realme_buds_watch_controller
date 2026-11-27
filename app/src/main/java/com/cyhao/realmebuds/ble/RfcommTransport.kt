package com.cyhao.realmebuds.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import android.os.Handler
import android.util.Log
import java.util.UUID
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 经典蓝牙 RFCOMM（SPP）传输层。
 *
 * realme Air 系列的控制协议实际走 BR/EDR RFCOMM：
 * - service UUID：realme Link 使用 OPO UUID `0000079a-d102-11e1-9b23-00025b00a5a5`；
 * - 兜底 UUID：标准 SPP `00001101-...`。
 *
 * 连接顺序：secure → insecure（未配对设备可能只能走 insecure）。
 * 读线程/写线程均在独立单线程执行器上，避免阻塞仓库的 Handler 线程。
 */
internal class RfcommTransport(
    private val adapter: BluetoothAdapter,
    private val handler: Handler,
    private val address: String,
    private val callbacks: Callbacks,
) {

    internal interface Callbacks {
        fun onRfcommReady()
        fun onRfcommData(data: ByteArray)
        fun onRfcommDisconnected()
        fun onRfcommFailed()
    }

    private val connectExecutor: ExecutorService = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "budswear-rfcomm-connect")
    }
    private val writeExecutor: ExecutorService = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "budswear-rfcomm-write")
    }

    @Volatile
    private var socket: BluetoothSocket? = null

    @Volatile
    private var running = false

    @Volatile
    private var closed = false

    private val disconnectNotified = AtomicBoolean(false)

    fun start() {
        if (closed) return
        running = true
        disconnectNotified.set(false)
        connectExecutor.execute { connectAndRead() }
    }

    fun write(bytes: ByteArray) {
        if (closed || bytes.isEmpty()) return
        writeExecutor.execute {
            val active = socket ?: return@execute
            try {
                active.outputStream.write(bytes)
                active.outputStream.flush()
            } catch (e: Exception) {
                Log.w(TAG, "RFCOMM write failed", e)
                notifyDisconnected()
            }
        }
    }

    fun close() {
        if (closed) return
        closed = true
        running = false
        val active = socket
        socket = null
        try {
            active?.close()
        } catch (e: Exception) {
            Log.w(TAG, "socket close failed", e)
        }
        connectExecutor.shutdownNow()
        writeExecutor.shutdownNow()
    }

    private fun connectAndRead() {
        val connected = tryConnect()
        if (connected == null) {
            if (!closed) {
                handler.post { if (running && !closed) callbacks.onRfcommFailed() }
            }
            return
        }
        socket = connected
        handler.post { if (running && !closed) callbacks.onRfcommReady() }
        readLoop(connected)
    }

    @SuppressLint("MissingPermission")
    private fun tryConnect(): BluetoothSocket? {
        val device = try {
            adapter.getRemoteDevice(address)
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "invalid address $address", e)
            return null
        }
        try {
            adapter.cancelDiscovery()
        } catch (e: Exception) {
            Log.w(TAG, "cancelDiscovery failed", e)
        }

        val uuids: List<UUID> = listOf(BleConstants.OPO_SERVICE, BleConstants.SPP_SERVICE)
        for (uuid in uuids) {
            for (secure in booleanArrayOf(true, false)) {
                var candidate: BluetoothSocket? = null
                try {
                    candidate = if (secure) {
                        device.createRfcommSocketToServiceRecord(uuid)
                    } else {
                        device.createInsecureRfcommSocketToServiceRecord(uuid)
                    }
                    candidate.connect()
                    Log.i(TAG, "RFCOMM connected uuid=$uuid secure=$secure")
                    return candidate
                } catch (e: Exception) {
                    Log.w(TAG, "RFCOMM connect failed uuid=$uuid secure=$secure: ${e.message}")
                    try {
                        candidate?.close()
                    } catch (closeError: Exception) {
                        Log.w(TAG, "candidate close failed", closeError)
                    }
                }
            }
        }
        return null
    }

    private fun readLoop(active: BluetoothSocket) {
        val buffer = ByteArray(1024)
        try {
            val input = active.inputStream
            while (running && !closed) {
                val count = input.read(buffer)
                if (count < 0) break
                if (count > 0) {
                    val chunk = buffer.copyOf(count)
                    handler.post { if (running && !closed) callbacks.onRfcommData(chunk) }
                }
            }
        } catch (e: Exception) {
            if (running && !closed) Log.w(TAG, "RFCOMM read loop ended", e)
        } finally {
            notifyDisconnected()
        }
    }

    private fun notifyDisconnected() {
        if (closed) return
        if (socket == null) return
        if (!disconnectNotified.compareAndSet(false, true)) return
        socket = null
        handler.post { if (!closed) callbacks.onRfcommDisconnected() }
    }

    private companion object {
        const val TAG = "RfcommTransport"
    }
}
