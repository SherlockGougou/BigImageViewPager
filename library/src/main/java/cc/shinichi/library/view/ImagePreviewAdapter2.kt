package cc.shinichi.library.view

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

/**
 * 仓库: <a href="http://git.xiaozhouintel.com/root/C-UI-App-Android">...</a>
 * 文件名: ImagePreviewAdapter2.java
 * 作者: kirito
 * 描述: ViewPager2适配器
 * 创建时间: 2024/11/25
 */
class ImagePreviewAdapter2(
    private val activity: ImagePreviewActivity,
    private val fragmentList: MutableList<ImagePreviewFragment>
) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int {
        return fragmentList.size
    }

    override fun createFragment(position: Int): Fragment {
        return fragmentList.get(position)
    }
}