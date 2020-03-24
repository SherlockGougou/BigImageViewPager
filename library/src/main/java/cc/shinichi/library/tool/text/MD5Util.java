package cc.shinichi.library.tool.text;

import java.security.MessageDigest;

/**
 * @author 工藤
 * @email 18883840501@163.com
 * cc.shinichi.library.tool.text
 * create at 2018/12/14  09:48
 * description:
 */
public class MD5Util {

    private static final String[] hexDigIts =
            {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "e", "f"};

    /**
     * MD5加密
     */
    public static String md5Encode(String origin) {
        return md5Encode(origin, "utf-8");
    }

    /**
     * MD5加密
     *
     * @param origin      字符
     * @param charsetName 编码
     */
    public static String md5Encode(String origin, String charsetName) {
        String resultString = null;
        try {
            resultString = origin;
            MessageDigest md = MessageDigest.getInstance("MD5");
            if (null == charsetName || "".equals(charsetName)) {
                resultString = byteArrayToHexString(md.digest(resultString.getBytes()));
            } else {
                resultString = byteArrayToHexString(md.digest(resultString.getBytes(charsetName)));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return resultString;
    }

    public static String byteArrayToHexString(byte[] b) {
        StringBuilder resultSb = new StringBuilder();
        for (int i = 0; i < b.length; i++) {
            resultSb.append(byteToHexString(b[i]));
        }
        return resultSb.toString();
    }

    public static String byteToHexString(byte b) {
        int n = b;
        if (n < 0) {
            n += 256;
        }
        int d1 = n / 16;
        int d2 = n % 16;
        return hexDigIts[d1] + hexDigIts[d2];
    }
}