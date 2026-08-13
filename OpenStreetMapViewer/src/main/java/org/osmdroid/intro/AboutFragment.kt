package org.osmdroid.intro

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import org.osmdroid.R

/**
 * created on 1/5/2017.
 *
 * @author Alex O'Ree
 */
class AboutFragment : Fragment(), View.OnClickListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val v = inflater.inflate(R.layout.intro_about, container, false)
        v.findViewById<View>(R.id.introbuttonsite).setOnClickListener(this)
        return v
    }

    override fun onClick(v: View?) {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/osmdroid/osmdroid/"))
        startActivity(browserIntent)
    }
}
