package com.tokoaksesoris.kasir.ui.statistik

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.max

/**
 * Grafik batang sederhana untuk menampilkan pemasukan per hari.
 * Sengaja dibuat manual (Canvas) supaya tidak menambah dependency library chart
 * yang berat -- sejalan dengan prinsip aplikasi ini harus tetap ringan.
 */
class BarChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var entries: List<Pair<String, Double>> = emptyList()

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#0A84FF") // iOS system blue
        style = Paint.Style.FILL
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#8E8E93") // iOS system gray
        textSize = 26f
        textAlign = Paint.Align.CENTER
    }

    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#8E8E93")
        textSize = 24f
        textAlign = Paint.Align.CENTER
    }

    private val rupiahFormatSingkat = NumberFormat.getNumberInstance(Locale("in", "ID"))

    fun setData(data: List<Pair<String, Double>>) {
        entries = data
        invalidate()
        requestLayout()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredHeight = 220
        val height = resolveSize(dpToPx(desiredHeight), heightMeasureSpec)
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY))
    }

    private fun dpToPx(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (entries.isEmpty()) return

        val maxValue = max(entries.maxOf { it.second }, 1.0)
        val paddingLabelBottom = dpToPx(36).toFloat()
        val paddingValueTop = dpToPx(22).toFloat()
        val chartTop = paddingValueTop
        val chartBottom = height - paddingLabelBottom
        val chartHeight = chartBottom - chartTop

        val totalWidth = width.toFloat()
        val slotWidth = totalWidth / entries.size
        val barWidth = slotWidth * 0.5f

        entries.forEachIndexed { index, (label, value) ->
            val slotCenterX = slotWidth * index + slotWidth / 2f
            val barHeight = (value / maxValue * chartHeight).toFloat()
            val barTop = chartBottom - barHeight
            val barLeft = slotCenterX - barWidth / 2f
            val barRight = slotCenterX + barWidth / 2f

            val radius = dpToPx(4).toFloat()
            canvas.drawRoundRect(RectF(barLeft, barTop, barRight, chartBottom), radius, radius, barPaint)

            // Label tanggal di bawah batang
            canvas.drawText(label, slotCenterX, chartBottom + dpToPx(24), labelPaint)

            // Nilai singkat di atas batang (hanya kalau nilainya > 0, supaya tidak penuh sesak)
            if (value > 0) {
                val teksNilai = formatSingkat(value)
                canvas.drawText(teksNilai, slotCenterX, barTop - dpToPx(6), valuePaint)
            }
        }
    }

    private fun formatSingkat(value: Double): String {
        return when {
            value >= 1_000_000 -> String.format(Locale("in", "ID"), "%.1fjt", value / 1_000_000)
            value >= 1_000 -> String.format(Locale("in", "ID"), "%.0frb", value / 1_000)
            else -> rupiahFormatSingkat.format(value)
        }
    }
}
