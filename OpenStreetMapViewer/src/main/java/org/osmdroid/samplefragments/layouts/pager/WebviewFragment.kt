package org.osmdroid.samplefragments.layouts.pager

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.fragment.app.Fragment
import org.osmdroid.R

/**
 * Created by alex on 10/22/16.
 */
class WebviewFragment : Fragment() {
    //webview1
    var webview: WebView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val v = inflater.inflate(R.layout.map_viewpager_webview, null)
        webview = v.findViewById<WebView>(R.id.webview1)
        return v
    }

    override fun onResume() {
        super.onResume()
        webview!!.loadUrl("https://github.com/osmdroid/osmdroid")
    }
}
