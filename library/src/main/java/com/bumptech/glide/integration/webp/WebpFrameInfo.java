package com.bumptech.glide.integration.webp;



public class WebpFrameInfo {
    public final int frameNumber;
    public final int xOffset;
    public final int yOffset;
    public final int width;
    public final int height;
    public final int duration;
    public final boolean blendPreviousFrame;
    public final boolean disposeBackgroundColor;

    WebpFrameInfo(int frameNumber, WebpFrame webpFrame) {
        this.frameNumber = frameNumber;
        this.xOffset = webpFrame.getXOffest();
        this.yOffset = webpFrame.getYOffest();
        this.width = webpFrame.getWidth();
        this.height = webpFrame.getHeight();
        this.duration = webpFrame.getDurationMs();
        this.blendPreviousFrame = webpFrame.isBlendWithPreviousFrame();
        this.disposeBackgroundColor = webpFrame.shouldDisposeToBackgroundColor();
    }

    @Override
    public String toString() {
        return "frameNumber=" + frameNumber + ", xOffset=" + xOffset + ", yOffset=" + yOffset
                + ", width=" + width + ", height=" + height + ", duration=" + duration +
                ", blendPreviousFrame=" + blendPreviousFrame + ", disposeBackgroundColor=" + disposeBackgroundColor;
    }
}
