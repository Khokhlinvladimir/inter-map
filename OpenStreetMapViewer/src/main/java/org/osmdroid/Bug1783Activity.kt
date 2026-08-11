package org.osmdroid

import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import org.osmdroid.bugtestfragments.Bug1783MyLocationOverlayNPE
import org.osmdroid.model.IBaseActivity

class Bug1783Activity : FragmentActivity(), IBaseActivity {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bug1783)
        val button = findViewById<Button>(R.id.bug1782Button)
        button.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                val dialog: DialogFragment = Bug1783MyLocationOverlayNPE()

                dialog.show(this@Bug1783Activity.getSupportFragmentManager(), "tag")


                /* try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {

                }
                Bug1783Activity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dialog.dismiss();

                    }
                });*/
            }
        })
    }

    override val activityTitle: String
        get() = "My location overview dialog fragment"
}
