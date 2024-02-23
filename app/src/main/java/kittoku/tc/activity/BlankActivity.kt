package kittoku.tc.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import kittoku.tc.R
import kittoku.tc.databinding.ActivityBlankBinding
import kittoku.tc.fragment.AppsFragment


internal const val BLANK_ACTIVITY_TYPE_PROFILES = 0
internal const val BLANK_ACTIVITY_TYPE_APPS = 1

class BlankActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val fragment: Fragment

        when (intent.extras!!.getInt("type")) {
            BLANK_ACTIVITY_TYPE_APPS -> {
                title = "Allowed Apps"
                fragment = AppsFragment()
            }

            else -> throw NotImplementedError(intent.toString())
        }

        val binding = ActivityBlankBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportFragmentManager.beginTransaction().also {
            it.replace(R.id.blank, fragment)
            it.commit()
        }
    }
}
