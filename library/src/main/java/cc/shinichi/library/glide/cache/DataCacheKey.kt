package cc.shinichi.library.glide.cache

import com.bumptech.glide.load.Key
import java.security.MessageDigest

/**
 * @author 工藤
 * @email qinglingou@gmail.com
 * create at 2018/5/10  11:12
 * description:
 */
class DataCacheKey(val sourceKey: Key, private val signature: Key) : Key {

    override fun equals(o: Any?): Boolean {
        if (o is DataCacheKey) {
            return sourceKey == o.sourceKey && signature == o.signature
        }
        return false
    }

    override fun hashCode(): Int {
        var result = sourceKey.hashCode()
        result = 31 * result + signature.hashCode()
        return result
    }

    override fun toString(): String {
        return "DataCacheKey{sourceKey=$sourceKey, signature=$signature}"
    }

    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        sourceKey.updateDiskCacheKey(messageDigest)
        signature.updateDiskCacheKey(messageDigest)
    }
}