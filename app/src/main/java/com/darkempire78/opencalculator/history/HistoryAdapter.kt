package com.darkempire78.opencalculator.history

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

class HistoryAdapter(
    private var history: MutableList<History>,
    private val onElementClick: (value: String) -> Unit,
    private val context: Context
    ) : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
            val view = LayoutInflater
                .from(parent.context)
                .inflate(R.layout.history_item, parent, false)
            return HistoryViewHolder(view)
        }

        override fun getItemCount(): Int = history.size

        override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
            holder.bind(history[position], position)
        }

        fun appendHistory(historyList: List<History>) {
            this.history.addAll(historyList)
            notifyItemRangeInserted(
                this.history.size,
                historyList.size - 1
            )
        }

    fun appendOneHistoryElement(history: History) {
        this.history.add(0, history)
        notifyItemInserted(0)
        // Update the first 2 elements to avoid to have the same date and bar separator
        if (this.history.size > 1) {
            notifyItemRangeChanged(0, 2)
        }
    }

        fun removeHistoryElement(position: Int){
            // No idea why, but time.isNotEmpty() is not working, only time.isNullOrEmpty() works
            val historyElement = history[position]
            if (!historyElement.time.isNullOrEmpty()){
                val nextHistoryElement = history.getOrNull(position + 1)
                nextHistoryElement?.let {
                    if (it.time.isNullOrEmpty()){
                        this.history[position + 1] = History(
                            calculation = nextHistoryElement.calculation,
                            result = nextHistoryElement.result,
                            time = historyElement.time,
                            id = nextHistoryElement.id
                        )
                    }
                }
            }
            this.history.removeAt(position)
            notifyItemRemoved(position)
        }


        fun updateHistoryList() {
            this.history = MyPreferences(context).getHistory()
        }
        fun updateHistoryElement(historyElement: History) {
            updateHistoryList()
            val position = this.history.indexOfFirst { it.id == historyElement.id }
            if (position != -1) {
                this.history[position] = historyElement
                notifyItemChanged(position)
            }
        }

        fun removeFirstHistoryElement() {
            this.history.removeAt(0)
            notifyItemRemoved(0)
        }

        fun clearHistory() {
            this.history.clear()
            notifyDataSetChanged()
        }

        inner class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val calculation: TextView = itemView.findViewById(R.id.history_item_calculation)
            private val result: TextView = itemView.findViewById(R.id.history_item_result)
            private val time: TextView = itemView.findViewById(R.id.history_time)
            private val separator: View = itemView.findViewById(R.id.history_separator)
            private val sameDateSeparator: View = itemView.findViewById(R.id.history_same_date_separator)

            private fun wrapInParenthesis(string: String): String {
                // Verify it is not already in parenthesis
                return if (string.first() != '(' || string.last() != ')') {
                    "($string)"
                } else {
                    string
                }
            }

            fun bind(historyElement: History, position : Int) {
                // Set calculation, result and time
                calculation.text = historyElement.calculation
                result.text = historyElement.result
                // To avoid crashes with former histories that do not have stored dates
                if (historyElement.time.isNullOrEmpty()) {
                    time.visibility = View.GONE
                } else {
                    time.text = DateUtils.getRelativeTimeSpanString(
                        historyElement.time.toLong(),
                        System.currentTimeMillis(),
                        DateUtils.DAY_IN_MILLIS,
                        DateUtils.FORMAT_ABBREV_RELATIVE,
                    )
                    // Check if the former result has the same date -> hide the date
                    if (position > 0) {
                        if (
                            !history[position-1].time.isNullOrEmpty()
                            && DateUtils.getRelativeTimeSpanString(
                                history[position-1].time.toLong(),
                                System.currentTimeMillis(),
                                DateUtils.DAY_IN_MILLIS,
                                DateUtils.FORMAT_ABBREV_RELATIVE,
                            ) == time.text)
                        {
                            time.visibility = View.GONE
                        } else {
                            time.visibility = View.VISIBLE
                        }
                    } else {
                        time.visibility = View.VISIBLE
                    }
                    // Check if the next result has the same date -> hide the separator
                    if (position+1 < history.size) {
                        if (
                            DateUtils.getRelativeTimeSpanString(
                                history[position+1].time.toLong(),
                                System.currentTimeMillis(),
                                DateUtils.DAY_IN_MILLIS,
                                DateUtils.FORMAT_ABBREV_RELATIVE,
                            ) == time.text)
                        {
                            separator.visibility = View.GONE
                            // Add more space when it's the same date than the next history element
                            sameDateSeparator.visibility = View.VISIBLE
                        } else {
                            separator.visibility = View.VISIBLE
                            sameDateSeparator.visibility = View.GONE
                        }
                    } else {
                        separator.visibility = View.VISIBLE
                        sameDateSeparator.visibility = View.GONE
                    }
                }

                // On click
                calculation.setOnClickListener {
                    val formattedCalculation = wrapInParenthesis(historyElement.calculation)
                    onElementClick.invoke(formattedCalculation)
                }
                result.setOnClickListener {
                    val formattedResult = wrapInParenthesis(historyElement.result)
                    onElementClick.invoke(formattedResult)
                }

                calculation.setOnLongClickListener {
                    if (MyPreferences(itemView.context).longClickToCopyValue) {
                        val clipboardManager = itemView.context.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                        clipboardManager.setPrimaryClip(ClipData.newPlainText(R.string.copied_history_calculation.toString(), historyElement.calculation))
                        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2)
                            Toast.makeText(itemView.context, R.string.value_copied, Toast.LENGTH_SHORT).show()
                        true // Or false if not consumed}
                    } else {
                        false
                    }

                }
                result.setOnLongClickListener {
                    if (MyPreferences(itemView.context).longClickToCopyValue) {
                        val clipboardManager = itemView.context.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                        clipboardManager.setPrimaryClip(ClipData.newPlainText(R.string.copied_history_result.toString(), historyElement.result))
                        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2)
                            Toast.makeText(itemView.context, R.string.value_copied, Toast.LENGTH_SHORT).show()
                        true // Or false if not consumed
                    } else {
                        false
                    }
                }
            }
        }
    }