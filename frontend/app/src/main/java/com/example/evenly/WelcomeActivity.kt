package com.example.evenly

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import android.view.animation.AccelerateInterpolator
import android.widget.TextView
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.Animator
import android.animation.AnimatorListenerAdapter

class WelcomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        val evenlyText = findViewById<TextView>(R.id.evenly_text)
        evenlyText.scaleX = 0.5f
        evenlyText.scaleY = 0.5f
        evenlyText.alpha = 1f

        // Grow (scale up)
        val scaleUpX = ObjectAnimator.ofFloat(evenlyText, "scaleX", 0.5f, 1.5f)
        val scaleUpY = ObjectAnimator.ofFloat(evenlyText, "scaleY", 0.5f, 1.5f)
        scaleUpX.duration = 2000
        scaleUpY.duration = 2000

        // Slide out to right
        val slideOut = ObjectAnimator.ofFloat(evenlyText, "translationX", 0f, 1000f)
        slideOut.duration = 1000
        slideOut.interpolator = AccelerateInterpolator()

        val animatorSet = AnimatorSet()
        animatorSet.playTogether(scaleUpX, scaleUpY)
        animatorSet.playSequentially(animatorSet.childAnimations[0], slideOut)
        animatorSet.start()

        animatorSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                val intent = Intent(this@WelcomeActivity, Login::class.java)
                startActivity(intent)
                finish()
            }
        })
    }
} 