package com.classmanager.app.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.classmanager.app.R
import com.classmanager.app.data.ClassDatabase
import com.classmanager.app.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // Warm the Room singleton on launch so later screens never block on it.
        ClassDatabase.get(this)
        if (savedInstanceState == null) show(DashboardFragment())
        binding.bottomNav.setOnItemSelectedListener { item ->
            val fragment: Fragment = when (item.itemId) {
                R.id.nav_dashboard -> DashboardFragment()
                R.id.nav_students -> StudentsFragment()
                R.id.nav_attendance -> AttendanceFragment()
                R.id.nav_tests -> TestsFragment()
                R.id.nav_fees -> FeesFragment()
                else -> return@setOnItemSelectedListener false
            }
            show(fragment)
            true
        }
    }

    private fun show(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}
