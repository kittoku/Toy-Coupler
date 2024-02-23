package kittoku.tc.preference.custom

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.AttributeSet
import android.widget.TextView
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder


internal abstract class LinkPreference(context: Context, attrs: AttributeSet) : Preference(context, attrs) {
    abstract val preferenceTitle: String
    abstract val preferenceSummary: String
    abstract val url: String

    override fun onAttached() {
        super.onAttached()

        title = preferenceTitle
        summary = preferenceSummary
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        holder.findViewById(android.R.id.summary)?.also {
            it as TextView
            it.maxLines = Int.MAX_VALUE
        }
    }

    override fun onClick() {
        val intent = Intent(Intent.ACTION_VIEW).also { it.data = Uri.parse(url) }

        intent.resolveActivity(context.packageManager)?.also {
            context.startActivity(intent)
        }
    }
}

internal class LinkOscPreference(context: Context, attrs: AttributeSet) : LinkPreference(context, attrs) {
    override val preferenceTitle = "Move to this app's project page"
    override val preferenceSummary = "github.com/kittoku/Toy-Coupler"
    override val url = "https://github.com/kittoku/Toy-Coupler"
}
