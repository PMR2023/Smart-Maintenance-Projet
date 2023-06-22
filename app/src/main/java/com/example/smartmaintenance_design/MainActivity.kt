package com.example.smartmaintenance_design

import android.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import kotlin.random.Random

class MainActivity : AppCompatActivity() {
    lateinit var codeTV : TextView
    lateinit var buttonExp : Button
    lateinit var targetKey : EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        codeTV = findViewById(R.id.codeTV)
        buttonExp = findViewById(R.id.buttonExp)
        targetKey = findViewById(R.id.targetKey)

        val randomNums = List(6) { Random.nextInt(0, 2) }
        var code = randomNums.map {
            if(it == 1) {
                Random.nextInt(65, 90).toChar()
            } else {
                Random.nextInt(0, 9).toString()
            }
        }.joinToString("")

        codeTV.text = code
        // Send code to backend

        buttonExp.setOnClickListener {
            if(buttonExp.text == "Call") {
                targetKey.visibility = View.GONE
                buttonExp.text = "Je suis an Expert"
            } else {
                targetKey.visibility = View.VISIBLE
                buttonExp.text = "Call"
            }
        }

    }
}