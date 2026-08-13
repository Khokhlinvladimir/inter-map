package org.osmdroid.model

import android.app.Activity

/**
 * Created by alex on 10/21/16.
 */
abstract class BaseActivity : Activity(), IBaseActivity {
    abstract override val activityTitle: String?
}
