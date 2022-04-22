package cc.shinichi.library.view.listener

import cc.shinichi.library.view.helper.SubsamplingScaleImageViewDragClose

open class SimpleOnImageEventListener : SubsamplingScaleImageViewDragClose.OnImageEventListener {

    override fun onReady() {}

    override fun onImageLoaded() {}

    override fun onPreviewLoadError(e: Exception) {}

    override fun onImageLoadError(e: Exception) {}

    override fun onTileLoadError(e: Exception) {}

    override fun onPreviewReleased() {}
}