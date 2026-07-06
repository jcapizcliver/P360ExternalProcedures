package mx.com.liverpool.p360.services.core.process;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.*;
import java.net.URL;
import java.util.*;

public class ImageCheck {

    public static class Result {
        public final boolean ok;
        public final String reason;
        public final String format;
        public final int width;
        public final int height;
        public final long bytes;
        public final double dpi;

        private Result(boolean ok, String reason, String format, int width, int height, long bytes, double dpi) {
            this.ok = ok;
            this.reason = reason;
            this.format = format;
            this.width = width;
            this.height = height;
            this.bytes = bytes;
            this.dpi = dpi;
        }

        public static Result ok(String format, int w, int h, long bytes, double dpi) {
            return new Result(true, "OK", format, w, h, bytes, dpi);
        }

        public static Result fail(String reason) {
            return new Result(false, reason, null, -1, -1, -1, -1);
        }

        @Override public String toString() {
            return ok
                    ? "OK: " + format + " " + width + "x" + height + " bytes=" + bytes + (dpi > 0 ? " dpi~" + dpi : "")
                    : "FAIL: " + reason;
        }
    }

    public static Result validateUrl(
            String url,
            Set<String> allowedFormats,
            long maxBytes,
            int maxW,
            int maxH,
            long maxPixels
    ) {
        try (InputStream in = new BufferedInputStream(new URL(url).openStream());
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            // 1) Tamaño: descarga limitada
            byte[] buf = new byte[8192];
            long total = 0;
            int r;
            while ((r = in.read(buf)) != -1) {
                total += r;
                if (total > maxBytes) return Result.fail("Excede tamaño máximo");
                baos.write(buf, 0, r);
            }

            byte[] data = baos.toByteArray();
            if (data.length == 0) return Result.fail("Descarga vacía");

            // 2) Formato real + 3) Dimensiones con ImageReader
            try (ImageInputStream iis = ImageIO.createImageInputStream(new ByteArrayInputStream(data))) {
                if (iis == null) return Result.fail("No se pudo abrir stream de imagen");

                Iterator<ImageReader> it = ImageIO.getImageReaders(iis);
                if (!it.hasNext()) return Result.fail("No soportado o corrupto");

                ImageReader reader = it.next();
                String format = normalize(reader.getFormatName());

                if (allowedFormats != null && !allowedFormats.isEmpty()) {
                    boolean okFmt = allowedFormats.stream().map(ImageCheck::normalize).anyMatch(format::equals);
                    if (!okFmt) return Result.fail("Formato no permitido: " + format);
                }

                reader.setInput(iis, true, true);

                int w = reader.getWidth(0);
                int h = reader.getHeight(0);

                if (w <= 0 || h <= 0) return Result.fail("Dimensiones inválidas");
                if (w > maxW || h > maxH) return Result.fail("Dimensiones excedidas: " + w + "x" + h);

                long pixels = (long) w * (long) h;
                if (pixels > maxPixels) return Result.fail("Exceso de píxeles");

                reader.dispose();

                // 4) DPI (opcional):
                // Con ImageIO puro es inconsistente según formato/archivo.
                // Aquí lo dejo como no leído.
                double dpi = -1;

                return Result.ok(format, w, h, data.length, dpi);
            }

        } catch (Exception e) {
            return Result.fail("Error: " + e.getClass().getSimpleName());
        }
    }

    private static String normalize(String f) {
        if (f == null) return "";
        f = f.trim().toLowerCase(Locale.ROOT);
        if (f.equals("jpg")) return "jpeg";
        return f;
    }

    // Demo
    public static void main(String[] args) {
        Result r = validateUrl(
                "https://example.com/image.png",
                Set.of("jpeg", "png", "webp", "gif"),
                5L * 1024 * 1024,
                4000,
                4000,
                20_000_000L
        );
        System.out.println(r);
    }
}
