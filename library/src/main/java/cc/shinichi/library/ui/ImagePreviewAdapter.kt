package cc.shinichi.library.ui

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter

/**
 * 文件名: ImagePreviewAdapter.kt
 * 作者: kirito
 * 描述: ViewPager适配器
 * 创建时间: 2024/11/25
 *
 * 注意：此适配器使用 FragmentStatePagerAdapter（已废弃但仍可用）
 * 未来版本建议迁移到 ViewPager2 + FragmentStateAdapter
 */
@Suppress("DEPRECATION")
class ImagePreviewAdapter(
    fragmentManager: FragmentManager,
    private val fragmentList: MutableList<ImagePreviewFragment>
) : FragmentStatePagerAdapter(fragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    override fun getItem(position: Int): Fragment = fragmentList[position]

    override fun getCount(): Int = fragmentList.size

    override fun getItemPosition(`object`: Any): Int {
        // 当数据更新时强制刷新
        return POSITION_NONE
    }
}