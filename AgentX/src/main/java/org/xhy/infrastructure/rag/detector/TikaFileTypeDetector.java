package org.xhy.infrastructure.rag.detector;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.apache.tika.Tika;

/**
 * @author shilong.zang
 * @date 11:32 <br/>
 */
public class TikaFileTypeDetector {

    public static String detectFileType(byte[] data) {
        if (data == null || data.length == 0) {
            return "未知类型";
        }

        try {
            Tika tika = new Tika();
            return tika.detect(new ByteArrayInputStream(data));
        } catch (IOException e) {
            e.printStackTrace();
            return "未知类型";
        }
    }

}
