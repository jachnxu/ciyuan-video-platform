package com.ciyuan.app.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.view.View
import com.ciyuan.app.data.DanmakuItem
import kotlin.math.abs

/**
 * 原生 Canvas 弹幕层：按时间轴滚动，多轨道避让，描边保证在任何画面上可读
 */
@SuppressLint("ViewConstructor")
class DanmakuView(context: Context) : View(context) {

    private data class Item(
        val text: String,
        val color: Int,
        var x: Float,
        val y: Float,
        val w: Float,
        val lane: Int,
        val speed: Float
    )

    var items: List<DanmakuItem> = emptyList()
        set(value) {
            field = value.filter { !it.text.isNullOrBlank() }.sortedBy { it.time }
            reset()
            invalidate()
        }

    var danmakuEnabled: Boolean = true
        set(value) {
            field = value
            if (!value) active.clear()
            invalidate()
        }

    var timeProvider: () -> Double = { 0.0 }
    var playingProvider: () -> Boolean = { false }

    private val active = ArrayList<Item>()
    private val laneLast = HashMap<Int, Item>()
    private var ptr = 0
    private var lastT = 0.0
    private var lastFrame = 0L

    private val density = resources.displayMetrics.density
    private val textSizePx = 15f * resources.displayMetrics.scaledDensity
    private val laneHeight = textSizePx + 12f * density

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = textSizePx
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = textSizePx
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        style = Paint.Style.STROKE
        strokeWidth = 3.2f * density
        color = 0x99000000.toInt()
    }

    private fun laneCount(): Int = (height / laneHeight).toInt().coerceAtLeast(2)

    private fun reset() {
        active.clear()
        laneLast.clear()
        ptr = 0
        val t = timeProvider()
        while (ptr < items.size && items[ptr].time < t) ptr++
        lastT = t
    }

    private fun spawn(d: DanmakuItem) {
        val text = d.text ?: return
        val w = fill.measureText(text)
        val lanes = laneCount()
        var lane = -1
        var best = Float.MAX_VALUE
        for (i in 0 until lanes) {
            val last = laneLast[i]
            if (last == null || last.x + last.w < width - 12f * density) {
                val right = last?.let { it.x + it.w } ?: -1f
                if (right < best) {
                    best = right
                    lane = i
                }
            }
        }
        if (lane < 0) return
        val color = runCatching { Color.parseColor(d.color ?: "#ffffff") }.getOrDefault(Color.WHITE)
        val item = Item(
            text = text,
            color = color,
            x = width.toFloat(),
            y = lane * laneHeight + textSizePx + 4f * density,
            w = w,
            lane = lane,
            speed = (115 + (text.hashCode() % 25)) * density
        )
        active.add(item)
        laneLast[lane] = item
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width == 0 || height == 0) return
        val playing = playingProvider()
        val t = timeProvider()
        val now = System.nanoTime()
        val dt = if (lastFrame == 0L) 0.016f else ((now - lastFrame) / 1_000_000_000.0).toFloat().coerceAtMost(0.05f)
        lastFrame = now

        if (!danmakuEnabled) return

        if (playing) {
            if (t < lastT - 0.4) reset()
            while (ptr < items.size && items[ptr].time <= t) {
                if (abs(t - items[ptr].time) < 1.5) spawn(items[ptr])
                ptr++
            }
            lastT = t
        }

        for (i in active.indices.reversed()) {
            val it = active[i]
            if (playing) it.x -= it.speed * dt
            if (it.x + it.w < -4f) {
                if (laneLast[it.lane] === it) laneLast.remove(it.lane)
                active.removeAt(i)
                continue
            }
            canvas.drawText(it.text, it.x, it.y, stroke)
            fill.color = it.color
            canvas.drawText(it.text, it.x, it.y, fill)
        }

        if (playing || active.isNotEmpty()) postInvalidateOnAnimation()
    }

    fun startLoop() {
        lastFrame = 0L
        postInvalidateOnAnimation()
    }
}
