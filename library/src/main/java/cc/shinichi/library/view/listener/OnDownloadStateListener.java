package cc.shinichi.library.view.listener;

import android.app.Activity;

public abstract class OnDownloadStateListener {

    public abstract void onDownloadStart(Activity activity, int position);

    public abstract void onDownloadSuccess(Activity activity, int position);

    public abstract void onDownloadFailed(Activity activity, int position);
}
