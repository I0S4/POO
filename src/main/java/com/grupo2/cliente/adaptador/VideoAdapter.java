package com.grupo2.cliente.adaptador;

import com.github.sarxos.webcam.Webcam;
import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;
import java.awt.image.BufferedImage;

public class VideoAdapter {
    private final Webcam webcam;

    public VideoAdapter() {
        this.webcam = Webcam.getDefault();
        if (webcam != null) webcam.open();
    }

    public byte[] obtenerCuadroAsBytes() throws Exception {
        if (webcam == null) return new byte[0];
        BufferedImage img = webcam.getImage();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "jpg", baos);
        return baos.toByteArray();
    }
}