package com.stockflow.fiscal.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.LuminanceSource;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.GlobalHistogramBinarizer;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import com.stockflow.exception.FiscalException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class QrCodeImageDecoder {

    private static final long MAX_PIXELS = 25_000_000L;
    private static final Map<DecodeHintType, Object> HINTS = Map.of(
        DecodeHintType.TRY_HARDER, Boolean.TRUE,
        DecodeHintType.POSSIBLE_FORMATS, List.of(BarcodeFormat.QR_CODE)
    );

    public String decode(byte[] imageBytes) {
        BufferedImage image = readImage(imageBytes);
        LuminanceSource source = new BufferedImageLuminanceSource(image);

        // Hybrid handles uneven lighting (photos); global works better on clean screenshots.
        for (BinaryBitmap bitmap : new BinaryBitmap[]{
            new BinaryBitmap(new HybridBinarizer(source)),
            new BinaryBitmap(new GlobalHistogramBinarizer(source)),
            new BinaryBitmap(new HybridBinarizer(source.invert()))}) {
            try {
                Result result = new QRCodeReader().decode(bitmap, HINTS);
                return result.getText();
            } catch (NotFoundException e) {
                // try next strategy
            } catch (Exception e) {
                log.debug("QR decode attempt failed: {}", e.getMessage());
            }
        }
        throw new FiscalException("No readable QR Code found in the image", HttpStatus.UNPROCESSABLE_ENTITY);
    }

    private BufferedImage readImage(byte[] bytes) {
        try (ImageInputStream in = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
            if (!readers.hasNext()) {
                throw new FiscalException("Unsupported image format (use PNG, JPEG, GIF or BMP)",
                    HttpStatus.UNSUPPORTED_MEDIA_TYPE);
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(in);
                // Check dimensions before decoding to avoid decompression bombs.
                if ((long) reader.getWidth(0) * reader.getHeight(0) > MAX_PIXELS) {
                    throw new FiscalException("Image resolution too large", HttpStatus.PAYLOAD_TOO_LARGE);
                }
                return reader.read(0);
            } finally {
                reader.dispose();
            }
        } catch (IOException e) {
            throw new FiscalException("Could not read image", HttpStatus.BAD_REQUEST);
        }
    }
}
