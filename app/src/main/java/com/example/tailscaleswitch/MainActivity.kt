package com.example.tailscaleswitch

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.JSch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Properties

class MainActivity : AppCompatActivity() {

    // ── SSH-параметры ──────────────────────────────────────────────
    private val SSH_HOST = "192.168.1.1"
    private val SSH_PORT = 222
    private val SSH_USER = "root"
    private val SSH_PASS = "keenetic"
    private val CONNECT_TIMEOUT_MS = 10_000
    // ──────────────────────────────────────────────────────────────

    private lateinit var btnOn: Button
    private lateinit var btnOff: Button
    private lateinit var tvStatus: TextView
    private lateinit var progress: ProgressBar

    // Цвета
    private val COLOR_IDLE_ON  = 0xFF2D5A27.toInt()  // тёмно-зелёный
    private val COLOR_ACTIVE   = 0xFF4CAF50.toInt()  // яркий зелёный
    private val COLOR_IDLE_OFF = 0xFF5A2727.toInt()  // тёмно-красный
    private val COLOR_DONE_OFF = 0xFFF44336.toInt()  // яркий красный
    private val COLOR_DISABLED = 0xFF444444.toInt()  // серый (пока ждём)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnOn    = findViewById(R.id.btnVpnOn)
        btnOff   = findViewById(R.id.btnVpnOff)
        tvStatus = findViewById(R.id.tvStatus)
        progress = findViewById(R.id.progressBar)

        btnOn.setOnClickListener  { runSsh("tailscale up",   isOn = true) }
        btnOff.setOnClickListener { runSsh("tailscale down", isOn = false) }
    }

    /**
     * Выполняет SSH-команду в фоне (Coroutine / IO dispatcher).
     * @param command  команда для роутера
     * @param isOn     true = нажата кнопка ON, false = OFF
     */
    private fun runSsh(command: String, isOn: Boolean) {
        setUiBusy(true)
        setStatus("⏳ Подключение…", "#FFCC00")

        CoroutineScope(Dispatchers.IO).launch {
            val result = runCatching {
                val jsch = JSch()

                // Отключаем проверку host key — роутер в локалке, это нормально
                val config = Properties().apply {
                    put("StrictHostKeyChecking", "no")
                    put("PreferredAuthentications", "password")
                }

                val session = jsch.getSession(SSH_USER, SSH_HOST, SSH_PORT).apply {
                    setPassword(SSH_PASS)
                    setConfig(config)
                    connect(CONNECT_TIMEOUT_MS)
                }

                try {
                    val channel = session.openChannel("exec") as ChannelExec
                    channel.setCommand(command)
                    channel.connect(CONNECT_TIMEOUT_MS)

                    // Ждём завершения команды (max 15 сек)
                    val deadline = System.currentTimeMillis() + 15_000
                    while (!channel.isClosed && System.currentTimeMillis() < deadline) {
                        Thread.sleep(200)
                    }

                    val exitCode = channel.exitStatus
                    channel.disconnect()
                    exitCode
                } finally {
                    session.disconnect()
                }
            }

            withContext(Dispatchers.Main) {
                setUiBusy(false)

                result.onSuccess { exitCode ->
                    if (exitCode == 0 || exitCode == -1) {
                        // exitCode -1 = роутер не вернул код, но команда ушла — тоже OK
                        if (isOn) {
                            highlightButton(btnOn,  COLOR_ACTIVE,   "✅ VPN включён")
                            resetButton    (btnOff, COLOR_IDLE_OFF)
                        } else {
                            highlightButton(btnOff, COLOR_DONE_OFF, "🔴 VPN выключен")
                            resetButton    (btnOn,  COLOR_IDLE_ON)
                        }
                    } else {
                        setStatus("⚠️ Команда вернула код $exitCode", "#FF6600")
                    }
                }

                result.onFailure { err ->
                    val msg = when {
                        err.message?.contains("timeout", true) == true  -> "Таймаут подключения"
                        err.message?.contains("refused",  true) == true -> "Соединение отклонено"
                        err.message?.contains("Auth",     true) == true -> "Ошибка аутентификации"
                        else -> err.message ?: "Неизвестная ошибка"
                    }
                    setStatus("❌ $msg", "#FF4444")
                }
            }
        }
    }

    // ── UI helpers ────────────────────────────────────────────────

    private fun setUiBusy(busy: Boolean) {
        btnOn.isEnabled  = !busy
        btnOff.isEnabled = !busy
        progress.visibility = if (busy) View.VISIBLE else View.GONE
        if (busy) {
            btnOn.backgroundTintList  = android.content.res.ColorStateList.valueOf(COLOR_DISABLED)
            btnOff.backgroundTintList = android.content.res.ColorStateList.valueOf(COLOR_DISABLED)
        }
    }

    private fun highlightButton(btn: Button, color: Int, statusText: String) {
        btn.backgroundTintList = android.content.res.ColorStateList.valueOf(color)
        setStatus(statusText, if (color == COLOR_ACTIVE) "#4CAF50" else "#F44336")
    }

    private fun resetButton(btn: Button, color: Int) {
        btn.backgroundTintList = android.content.res.ColorStateList.valueOf(color)
    }

    private fun setStatus(text: String, hexColor: String) {
        tvStatus.text = text
        tvStatus.setTextColor(android.graphics.Color.parseColor(hexColor))
    }
}
