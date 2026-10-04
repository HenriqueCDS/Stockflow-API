package com.stockflow.fiscal;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.stockflow.exception.FiscalException;
import com.stockflow.fiscal.service.QrCodeImageDecoder;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QrCodeImageDecoderTest {

    private final QrCodeImageDecoder decoder = new QrCodeImageDecoder();

    @Test
    void decode_shouldReadGeneratedQrCode() throws Exception {
        String content = "https://www.nfce.fazenda.sp.gov.br/qrcode?p=35240911222333000181650010000001231000000454|2|1|1|ABC123";

        assertThat(decoder.decode(qrPng(content, "png"))).isEqualTo(content);
        assertThat(decoder.decode(qrPng(content, "jpg"))).isEqualTo(content);
    }

    @Test
    void decode_shouldFailWhenImageHasNoQrCode() throws Exception {
        BufferedImage blank = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(blank, "png", out);

        assertThatThrownBy(() -> decoder.decode(out.toByteArray()))
            .isInstanceOf(FiscalException.class).hasMessageContaining("No readable QR Code");
    }

    @Test
    void decode_shouldRejectNonImageBytes() {
        assertThatThrownBy(() -> decoder.decode("not an image".getBytes()))
            .isInstanceOf(FiscalException.class).hasMessageContaining("Unsupported image format");
    }

    private byte[] qrPng(String content, String format) throws Exception {
        BitMatrix matrix = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 400, 400);
        BufferedImage image = MatrixToImageWriter.toBufferedImage(matrix);
        BufferedImage rgb = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        rgb.getGraphics().drawImage(image, 0, 0, null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(rgb, format, out);
        return out.toByteArray();
    }
}
