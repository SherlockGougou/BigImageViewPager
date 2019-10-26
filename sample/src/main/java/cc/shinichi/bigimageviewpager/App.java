package cc.shinichi.bigimageviewpager;

import android.app.Application;

import com.squareup.leakcanary.LeakCanary;

/**
 * @author 工藤
 * @email gougou@16fan.com
 * cc.shinichi.bigimageviewpager
 * create at 2019/1/10  10:26
 * description:
 */
public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        /**
         * LeakCanary 内存泄漏检测
         */
        if (LeakCanary.isInAnalyzerProcess(this)) {
            // This process is dedicated to LeakCanary for heap analysis.
            // You should not init your app in this process.
            return;
        }
        LeakCanary.install(this);
    }
}