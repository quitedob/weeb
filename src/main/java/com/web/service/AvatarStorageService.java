package com.web.service;

import com.web.exception.WeebException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.UUID;

@Service
public class AvatarStorageService {
    private static final String URL_PREFIX = "/uploads/avatars/";
    private final Path directory;

    public AvatarStorageService(@Value("${upload.avatar-directory:uploads/avatars}") String directory) {
        this.directory = Path.of(directory).toAbsolutePath().normalize();
    }

    public String store(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty() || file.getSize() > 5 * 1024 * 1024) {
            throw new WeebException("头像大小必须在5MB以内");
        }
        BufferedImage decoded;
        try (var input = file.getInputStream(); ImageInputStream image = ImageIO.createImageInputStream(input)) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(image);
            if (!readers.hasNext()) throw new WeebException("请上传PNG、JPEG或GIF图片");
            ImageReader reader = readers.next();
            try {
                reader.setInput(image, true, true);
                String format = reader.getFormatName();
                if (!format.matches("(?i)png|jpeg|jpg|gif")) throw new WeebException("不支持的头像格式");
                int width = reader.getWidth(0), height = reader.getHeight(0);
                if (width < 1 || height < 1 || width > 2048 || height > 2048) {
                    throw new WeebException("头像尺寸不能超过2048像素");
                }
                decoded = reader.read(0);
            } finally { reader.dispose(); }
        }
        Files.createDirectories(directory);
        String filename = UUID.randomUUID() + ".png";
        Path target = directory.resolve(filename);
        // Re-encode pixels so uploaded scripts, metadata and client filenames are never served.
        try {
            if (!ImageIO.write(decoded, "png", target.toFile())) throw new IOException("PNG encoder is unavailable");
        } catch (IOException e) {
            Files.deleteIfExists(target);
            throw e;
        }
        return URL_PREFIX + filename;
    }

    public void remove(String url) throws IOException {
        if (url != null && url.matches("/uploads/avatars/[0-9a-f-]{36}\\.png")) {
            Files.deleteIfExists(directory.resolve(url.substring(URL_PREFIX.length())));
        }
    }
}
