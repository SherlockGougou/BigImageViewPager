package cc.shinichi.library.view

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter

/**
 * 文件名: ImagePreviewAdapter2.java
 * 作者: kirito
 * 描述: ViewPager适配器
 * 创建时间: 2024/11/25
 */
class ImagePreviewAdapter(
    fragmentManager: FragmentManager,
    private val fragmentList: MutableList<ImagePreviewFragment>
) : FragmentStatePagerAdapter(fragmentManager) {

    override fun getItem(position: Int): Fragment {
        return fragmentList[position]
    }

    override fun getCount(): Int {
        return fragmentList.size
    }
}