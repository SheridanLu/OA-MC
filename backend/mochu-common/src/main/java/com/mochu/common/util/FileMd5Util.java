package com.mochu.common.util;

import java.io.InputStream;
import java.security.MessageDigest;

/**
 * 文件 MD5 计算（用于秒传校验）
 */
public final class FileMd5Util {

    private FileMd5Util() {}

    public static String computeMd5(InputStream is) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = is.read(buffer)) != -1) {
                md.update(buffer, 0, read);
            }
            byte[] digest = md.digest();
            StringBuilder sb = new StringBuilder(32);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("MD5计算失败", e);
        }
    }
}
