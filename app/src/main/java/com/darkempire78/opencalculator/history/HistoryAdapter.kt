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

/**
 * RecyclerView adapter for displaying calculation history.
 * Manages history items with features including:
 * - Click to insert calculation/result back into calculator
 * - Long-press to copy values to clipboard
 * - Intelligent date grouping and separators
 * - Swipe-to-delete support
 * 
 * @property history Mutable list of history entries
 * @property onElementClick Callback invoked when a history item is clicked
 * @property context Context for accessing resources and preferences
 */
class HistoryAdapter(
    private var history: MutableList<History>,
    private val onElementClick: (value: String) -> Unit,
    private val context: Context
    ) : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

        // RecyclerView lifecycle methods
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

        /**
         * Appends multiple history entries to the list.
         * @param historyList List of history entries to append
         */
        fun appendHistory(historyList: List<History>) {
            this.history.addAll(historyList)
            notifyItemRangeInserted(
                this.history.size,
                historyList.size - 1
            )
        }

        /**
         * Appends a single history entry to the list.
         * Updates the last 2 elements to ensure proper date grouping and separators.
         * @param history The history entry to append
         */
        fun appendOneHistoryElement(history: History) {
            this.history.add(history)
            // Update the last 2 elements to avoid to have the same date and bar separator
            if (this.history.size > 1) {
                notifyItemInserted(this.history.size - 1)
                notifyItemRangeChanged(this.history.size - 2, 2)
            } else {
                notifyItemInserted(this.history.size - 1)
            }
        }

        /**
         * Removes a history entry at the specified position.
         * Transfers the timestamp to the next element if needed to maintain date headers.
         * @param position The position of the history entry to remove
         */
        fun removeHistoryElement(position: Int){
            // No idea why, but time.isNotEmpty() is not working, only time.isNullOrEmpty() works
            val historyElement = history[position]
            // If the removed element has a timestamp, transfer it to the next element
            if (!historyElement.time.isNullOrEmpty()){
                val nextHistoryElement = history.getOrNull(position + 1)
                nextHistoryElement?.let {
                    // If next element doesn't have a timestamp, give it this one
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

        /**
         * Reloads the history list from SharedPreferences.
         */
        fun updateHistoryList() {
            this.history = MyPreferences(context).getHistory()
        }
        
        /**
         * Updates a specific history entry by ID.
         * Reloads the entire list first, then updates the matching entry.
         * @param historyElement The updated history entry
         */
        fun updateHistoryElement(historyElement: History) {
            updateHistoryList()
            val position = this.history.indexOfFirst { it.id == historyElement.id }
            if (position != -1) {
                this.history[position] = historyElement
                notifyItemChanged(position)
            }
        }

        /**
         * Removes the first (oldest) history entry.
         * Used to enforce history size limits.
         */
        fun removeFirstHistoryElement() {
            this.history.removeAt(0)
            notifyItemRemoved(0)
        }

        /**
         * Clears all history entries from the adapter.
         */
        fun clearHistory() {
            this.history.clear()
            notifyDataSetChanged()
        }

        /**
         * ViewHolder for individual history items.
         * Handles binding data to views and managing click/long-click interactions.
         */
        inner class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val calculation: TextView = itemView.findViewById(R.id.history_item_calculation)
            private val result: TextView = itemView.findViewById(R.id.history_item_result)
            private val time: TextView = itemView.findViewById(R.id.history_time)
            private val separator: View = itemView.findViewById(R.id.history_separator)
            private val sameDateSeparator: View = itemView.findViewById(R.id.history_same_date_separator)

            /**
             * Wraps a string in parentheses if not already wrapped.
             * Used when inserting history values back into the calculator.
             * @param string The string to wrap
             * @return The string wrapped in parentheses
             */
            private fun wrapInParenthesis(string: String): String {
                // Verify it is not already in parenthesis
                return if (string.first() != '(' || string.last() != ')') {
                    "($string)"
                } else {
                    string
                }
            }

            /**
             * Binds a history entry to the ViewHolder's views.
             * Handles date grouping, separators, and click listeners.
             * @param historyElement The history entry to bind
             * @param position The position in the adapter
             */
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

                // Click listeners: Insert calculation/result back into calculator
                // On click
                calculation.setOnClickListener {
                    val formattedCalculation = wrapInParenthesis(historyElement.calculation)
                    onElementClick.invoke(formattedCalculation)
                }
                result.setOnClickListener {
                    val formattedResult = wrapInParenthesis(historyElement.result)
                    onElementClick.invoke(formattedResult)
                }

                // Long-press listeners: Copy to clipboard (if enabled in settings)
                calculation.setOnLongClickListener {
                    if (MyPreferences(itemView.context).longClickToCopyValue) {
                        val clipboardManager = itemView.context.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                        clipboardManager.setPrimaryClip(ClipData.newPlainText(R.string.copied_history_calculation.toString(), historyElement.calculation))
                        // Show toast only on Android 12 and lower (13+ shows system UI)
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
                        // Show toast only on Android 12 and lower (13+ shows system UI)
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