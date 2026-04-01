package com.androcalc

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.androcalc.databinding.ActivityMainBinding
import kotlin.math.abs
import java.util.Locale

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var binding: ActivityMainBinding
    private var tts: TextToSpeech? = null
    private var ttsReady = false

    private var firstOperand: Int? = null
    private var secondOperand: String = ""
    private var operation: Char? = null
    private var currentInput: String = ""
    private var showingResult = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tts = TextToSpeech(this, this)

        setupNumberButtons()
        setupActionButtons()
        updateDisplay()
    }

    private fun setupNumberButtons() {
        val numberButtons = listOf(
            binding.btn0,
            binding.btn1,
            binding.btn2,
            binding.btn3,
            binding.btn4,
            binding.btn5,
            binding.btn6,
            binding.btn7,
            binding.btn8,
            binding.btn9
        )

        numberButtons.forEach { button ->
            button.setOnClickListener {
                handleNumberInput((it as Button).text.toString())
            }
        }
    }

    private fun setupActionButtons() {
        binding.btnPlus.setOnClickListener { handleOperator('+') }
        binding.btnMinus.setOnClickListener { handleOperator('-') }
        binding.btnMultiply.setOnClickListener { handleOperator('*') }
        binding.btnDivide.setOnClickListener { handleOperator('/') }

        binding.btnEquals.setOnClickListener { handleEquals() }
        binding.btnClear.setOnClickListener { clearAll() }
    }

    private fun handleNumberInput(digit: String) {
        if (showingResult && operation == null) {
            clearAll(speak = false)
        }

        if (operation == null) {
            currentInput = appendDigit(currentInput, digit)
        } else {
            secondOperand = appendDigit(secondOperand, digit)
        }

        speak(numberToWords(digit.toInt()))
        updateDisplay()
    }

    private fun handleOperator(newOperator: Char) {
        if (currentInput.isEmpty()) {
            return
        }

        if (showingResult) {
            showingResult = false
        }

        if (firstOperand == null) {
            firstOperand = currentInput.toInt()
        }

        operation = newOperator
        speak(operatorWord(newOperator))
        updateDisplay()
    }

    private fun handleEquals() {
        val first = firstOperand ?: currentInput.toIntOrNull() ?: return
        val op = operation ?: return
        val second = secondOperand.toIntOrNull() ?: return

        val result = calculate(first, second, op)

        if (result == null) {
            binding.tvExpression.text = getString(R.string.error_text)
            speak(getString(R.string.error_text))
            resetAfterError()
            return
        }

        val sentence = "${numberToWords(first)} ${operatorWord(op)} ${numberToWords(second)} equals ${numberToWords(result)}"
        speak(sentence)

        currentInput = result.toString()
        firstOperand = null
        secondOperand = ""
        operation = null
        showingResult = true

        updateDisplay()
    }

    private fun calculate(first: Int, second: Int, op: Char): Int? {
        return when (op) {
            '+' -> first + second
            '-' -> first - second
            '*' -> first * second
            '/' -> if (second == 0) null else first / second
            else -> null
        }
    }

    private fun appendDigit(existing: String, digit: String): String {
        return if (existing == "0") digit else existing + digit
    }

    private fun clearAll(speak: Boolean = true) {
        firstOperand = null
        secondOperand = ""
        operation = null
        currentInput = ""
        showingResult = false
        updateDisplay()
        if (speak) {
            speak("clear")
        }
    }

    private fun resetAfterError() {
        firstOperand = null
        secondOperand = ""
        operation = null
        currentInput = ""
        showingResult = false
    }

    private fun updateDisplay() {
        val text = when {
            currentInput.isEmpty() && operation == null -> "0"
            operation == null -> currentInput
            else -> {
                val first = firstOperand?.toString().orEmpty()
                val second = secondOperand
                "$first $operation $second".trim()
            }
        }
        binding.tvExpression.text = text
    }

    private fun operatorWord(op: Char): String {
        return when (op) {
            '+' -> "plus"
            '-' -> "minus"
            '*' -> "times"
            '/' -> "divided by"
            else -> ""
        }
    }

    private fun numberToWords(number: Int): String {
        if (number == 0) return "zero"

        val ones = arrayOf(
            "", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine"
        )
        val teens = arrayOf(
            "ten", "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen", "nineteen"
        )
        val tens = arrayOf(
            "", "", "twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety"
        )

        fun threeDigitsToWords(value: Int): String {
            val hundreds = value / 100
            val remainder = value % 100
            val parts = mutableListOf<String>()

            if (hundreds > 0) {
                parts += "${ones[hundreds]} hundred"
            }

            when {
                remainder in 10..19 -> parts += teens[remainder - 10]
                remainder >= 20 -> {
                    parts += tens[remainder / 10]
                    if (remainder % 10 != 0) {
                        parts += ones[remainder % 10]
                    }
                }
                remainder > 0 -> parts += ones[remainder]
            }

            return parts.joinToString(" ")
        }

        val sign = if (number < 0) "minus " else ""
        val absolute = abs(number)
        val thousands = absolute / 1000
        val remainder = absolute % 1000

        val words = mutableListOf<String>()
        if (thousands > 0) {
            words += "${threeDigitsToWords(thousands)} thousand"
        }
        if (remainder > 0) {
            words += threeDigitsToWords(remainder)
        }

        return sign + words.joinToString(" ")
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            ttsReady = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
        }
    }

    private fun speak(text: String) {
        if (!ttsReady) {
            return
        }
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "androcalc-utterance")
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}
