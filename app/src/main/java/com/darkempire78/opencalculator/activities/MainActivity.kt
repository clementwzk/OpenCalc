package com.darkempire78.opencalculator.activities

import android.animation.LayoutTransition
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.HapticFeedbackConstants
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.TableRow
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.darkempire78.opencalculator.MyPreferences
import com.darkempire78.opencalculator.R
import com.darkempire78.opencalculator.TextSizeAdjuster
import com.darkempire78.opencalculator.Themes
import com.darkempire78.opencalculator.calculator.Calculator
import com.darkempire78.opencalculator.calculator.division_by_0
import com.darkempire78.opencalculator.calculator.domain_error
import com.darkempire78.opencalculator.calculator.is_infinity
import com.darkempire78.opencalculator.calculator.parser.Expression
import com.darkempire78.opencalculator.calculator.parser.NumberFormatter
import com.darkempire78.opencalculator.calculator.parser.NumberingSystem
import com.darkempire78.opencalculator.calculator.parser.NumberingSystem.Companion.toNumberingSystem
import com.darkempire78.opencalculator.calculator.require_real_number
import com.darkempire78.opencalculator.calculator.syntax_error
import com.darkempire78.opencalculator.databinding.ActivityMainBinding
import com.darkempire78.opencalculator.dialogs.DonationDialog
import com.darkempire78.opencalculator.history.History
import com.darkempire78.opencalculator.history.HistoryAdapter
import com.darkempire78.opencalculator.util.ScientificMode
import com.darkempire78.opencalculator.util.ScientificModeTypes
import com.sothree.slidinguppanel.PanelSlideListener
import com.sothree.slidinguppanel.PanelState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormatSymbols
import java.util.Locale
import java.util.UUID


var appLanguage: Locale = Locale.getDefault()
var currentTheme: Int = 0

class MainActivity : AppCompatActivity() {
    private lateinit var view: View

    private val decimalSeparatorSymbol =
        DecimalFormatSymbols.getInstance().decimalSeparator.toString()
    private val groupingSeparatorSymbol =
        DecimalFormatSymbols.getInstance().groupingSeparator.toString()

    private var numberingSystem = NumberingSystem.INTERNATIONAL
    private var scientificModeType = ScientificModeTypes.NOT_ACTIVE

    private var isInvButtonClicked = false
    private var isEqualLastAction = false
    private var isDegreeModeActivated = true // Set degree by default
    private var errorStatusOld = false

    private var isStillTheSameCalculation_autoSaveCalculationWithoutEqualOption = false
    private var lastHistoryElementId = ""

    private var calculationResult = BigDecimal.ZERO

    private lateinit var itemTouchHelper: ItemTouchHelper

    private lateinit var binding: ActivityMainBinding
    private lateinit var historyAdapter: HistoryAdapter
    private lateinit var historyLayoutMgr: LinearLayoutManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //keeping screen on
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        )

        // Themes
        val themes = Themes(this)
        themes.applyDayNightOverride()
        setTheme(themes.getTheme())

        val fromPrefs = MyPreferences(this).numberingSystem
        numberingSystem = fromPrefs.toNumberingSystem()

        currentTheme = themes.getTheme()

        binding = ActivityMainBinding.inflate(layoutInflater)
        view = binding.root
        setContentView(view)

        // Disable the keyboard on display EditText
        binding.input.showSoftInputOnFocus = false

        // https://www.geeksforgeeks.org/how-to-detect-long-press-in-android/
        binding.backspaceButton.setOnLongClickListener {
            binding.input.setText("")
            binding.resultDisplay.text = ""
            isStillTheSameCalculation_autoSaveCalculationWithoutEqualOption = false
            true
        }

        // Long click to view popup options for double and triple zeroes
        binding.zeroButton.setOnLongClickListener {
            showPopupMenu(binding.zeroButton)
            true
        }

        // Set default animations and disable the fade out default animation
        // https://stackoverflow.com/questions/19943466/android-animatelayoutchanges-true-what-can-i-do-if-the-fade-out-effect-is-un
        val lt = LayoutTransition()
        lt.disableTransitionType(LayoutTransition.DISAPPEARING)
        binding.tableLayout.layoutTransition = lt

        // Set decimalSeparator
        binding.pointButton.setImageResource(if (decimalSeparatorSymbol == ",") R.drawable.comma else R.drawable.dot)

        // Set history
        historyLayoutMgr = LinearLayoutManager(
            this,
            LinearLayoutManager.VERTICAL,
            false
        )
        binding.historyRecylcleView.layoutManager = historyLayoutMgr
        historyAdapter = HistoryAdapter(
            mutableListOf(),
            { value ->
                updateDisplay(window.decorView, value)
            },
            this // Assuming this is an Activity or Fragment with a Context
        )
        historyAdapter.updateHistoryList()
        binding.historyRecylcleView.adapter = historyAdapter

        // Scroll to the bottom of the recycle view
        if (historyAdapter.itemCount > 0) {
            binding.historyRecylcleView.scrollToPosition(historyAdapter.itemCount - 1)
        }

        setSwipeTouchHelperForRecyclerView()

        // Disable history if setting enabled
        val historySize = MyPreferences(this).historySize!!.toInt()
        if (historySize == 0) {
            binding.historyRecylcleView.visibility = View.GONE
            binding.slidingLayoutButton.visibility = View.GONE
            binding.slidingLayout.isEnabled = false
        } else {
            binding.slidingLayoutButton.visibility = View.VISIBLE
            binding.slidingLayout.isEnabled = true
            checkEmptyHistoryForNoHistoryLabel()
        }


        // Set the sliding layout
        binding.slidingLayout.addPanelSlideListener(object : PanelSlideListener {
            override fun onPanelSlide(panel: View, slideOffset: Float) {
                if (slideOffset == 0f) { // If the panel got collapsed
                    binding.slidingLayout.scrollableView = binding.historyRecylcleView
                }
            }

            override fun onPanelStateChanged(
                panel: View,
                previousState: PanelState,
                newState: PanelState
            ) {
                if (newState == PanelState.ANCHORED) { // To prevent the panel from getting stuck in the middle
                    binding.slidingLayout.setPanelState(PanelState.EXPANDED)
                }
            }
        })

        // Set the history sliding layout button (click to open or close the history panel)
        binding.historySlidingLayoutButton.setOnClickListener {
            if (binding.slidingLayout.getPanelState() == PanelState.EXPANDED) {
                binding.slidingLayout.setPanelState(PanelState.COLLAPSED)
            } else {
                binding.slidingLayout.setPanelState(PanelState.EXPANDED)
            }
        }


        val textSizeAdjuster = TextSizeAdjuster(this)

        // Prevent the phone from sleeping (if option enabled)
        if (MyPreferences(this).preventPhoneFromSleepingMode) {
            view.keepScreenOn = true
        }

        // use radians instead of degrees by default (if option enabled)
        if (MyPreferences(this).useRadiansByDefault) {
            toggleDegreeMode()
        }

        // Check backspace location preference
        checkBackspacePosition()

        // Focus by default
        binding.input.requestFocus()

        // Makes the input take the whole width of the screen by default
        val screenWidthPX = resources.displayMetrics.widthPixels
        binding.input.minWidth =
            screenWidthPX - (binding.input.paddingRight + binding.input.paddingLeft) // remove the paddingHorizontal

        // Do not clear after equal button if you move the cursor
        binding.input.accessibilityDelegate = object : View.AccessibilityDelegate() {
            override fun sendAccessibilityEvent(host: View, eventType: Int) {
                super.sendAccessibilityEvent(host, eventType)
                if (eventType == AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED) {
                    isEqualLastAction = false
                }
                if (!binding.input.isCursorVisible) {
                    binding.input.isCursorVisible = true
                }
            }
        }

        // LongClick on result to copy it
        binding.resultDisplay.setOnLongClickListener {
            when {
                binding.resultDisplay.text.toString() != "" -> {
                    if (MyPreferences(this).longClickToCopyValue) {
                        val clipboardManager =
                            getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                        clipboardManager.setPrimaryClip(
                            ClipData.newPlainText(
                                R.string.copied_result.toString(),
                                binding.resultDisplay.text
                            )
                        )
                        // Only show a toast for Android 12 and lower.
                        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2)
                            Toast.makeText(this, R.string.value_copied, Toast.LENGTH_SHORT).show()
                        true
                    } else {
                        false
                    }
                }

                else -> false
            }
        }

        // Handle changes into input to update resultDisplay
        binding.input.addTextChangedListener(object : TextWatcher {
            private var beforeTextLength = 0

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                beforeTextLength = s?.length ?: 0
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateResultDisplay()
                textSizeAdjuster.adjustTextSize(binding.input,
                    TextSizeAdjuster.AdjustableTextType.Input
                )
            }

            override fun afterTextChanged(s: Editable?) {
                // Do nothing
            }
        })

        binding.resultDisplay.addTextChangedListener(object: TextWatcher {
            private var beforeTextLength = 0

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                beforeTextLength = s?.length ?: 0
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                textSizeAdjuster.adjustTextSize(binding.resultDisplay,
                    TextSizeAdjuster.AdjustableTextType.Output
                )
            }

            override fun afterTextChanged(s: Editable?) {
                // Do nothing
            }
        })

        // Close the history panel if the user use the back button else close the app
        // https://developer.android.com/guide/navigation/navigation-custom-back#kotlin
        this.onBackPressedDispatcher.addCallback(this) {
            if (binding.slidingLayout.getPanelState() == PanelState.EXPANDED) {
                binding.slidingLayout.setPanelState(PanelState.COLLAPSED)
            } else {
                finish()
            }
        }

    }

    private fun handleOnLockScreenAppStatus(canShow: Boolean) {
        if (canShow) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                setShowWhenLocked(true)
                setTurnScreenOn(true)
            } else {
                window.addFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                )
            }
        }else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                setShowWhenLocked(false)
                setTurnScreenOn(false)
            }else {
                window.clearFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                )
            }
        }
    }

    // Displays a popup menu with options to insert double zeros ("00") or triple zeros ("000") into the specified EditText when the zero button is long-pressed.
    private fun showPopupMenu(zeroButton: Button) {
        val popupMenu = PopupMenu(this, zeroButton)
        popupMenu.menuInflater.inflate(R.menu.popup_menu_zero, popupMenu.menu)
        popupMenu.setOnMenuItemClickListener { menuItem: MenuItem ->
            when (menuItem.itemId) {
                R.id.option_double_zero -> {
                    updateDisplay(view, "00")
                    true
                }
                R.id.option_triple_zero -> {
                    updateDisplay(view, "000")
                    true
                }
                else -> false
            }
        }
        popupMenu.show()

    }

    private fun setSwipeTouchHelperForRecyclerView() {
        val callBack = object :
            ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }
            override fun isItemViewSwipeEnabled(): Boolean {
                return MyPreferences(this@MainActivity).deleteHistoryOnSwipe
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                historyAdapter.removeHistoryElement(position)
                checkEmptyHistoryForNoHistoryLabel()
                deleteElementFromHistory(position)
            }
        }

        itemTouchHelper = ItemTouchHelper(callBack)
        itemTouchHelper.attachToRecyclerView(binding.historyRecylcleView)
    }

    private fun deleteElementFromHistory(position: Int) {
        lifecycleScope.launch(Dispatchers.Default) {
            val history = MyPreferences(this@MainActivity).getHistory()
            history.removeAt(position)
            MyPreferences(this@MainActivity).saveHistory(history)
        }
    }

    fun openAppMenu(view: View) {
        val popup = PopupMenu(this, view)
        val inflater = popup.menuInflater
        inflater.inflate(R.menu.app_menu, popup.menu)
        popup.show()
    }

    fun openAbout(menuItem: MenuItem) {
        val intent = Intent(this, AboutActivity::class.java)
        startActivity(intent, null)
    }

    fun openSettings(menuItem: MenuItem) {
        val intent = Intent(this, SettingsActivity::class.java)
        startActivity(intent, null)
    }

    fun openDonation(menuItem: MenuItem) {
        DonationDialog(this, layoutInflater).openDonationDialog()
    }

    fun clearHistory(menuItem: MenuItem) {
        // Clear preferences
        MyPreferences(this@MainActivity).saveHistory(mutableListOf())
        // Clear drawer
        historyAdapter.clearHistory()
        checkEmptyHistoryForNoHistoryLabel()
    }

    private fun keyVibration(view: View) {
        if (MyPreferences(this).vibrationMode) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            }
        }
    }

    private fun setErrorColor(errorStatus: Boolean) {
        // Only run if the color needs to be updated
        runOnUiThread {
            if (errorStatus != errorStatusOld) {
                // Set error color
                if (errorStatus) {
                    binding.input.setTextColor(
                        ContextCompat.getColor(
                            this,
                            R.color.calculation_error_color
                        )
                    )
                    binding.resultDisplay.setTextColor(
                        ContextCompat.getColor(
                            this,
                            R.color.calculation_error_color
                        )
                    )
                }
                // Clear error color
                else {
                    binding.input.setTextColor(ContextCompat.getColor(this, R.color.text_color))
                    binding.resultDisplay.setTextColor(
                        ContextCompat.getColor(
                            this,
                            R.color.text_second_color
                        )
                    )
                }
                errorStatusOld = errorStatus
            }
        }
    }

    private fun updateDisplay(view: View, value: String) {
        val valueNoSeparators = value.replace(groupingSeparatorSymbol, "")
        val isValueInt = valueNoSeparators.toIntOrNull() != null

        // Reset input with current number if following "equal"
        if (isEqualLastAction) {
            if (isValueInt || value == decimalSeparatorSymbol) {
                binding.input.setText("")
            } else {
                binding.input.setSelection(binding.input.text.length)
                binding.inputHorizontalScrollView.fullScroll(HorizontalScrollView.FOCUS_RIGHT)
            }
            isEqualLastAction = false
        }

        if (!binding.input.isCursorVisible) {
            binding.input.isCursorVisible = true
        }

        lifecycleScope.launch(Dispatchers.Default) {
            withContext(Dispatchers.Main) {
                // Vibrate when key pressed
                keyVibration(view)
            }

            val formerValue = binding.input.text.toString()
            val cursorPosition = binding.input.selectionStart
            val leftValue = formerValue.subSequence(0, cursorPosition).toString()
            val leftValueFormatted =
                NumberFormatter.format(leftValue, decimalSeparatorSymbol, groupingSeparatorSymbol, numberingSystem)
            val rightValue = formerValue.subSequence(cursorPosition, formerValue.length).toString()

            val newValue = leftValue + value + rightValue

            val newValueFormatted =
                NumberFormatter.format(newValue, decimalSeparatorSymbol, groupingSeparatorSymbol, numberingSystem)

            withContext(Dispatchers.Main) {
                // Update Display
                binding.input.setText(newValueFormatted)

                // Set cursor position
                if (isValueInt) {
                    val cursorOffset = newValueFormatted.length - newValue.length
                    binding.input.setSelection(cursorPosition + value.length + cursorOffset)
                } else {
                    val desiredCursorPosition = leftValueFormatted.length + value.length
                    // Limit the cursor position to the length of the input
                    val safeCursorPosition = desiredCursorPosition.coerceAtMost(binding.input.text.length)
                    binding.input.setSelection(safeCursorPosition)
                }
            }
        }
    }

    private fun roundResult(result: BigDecimal): BigDecimal {
        val numberPrecision = MyPreferences(this).numberPrecision!!.toInt()
        var newResult = result.setScale(numberPrecision, RoundingMode.HALF_EVEN)
        if (MyPreferences(this).numberIntoScientificNotation && (newResult >= BigDecimal(9999) || newResult <= BigDecimal(
                0.1
            ))
        ) {
            val scientificString = String.format(Locale.US, "%.4g", result)
            newResult = BigDecimal(scientificString)
        }

        // Fix how is displayed 0 with BigDecimal
        val tempResult = newResult.toString().replace("E-", "").replace("E", "")
        val allCharsEqualToZero = tempResult.all { it == '0' }
        if (
            allCharsEqualToZero
            || newResult.toString().startsWith("0E")
        ) {
            return BigDecimal.ZERO
        }

        return newResult
    }

    private fun enableOrDisableScientistMode() {
        if (binding.scientistModeRow2.visibility != View.VISIBLE) {
            binding.scientistModeRow2.visibility = View.VISIBLE
            binding.scientistModeRow3.visibility = View.VISIBLE
            binding.scientistModeSwitchButton?.setImageResource(R.drawable.ic_baseline_keyboard_arrow_up_24)
            binding.degreeTextView.visibility = View.VISIBLE
            if (isDegreeModeActivated) {
                binding.degreeButton.text = getString(R.string.radian)
                binding.degreeTextView.text = getString(R.string.degree)
            }
            else {
                binding.degreeButton.text = getString(R.string.degree)
                binding.degreeTextView.text = getString(R.string.radian)
            }
        } else {
            binding.scientistModeRow2.visibility = View.GONE
            binding.scientistModeRow3.visibility = View.GONE
            binding.scientistModeSwitchButton?.setImageResource(R.drawable.ic_baseline_keyboard_arrow_down_24)
            binding.degreeTextView.visibility = View.GONE
        }
    }

    // Switch between degree and radian mode
    private fun toggleDegreeMode() {
        isDegreeModeActivated = !isDegreeModeActivated
        if (isDegreeModeActivated) {
            binding.degreeButton.text = getString(R.string.radian)
            binding.degreeTextView.text = getString(R.string.degree)
        }
        else {
            binding.degreeButton.text = getString(R.string.degree)
            binding.degreeTextView.text = getString(R.string.radian)
        }

        // Flip the variable afterwards
        //isDegreeModeActivated = !isDegreeModeActivated
    }

    @SuppressLint("SetTextI18n")
    private fun updateResultDisplay() {
        lifecycleScope.launch(Dispatchers.Default) {
            // Reset text color
            setErrorColor(false)

            val calculation = binding.input.text.toString()

            if (calculation != "") {
                division_by_0 = false
                domain_error = false
                syntax_error = false
                is_infinity = false
                require_real_number = false

                val calculationTmp = Expression().getCleanExpression(
                    binding.input.text.toString(),
                    decimalSeparatorSymbol,
                    groupingSeparatorSymbol
                )
                calculationResult =
                    Calculator(MyPreferences(this@MainActivity).numberPrecision!!.toInt()).evaluate(
                        calculationTmp,
                        isDegreeModeActivated
                    )

                // If result is a number and it is finite
                if (!(division_by_0 || domain_error || syntax_error || is_infinity || require_real_number)) {

                    // Round
                    calculationResult = roundResult(calculationResult)
                    var formattedResult = NumberFormatter.format(
                        calculationResult.toString().replace(".", decimalSeparatorSymbol),
                        decimalSeparatorSymbol,
                        groupingSeparatorSymbol,
                        numberingSystem
                    )

                    // Remove zeros at the end of the results (after point)
                    if (!MyPreferences(this@MainActivity).numberIntoScientificNotation || !(calculationResult >= BigDecimal(
                            9999
                        ) || calculationResult <= BigDecimal(0.1))
                    ) {
                        val resultSplited = calculationResult.toString().split('.')
                        if (resultSplited.size > 1) {
                            val resultPartAfterDecimalSeparator = resultSplited[1].trimEnd('0')
                            var resultWithoutZeros = resultSplited[0]
                            if (resultPartAfterDecimalSeparator != "") {
                                resultWithoutZeros =
                                    resultSplited[0] + "." + resultPartAfterDecimalSeparator
                            }
                            formattedResult = NumberFormatter.format(
                                resultWithoutZeros.replace(
                                    ".",
                                    decimalSeparatorSymbol
                                ), decimalSeparatorSymbol,
                                groupingSeparatorSymbol,
                                numberingSystem
                            )
                        }
                    }

                    withContext(Dispatchers.Main) {
                        if (formattedResult != calculation) {
                            binding.resultDisplay.text = formattedResult
                        } else {
                            binding.resultDisplay.text = ""
                        }
                    }

                    // Save to history if the option autoSaveCalculationWithoutEqualButton is enabled
                    if (MyPreferences(this@MainActivity).autoSaveCalculationWithoutEqualButton) {
                        if (calculation != formattedResult) {
                            val history = MyPreferences(this@MainActivity).getHistory()

                            if (isStillTheSameCalculation_autoSaveCalculationWithoutEqualOption) {
                                // If it's the same calculation as the previous one
                                // Get previous calculation and update it
                                val previousHistoryElement = MyPreferences(this@MainActivity).getHistoryElementById(
                                    lastHistoryElementId
                                )
                                if (previousHistoryElement != null) {
                                    previousHistoryElement.calculation = calculation
                                    previousHistoryElement.result = formattedResult
                                    previousHistoryElement.time = System.currentTimeMillis().toString()
                                    MyPreferences(this@MainActivity).updateHistoryElementById(lastHistoryElementId, previousHistoryElement)
                                    withContext(Dispatchers.Main) {
                                        historyAdapter.updateHistoryElement(previousHistoryElement)
                                    }
                                }
                            } else {
                                // if it's a new calculation

                                // Store time
                                val currentTime = System.currentTimeMillis().toString()

                                // Save to history
                                val historyElementId = UUID.randomUUID().toString()
                                history.add(
                                    History(
                                        calculation = calculation,
                                        result = formattedResult,
                                        time = currentTime,
                                        id = historyElementId
                                    )
                                )

                                lastHistoryElementId = historyElementId
                                isStillTheSameCalculation_autoSaveCalculationWithoutEqualOption = true

                                MyPreferences(this@MainActivity).saveHistory(history)

                                // Update history variables in the UI
                                withContext(Dispatchers.Main) {
                                    historyAdapter.appendOneHistoryElement(
                                        History(
                                            calculation = calculation,
                                            result = formattedResult,
                                            time = currentTime,
                                            id = UUID.randomUUID().toString() // Generate a random id
                                        )
                                    )

                                    // Remove former results if > historySize preference
                                    val historySize = MyPreferences(this@MainActivity).historySize!!.toInt()
                                    while (historySize != -1 && historyAdapter.itemCount >= historySize && historyAdapter.itemCount > 0) {
                                        historyAdapter.removeFirstHistoryElement()
                                    }
                                    checkEmptyHistoryForNoHistoryLabel()

                                    // Scroll to the bottom of the recycle view
                                    binding.historyRecylcleView.scrollToPosition(historyAdapter.itemCount - 1)
                                }
                            }
                        }
                    }

                } else withContext(Dispatchers.Main) {
                    if (is_infinity && !division_by_0 && !domain_error && !require_real_number) {
                        if (calculationResult < BigDecimal.ZERO) binding.resultDisplay.text = "-" + getString(
                            R.string.infinity
                        )
                        else binding.resultDisplay.text = getString(R.string.value_too_large)
                    } else {
                        withContext(Dispatchers.Main) {
                            binding.resultDisplay.text = ""
                        }
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    binding.resultDisplay.text = ""
                }
            }
        }
    }

    fun keyDigitPadMappingToDisplay(view: View) {
        updateDisplay(view, (view as Button).text as String)
    }

    @SuppressLint("SetTextI18n")
    private fun addSymbol(view: View, currentSymbol: String) {
        // Get input text length
        val textLength = binding.input.text.length

        // If the input is not empty
        if (textLength > 0) {
            // Get cursor's current position
            val cursorPosition = binding.input.selectionStart

            // Get next / previous characters relative to the cursor
            val nextChar =
                if (textLength - cursorPosition > 0) binding.input.text[cursorPosition].toString() else "0" // use "0" as default like it's not a symbol
            val previousChar =
                if (cursorPosition > 0) binding.input.text[cursorPosition - 1].toString() else "0"
            val prevSymbol =
                if (cursorPosition > 1) binding.input.text[cursorPosition - 2].toString() else "0"

            if (currentSymbol != previousChar // Ignore multiple presses of the same button
                && currentSymbol != nextChar
                && previousChar != "√" // No symbol can be added on an empty square root
                && previousChar != decimalSeparatorSymbol // Ensure that the previous character is not a comma
                && (previousChar != "(" // Ensure that we are not at the beginning of a parenthesis
                        || currentSymbol == "-")
                && (prevSymbol !in "+\\-÷×" || previousChar !in "+\\-÷×")
            ) { // Minus symbol is an override
                // If previous character is a symbol, replace it
                if (previousChar.matches("[+\\-÷×^]".toRegex())) {
                    keyVibration(view)

                    val leftString = binding.input.text.subSequence(0, cursorPosition - 1).toString()
                    val rightString = binding.input.text.subSequence(cursorPosition, textLength).toString()

                    // Add a parenthesis if there is another symbol before minus
                    if (currentSymbol == "-") {
                        if (previousChar in "+-") {
                            binding.input.setText(leftString + currentSymbol + rightString)
                            binding.input.setSelection(cursorPosition)
                        } else {
                            binding.input.setText(leftString + previousChar + currentSymbol + rightString)
                            binding.input.setSelection(cursorPosition + 1)
                        }
                    } else if (cursorPosition > 1 && binding.input.text[cursorPosition - 2] != '(') {
                        binding.input.setText(leftString + currentSymbol + rightString)
                        binding.input.setSelection(cursorPosition)
                    } else if (currentSymbol == "+") {
                        binding.input.setText(leftString + rightString)
                        binding.input.setSelection(cursorPosition - 1)
                    }
                }
                // If next character is a symbol, replace it
                else if (nextChar.matches("[+\\-÷×^%!]".toRegex())
                    && currentSymbol != "%"
                ) { // Make sure that percent symbol doesn't replace succeeding symbols
                    keyVibration(view)

                    val leftString = binding.input.text.subSequence(0, cursorPosition).toString()
                    val rightString = binding.input.text.subSequence(cursorPosition + 1, textLength).toString()

                    if (cursorPosition > 0 && previousChar != "(") {
                        binding.input.setText(leftString + currentSymbol + rightString)
                        binding.input.setSelection(cursorPosition + 1)
                    } else if (currentSymbol == "+") binding.input.setText(leftString + rightString)
                }
                // Otherwise just update the display
                else if (cursorPosition > 0 || nextChar != "0" && currentSymbol == "-") {
                    updateDisplay(view, currentSymbol)
                } else keyVibration(view)
            } else keyVibration(view)
        } else { // Allow minus symbol, even if the input is empty
            if (currentSymbol == "-") updateDisplay(view, currentSymbol)
            else keyVibration(view)
        }
    }

    fun addButton(view: View) {
        addSymbol(view, "+")
    }

    fun subtractButton(view: View) {
        addSymbol(view, "-")
    }

    fun divideButton(view: View) {
        addSymbol(view, "÷")
    }

    fun multiplyButton(view: View) {
        addSymbol(view, "×")
    }

    fun exponentButton(view: View) {
        addSymbol(view, "^")
    }

    fun pointButton(view: View) {
        val cursorPosition = binding.input.selectionStart
        var currentNumber = ""
        if (binding.input.text.toString().isNotEmpty()) {
            var startPosition = 0
            var endPosition = 0
            if (cursorPosition > 0) {
                startPosition = cursorPosition
                while (startPosition > 0 && (binding.input.text[startPosition - 1].isDigit()
                            || binding.input.text[startPosition - 1].toString() == decimalSeparatorSymbol
                            || binding.input.text[startPosition - 1].toString() == groupingSeparatorSymbol)) {
                    startPosition -= 1
                }
            }
            if (cursorPosition == binding.input.text.length) {
                endPosition = binding.input.text.length
            }
            if (cursorPosition < binding.input.text.length) {
                endPosition = if (cursorPosition != 0) cursorPosition else 0
                while (endPosition < binding.input.text.length
                    && (binding.input.text[endPosition].isDigit()
                            || binding.input.text[endPosition].toString() == decimalSeparatorSymbol
                            || binding.input.text[endPosition].toString() == groupingSeparatorSymbol)) {
                        endPosition += 1
                    }
            }
            currentNumber = binding.input.text.substring(startPosition, endPosition)
        }
        if (decimalSeparatorSymbol !in currentNumber) {
            updateDisplay(view, decimalSeparatorSymbol)
        }
    }

    fun sineButton(view: View) {
        if (!isInvButtonClicked) {
            updateDisplay(view, "sin(")
        } else {
            updateDisplay(view, "sin⁻¹(")
        }
    }

    fun cosineButton(view: View) {
        if (!isInvButtonClicked) {
            updateDisplay(view, "cos(")
        } else {
            updateDisplay(view, "cos⁻¹(")
        }
    }

    fun tangentButton(view: View) {
        if (!isInvButtonClicked) {
            updateDisplay(view, "tan(")
        } else {
            updateDisplay(view, "tan⁻¹(")
        }
    }

    fun eButton(view: View) {
        updateDisplay(view, "e")
    }

    fun naturalLogarithmButton(view: View) {
        if (!isInvButtonClicked) {
            updateDisplay(view, "ln(")
        } else {
            updateDisplay(view, "exp(")
        }
    }

    fun logarithmButton(view: View) {
        if (!isInvButtonClicked) {
            updateDisplay(view, "log(")
        } else {
            updateDisplay(view, "10^")
        }
    }

    fun log2Button(view: View) {
        if (!isInvButtonClicked) {
            updateDisplay(view, "log₂(")
        } else {
            updateDisplay(view, "2^")
        }
    }

    fun piButton(view: View) {
        updateDisplay(view, "π")
    }

    fun factorialButton(view: View) {
        addSymbol(view, "!")
    }

    fun squareButton(view: View) {
        if (!isInvButtonClicked) {
            updateDisplay(view, "√")
        } else {
            if (MyPreferences(this).addModuloButton) {
                updateDisplay(view, "#")
            } else {
                updateDisplay(view, "^2")
            }

        }
    }

    fun percent(view: View) {
        addSymbol(view, "%")
    }

    @SuppressLint("SetTextI18n")
    fun degreeButton(view: View) {
        keyVibration(view)
        toggleDegreeMode()
        updateResultDisplay()
    }

    fun invButton(view: View) {
        keyVibration(view)

        if (!isInvButtonClicked) {
            isInvButtonClicked = true

            // change buttons
            binding.sineButton.setText(R.string.sineInv)
            binding.cosineButton.setText(R.string.cosineInv)
            binding.tangentButton.setText(R.string.tangentInv)
            binding.naturalLogarithmButton.setText(R.string.naturalLogarithmInv)
            binding.logarithmButton.setText(R.string.logarithmInv)
            binding.log2Button?.setText(R.string.logtwoInv)
            if (MyPreferences(this).addModuloButton) {
                binding.squareButton.setText(R.string.squareInvModuloVersion)
            } else {
                binding.squareButton.setText(R.string.squareInv)
            }

        } else {
            isInvButtonClicked = false

            // change buttons
            binding.sineButton.setText(R.string.sine)
            binding.cosineButton.setText(R.string.cosine)
            binding.tangentButton.setText(R.string.tangent)
            binding.naturalLogarithmButton.setText(R.string.naturalLogarithm)
            binding.logarithmButton.setText(R.string.logarithm)
            binding.log2Button?.setText(R.string.logtwo)
            binding.squareButton.setText(R.string.square)
        }
    }

    fun clearButton(view: View) {
        keyVibration(view)
        binding.input.setText("")
        binding.resultDisplay.text = ""
        isStillTheSameCalculation_autoSaveCalculationWithoutEqualOption = false
    }

    @SuppressLint("SetTextI18n")
    fun equalsButton(view: View) {
        lifecycleScope.launch(Dispatchers.Default) {
            keyVibration(view)

            val calculation = binding.input.text.toString()

            Expression().addParenthesis(calculation)

            if (calculation != "") {

                val resultString = calculationResult.toString()
                var formattedResult = NumberFormatter.format(
                    resultString.replace(".", decimalSeparatorSymbol),
                    decimalSeparatorSymbol,
                    groupingSeparatorSymbol,
                    numberingSystem
                )

                // If result is a number and it is finite
                if (!(division_by_0 || domain_error || syntax_error || is_infinity || require_real_number)) {

                    // Remove zeros at the end of the results (after point)
                    val resultSplited = resultString.split('.')
                    if (resultSplited.size > 1) {
                        val resultPartAfterDecimalSeparator = resultSplited[1].trimEnd('0')
                        var resultWithoutZeros = resultSplited[0]
                        if (resultPartAfterDecimalSeparator != "") {
                            resultWithoutZeros =
                                resultSplited[0] + "." + resultPartAfterDecimalSeparator
                        }
                        formattedResult = NumberFormatter.format(
                            resultWithoutZeros.replace(
                                ".",
                                decimalSeparatorSymbol
                            ), decimalSeparatorSymbol, groupingSeparatorSymbol,
                            numberingSystem
                        )
                    }

                    // Hide the cursor before updating binding.input to avoid weird cursor movement
                    withContext(Dispatchers.Main) {
                        binding.input.isCursorVisible = false
                    }

                    // Display result
                    withContext(Dispatchers.Main) { binding.input.setText(formattedResult) }

                    // Set cursor
                    withContext(Dispatchers.Main) {
                        // Scroll to the end
                        binding.input.setSelection(binding.input.length())

                        // Hide the cursor (do not remove this, it's not a duplicate)
                        binding.input.isCursorVisible = false

                        // Clear resultDisplay
                        binding.resultDisplay.text = ""
                    }

                    if (calculation != formattedResult) {
                        val history = MyPreferences(this@MainActivity).getHistory()

                        isStillTheSameCalculation_autoSaveCalculationWithoutEqualOption = false

                        // Do not save to history if the previous entry is the same as the current one
                        if (history.isEmpty() || history[history.size - 1].calculation != calculation) {
                            // Store time
                            val currentTime = System.currentTimeMillis().toString()

                            // Save to history
                            val historyElementId = UUID.randomUUID().toString()
                            history.add(
                                History(
                                    calculation = calculation,
                                    result = formattedResult,
                                    time = currentTime,
                                    id = historyElementId // Generate a random id
                                )
                            )

                            MyPreferences(this@MainActivity).saveHistory(history)

                            lastHistoryElementId = historyElementId

                            // Update history variables in the UI
                            withContext(Dispatchers.Main) {
                                historyAdapter.appendOneHistoryElement(
                                    History(
                                        calculation = calculation,
                                        result = formattedResult,
                                        time = currentTime,
                                        id = UUID.randomUUID().toString() // Generate a random id
                                    )
                                )

                                // Remove former results if > historySize preference
                                val historySize = MyPreferences(this@MainActivity).historySize!!.toInt()
                                while (historySize != -1 && historyAdapter.itemCount >= historySize && historyAdapter.itemCount > 0) {
                                    historyAdapter.removeFirstHistoryElement()
                                }
                                checkEmptyHistoryForNoHistoryLabel()
                                // Scroll to the bottom of the recycle view
                                binding.historyRecylcleView.scrollToPosition(historyAdapter.itemCount - 1)
                            }
                        }
                    }
                    isEqualLastAction = true
                } else {
                    withContext(Dispatchers.Main) {
                        if (syntax_error) {
                            setErrorColor(true)
                            binding.resultDisplay.text = getString(R.string.syntax_error)
                        } else if (domain_error) {
                            setErrorColor(true)
                            binding.resultDisplay.text = getString(R.string.domain_error)
                        } else if (require_real_number) {
                            setErrorColor(true)
                            binding.resultDisplay.text = getString(R.string.require_real_number)
                        } else if (division_by_0) {
                            setErrorColor(true)
                            binding.resultDisplay.text = getString(R.string.division_by_0)
                        } else if (is_infinity) {
                            if (calculationResult < BigDecimal.ZERO) binding.resultDisplay.text = "-" + getString(
                                R.string.infinity
                            )
                            else binding.resultDisplay.text = getString(R.string.value_too_large)
                            //} else if (result.isNaN()) {
                            //    setErrorColor(true)
                            //    binding.resultDisplay.setText(getString(R.string.math_error))
                        } else {
                            binding.resultDisplay.text = formattedResult
                            isEqualLastAction =
                                true // Do not clear the calculation (if you click into a number) if there is an error
                        }
                    }
                }

            } else {
                withContext(Dispatchers.Main) { binding.resultDisplay.text = "" }
            }
        }
    }

    fun leftParenthesisButton(view: View) {
        updateDisplay(view, "(")
    }

    fun rightParenthesisButton(view: View) {
        updateDisplay(view, ")")
    }

    fun parenthesesButton(view: View) {
        val cursorPosition = binding.input.selectionStart
        val textLength = binding.input.text.length

        var openParentheses = 0
        var closeParentheses = 0

        val text = binding.input.text.toString()

        for (i in 0 until cursorPosition) {
            if (text[i] == '(') {
                openParentheses += 1
            }
            if (text[i] == ')') {
                closeParentheses += 1
            }
        }

        if (
            !(textLength > cursorPosition && binding.input.text.toString()[cursorPosition] in "×÷+-^")
            && (
                    openParentheses == closeParentheses
                            || binding.input.text.toString()[cursorPosition - 1] == '('
                            || binding.input.text.toString()[cursorPosition - 1] in "×÷+-^"
                    )
        ) {
            updateDisplay(view, "(")
        } else {
            updateDisplay(view, ")")
        }
    }

    fun backspaceButton(view: View) {
        keyVibration(view)

        var cursorPosition = binding.input.selectionStart
        val textLength = binding.input.text.length
        var newValue = ""
        var isFunction = false
        var isDecimal = false
        var functionLength = 0

        if (isEqualLastAction) {
            cursorPosition = textLength
        }

        if (cursorPosition != 0 && textLength != 0) {
            // Check if it is a function to delete
            val functionsList =
                listOf("cos⁻¹(", "sin⁻¹(", "tan⁻¹(", "cos(", "sin(", "tan(", "ln(", "log(", "log₂(", "exp(")
            for (function in functionsList) {
                val leftPart = binding.input.text.subSequence(0, cursorPosition).toString()
                if (leftPart.endsWith(function)) {
                    newValue = binding.input.text.subSequence(0, cursorPosition - function.length)
                        .toString() +
                            binding.input.text.subSequence(cursorPosition, textLength).toString()
                    isFunction = true
                    functionLength = function.length - 1
                    break
                }
            }
            // Else
            if (!isFunction) {
                // remove the grouping separator
                val leftPart = binding.input.text.subSequence(0, cursorPosition).toString()
                val leftPartWithoutSpaces = leftPart.replace(groupingSeparatorSymbol, "")
                functionLength = leftPart.length - leftPartWithoutSpaces.length

                newValue = leftPartWithoutSpaces.subSequence(0, leftPartWithoutSpaces.length - 1)
                    .toString() +
                        binding.input.text.subSequence(cursorPosition, textLength).toString()

                isDecimal = binding.input.text[cursorPosition - 1] == decimalSeparatorSymbol[0]
            }

            // Handle decimal deletion as a special case when finding cursor position
            var rightSideCommas = 0
            if (isDecimal) {
                val oldString = binding.input.text
                var immediateRightDigits = 0
                var index = cursorPosition
                // Find number of digits that were previously to the right of the decimal
                while (index < textLength && oldString[index].isDigit()) {
                    index++
                    immediateRightDigits++
                }
                // Determine how many thousands separators that gives us to our right
                if (immediateRightDigits > 3)
                    rightSideCommas = immediateRightDigits / 3
            }

            val newValueFormatted =
                NumberFormatter.format(newValue, decimalSeparatorSymbol, groupingSeparatorSymbol, numberingSystem)
            var cursorOffset = newValueFormatted.length - newValue.length - rightSideCommas
            if (cursorOffset < 0) cursorOffset = 0

            binding.input.setText(newValueFormatted)
            binding.input.setSelection((cursorPosition - 1 + cursorOffset - functionLength).takeIf { it > 0 }
                ?: 0)
        }
    }

    // Check to see if the location of the backspace button should be to the left or default
    private fun checkBackspacePosition() {
        if (MyPreferences(this).moveBackButtonLeft
            && resources.configuration.orientation != Configuration.ORIENTATION_LANDSCAPE) {
            val row = findViewById<TableRow>(R.id.delRow)
            val backButton = row.getChildAt(2)
            if (backButton.id == R.id.backspaceButton) {
                row.removeViewAt(2)
                row.addView(backButton, 0)
            }
        } else if (!MyPreferences(this).moveBackButtonLeft
            && resources.configuration.orientation != Configuration.ORIENTATION_LANDSCAPE) {
            val row = findViewById<TableRow>(R.id.delRow)
            val backButton = row.getChildAt(0)
            // Only move if backspace is in position zero in row.
            if (backButton.id == R.id.backspaceButton) {
                row.removeViewAt(0)
                row.addView(backButton, 2)
            }
        }
    }

    fun scientistModeSwitchButton(view: View) {
        enableOrDisableScientistMode()
    }

    private fun updateInputDisplay() {
        val expression = binding.input.text.toString()
        val formatted = NumberFormatter.format(expression, decimalSeparatorSymbol, groupingSeparatorSymbol, numberingSystem)
        val cursorPosition = binding.input.selectionStart
        binding.input.setText(formatted)
        // Set cursor to previous location before resume.
        // Setting text on resume resets cursor to position 0
        binding.input.setSelection(cursorPosition)
    }

    // Update settings
    override fun onResume() {
        super.onResume()

        if (resources.configuration.orientation != Configuration.ORIENTATION_LANDSCAPE) {
            // scientific mode enabled by default in portrait mode (if option enabled)
            val storedType = MyPreferences(this).scientificMode
            scientificModeType = ScientificMode.getScientificModeType(storedType)
            manageScientificMode(scientificModeType)
        }


        val fromPrefs = MyPreferences(this).numberingSystem
        numberingSystem = fromPrefs.toNumberingSystem()

        // Update the theme
        val themes = Themes(this)
        if (currentTheme != themes.getTheme()) {
            (this as Activity).finish()
            ContextCompat.startActivity(this, this.intent, null)
        }

        if (appLanguage != Locale.getDefault()) {
            appLanguage = Locale.getDefault()
            // Clear inputs to avoid conflicts with decimal & grouping separators
            binding.input.setText("")
            binding.resultDisplay.text = ""
        } else {
            updateResultDisplay()
            updateInputDisplay()
        }

        // Split the parentheses button (if option is enabled)
        if (MyPreferences(this).splitParenthesisButton) {
            // Hide the AC button
            binding.clearButton.visibility = View.GONE
            binding.parenthesesButton.visibility = View.GONE

            // Display the left & right parenthesis buttons
            binding.leftParenthesisButton?.visibility = View.VISIBLE
            binding.rightParenthesisButton?.visibility = View.VISIBLE
        } else {
            // Display the AC button
            binding.clearButton.visibility = View.VISIBLE
            binding.parenthesesButton.visibility = View.VISIBLE

            // Hide the left & right parenthesis buttons
            binding.leftParenthesisButton?.visibility = View.GONE
            binding.rightParenthesisButton?.visibility = View.GONE
        }

        // Prevent phone from sleeping while the app is in foreground
        view.keepScreenOn = MyPreferences(this).preventPhoneFromSleepingMode

        // Remove former results if > historySize preference
        // Remove from the RecycleView
        val historySize = MyPreferences(this@MainActivity).historySize!!.toInt()
        while (historySize != -1 && historyAdapter.itemCount >= historySize && historyAdapter.itemCount > 0) {
            historyAdapter.removeFirstHistoryElement()
        }
        // Remove from the preference store data
        val history = MyPreferences(this@MainActivity).getHistory()
        while (historySize > 0 && history.size > historySize) {
            history.removeAt(0)
        }
        MyPreferences(this@MainActivity).saveHistory(history)

        // Disable history if setting enabled
        if (historySize == 0) {
            binding.historyRecylcleView.visibility = View.GONE
            binding.slidingLayoutButton.visibility = View.GONE
            binding.slidingLayout.isEnabled = false
        } else {
            binding.slidingLayoutButton.visibility = View.VISIBLE
            binding.slidingLayout.isEnabled = true
            checkEmptyHistoryForNoHistoryLabel()
        }

        // Check backspace location preference
        checkBackspacePosition()

        // Disable the keyboard on display EditText
        binding.input.showSoftInputOnFocus = false

        // Enable the possibility to show the activity on the lock screen
        val canShowOnLockScreen = MyPreferences(this).showOnLockScreen
        handleOnLockScreenAppStatus(canShowOnLockScreen)
}

    fun checkEmptyHistoryForNoHistoryLabel() {
        if (historyAdapter.itemCount==0) {
            binding.historyRecylcleView.visibility = View.GONE
            binding.noHistoryText.visibility = View.VISIBLE
        }else {
            binding.noHistoryText.visibility = View.GONE
            binding.historyRecylcleView.visibility = View.VISIBLE
        }
    }

    private fun manageScientificMode(scientificModeTypes: ScientificModeTypes) {
        when (scientificModeTypes) {
            ScientificModeTypes.OFF -> hideScientificMode()
            ScientificModeTypes.ACTIVE -> enableOrDisableScientistMode(true)
            ScientificModeTypes.NOT_ACTIVE -> enableOrDisableScientistMode(false)
        }
    }


    private fun enableOrDisableScientistMode(isEnabled: Boolean) {
        val imageId = if (isEnabled) R.drawable.ic_baseline_keyboard_arrow_up_24 else R.drawable.ic_baseline_keyboard_arrow_down_24
        binding.scientistModeRow1?.isVisible = true
        binding.scientistModeRow2.isVisible = isEnabled
        binding.scientistModeRow3.isVisible = isEnabled
        binding.degreeTextView.visibility = View.VISIBLE
        binding.scientistModeSwitchButton?.setImageResource(imageId)

        if (isDegreeModeActivated) {
            binding.degreeButton.text = getString(R.string.radian)
            binding.degreeTextView.text = getString(R.string.degree)
        } else {
            binding.degreeButton.text = getString(R.string.degree)
            binding.degreeTextView.text = getString(R.string.radian)
        }

    }

    private fun hideScientificMode() {
        binding.scientistModeRow1?.visibility = View.GONE
        binding.scientistModeRow2.visibility = View.GONE
        binding.scientistModeRow3.visibility = View.GONE
        binding.degreeTextView.visibility = View.GONE
    }
}