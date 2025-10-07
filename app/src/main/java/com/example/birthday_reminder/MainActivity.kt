package com.example.birthday_reminder

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.birthday_reminder.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Catch uncaught exceptions
        Thread.setDefaultUncaughtExceptionHandler { _, e ->
            Log.e("MainActivity", "Uncaught exception", e)
            e.printStackTrace()
        }

        try {
            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)

            // Default fragment saat aplikasi dibuka
            replaceFragment(HomeFragment())

            binding.bottomNavigation.setOnItemSelectedListener { item ->
                when (item.itemId) {
                    R.id.nav_home -> replaceFragment(HomeFragment())
                    R.id.nav_add -> replaceFragment(AddBirthdayFragment())
                    R.id.nav_more -> replaceFragment(MoreFragment())
                }
                true
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error in onCreate", e)
            e.printStackTrace()
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        try {
            supportFragmentManager.beginTransaction()
                .replace(R.id.frame_container, fragment)
                .commit()
        } catch (e: Exception) {
            Log.e("MainActivity", "Error replacing fragment", e)
            e.printStackTrace()
        }
    }
}