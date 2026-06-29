package com.app.vault.marketplace

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.app.vault.marketplace.databinding.ActivitySplashBinding

class SplashActivity : AppCompatActivity() {
    private lateinit var b: ActivitySplashBinding
    private val handler = Handler(Looper.getMainLooper())
    private var isPaused = false
    private var currentProgress = 0
    private val pageDuration = 2000L
    private val updateInterval = 20L
    private lateinit var pages: List<OnboardingPage>
    private lateinit var sm: SessionManager

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        b = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(b.root)
        sm = SessionManager(this)

        pages = listOf(
            OnboardingPage("Welcome to VAULT", "The safest marketplace for your gaming needs.", R.drawable.logo_vault),
            OnboardingPage("Secure Transactions", "Every deal is protected by our vault system.", android.R.drawable.ic_lock_idle_lock),
            OnboardingPage("Fast Delivery", "Get your items instantly after verification.", android.R.drawable.ic_menu_send)
        )

        b.layoutInitial.visibility = View.VISIBLE
        b.layoutOnboarding.visibility = View.GONE

        handler.postDelayed({
            if (sm.isFirstRun()) {
                showOnboarding()
            } else {
                finishSplash()
            }
        }, 1500)
    }

    private fun showOnboarding() {
        b.layoutInitial.visibility = View.GONE
        b.layoutOnboarding.visibility = View.VISIBLE

        b.viewPager.adapter = OnboardingAdapter(pages)
        b.viewPager.isUserInputEnabled = false

        val touchListener = View.OnTouchListener { v, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> isPaused = true
                android.view.MotionEvent.ACTION_UP -> {
                    isPaused = false
                    v.performClick()
                }
            }
            true
        }
        b.viewPager.getChildAt(0).setOnTouchListener(touchListener)

        startProgress()
    }

    private fun startProgress() {
        val runnable = object : Runnable {
            override fun run() {
                if (!isPaused) {
                    currentProgress += (updateInterval.toFloat() / pageDuration * 1000).toInt()
                    b.progressBar.progress = currentProgress % 1000

                    if (currentProgress >= 1000) {
                        currentProgress = 0
                        if (b.viewPager.currentItem < pages.size - 1) {
                            b.viewPager.currentItem += 1
                        } else {
                            finishSplash()
                            return
                        }
                    }
                }
                handler.postDelayed(this, updateInterval)
            }
        }
        handler.post(runnable)
    }

    private fun finishSplash() {
        handler.removeCallbacksAndMessages(null)
        val repo = FirebaseRepository()
        val user = repo.currentUser

        if (user == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        repo.getUserProfile(
            uid = user.uid,
            onSuccess = { profile ->
                sm.saveProfile(profile.uid, profile.username)

                // Daftarkan FCM token device ini ke profil user
                com.google.firebase.messaging.FirebaseMessaging.getInstance().token
                    .addOnSuccessListener { token ->
                        repo.firestore.collection("users").document(profile.uid)
                            .set(mapOf("fcmToken" to token), com.google.firebase.firestore.SetOptions.merge())
                    }

                startActivity(Intent(this, MainActivity::class.java))
                finish()
            },
            onError = {
                repo.logout()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
        )
    }

    data class OnboardingPage(val title: String, val desc: String, val imageRes: Int)

    inner class OnboardingAdapter(private val items: List<OnboardingPage>) : RecyclerView.Adapter<OnboardingAdapter.VH>() {
        inner class VH(view: View) : RecyclerView.ViewHolder(view) {
            val tvTitle: TextView = view.findViewById(R.id.tvTitle)
            val tvDesc: TextView = view.findViewById(R.id.tvDesc)
            val ivImage: ImageView = view.findViewById(R.id.ivImage)
        }
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_onboarding, parent, false)
            return VH(v)
        }
        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = items[position]
            holder.tvTitle.text = item.title
            holder.tvDesc.text = item.desc
            holder.ivImage.setImageResource(item.imageRes)
        }
        override fun getItemCount() = items.size
    }
}
