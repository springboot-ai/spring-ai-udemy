package com.llm.service;

import com.llm.utils.RagUtiils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.ParagraphPdfDocumentReader;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Component
public class IngestionService implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(IngestionService.class);

    @Value("${ingestion.enabled:false}")
    private boolean ingestionEnabled;

    private final VectorStore vectorStore;
    @Value("classpath:/docs/Flexora_FAQ.pdf")
    private Resource faqPdf;

    public IngestionService(@Qualifier(value = "qaVectorStore") PgVectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Override
    public void run(String... args) throws Exception {
        if (ingestionEnabled) {
            log.info("IngestionService is invoked - run");
            ingestPDFDocs("page",faqPdf);
            log.info("VectorStore: {}", vectorStore);
        }

    }

    public void ingest(byte[] fileContent, String fileName, String ingestType) {
        try {
            log.info("IngestionService is invoked - ingest with fileName: {}, ingestType: {}", fileName, ingestType);
            Resource docSource = new ByteArrayResource(fileContent) {
                @Override
                public String getFilename() {
                    return fileName;
                }
            };

            var fileExtension = RagUtiils.getFileExtension(fileName);

            switch (fileExtension) {
                case "pdf" -> {
                    ingestPDFDocs(ingestType, docSource);
                }
                case "docx" -> {
                    ingestWordDocs(fileName, ingestType, docSource);
                }
                case "txt" -> {
                    ingestTextDocs(fileName, ingestType, docSource);
                }
                default -> throw new IllegalArgumentException("Unsupported file type: " + fileExtension);
            };

            log.info("VectorStore: {}", vectorStore);
            log.info("Ingestion completed successfully.");
        } catch (Exception e) {
            log.error("Exception occurred during ingestion: ", e);
            throw new RuntimeException(e);
        }
    }

    private void ingestTextDocs(String fileName, String ingestType, Resource docSource) {
        TextReader textReader = new TextReader(docSource);
        textReader.getCustomMetadata().put("filename", "fileName");
        var docs = textReader.read();

        vectorStore.add(docs);
        log.info("Ingested {} documents from Text file: {}", docs.size(), fileName);
    }

    private void ingestWordDocs(String fileName, String ingestType, Resource pdfResource) {
        var docs = getWordDocuments(pdfResource, ingestType);
        vectorStore.add(docs);
        log.info("Ingested {} documents from Word file: {}", docs.size(), fileName);
    }

    private void ingestPDFDocs(String ingestType, Resource pdfResource) {
        log.info("Ingesting PDF Docs");
        var docs = getPDFDocuments( ingestType, pdfResource);
        vectorStore.add(docs);
        log.info("Ingested {} documents from PDF Document Successfully", docs.size());
    }

    private static List<Document> getPDFDocuments(String ingestType, Resource pdfResource) {
        return switch (ingestType) {
            case "page" -> new PagePdfDocumentReader(pdfResource).get();
            case "paragraph" -> {
                log.info("Ingesting PDF Document as Paragraph");
                yield new ParagraphPdfDocumentReader(pdfResource).get();
            }
            default -> throw new IllegalArgumentException("Invalid ingest type: " + ingestType);
        };
    }

    private static List<Document> getWordDocuments(Resource pdfResource, String ingestType) {
        var docs = new TikaDocumentReader(pdfResource).get();
        return switch (ingestType){
            case "token" -> {
                TokenTextSplitter splitter = new TokenTextSplitter();
//                TokenTextSplitter splitter = new TokenTextSplitter(1000, 400, 10, 5000, true);

                yield splitter.apply(docs);
            }
            default -> docs;
        };

    }

    /**
     * Ingests a PDF, extracts text and images, normalizes text, and logs image extraction.
     * You can extend this to generate image embeddings for pgvector.
     */
    public void ingestPDFWithImages(Resource pdfResource) {
        List<Document> docs = new ArrayList<>();
        try (PDDocument document = PDDocument.load(pdfResource.getInputStream())) {
            int pageNum = 0;
            for (PDPage page : document.getPages()) {
                pageNum++;
                // Extract text using ParagraphPdfDocumentReader (or other reader)
                List<Document> pageDocs = new ParagraphPdfDocumentReader(pdfResource).get();
                for (Document doc : pageDocs) {
                    // Normalize whitespace in text

                    String cleaned = doc.getText().trim().replaceAll("\\s+", " ");
                    // Create a new Document with cleaned content and copy metadata
                    Document cleanedDoc = new Document(cleaned, doc.getMetadata());
                    docs.add(cleanedDoc);
                }
                // Extract images
                PDResources resources = page.getResources();
                for (var xObjectName : resources.getXObjectNames()) {
                    var xObject = resources.getXObject(xObjectName);
                    if (xObject instanceof PDImageXObject imageXObject) {
                        BufferedImage image = imageXObject.getImage();
                        // Example: Save image to disk (or process for embedding)
                        String imageFileName = "extracted_image_page" + pageNum + "_" + xObjectName.getName() + ".png";
                        File outputFile = new File(imageFileName);
                        ImageIO.write(image, "png", outputFile);
                        log.info("Extracted image saved: {}", outputFile.getAbsolutePath());
                        // TODO: Generate image embedding and store in pgvector if needed
                    }
                }
            }
            vectorStore.add(docs);
            log.info("Ingested {} documents and extracted images from PDF.", docs.size());
        } catch (Exception e) {
            log.error("Error during PDF with images ingestion", e);
            throw new RuntimeException(e);
        }
    }


}
