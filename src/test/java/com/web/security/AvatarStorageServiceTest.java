package com.web.security;

import com.web.exception.WeebException;
import com.web.service.AvatarStorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class AvatarStorageServiceTest {
    @TempDir Path directory;

    @Test void realImageIsStoredAsPngUsingServerGeneratedFilename() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "jpeg", bytes);
        AvatarStorageService service = new AvatarStorageService(directory.toString());
        String url = service.store(new MockMultipartFile("file", "../../attack.html", "text/html", bytes.toByteArray()));
        assertTrue(url.matches("/uploads/avatars/[a-f0-9-]{36}\\.png"));
        Path stored = directory.resolve(url.substring(url.lastIndexOf('/') + 1));
        assertEquals(2, ImageIO.read(stored.toFile()).getWidth());
        service.remove(url);
        assertFalse(Files.exists(stored));
    }

    @Test void executableContentAndOversizedImagesAreRejected() throws Exception {
        AvatarStorageService service = new AvatarStorageService(directory.toString());
        assertThrows(WeebException.class, () -> service.store(new MockMultipartFile("file", "x.png", "image/png", "<svg onload='alert(1)'/>".getBytes())));
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(2049, 1, BufferedImage.TYPE_INT_RGB), "png", bytes);
        assertThrows(WeebException.class, () -> service.store(new MockMultipartFile("file", "x.png", "image/png", bytes.toByteArray())));
        try (var files = Files.list(directory)) { assertEquals(0, files.count()); }
    }
}
