package cc.shinichi.library.view;

import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import cc.shinichi.library.R;

/**
 * @author 绿旋
 * Created by 绿旋 on 2020/6/3 10:51.
 */
class DeleteTipsDialog extends Dialog {
    private View mRootView;

    private OnClickListener mOnClickOkListener;

    public DeleteTipsDialog(@NonNull Context context) {
        super(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mRootView = getLayoutInflater().inflate(R.layout.dialog_delte_tips, null);
        setContentView(mRootView);

        mRootView.findViewById(R.id.tv_btn_calcel).setOnClickListener(v -> {
            dismiss();
        });

        mRootView.findViewById(R.id.tv_btn_ok).setOnClickListener(v -> {
            if (this.mOnClickOkListener != null) {
                mOnClickOkListener.onClick(this, 1);
            }
        });

        if (getWindow() != null) {
            int widthPixels = getContext().getResources().getDisplayMetrics().widthPixels;
            int dp26 = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 26, getContext().getResources().getDisplayMetrics());

            getWindow().setLayout(widthPixels - (dp26 * 2), WindowManager.LayoutParams.WRAP_CONTENT);
            getWindow().setBackgroundDrawable(new ColorDrawable());
        }
    }

    public void setOnClickOkListener(OnClickListener listener) {
        this.mOnClickOkListener = listener;
    }

}
