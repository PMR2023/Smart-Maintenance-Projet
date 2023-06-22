package com.example.smartmaintenance_design

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import kotlin.random.Random

class MainActivity : AppCompatActivity() {
    lateinit var codeTV : TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        codeTV = findViewById(R.id.codeTV)

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
    }
}