package com.azerty_liet

import android.content.ClipboardManager
import android.inputmethodservice.InputMethodService
import android.inputmethodservice.Keyboard
import android.inputmethodservice.Keyboard.Key
import android.inputmethodservice.KeyboardView
import android.inputmethodservice.KeyboardView.OnKeyboardActionListener
import android.text.InputType
import android.view.ContextThemeWrapper
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.LinearLayout


public class AzertyLiet : InputMethodService(), OnKeyboardActionListener {
    private val KEYCODE_CHANGE_SYMBOLS = -15

    private lateinit var rootView: View
    private lateinit var keyboardView: KeyboardView
    private lateinit var charKeyboard: Keyboard
    private lateinit var numKeyboard: Keyboard
    private lateinit var shiftKey: Key
    private var capsStatus: Int = 0
    private var symbolsPage: Int = 1
    private var symbols: Boolean = false
    private lateinit var clipboard: ClipboardManager
    private lateinit var container: LinearLayout


    override fun onCreateInputView(): View {
        rootView = layoutInflater.inflate(R.layout.azerty_liet_layout, null) as View
        charKeyboard = Keyboard(this, R.xml.azerty_liet_keyboard)
        numKeyboard = Keyboard(this, R.xml.num_keyboard)
        keyboardView = rootView.findViewById(R.id.keyboardView)
        keyboardView.keyboard = charKeyboard
        keyboardView.setOnKeyboardActionListener(this)

        clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        container = rootView.findViewById(R.id.container)
        updateClipboardItems()
        clipboard.addPrimaryClipChangedListener(ClipboardManager.OnPrimaryClipChangedListener { updateClipboardItems() })
        return rootView
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        updateClipboardItems()
        val isNumericInput = info != null && (
            info.inputType and InputType.TYPE_CLASS_NUMBER == InputType.TYPE_CLASS_NUMBER ||
            info.inputType and InputType.TYPE_NUMBER_FLAG_DECIMAL == InputType.TYPE_NUMBER_FLAG_DECIMAL ||
            info.inputType and InputType.TYPE_NUMBER_FLAG_SIGNED == InputType.TYPE_NUMBER_FLAG_SIGNED ||
            info.inputType and InputType.TYPE_NUMBER_FLAG_DECIMAL != 0
        )
        keyboardView.keyboard = if (isNumericInput) numKeyboard else charKeyboard
        if (!isNumericInput) {
            shiftKey = keyboardView.keyboard.keys.find { it.codes[0] == -1 }!!
            capsStatus = 0
        }
    }


    override fun onKey(i: Int, ints: IntArray) {
        when (i) {
            Keyboard.KEYCODE_DELETE -> keyDelete()
            Keyboard.KEYCODE_SHIFT -> keyShift()
            Keyboard.KEYCODE_DONE -> keyEnter()
            Keyboard.KEYCODE_MODE_CHANGE -> keyModeChange()
            KEYCODE_CHANGE_SYMBOLS -> keySymbolsChange()
            else -> keyDefault(i.toChar())
        }
    }

    private fun keyDelete() {
        currentInputConnection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL))
        currentInputConnection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DEL))
    }

    private fun keyShift() {
        capsStatus++
        if(capsStatus == 1){
            charKeyboard.isShifted = true
            shiftKey.icon = resources.getDrawable(R.drawable.keyboard_shift_svgrepo_capped)
        }
        else if(capsStatus == 2){
            shiftKey.icon = resources.getDrawable(R.drawable.keyboard_shift_svgrepo_capped_all)
        }
        else{
            charKeyboard.isShifted = false
            capsStatus = 0
            shiftKey.icon = resources.getDrawable(R.drawable.keyboard_shift_svgrepo_uncapped)
        }
        keyboardView.invalidateAllKeys()
    }

    private fun keyEnter() {
        currentInputConnection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
        currentInputConnection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
    }

    private fun keyModeChange() {
        symbols = !symbols
        if(symbols) {
            charKeyboard = Keyboard(this, R.xml.symbols_keyboard1)
            keyboardView.keyboard = charKeyboard
            symbolsPage = 1
        }
        else {
            charKeyboard= Keyboard(this, R.xml.azerty_liet_keyboard)
            keyboardView.keyboard = charKeyboard
            shiftKey = keyboardView.keyboard.keys.find { it.codes[0] == -1 }!!
            capsStatus = 0
        }
    }

    private fun keySymbolsChange() {
        symbolsPage++
        if(symbolsPage == 2) {
            charKeyboard = Keyboard(this, R.xml.symbols_keyboard2)
            keyboardView.keyboard = charKeyboard
        }
        else {
            charKeyboard= Keyboard(this, R.xml.symbols_keyboard1)
            keyboardView.keyboard = charKeyboard
            symbolsPage = 1
        }
    }

    private fun keyDefault(code: Char) {
        var codeCopy = code
        if (Character.isLetter(codeCopy) && capsStatus != 0) {
            codeCopy = codeCopy.uppercaseChar()
            if(capsStatus == 1){
                charKeyboard.isShifted = false
                capsStatus = 0
                shiftKey.icon = resources.getDrawable(R.drawable.keyboard_shift_svgrepo_uncapped)
                keyboardView.invalidateAllKeys()
            }
        }
        currentInputConnection.commitText(codeCopy.toString(), 1)
    }

    private fun updateClipboardItems() {
        container.removeAllViews()
        val clipData = clipboard.primaryClip
        if (clipData != null) {
            val itemCount = clipData.itemCount
            for (i in 0 until itemCount) {
                val temp = Button(ContextThemeWrapper(this, R.style.Clipboard), null, 0)
                val text = clipData.getItemAt(i).text.toString().lines().joinToString(" ")
                container.addView(temp)
                temp.setOnClickListener{ currentInputConnection.commitText(text, 1)}
                temp.text = text
            }
        }
    }

    override fun onPress(i: Int) {}
    override fun onRelease(i: Int) {}
    override fun onText(charSequence: CharSequence) {}
    override fun swipeLeft() {}
    override fun swipeRight() {}
    override fun swipeDown() {}
    override fun swipeUp() {}
}