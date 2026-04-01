package com.androcalc

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.androcalc.databinding.ActivityMainBinding
import java.util.Locale
import kotlin.math.abs

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private enum class SpokenLanguage(val locale: Locale) {
        SPANISH(Locale("es", "ES")),
        ENGLISH(Locale.US)
    }

    private lateinit var binding: ActivityMainBinding
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var spokenLanguage = SpokenLanguage.SPANISH

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
        setupLanguageButtons()
        updateLanguageButtons()
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

    private fun setupLanguageButtons() {
        binding.btnLanguageEs.setOnClickListener { setSpokenLanguage(SpokenLanguage.SPANISH) }
        binding.btnLanguageEn.setOnClickListener { setSpokenLanguage(SpokenLanguage.ENGLISH) }
    }

    private fun setSpokenLanguage(language: SpokenLanguage) {
        if (spokenLanguage == language) {
            return
        }

        spokenLanguage = language
        applyTtsLanguage()
        updateLanguageButtons()
    }

    private fun updateLanguageButtons() {
        binding.btnLanguageEs.isEnabled = spokenLanguage != SpokenLanguage.SPANISH
        binding.btnLanguageEn.isEnabled = spokenLanguage != SpokenLanguage.ENGLISH
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

        val sentence = "${numberToWords(first)} ${operatorWord(op)} ${numberToWords(second)} ${equalsWord()} ${numberToWords(result)}"
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
            speak(clearWord())
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
        return when (spokenLanguage) {
            SpokenLanguage.ENGLISH -> when (op) {
                '+' -> "plus"
                '-' -> "minus"
                '*' -> "times"
                '/' -> "divided by"
                else -> ""
            }

            SpokenLanguage.SPANISH -> when (op) {
                '+' -> "más"
                '-' -> "menos"
                '*' -> "por"
                '/' -> "dividido por"
                else -> ""
            }
        }
    }

    private fun equalsWord(): String {
        return when (spokenLanguage) {
            SpokenLanguage.ENGLISH -> "equals"
            SpokenLanguage.SPANISH -> "es igual a"
        }
    }

    private fun clearWord(): String {
        return when (spokenLanguage) {
            SpokenLanguage.ENGLISH -> "clear"
            SpokenLanguage.SPANISH -> "borrar"
        }
    }

    private fun numberToWords(number: Int): String {
        return when (spokenLanguage) {
            SpokenLanguage.ENGLISH -> numberToEnglishWords(number)
            SpokenLanguage.SPANISH -> numberToSpanishWords(number)
        }
    }

    private fun numberToEnglishWords(number: Int): String {
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

    private fun numberToSpanishWords(number: Int): String {
        if (number == 0) return "cero"

        fun underHundred(value: Int): String {
            return when (value) {
                0 -> ""
                1 -> "uno"
                2 -> "dos"
                3 -> "tres"
                4 -> "cuatro"
                5 -> "cinco"
                6 -> "seis"
                7 -> "siete"
                8 -> "ocho"
                9 -> "nueve"
                10 -> "diez"
                11 -> "once"
                12 -> "doce"
                13 -> "trece"
                14 -> "catorce"
                15 -> "quince"
                16 -> "dieciséis"
                17 -> "diecisiete"
                18 -> "dieciocho"
                19 -> "diecinueve"
                20 -> "veinte"
                in 21..29 -> "veinti${underHundred(value - 20)}"
                30 -> "treinta"
                40 -> "cuarenta"
                50 -> "cincuenta"
                60 -> "sesenta"
                70 -> "setenta"
                80 -> "ochenta"
                90 -> "noventa"
                else -> {
                    val tens = (value / 10) * 10
                    val units = value % 10
                    "${underHundred(tens)} y ${underHundred(units)}"
                }
            }
        }

        fun underThousand(value: Int): String {
            if (value < 100) return underHundred(value)

            val hundreds = value / 100
            val remainder = value % 100
            val hundredWord = when (hundreds) {
                1 -> if (remainder == 0) "cien" else "ciento"
                2 -> "doscientos"
                3 -> "trescientos"
                4 -> "cuatrocientos"
                5 -> "quinientos"
                6 -> "seiscientos"
                7 -> "setecientos"
                8 -> "ochocientos"
                9 -> "novecientos"
                else -> ""
            }

            return if (remainder == 0) hundredWord else "$hundredWord ${underHundred(remainder)}"
        }

        val sign = if (number < 0) "menos " else ""
        val absolute = abs(number)
        val thousands = absolute / 1000
        val remainder = absolute % 1000

        val parts = mutableListOf<String>()
        if (thousands > 0) {
            parts += if (thousands == 1) "mil" else "${underThousand(thousands)} mil"
        }
        if (remainder > 0) {
            parts += underThousand(remainder)
        }

        return sign + parts.joinToString(" ")
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            applyTtsLanguage()
        }
    }

    private fun applyTtsLanguage() {
        val result = tts?.setLanguage(spokenLanguage.locale)
        ttsReady = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
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
