package cc.shinichi.library.callback

import cc.shinichi.library.ui.subsampling.SubsamplingScaleImageView

open class SimpleOnImageEventListener : SubsamplingScaleImageView.OnImageEventListener {

    override fun onReady() {}

    override fun onImageLoaded() {}

    override fun onPreviewLoadError(e: Exception) {}

    override fun onImageLoadError(e: Exception) {}

    override fun onTileLoadError(e: Exception) {}

    override fun onPreviewReleased() {}
}