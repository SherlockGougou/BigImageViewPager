package cc.shinichi.library.view.listener

import android.app.Activity
import android.view.View

interface OnCustomLayoutCallback {

    fun onLayout(activity: Activity, parentView: View)
}