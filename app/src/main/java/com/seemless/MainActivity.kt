package com.seemless

import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.text.Editable
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.Toast
import androidx.annotation.Nullable
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import com.google.android.material.floatingactionbutton.FloatingActionButton
import io.shubham0204.smollm.SmolLM
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import android.util.Log


class MainActivity : AppCompatActivity() {

    private lateinit var etvResult: EditText
    private lateinit var etvInput: EditText
    private lateinit var progressBar: ProgressBar
    private lateinit var runInferenceButton: FloatingActionButton

    private lateinit var ttsButton: FloatingActionButton
    private lateinit var inputButton: FloatingActionButton
    private lateinit var resetButton: FloatingActionButton

    private lateinit var userButton: FloatingActionButton
    private lateinit var spinnerSource: Spinner

    private var mainActiveUserId: Long = -1L
    private var mainPatientPrompt: String = ""

    private var tts: TextToSpeech? = null

    private var smolLM: SmolLM? = null

    private val LANGUAGES = listOf(
        "ar-EG", "ar-SA", "bg-BG", "bn-IN", "ca-ES",
        "cs-CZ", "da-DK", "de-DE", "el-GR", "en-US",
        "es-MX", "et-EE", "fa-IR", "fi-FI", "fil-PH",
        "fr-CA", "fr-FR", "gu-IN", "he-IL", "hi-IN",
        "hr-HR", "hu-HU", "id-ID", "is-IS", "it-IT",
        "ja-JP", "kn-IN", "ko-KR", "lt-LT", "lv-LV",
        "ml-IN", "mr-IN", "nl-NL", "no-NO", "pa-IN",
        "pl-PL", "pt-BR", "pt-PT", "ro-RO", "ru-RU",
        "sk-SK", "sl-SI", "sr-RS", "sv-SE", "sw-KE",
        "sw-TZ", "ta-IN", "te-IN", "th-TH", "tr-TR",
        "uk-UA", "ur-PK", "vi-VN", "zh-CN", "zh-TW",
        "zu-ZA"
    )

    private var userDbHelper: UserDbHelper? = null
    private val PREFS_NAME: String = "medassist_prefs"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ThemeUtils.setStatusBarAppearance(this)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)

        etvResult = findViewById(R.id.etvResult)
        etvInput = findViewById(R.id.etvInput)
        progressBar = findViewById(R.id.progressBar)
        runInferenceButton = findViewById(R.id.runInferenceButton)
        inputButton = findViewById(R.id.inputButton)
        ttsButton = findViewById(R.id.ttsButton)
        resetButton = findViewById(R.id.resetButton)
        userButton = findViewById(R.id.userButton)
        spinnerSource = findViewById(R.id.spinnerSource)

        inputButton.setOnClickListener { view: View? -> openSpeechRecognizer() }
        resetButton.setOnClickListener { view: View? -> loadModelWithProgress() }
        ttsButton.setOnClickListener { view: View? -> tts?.speak(etvResult.text.split("<Answer>")[1], TextToSpeech.QUEUE_FLUSH, null, null) }

        runInferenceButton.setOnClickListener { runInference() }

        val srcAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, LANGUAGES).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        spinnerSource.adapter = srcAdapter
        spinnerSource.setSelection(0)   // auto

        // Init progress bar
        progressBar.isIndeterminate = true
        progressBar.visibility = ProgressBar.INVISIBLE

        userDbHelper = UserDbHelper(this)
        userDbHelper?.writableDatabase

        restorePrefs()
        loadModelWithProgress()

        userButton.setOnClickListener { v: View? ->
            val intent = Intent(this@MainActivity, SettingsActivity::class.java)
            startActivity(intent)
        }

    }

    override fun onResume() {
        super.onResume()
        val prefs = getSharedPreferences("medassist_settings", MODE_PRIVATE)
        val activeId = prefs.getLong("last_active_user_id", 1L)

        if (activeId != mainActiveUserId) {
            etvResult.text.clear()
            etvInput.text.clear()
            loadModelWithProgress()
            return
        }

        if (activeId != -1L && userDbHelper != null) {
            val currentPrompt = userDbHelper!!.getUser(activeId)?.toPromptBlock() ?: ""
            if (currentPrompt != mainPatientPrompt) {
                loadModelWithProgress()
            }
        }
    }


    private fun getLang(): String {
        val selected = LANGUAGES[spinnerSource.selectedItemPosition]
        return selected
    }

    /** Saves current language selection to SharedPreferences. */
    private fun savePrefs() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
        prefs.putString("KEY_LANG", getLang())
        prefs.apply()
    }

    /** Restores previously saved language selection into the UI. */
    private fun restorePrefs() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val language = prefs.getString("KEY_LANG", "en-US") ?: "en-US"

        val langIdx = LANGUAGES.indexOf(language)
            spinnerSource.setSelection(langIdx)
    }

    private fun loadModelWithProgress() {
        val modelFile = File(getExternalFilesDir(null), "model.gguf")

        if (!modelFile.exists()) {
            startActivity(Intent(this, SetupActivity::class.java))
            return
        }
        etvResult.text.clear()
        etvInput.text.clear()
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = ProgressBar.VISIBLE
                }
                smolLM?.close()
                val smolLMInstance = SmolLM()
                val params = SmolLM.InferenceParams(
                    contextSize = 8192,
                    storeChats = true,
                    temperature = 0.15f,
                )
                smolLMInstance.load(modelFile.absolutePath, params)

                this@MainActivity.smolLM = smolLMInstance

                withContext(Dispatchers.Main) {
                    progressBar.visibility = ProgressBar.INVISIBLE
                    // Build system prompt — base instructions + optional patient profile
                    val basePrompt = "Only provide short answers unless the user requests a detailed answer."


                    val db = userDbHelper
                    if (db == null) {
                        smolLM?.addSystemPrompt(basePrompt)
                    } else {
                        val prefs = getSharedPreferences("medassist_settings", MODE_PRIVATE)
                        val activeId = prefs.getLong("last_active_user_id", -1L)
                        val profilePrompt = if (activeId != -1L) {
                            val p = db.getUser(activeId)
                            if (p != null ) p.toPromptBlock() else ""
                        } else ""

                        smolLM?.addSystemPrompt("$basePrompt\n\n$profilePrompt")
                        mainActiveUserId = activeId
                        mainPatientPrompt = profilePrompt
                        Log.d("Patient", profilePrompt)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = ProgressBar.INVISIBLE
                    etvResult.text = Editable.Factory.getInstance().newEditable("❌ Error loading model:\n${e.message ?: "Unknown"}")
                    Toast.makeText(this@MainActivity, "Failed to load model: ${e.message}", Toast.LENGTH_LONG).show()
                }
                e.printStackTrace()
            }
        }
    }

    private fun runInference() {
        savePrefs()
        val smolLM = this.smolLM ?: run {
            etvResult.text = Editable.Factory.getInstance().newEditable("❌ Model not initialized")
            return
        }

        val userPrompt = escapeJson(etvInput.text.toString().trim())
        if (userPrompt.isEmpty()) {
            Toast.makeText(this, "Please enter question.", Toast.LENGTH_SHORT).show()
            return
        }

        etvResult.text.clear()
        initTTS(Locale(getLang()))

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = ProgressBar.VISIBLE
                    progressBar.isIndeterminate = true
                    runInferenceButton.isEnabled = false
                }

                var response = ""
                smolLM.getResponseAsFlow(userPrompt).collect { token ->
                    response += token

                    //Todo: Reasoning is between <unused94> and <unused95> tags Replace <unused94> with "Thinking" and <unused95> with "\n\nReply\n"
                    // Unescape JSON escape sequences in the FULL accumulated response
                    val decoded = response
                        .replace("\\\\", "\u0000")   // protect real backslashes
                        .replace("\\n", "\n")        // \n → actual newline
                        .replace("\\t", "\t")        // \t → tab
                        .replace("\\r", "\r")        // \r → carriage return
                        .replace("\u0000", "\\")     // restore real backslashes
                        .replace("<unused94>thought", "<Thinking>\n")
                        .replace("<unused95>", "\n\n<Answer>\n")

                    withContext(Dispatchers.Main) {
                        etvResult.text = Editable.Factory.getInstance().newEditable(
                            decoded.ifEmpty { "⚠️ Empty response" }
                        )
                    }
                }

                withContext(Dispatchers.Main) {
                    progressBar.visibility = ProgressBar.INVISIBLE
                    runInferenceButton.isEnabled = true
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = ProgressBar.INVISIBLE
                    etvResult.text = Editable.Factory.getInstance().newEditable("❌ Error:\n${e.message}")
                    Toast.makeText(this@MainActivity, "Inference failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    runInferenceButton.isEnabled = true
                }
                e.printStackTrace()
            }
        }
    }

    private fun openSpeechRecognizer() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, getLang());

        startActivityForResult(intent, 123)
    }

    /** JSON-escape a string value for embedding inside a template literal. */
    private fun escapeJson(value: String): String {
        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    override fun onDestroy() {
        super.onDestroy();
        deinitTTS();
        smolLM?.close();
        smolLM = null;
        userDbHelper?.close();   // close SQLite
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, @Nullable data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 123) {
            if (resultCode == RESULT_OK && data != null) {
                val results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                if (results != null && results.size > 0) {
                    val spokenText = results.get(0)
                    runOnUiThread(Runnable { etvInput.setText(spokenText) })
                }
            } else {
                runOnUiThread(Runnable { etvInput.setText("Speech recognition failed") })
            }
        }
    }

    private fun deinitTTS() {
        if (tts != null) {
            tts!!.stop()
            tts!!.shutdown()
        }
    }

    private fun initTTS(locale: Locale?) {
        tts = TextToSpeech(this) { status: Int ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts!!.setLanguage(locale)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts = null
                    runOnUiThread {
                        Toast.makeText(
                            this,
                            getString(R.string.language_not_supported),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } else {
                tts = null
                runOnUiThread {
                    Toast.makeText(
                        this,
                        getString(R.string.tts_init_failed),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}
