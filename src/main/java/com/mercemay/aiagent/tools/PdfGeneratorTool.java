package com.mercemay.aiagent.tools;


import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.font.FontProvider;
import com.itextpdf.layout.properties.Property;
import com.mercemay.aiagent.manager.WorkspaceManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.io.File;

@Slf4j
@Component
public class PdfGeneratorTool {
    private final WorkspaceManager workspaceManager;

    public PdfGeneratorTool(WorkspaceManager workspaceManager) {
        this.workspaceManager = workspaceManager;
    }

    @Tool(description = "Generate a PDF file supporting Chinese characters using iText 9.5.0.")
    public String generatePdf(
            @ToolParam(description = "The name of the PDF file") String filename,
            @ToolParam(description = "The text content (supports Chinese)") String content) {

        if (!filename.toLowerCase().endsWith(".pdf")) {
            filename += ".pdf";
        }

        File targetFile = workspaceManager.resolve(filename);

        try (PdfWriter writer = new PdfWriter(targetFile);
             PdfDocument pdf = new PdfDocument(writer);
             Document document = new Document(pdf)) {

            FontProvider fontProvider = new FontProvider();
            // Add system fonts to support Chinese characters
            fontProvider.addSystemFonts();
            document.setFontProvider(fontProvider);
            // Set font priority to include common Chinese fonts
            document.setProperty(Property.FONT, new String[]{"Microsoft YaHei", "SimSun", "STSong", "Arial Unicode MS"});

            document.add(new Paragraph(content));

            log.info("PDF generated successfully: {}", targetFile.getAbsolutePath());
            return "PDF successfully generated at: " + filename;

        } catch (Exception e) {
            log.error("Failed to generate PDF", e);
            return "Error generating PDF: " + e.getMessage();
        }
    }
}
