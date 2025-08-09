package com.devs169.mouliksluckyfinger

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.os.CountDownTimer
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.Build
import android.view.MotionEvent
import android.view.View
import kotlin.random.Random

class TouchPickerView(context: Context) : View(context) {
    private val touches = mutableMapOf<Int, Pair<Float, Float>>() // pointerId → (x, y)
    private var pickedId: Int? = null

    private val paintTouch = Paint().apply {
        color = Color.parseColor("#66CCCCCC") // semi-transparent grey
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val paintPicked = Paint().apply {
        color = Color.parseColor("#99FF4444") // brighter semi-transparent red
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val paintRing = Paint().apply {
        color = Color.RED
        style = Paint.Style.STROKE
        strokeWidth = 14f
        isAntiAlias = true
    }

    private val paintText = Paint().apply {
        color = Color.WHITE
        textSize = 150f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
        typeface = Typeface.DEFAULT_BOLD
    }

    private var countdownText: String? = null
    private var timerRunning = false
    private var countDownTimer: CountDownTimer? = null

    // Pulse animation
    private var pulseScale = 1f

    init {
        startPulseAnimation()
    }

    private fun startPulseAnimation() {
        ValueAnimator.ofFloat(0.9f, 1.1f).apply {
            duration = 800
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            addUpdateListener {
                pulseScale = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val pointerIndex = event.actionIndex
        val pointerId = event.getPointerId(pointerIndex)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                touches[pointerId] = event.getX(pointerIndex) to event.getY(pointerIndex)
                if (!timerRunning) {
                    startCountdown()
                }
            }
            MotionEvent.ACTION_MOVE -> {
                for (i in 0 until event.pointerCount) {
                    val id = event.getPointerId(i)
                    touches[id] = event.getX(i) to event.getY(i)
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                touches.remove(pointerId)
                if (touches.isEmpty()) {
                    stopCountdown()
                    pickedId = null
                }
            }
        }
        invalidate()
        return true
    }

    private fun startCountdown() {
        timerRunning = true
        pickedId = null

        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(5000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                countdownText = ((millisUntilFinished / 1000) + 1).toString()
                invalidate()
            }
            override fun onFinish() {
                countdownText = null
                timerRunning = false
                if (touches.isNotEmpty()) {
                    pickedId = touches.keys.random(Random)
                    vibrateDevice(150)
                }
                invalidate()
            }
        }.start()
    }

    private fun stopCountdown() {
        timerRunning = false
        countdownText = null
        countDownTimer?.cancel()
        invalidate()
    }

    private fun vibrateDevice(ms: Long) {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(ms)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        for ((id, pos) in touches) {
            val baseRadius = if (id == pickedId) 230f else 200f
            val scaledRadius = baseRadius * pulseScale

            if (id == pickedId) {
                canvas.drawCircle(pos.first, pos.second, scaledRadius, paintPicked)
                canvas.drawCircle(pos.first, pos.second, (scaledRadius + 20f), paintRing)
            } else {
                canvas.drawCircle(pos.first, pos.second, scaledRadius, paintTouch)
            }
        }

        countdownText?.let {
            canvas.drawText(it, width / 2f, height / 2f + 50f, paintText)
        }
    }
}
