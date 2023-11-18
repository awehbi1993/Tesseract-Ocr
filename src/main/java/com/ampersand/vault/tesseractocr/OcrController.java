package com.ampersand.vault.tesseractocr;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.tika.Tika;
import org.json.JSONObject;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

@RestController
public class OcrController {
	private static final Tika TIKA = new Tika();
	private static final List<String> IMAGE_MIME_TYPES = Arrays.asList("image/png", "image/jpeg", "image/tiff",
			"image/gif", "image/bmp", "image/webp");
	private static final String PDF_MIME_TYPE = "application/pdf";
	// private Logger logger = LoggerFactory.getLogger(OcrController.class);

	@RequestMapping("/")
	public String index() {
		return "The server is online";
	}
	
	@RequestMapping(value = "/process", method = RequestMethod.POST)
	public String singleFileUpload(@RequestParam("file") MultipartFile file) throws IOException, TesseractException {
		String mimeType = TIKA.detect(file.getBytes());
//check the file mime type of the uploaded file and execute the right function
		if (IMAGE_MIME_TYPES.contains(mimeType)) {
			File convFile = convert(file);
			Tesseract tesseract = new Tesseract();
			tesseract.setDatapath("C:\\Workspace1\\Tesseract-Ocr\\tessdata");
			String text = tesseract.doOCR(convFile);
			convFile.delete();
			return text;
		} else if (mimeType.equals(PDF_MIME_TYPE)) {
			try {
				// Load file into PDFBox class
				PDDocument document = PDDocument.load(file.getBytes());
				PDFTextStripper stripper = new PDFTextStripper();
				String strippedText = stripper.getText(document);

				// Check if there is an existing text in the loaded file if loaded file is empty
				// extractTextFromImage(TesseractOCR function) function will take place
				if (strippedText.trim().isEmpty()) {
					strippedText = extractTextFromPdf(document);
				}
				JSONObject obj = new JSONObject();
				obj.put("fileName", file.getOriginalFilename());
				obj.put("text", strippedText.toString());
				return (obj.toString());
			} catch (Exception e) {
				return (e.getMessage());
			}
		}

		return ("invalid input type");
	}

	public static File convert(MultipartFile file) throws IOException {
		File convFile = new File(file.getOriginalFilename());
		convFile.createNewFile();
		FileOutputStream fos = new FileOutputStream(convFile);
		fos.write(file.getBytes());
		fos.close();
		return convFile;
	}

// if the file is a pdf file extractTextFromPdf will take place  
	String extractTextFromPdf(PDDocument document) throws IOException, TesseractException {
		// Extract images from file
		PDFRenderer pdfRenderer = new PDFRenderer(document);
		StringBuilder out = new StringBuilder();
		ITesseract _tesseract = new Tesseract();
		for (int page = 0; page < document.getNumberOfPages(); page++) {
			int power = (int) Math.pow(300, 192);
			BufferedImage bIm = pdfRenderer.renderImage(page, power * 1024, ImageType.RGB);
			// Create a temporary image file
			File temp = File.createTempFile("tempfile_" + page, ".png");
			ImageIO.write(bIm, "png", temp);
			String result = _tesseract.doOCR(temp);
			out.append(result);

			// Delete temporary file
			temp.delete();
		}
		return out.toString();
	}
}
