package com.smartshare.app.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.setPadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.card.MaterialCardView
import com.smartshare.app.R
import com.smartshare.app.domain.ai.AiCategorizer
import com.smartshare.app.domain.model.FileItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * PUBLIC_INTERFACE
 * DashboardFragment shows AI-powered recommendations and quick actions.
 * It calls AiCategorizer to propose files for quick share and displays categories.
 */
class DashboardFragment : Fragment() {

    companion object {
        fun newInstance() = DashboardFragment()
    }

    private lateinit var container: LinearLayout

    override fun onCreateView(inflater: LayoutInflater, parent: ViewGroup?, state: Bundle?): View {
        container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24)
            setBackgroundColor(resources.getColor(R.color.backgroundLight, requireContext().theme))
        }
        renderHeader("AI Recommendations")
        renderInfo("Analyzing your recent files to suggest quick share candidates...")
        runAi()
        renderHeader("Quick Actions")
        renderQuickActions()
        return container
    }

    private fun renderHeader(text: String) {
        container.addView(TextView(requireContext()).apply {
            this.text = text
            textSize = 18f
            setTextColor(resources.getColor(R.color.textPrimary, requireContext().theme))
        })
    }

    private fun renderInfo(text: String) {
        container.addView(TextView(requireContext()).apply {
            this.text = text
            textSize = 14f
            setTextColor(resources.getColor(R.color.textSecondary, requireContext().theme))
        })
    }

    private fun renderQuickActions() {
        val row = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        row.addView(makePill("Share", R.color.accentColor))
        row.addView(makePill(getString(R.string.connect_cloud), R.color.secondaryColor))
        container.addView(row)
    }

    private fun makePill(label: String, colorRes: Int): View {
        val card = MaterialCardView(requireContext()).apply {
            setCardBackgroundColor(resources.getColor(colorRes, requireContext().theme))
            radius = 24f
            useCompatPadding = true
            val tv = TextView(context).apply {
                text = label
                setPadding(32)
                setTextColor(resources.getColor(android.R.color.white, requireContext().theme))
            }
            addView(tv)
        }
        val params = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        params.setMargins(8, 16, 8, 16)
        card.layoutParams = params
        return card
    }

    private fun runAi() {
        viewLifecycleOwner.lifecycleScope.launch {
            val suggestions: List<FileItem> = withContext(Dispatchers.IO) {
                AiCategorizer(requireContext()).suggestQuickShare()
            }
            if (suggestions.isNotEmpty()) {
                suggestions.take(5).forEach {
                    container.addView(MaterialCardView(requireContext()).apply {
                        radius = 12f
                        setCardBackgroundColor(resources.getColor(R.color.surfaceLight, requireContext().theme))
                        useCompatPadding = true
                        addView(TextView(context).apply {
                            text = "• ${it.displayName} (${it.category})"
                            setPadding(24)
                            setTextColor(resources.getColor(R.color.textPrimary, requireContext().theme))
                        })
                    })
                }
            } else {
                renderInfo("No suggestions yet. Start sharing or connect cloud storage.")
            }
        }
    }
}
