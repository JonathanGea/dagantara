package com.dagantara.backend.modules.storesettings.service;

import com.dagantara.backend.common.exception.ApiException;
import com.dagantara.backend.modules.storesettings.dto.StoreLogo;
import jakarta.servlet.http.Part;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class StoreLogoStorage {
    private static final Logger LOG = LoggerFactory.getLogger(StoreLogoStorage.class);
    private static final int MAX_BYTES = 2 * 1024 * 1024;
    private final Path directory;

    public StoreLogoStorage(@Value("${app.store-settings.logo-directory}") Path directory) {
        this.directory = directory.toAbsolutePath().normalize();
    }

    public StoreLogo write(Part part) {
        if (part.getSize() > MAX_BYTES) {
            throw tooLarge();
        }
        byte[] bytes;
        try (var input = part.getInputStream()) {
            bytes = input.readNBytes(MAX_BYTES + 1);
        } catch (IOException e) {
            throw invalidImage();
        }
        if (bytes.length > MAX_BYTES) {
            throw tooLarge();
        }
        var logo = new StoreLogo(UUID.randomUUID(), imageType(bytes));
        boolean created = false;
        try {
            Files.createDirectories(directory);
            try (var output = Files.newOutputStream(path(logo), StandardOpenOption.CREATE_NEW,
                    StandardOpenOption.WRITE)) {
                created = true;
                output.write(bytes);
            }
        } catch (IOException e) {
            if (created) {
                discard(logo);
            }
            LOG.warn("Could not write a store logo");
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "LOGO_STORAGE_ERROR",
                    "Could not save the store logo");
        }
        return logo;
    }

    public Optional<byte[]> read(StoreLogo logo) {
        try (var input = Files.newInputStream(path(logo))) {
            byte[] bytes = input.readNBytes(MAX_BYTES + 1);
            return bytes.length == 0 || bytes.length > MAX_BYTES ? Optional.empty() : Optional.of(bytes);
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    public void discard(StoreLogo logo) {
        try {
            Files.deleteIfExists(path(logo));
        } catch (IOException e) {
            LOG.warn("Could not remove an inactive store logo");
        }
    }

    private Path path(StoreLogo logo) {
        return directory.resolve(logo.id().toString());
    }

    private static String imageType(byte[] bytes) {
        try (var stream = new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))) {
            var readers = ImageIO.getImageReaders(stream);
            if (!readers.hasNext()) {
                throw invalidImage();
            }
            var reader = readers.next();
            try {
                String contentType = switch (reader.getFormatName().toLowerCase(Locale.ROOT)) {
                    case "png" -> "image/png";
                    case "jpeg" -> "image/jpeg";
                    case "webp" -> "image/webp";
                    default -> throw invalidImage();
                };
                reader.setInput(stream, false, true);
                int frames = reader.getNumImages(true);
                if (frames <= 0) {
                    throw invalidImage();
                }
                // Bound the decoded raster size even when a small file declares very large dimensions.
                for (int i = 0; i < frames; i++) {
                    int stride = Math.ceilDiv(Math.max(reader.getWidth(i), reader.getHeight(i)), 2048);
                    var params = reader.getDefaultReadParam();
                    params.setSourceSubsampling(stride, stride, 0, 0);
                    reader.read(i, params);
                }
                return contentType;
            } finally {
                reader.dispose();
            }
        } catch (IOException | RuntimeException e) {
            throw invalidImage();
        }
    }

    private static ApiException tooLarge() {
        return new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "PAYLOAD_TOO_LARGE",
                "The logo must be at most 2 MiB");
    }

    private static ApiException invalidImage() {
        return new ApiException(HttpStatus.BAD_REQUEST, "INVALID_LOGO",
                "The logo must be a readable PNG, JPEG, or WebP image");
    }
}
