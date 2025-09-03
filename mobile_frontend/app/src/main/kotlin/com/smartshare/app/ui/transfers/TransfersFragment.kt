package com.smartshare.app.ui.transfers

import android.os.Bundle
import android.view.*
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.setPadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.smartshare.app.R
import com.smartshare.app.domain.share.TransferManager
import com.smartshare.app.domain.share.TransferStatus

/**
 * PUBLIC_INTERFACE
 * TransfersFragment displays real-time transfer progress and status using LiveData from TransferManager.
 */
class TransfersFragment : Fragment() {

    companion object {
        fun newInstance() = TransfersFragment()
    }

    private lateinit var recyclerView: RecyclerView
    private val adapter = TransferAdapter()

    override fun onCreateView(inflater: LayoutInflater, parent: ViewGroup?, state: Bundle?): View {
        val root = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16)
        }
        root.addView(TextView(requireContext()).apply {
            text = getString(R.string.tab_transfers)
            textSize = 18f
        })
        recyclerView = RecyclerView(requireContext()).apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@TransfersFragment.adapter
        }
        root.addView(recyclerView)

        TransferManager.getInstance(requireContext()).statuses.observe(viewLifecycleOwner, Observer {
            adapter.submit(it)
        })

        return root
    }
}

private class TransferAdapter : RecyclerView.Adapter<TransferVH>() {
    private val items = mutableListOf<TransferStatus>()
    fun submit(data: List<TransferStatus>) {
        items.clear()
        items.addAll(data)
        notifyDataSetChanged()
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransferVH {
        val ctx = parent.context
        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16)
        }
        val title = TextView(ctx).apply { textSize = 16f }
        val subtitle = TextView(ctx).apply { textSize = 12f }
        row.addView(title); row.addView(subtitle)
        return TransferVH(row, title, subtitle)
    }
    override fun getItemCount(): Int = items.size
    override fun onBindViewHolder(holder: TransferVH, position: Int) {
        val it = items[position]
        holder.title.text = it.displayName
        holder.subtitle.text = "${it.progress}% • ${it.state}"
    }
}

private class TransferVH(
    root: View,
    val title: TextView,
    val subtitle: TextView
) : RecyclerView.ViewHolder(root)
