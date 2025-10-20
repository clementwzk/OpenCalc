package com.darkempire78.opencalculator.bookmarks

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Context.CLIPBOARD_SERVICE
import android.os.Build
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.darkempire78.opencalculator.MyPreferences
import com.darkempire78.opencalculator.R

class BookmarksAdapter(
    private var items: MutableList<Bookmark>,
    private val onElementClick: (value: String) -> Unit,
    private val context: Context
) : RecyclerView.Adapter<BookmarksAdapter.VH>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater
            .from(parent.context)
            .inflate(R.layout.history_item, parent, false)
        return VH(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position], position)
    }

    fun updateList(newItems: MutableList<Bookmark>) {
        items = newItems
        notifyDataSetChanged()
    }

    fun appendOne(b: Bookmark) {
        items.add(b)
        if (items.size > 1) {
            notifyItemInserted(items.size - 1)
            notifyItemRangeChanged(items.size - 2, 2)
        } else {
            notifyItemInserted(items.size - 1)
        }
    }

    fun removeAt(position: Int) {
        items.removeAt(position)
        notifyItemRemoved(position)
    }

    fun clear() {
        items.clear()
        notifyDataSetChanged()
    }

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val calculation: TextView = itemView.findViewById(R.id.history_item_calculation)
        private val result: TextView = itemView.findViewById(R.id.history_item_result)
        private val time: TextView = itemView.findViewById(R.id.history_time)
        private val separator: View = itemView.findViewById(R.id.history_separator)
        private val sameDateSeparator: View = itemView.findViewById(R.id.history_same_date_separator)

        private fun wrapInParenthesis(s: String): String {
            return if (s.first() != '(' || s.last() != ')') "($s)" else s
        }

        fun bind(b: Bookmark, position: Int) {
            calculation.text = b.calculation
            result.text = b.result

            if (b.time.isEmpty()) {
                time.visibility = View.GONE
            } else {
                val rel = DateUtils.getRelativeTimeSpanString(
                    b.time.toLong(),
                    System.currentTimeMillis(),
                    DateUtils.DAY_IN_MILLIS,
                    DateUtils.FORMAT_ABBREV_RELATIVE
                )
                time.text = rel
                // Keep same grouping to look like history for now:
                if (position + 1 < items.size) {
                    val nextRel = DateUtils.getRelativeTimeSpanString(
                        items[position + 1].time.toLong(),
                        System.currentTimeMillis(),
                        DateUtils.DAY_IN_MILLIS,
                        DateUtils.FORMAT_ABBREV_RELATIVE
                    )
                    if (nextRel == rel) {
                        // Show only sameDateSeparator
                        separator.visibility = View.GONE
                        sameDateSeparator.visibility = View.VISIBLE
                    } else {
                        // Relative times don't match -> show only the main separator
                        separator.visibility = View.VISIBLE
                        sameDateSeparator.visibility = View.GONE
                    }
                } else {
                    separator.visibility = View.VISIBLE
                    sameDateSeparator.visibility = View.GONE
                }
            }

            calculation.setOnClickListener { onElementClick.invoke(wrapInParenthesis(b.calculation)) }
            result.setOnClickListener { onElementClick.invoke(wrapInParenthesis(b.result)) }

            if (MyPreferences(itemView.context).longClickToCopyValue) {
                calculation.setOnLongClickListener {
                    val cm = itemView.context.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                    cm.setPrimaryClip(ClipData.newPlainText(itemView.context.getString(R.string.copied_history_calculation), b.calculation))
                    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2)
                        Toast.makeText(itemView.context, R.string.value_copied, Toast.LENGTH_SHORT).show()
                    true
                }
                result.setOnLongClickListener {
                    val cm = itemView.context.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                    cm.setPrimaryClip(ClipData.newPlainText(itemView.context.getString(R.string.copied_history_result), b.result))
                    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2)
                        Toast.makeText(itemView.context, R.string.value_copied, Toast.LENGTH_SHORT).show()
                    true
                }
            }
        }
    }
}
