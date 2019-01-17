package cc.shinichi.library.glide.cache;

import com.bumptech.glide.load.Key;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;

/**
 * @author 工藤
 * @email gougou@16fan.com
 * com.fan16.cn.loader.glide
 * create at 2018/5/10  11:12
 * description:
 */
public class OriginalKey implements Key {
    private final String id;
    private final Key signature;

    public OriginalKey(String id, Key signature) {
        this.id = id;
        this.signature = signature;
    }

    @Override public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        OriginalKey that = (OriginalKey) o;

        if (!id.equals(that.id)) {
            return false;
        }
        if (!signature.equals(that.signature)) {
            return false;
        }

        return true;
    }

    @Override public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + signature.hashCode();
        return result;
    }

    @Override public void updateDiskCacheKey(MessageDigest messageDigest) throws UnsupportedEncodingException {
        messageDigest.update(id.getBytes(STRING_CHARSET_NAME));
        signature.updateDiskCacheKey(messageDigest);
    }
}