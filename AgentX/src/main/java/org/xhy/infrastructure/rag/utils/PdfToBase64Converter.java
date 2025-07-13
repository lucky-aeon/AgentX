package org.xhy.infrastructure.rag.utils;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** @author shilong.zang
 * @date 09:08 <br/>
 */
public class PdfToBase64Converter {

    private static final Logger log = LoggerFactory.getLogger(PdfToBase64Converter.class);

    // 默认DPI降低到150，以减少内存占用
    private static final float DEFAULT_DPI = 150;
    // 默认图像压缩质量 (0.0-1.0)
    private static final float DEFAULT_COMPRESSION_QUALITY = 0.7f;

    /** 转换PDF文件为base64图像列表(批量处理方式) 注意：此方法将整个PDF加载到内存中，可能导致内存溢出 推荐使用 processPdfPageByPage 方法进行流式处理 */
    public static List<String> convertPdfToBase64Images(byte[] pdfData, String imageFormat) throws IOException {
        List<String> base64Images = new ArrayList<>();

        // 加载PDF文档
        try (PDDocument document = Loader.loadPDF(pdfData)) {
            PDFRenderer renderer = new PDFRenderer(document);
            int pageCount = document.getNumberOfPages();

            log.info("开始处理PDF，共{}页", pageCount);

            // 遍历每一页
            for (int pageIndex = 0; pageIndex < pageCount; pageIndex++) {
                // 使用较低的DPI以减少内存使用
                BufferedImage image = renderer.renderImageWithDPI(pageIndex, DEFAULT_DPI);

                // 将图片转换为base64字符串(使用压缩)
                String base64 = convertImageToBase64Compressed(image, imageFormat);
                base64Images.add(base64);

                // 立即释放图像内存
                image.flush();
                image = null;

                log.info("已处理第{}页，当前内存使用: {}MB", (pageIndex + 1),
                        (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024 * 1024));
            }
        }

        return base64Images;
    }

    /** 流式处理PDF文件的单页并转换为base64 此方法每次只处理一页，大大减少内存占用
     *
     * @param pdfData PDF文件字节数组
     * @param pageIndex 要处理的页码（从0开始）
     * @param imageFormat 图像格式（如"jpg"）
     * @return 指定页面的base64编码字符串 */
    public static String processPdfPageToBase64(byte[] pdfData, int pageIndex, String imageFormat) throws IOException {
        // 加载PDF文档
        try (PDDocument document = Loader.loadPDF(pdfData)) {
            if (pageIndex >= document.getNumberOfPages() || pageIndex < 0) {
                throw new IllegalArgumentException("页码超出范围: " + pageIndex);
            }

            PDFRenderer renderer = new PDFRenderer(document);
            // 使用较低的DPI以减少内存使用
            BufferedImage image = renderer.renderImageWithDPI(pageIndex, DEFAULT_DPI);

            // 将图片转换为base64字符串并压缩
            String base64 = convertImageToBase64Compressed(image, imageFormat);

            // 立即释放资源
            image.flush();

            return base64;
        }
    }

    /** 获取PDF总页数 */
    public static int getPdfPageCount(byte[] pdfData) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdfData)) {
            return document.getNumberOfPages();
        }
    }

    /** 将图像转换为压缩的base64字符串 */
    private static String convertImageToBase64Compressed(BufferedImage image, String formatName) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // 使用压缩参数
        ImageWriter writer = ImageIO.getImageWritersByFormatName(formatName).next();
        ImageWriteParam writeParam = writer.getDefaultWriteParam();

        // 如果支持压缩，则使用压缩
        if (writeParam.canWriteCompressed()) {
            writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            writeParam.setCompressionQuality(DEFAULT_COMPRESSION_QUALITY);
        }

        // 写入压缩图像
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
            writer.setOutput(ios);
            writer.write(null, new IIOImage(image, null, null), writeParam);
        } finally {
            writer.dispose();
        }

        byte[] imageBytes = baos.toByteArray();
        return Base64.getEncoder().encodeToString(imageBytes);
    }

    /** 原始未压缩转换方法 - 保留用于兼容性 */
    private static String convertImageToBase64(BufferedImage image, String formatName) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, formatName, baos);
        byte[] imageBytes = baos.toByteArray();
        return Base64.getEncoder().encodeToString(imageBytes);
    }
}
