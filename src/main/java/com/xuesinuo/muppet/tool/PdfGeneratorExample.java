package com.xuesinuo.muppet.tool;

import lombok.extern.slf4j.Slf4j;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Slf4j
public class PdfGeneratorExample {
    public static void main(String[] args) throws Exception {
        // Prepare a local image as sibling resource (optional)
        Path tmpImg = Paths.get("./pdf_output", "./logo.png");
        Files.createDirectories(tmpImg.getParent());
        // Create a tiny red dot PNG if not exists (placeholder)
        if (!Files.exists(tmpImg)) {
            byte[] png = Base64PngTinyRedDot.DATA;
            Files.write(tmpImg, png);
        }

        String html = """
                <!DOCTYPE html>
                <html>
                <head>
                  <meta charset="utf-8" />
                  <title>PDF Demo</title>
                  <style>
                    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif; margin: 0; padding: 16px; }
                    h1 { color: #333; }
                    .card { border: 1px solid #ddd; border-radius: 8px; padding: 16px; margin: 12px 0; }
                  </style>
                  <!-- External font -->
                  <link rel="preconnect" href="https://fonts.googleapis.com">
                  <link rel="stylesheet" href="https://fonts.googleapis.com/css2?family=Roboto:wght@400;700&display=swap">
                </head>
                <body>
                  <h1 style="font-family: Roboto, sans-serif;">Playwright HTML → PDF</h1>
                  <div class="card">Inline CSS works; network fonts/images/CSS also work.</div>
                  <div class="card">
                    Network image: <img src="https://placekitten.com/200/120" />
                  </div>
                  <div class="card">
                    Local sibling image: <img src="logo.png" width="64" height="64" />
                  </div>
                </body>
                </html>
                """;

        String out = PdfGenerator.generatePdf(
                html,
                "./pdf_output",                // output directory
                "example_custom",              // filename (no .pdf)
                210,                            // width in mm (A4 width)
                210,                            // height in mm (A4 height)
                List.of(tmpImg.toString())      // sibling resource files
        );

        log.info("Generated PDF at: {}", out);
    }

    // A 2x2 red dot PNG in base64 for placeholder usage
    static class Base64PngTinyRedDot {
        static final byte[] DATA = java.util.Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAIAAAACCAYAAABytg0kAAAAFklEQVR42mP8z8DAwMDAwMAAAgwCABWwB2xH6jSdAAAAAElFTkSuQmCC"
        );
    }
}
