package cc.shinichi.library.glide

import android.graphics.drawable.Drawable
import com.bumptech.glide.request.Request
import com.bumptech.glide.request.target.SizeReadyCallback
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import java.io.File

/**
 * @author 工藤
 * @email qinglingou@gmail.com
 * cc.shinichi.library.glide
 * create at 2018/11/2  17:12
 * description:
 */
open class FileTarget : Target<File> {

    override fun onLoadStarted(placeholder: Drawable?) {}

    override fun onLoadFailed(errorDrawable: Drawable?) {}

    override fun onResourceReady(resource: File, transition: Transition<in File>?) {}

    override fun onLoadCleared(placeholder: Drawable?) {}

    override fun getSize(cb: SizeReadyCallback) {
        cb.onSizeReady(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
    }

    override fun removeCallback(cb: SizeReadyCallback) {}

    override fun getRequest(): Request? {
        return null
    }

    override fun setRequest(request: Request?) {}

    override fun onStart() {}

    override fun onStop() {}

    override fun onDestroy() {}
}