package com.rl04x.koracharts.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.graphics.toColorInt
import com.google.android.material.card.MaterialCardView
import com.rl04x.koracharts.core.model.KoraChartStyle

/**
 * Material Card container for Kora Chart XML Views providing header title, subtitle, and badge pills.
 */
public class KoraChartCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : MaterialCardView(context, attrs, defStyleAttr) {

    private val titleView: TextView
    private val subtitleView: TextView
    private val badgeView: TextView
    private val badgeContainer: View
    private val contentContainer: FrameLayout

    init {
        radius = resources.displayMetrics.density * 16f
        strokeWidth = (resources.displayMetrics.density * 1f).toInt()
        strokeColor = "#1E293B".toColorInt()
        setCardBackgroundColor("#0F172A".toColorInt())
        cardElevation = resources.displayMetrics.density * 2f

        val root = LayoutInflater.from(context).inflate(R.layout.kora_chart_card_layout, this, true)
        titleView = root.findViewById(R.id.koraCardTitle)
        subtitleView = root.findViewById(R.id.koraCardSubtitle)
        badgeView = root.findViewById(R.id.koraCardBadgeText)
        badgeContainer = root.findViewById(R.id.koraCardBadgeContainer)
        contentContainer = root.findViewById(R.id.koraCardContentContainer)

        if (attrs != null) {
            val a = context.obtainStyledAttributes(
                attrs,
                R.styleable.KoraChartCardView,
                defStyleAttr,
                0
            )
            val title = a.getString(R.styleable.KoraChartCardView_kora_cardTitle)
            val subtitle = a.getString(R.styleable.KoraChartCardView_kora_cardSubtitle)
            val badge = a.getString(R.styleable.KoraChartCardView_kora_cardBadgeText)
            a.recycle()

            setTitle(title)
            setSubtitle(subtitle)
            setBadge(badge)
        }
    }

    public fun setTitle(title: String?) {
        titleView.text = title ?: ""
        titleView.visibility = if (!title.isNullOrEmpty()) View.VISIBLE else View.GONE
    }

    public fun setSubtitle(subtitle: String?) {
        subtitleView.text = subtitle ?: ""
        subtitleView.visibility = if (!subtitle.isNullOrEmpty()) View.VISIBLE else View.GONE
    }

    public fun setBadge(badge: String?) {
        badgeView.text = badge ?: ""
        badgeContainer.visibility = if (!badge.isNullOrEmpty()) View.VISIBLE else View.GONE
    }

    public fun applyStyle(style: KoraChartStyle) {
        setCardBackgroundColor(style.cardBackgroundColor)
        strokeColor = style.cardBorderColor
        titleView.setTextColor(style.titleTextColor)
        subtitleView.setTextColor(style.subtitleTextColor)
        badgeView.setTextColor(style.badgeTextColor)
        badgeContainer.background?.setTint(style.badgeBackgroundColor)
    }

    override fun onViewAdded(child: View) {
        if (child.id != R.id.koraCardRootLayout && child.parent != contentContainer) {
            removeView(child)
            contentContainer.addView(child)
        } else {
            super.onViewAdded(child)
        }
    }
}
