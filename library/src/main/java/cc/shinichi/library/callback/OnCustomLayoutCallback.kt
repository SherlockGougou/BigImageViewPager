package cc.shinichi.library.callback

import android.app.Activity
import android.view.View

interface OnCustomLayoutCallback {

    fun onLayout(activity: Activity, parentView: View)
}