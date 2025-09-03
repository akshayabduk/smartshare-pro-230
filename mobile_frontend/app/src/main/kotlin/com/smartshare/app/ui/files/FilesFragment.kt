package com.smartshare.app.ui.files

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.*
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.setPadding
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.smartshare.app.domain.model.FileItem
import com.smartshare.app.R
import com.smartshare.app.domain.share.TransferManager
import com.smartshare.app.ui.preview.PreviewActivity

/**
 * PUBLIC_INTERFACE
 * FilesFragment shows categorized local files and offers share & preview.
 * It supports launching a system picker for quick share and uses TransferManager to queue transfers.
 */
class FilesFragment : Fragment() {

    companion object {
        fun newInstance() = FilesFragment()

        // PUBLIC_INTERFACE
        fun startSystemPickerForShare(activity: Activity) {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            }
            activity.startActivityForResult(intent, 9001)
        }
    }

    private lateinit var recyclerView: RecyclerView
    private val adapter = FileListAdapter { item, action ->
        when (action) {
            FileItemAction.SHARE -> TransferManager.getInstance(requireContext())
                .enqueueShare(item)
            FileItemAction.PREVIEW -> startActivity(
                Intent(requireContext(), PreviewActivity::class.java).putExtra("uri", item.uri.toString())
            )
        }
    }

    private val pickerLauncher = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        uris?.forEach { uri ->
            val item = uri.toFileItem(requireContext())
            TransferManager.getInstance(requireContext()).enqueueShare(item)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, parent: ViewGroup?, state: Bundle?): View {
        val root = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16)
        }
        root.addView(TextView(requireContext()).apply {
            text = getString(R.string.tab_files)
            textSize = 18f
        })
        recyclerView = RecyclerView(requireContext()).apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@FilesFragment.adapter
        }
        root.addView(recyclerView)
        loadSample()
        return root
    }

    private fun loadSample() {
        // In a real implementation, query MediaStore and Contacts.
        val sample = listOf(
            FileItem(Uri.parse("content://local/photo1"), "Photo 1.jpg", "Photos", 1_024_000),
            FileItem(Uri.parse("content://local/song1"), "Track A.mp3", "Music", 5_024_000),
            FileItem(Uri.parse("content://local/video1"), "Clip 1.mp4", "Videos", 50_024_000),
        )
        adapter.submit(sample)
    }

    private fun Uri.toFileItem(ctx: Context): FileItem {
        var name = "Unknown"
        var size = 0L
        ctx.contentResolver.query(this, null, null, null, null)?.use { c ->
            val nameIdx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIdx = c.getColumnIndex(OpenableColumns.SIZE)
            if (c.moveToFirst()) {
                if (nameIdx >= 0) name = c.getString(nameIdx) ?: name
                if (sizeIdx >= 0) size = c.getLong(sizeIdx)
            }
        }
        return FileItem(this, name, "Files", size)
    }
}

private enum class FileItemAction { SHARE, PREVIEW }

private class FileListAdapter(
    val onAction: (FileItem, FileItemAction) -> Unit
) : RecyclerView.Adapter<FileViewHolder>() {

    private val items = mutableListOf<FileItem>()

    fun submit(data: List<FileItem>) {
        items.clear()
        items.addAll(data)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val ctx = parent.context
        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16)
        }
        val title = TextView(ctx).apply { textSize = 16f }
        val subtitle = TextView(ctx).apply { textSize = 12f }
        val actions = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            val share = TextView(ctx).apply { text = "Share"; setTextColor(ctx.getColor(R.color.accentColor)) }
            val preview = TextView(ctx).apply { text = "Preview"; setTextColor(ctx.getColor(R.color.secondaryColor)) }
            addView(share); addView(View(ctx).apply { layoutParams = ViewGroup.LayoutParams(32, 1) }); addView(preview)
            setPadding(0, 8, 0, 0)
        }
        row.addView(title); row.addView(subtitle); row.addView(actions)
        val holder = FileViewHolder(row, title, subtitle)
        // Attach listeners after holder creation to access bindingAdapterPosition safely
        val shareView = (actions.getChildAt(0) as TextView)
        val previewView = (actions.getChildAt(2) as TextView)
        shareView.setOnClickListener {
            val pos = holder.bindingAdapterPosition.takeIf { it != RecyclerView.NO_POSITION } ?: return@setOnClickListener
            onAction(items[pos], FileItemAction.SHARE)
        }
        previewView.setOnClickListener {
            val pos = holder.bindingAdapterPosition.takeIf { it != RecyclerView.NO_POSITION } ?: return@setOnClickListener
            onAction(items[pos], FileItemAction.PREVIEW)
        }
        return holder
    }

    override fun getItemCount(): Int = items.size
    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        val it = items[position]
        holder.title.text = it.displayName
        holder.subtitle.text = "${it.category} • ${it.size} bytes"
    }
}

private class FileViewHolder(
    root: View,
    val title: TextView,
    val subtitle: TextView
) : RecyclerView.ViewHolder(root)
