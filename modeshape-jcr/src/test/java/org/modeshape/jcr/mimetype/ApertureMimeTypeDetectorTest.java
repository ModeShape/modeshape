/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of
 * individual contributors.
 *
 * ModeShape is free software. Unless otherwise indicated, all code in ModeShape
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * ModeShape is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org
 */
package org.modeshape.jcr.mimetype;

import static org.modeshape.jcr.api.mimetype.MimeTypeConstants.BASH;
import static org.modeshape.jcr.api.mimetype.MimeTypeConstants.BMP;
import static org.modeshape.jcr.api.mimetype.MimeTypeConstants.CALENDAR;
import static org.modeshape.jcr.api.mimetype.MimeTypeConstants.COREL_PRESENTATION;
import static org.modeshape.jcr.api.mimetype.MimeTypeConstants.COREL_QUATTRO_PRO;
import static org.modeshape.jcr.api.mimetype.MimeTypeConstants.COREL_QUATTRO_SPREADSHEET;
import static org.modeshape.jcr.api.mimetype.MimeTypeConstants.COREL_WORD_PERFECT;
import static org.modeshape.jcr.api.mimetype.MimeTypeConstants.GIF;
import static org.modeshape.jcr.api.mimetype.MimeTypeConstants.GZIP;
import static org.modeshape.jcr.api.mimetype.MimeTypeConstants.HTML;
import static org.modeshape.jcr.api.mimetype.MimeTypeConstants.ICON;
import static org.modeshape.jcr.api.mimetype.MimeTypeConstants.JAR;
import static org.modeshape.jcr.api.mimetype.MimeTypeConstants.JAR_MANIFEST;
import static org.modeshape.jcr.api.mimetype.MimeTypeConstants.JAVA;
import static org.modeshape.jcr.api.mimetype.MimeTypeConstants.JAVA_CLASS;
import static org.modeshape.jcr.api.mimetype.MimeTypeConstants.JPEG;
import static org.modeshape.jcr.api.mimetype.MimeTypeConstants.MESSAGE_RFC;
import static org.modeshape.jcr.api.mimetype.MimeTypeConstants.MICROSOFT_EXCEL;
import static org.modeshape.jcr.api.mimetype.MimeTypeConstants.MICROSOFT_EXCEL_OPENXML;
import static org.modeshape.jcr.api.mimetype.MimeTypeConstants.MICROSOFT_OFFICE;
import static org.modeshape.jcr.api.mimetype.MimeTypeConstants.MICROSOFT_OFFICE_DOCUMENT_OPENXML;
import static org.modeshape.jcr.api.mimetype.MimeTypeConstants.MICROSOFT_OUTLOOK;
import static org.modeshape.jcr.api.mimetype.MimeTypeConstants.MICROSOFT_POWERPOINT;
import static org.modeshape.jcr.api.mimetype.MimeTypeConstants.MICROSOFT_POWERPOINT_OPENXML;
import static org.modeshape.jcr.api.mimetype.MimeTypeConstants.MICROSOFT_PUBLISHER;
import static org.modeshape.jcr.api.mimetype.MimeTypeConstants.MICROSOFT_VISIO;
import static org.modeshape.jcr.api.mimetype.MimeTypeConstants.MICROSOFT_WORD;
import static org.modeshape.jcr.api.mimetype.MimeTypeConstants.MICROSOFT_WORKS;
import static org.modeshape.jcr.api.mimetype.MimeTypeConstants.MOZILLA_ADDRESS_BOOK;
import static org.modeshape.jcr.api.mimetype.MimeTypeConstants.MP3;
import static org.modeshape.jcr.api.mimetype.MimeTypeConstants.OGG;
import static org.modeshape.jcr.api.mimetype.MimeTypeConstants.OPEN_DOC_FORMULA;
import static org.modeshape.jcr.api.mimetype.MimeTypeConstants.OPEN_DOC_GRAPHICS;
import static org.modeshape.jcr.api.mimetype.MimeTypeConstants.OPEN_DOC_GRAPHICS_TEMPLATE;
import static org.modeshape.jcr.api.mimetype.MimeTypeConstants.OPEN_DOC_PRESENTATION;
import static org.modeshape.jcr.api.mimetype.MimeTypeConstants.OPEN_DOC_PRESENTATION_TEMPLATE;
import static org.modeshape.jcr.api.mimetype.MimeTypeConstants.OPEN_DOC_SPREADSHEET;
import static org.modeshape.jcr.api.mimetype.MimeTypeConstants.OPEN_DOC_SPREADSHEET_TEMPLATE;
import static org.modeshape.jcr.api.mimetype.MimeTypeConstants.OPEN_DOC_TEXT;
import static org.modeshape.jcr.api.mimetype.MimeTypeConstants.OPEN_DOC_TEXT_TEMPLATE;
import static org.modeshape.jcr.api.mimetype.MimeTypeConstants.OPEN_OFFICE_CALC;
import static org.modeshape.jcr.api.mimetype.MimeTypeConstants.OPEN_OFFICE_CALC_TEMPLATE;
import static org.modeshape.jcr.api.mimetype.MimeTypeConstants.OPEN_OFFICE_DRAW;
import static org.modeshape.jcr.api.mimetype.MimeTypeConstants.OPEN_OFFICE_DRAW_TEMPLATE;
import static org.modeshape.jcr.api.mimetype.MimeTypeConstants.OPEN_OFFICE_IMPRESS;
import static org.modeshape.jcr.api.mimetype.MimeTypeConstants.OPEN_OFFICE_IMPRESS_TEMPLATE;
import static org.modeshape.jcr.api.mimetype.MimeTypeConstants.OPEN_OFFICE_WRITER;
import static org.modeshape.jcr.api.mimetype.MimeTypeConstants.OPEN_OFFICE_WRITER_TEMPLATE;
import static org.modeshape.jcr.api.mimetype.MimeTypeConstants.PDF;
import static org.modeshape.jcr.api.mimetype.MimeTypeConstants.PNG;
import static org.modeshape.jcr.api.mimetype.MimeTypeConstants.PORTABLE_PIXMAP;
import static org.modeshape.jcr.api.mimetype.MimeTypeConstants.POSTSCRIPT;
import static org.modeshape.jcr.api.mimetype.MimeTypeConstants.RTF;
import static org.modeshape.jcr.api.mimetype.MimeTypeConstants.STAR_OFFICE_CALC;
import static org.modeshape.jcr.api.mimetype.MimeTypeConstants.STAR_OFFICE_DRAW;
import static org.modeshape.jcr.api.mimetype.MimeTypeConstants.STAR_OFFICE_IMPRESS;
import static org.modeshape.jcr.api.mimetype.MimeTypeConstants.STAR_OFFICE_WRITER;
import static org.modeshape.jcr.api.mimetype.MimeTypeConstants.TEXT_PLAIN;
import static org.modeshape.jcr.api.mimetype.MimeTypeConstants.TEXT_XML;
import static org.modeshape.jcr.api.mimetype.MimeTypeConstants.TGA;
import static org.modeshape.jcr.api.mimetype.MimeTypeConstants.TIFF;
import static org.modeshape.jcr.api.mimetype.MimeTypeConstants.VCARD;
import static org.modeshape.jcr.api.mimetype.MimeTypeConstants.WAV;
import static org.modeshape.jcr.api.mimetype.MimeTypeConstants.WMF;
import static org.modeshape.jcr.api.mimetype.MimeTypeConstants.XCF;
import static org.modeshape.jcr.api.mimetype.MimeTypeConstants.XPM;
import static org.modeshape.jcr.api.mimetype.MimeTypeConstants.ZIP;
import org.junit.Test;
import org.modeshape.jcr.api.mimetype.MimeTypeDetector;

/**
 * Unit test for {@link ApertureMimeTypeDetector}
 * 
 * @author Horia Chiorean
 */
public class ApertureMimeTypeDetectorTest extends AbstractMimeTypeTest {

    @Override
    protected MimeTypeDetector getDetector() {
        return new ApertureMimeTypeDetector();
    }

    @Test
    public void shouldProvideMimeTypeForText_test_txt() throws Exception {
        testMimeType("test.txt", TEXT_PLAIN);
    }

    @Test
    public void shouldProvideMimeTypeForText_plain_text_txt() throws Exception {
        testMimeType("docs/plain-text.txt", TEXT_PLAIN);
    }

    @Test
    public void shouldProvideMimeTypeForText_plain_text_ansi_txt() throws Exception {
        testMimeType("docs/plain-text-ansi.txt", TEXT_PLAIN);
    }

    @Test
    public void shouldProvideMimeTypeForText_plain_text_empty_txt() throws Exception {
        testMimeType("docs/plain-text-empty.txt", TEXT_PLAIN);
    }

    @Test
    public void shouldProvideMimeTypeForText_plain_text_utf16be_txt() throws Exception {
        testMimeType("docs/plain-text-utf16be.txt", TEXT_PLAIN);
    }

    @Test
    public void shouldProvideMimeTypeForText_plain_text_utf16le_txt() throws Exception {
        testMimeType("docs/plain-text-utf16le.txt", TEXT_PLAIN);
    }

    @Test
    public void shouldProvideMimeTypeForText_plain_text_utf8_txt() throws Exception {
        testMimeType("docs/plain-text-utf8.txt", TEXT_PLAIN);
    }

    @Test
    public void shouldProvideMimeTypeForText_plain_text_with_null_character_txt() throws Exception {
        testMimeType("docs/plain-text-with-null-character.txt", TEXT_PLAIN);
    }

    @Test
    public void shouldProvideMimeTypeForText_plain_text_without_extension() throws Exception {
        testMimeType("docs/plain-text-without-extension", TEXT_PLAIN);
    }

    @Test
    public void shouldProvideMimeTypeForRtf_test_rtf() throws Exception {
        testMimeType("test.rtf", RTF);
    }

    @Test
    public void shouldProvideMimeTypeForRtf_rtf_openoffice_1_1_5_rtf() throws Exception {
        testMimeType("docs/rtf-openoffice-1.1.5.rtf", RTF);
    }

    @Test
    public void shouldProvideMimeTypeForRtf_rtf_openoffice_2_0_rtf() throws Exception {
        testMimeType("docs/rtf-openoffice-2.0.rtf", RTF);
    }

    @Test
    public void shouldProvideMimeTypeForRtf_rtf_staroffice_5_2_rtf() throws Exception {
        testMimeType("docs/rtf-staroffice-5.2.rtf", RTF);
    }

    @Test
    public void shouldProvideMimeTypeForRtf_rtf_word_2000_rtf() throws Exception {
        testMimeType("docs/rtf-word-2000.rtf", RTF);
    }

    @Test
    public void shouldProvideMimeTypeForMp3_test_mp3() throws Exception {
        testMimeType("test.mp3", MP3);
    }

    @Test
    public void shouldProvideMimeTypeForMp3_test_128_44_jstereo_mp3() throws Exception {
        testMimeType("test_128_44_jstereo.mp3", MP3);
    }

    @Test
    public void shouldProvideMimeTypeForMp3_jingle1_mp3() throws Exception {
        testMimeType("docs/jingle1.mp3", MP3);
    }

    @Test
    public void shouldProvideMimeTypeForMp3_jingle2_mp3() throws Exception {
        testMimeType("docs/jingle2.mp3", MP3);
    }

    @Test
    public void shouldProvideMimeTypeForMp3_jingle3_mp3() throws Exception {
        testMimeType("docs/jingle3.mp3", MP3);
    }

    @Test
    public void shouldProvideMimeTypeForWav() throws Exception {
        testMimeType("test.wav", WAV);
    }

    @Test
    public void shouldProvideMimeTypeForBmp() throws Exception {
        testMimeType("test.bmp", BMP);
    }

    @Test
    public void shouldProvideMimeTypeForGif() throws Exception {
        testMimeType("test.gif", GIF);
    }

    @Test
    public void shouldProvideMimeTypeForIcon() throws Exception {
        testMimeType("test.ico", ICON);
    }

    @Test
    public void shouldProvideMimeTypeForJpeg_test_jpg() throws Exception {
        testMimeType("test.jpg", JPEG);
    }

    @Test
    public void shouldProvideMimeTypeForJpeg_jpg_exif_img_9367_JPG() throws Exception {
        testMimeType("docs/jpg-exif-img_9367.JPG", JPEG);
    }

    @Test
    public void shouldProvideMimeTypeForJpeg_jpg_exif_zerolength_jpg() throws Exception {
        testMimeType("docs/jpg-exif-zerolength.jpg", JPEG);
    }

    @Test
    public void shouldProvideMimeTypeForJpeg_jpg_geotagged_jpg() throws Exception {
        testMimeType("docs/jpg-geotagged.jpg", JPEG);
    }

    @Test
    public void shouldProvideMimeTypeForJpeg_jpg_geotagged_ipanema_jpg() throws Exception {
        testMimeType("docs/jpg-geotagged-ipanema.jpg", JPEG);
    }

    @Test
    public void shouldProvideMimeTypeForPortablePixelMap_test_ppm() throws Exception {
        testMimeType("test.ppm", PORTABLE_PIXMAP);
    }

    @Test
    public void shouldProvideMimeTypeForPortablePixelMap_test_pnm() throws Exception {
        testMimeType("test.pnm", PORTABLE_PIXMAP);
    }

    @Test
    public void shouldProvideMimeTypeForPng() throws Exception {
        testMimeType("test.png", PNG);
    }

    @Test
    public void shouldProvideMimeTypeForTiff() throws Exception {
        testMimeType("test_nocompress.tif", TIFF);
    }

    @Test
    public void shouldProvideMimeTypeForTga() throws Exception {
        testMimeType("test.tga", TGA);
    }

    @Test
    public void shouldProvideMimeTypeForWmf() throws Exception {
        testMimeType("test.wmf", WMF);
    }

    @Test
    public void shouldProvideMimeTypeForXcf() throws Exception {
        testMimeType("test.xcf", XCF);
    }

    @Test
    public void shouldProvideMimeTypeForXpm() throws Exception {
        testMimeType("test.xpm", XPM);
    }

    @Test
    public void shouldProvideMimeTypeForXml_test_xml() throws Exception {
        testMimeType("test.xml", TEXT_XML);
    }

    @Test
    public void shouldProvideMimeTypeForXml_test_excel_spreadsheet_xml() throws Exception {
        testMimeType("test_excel_spreadsheet.xml", TEXT_XML);
    }

    @Test
    public void shouldProvideMimeTypeForXml_CurrencyFormatterExample_mxml() throws Exception {
        testMimeType("CurrencyFormatterExample.mxml", TEXT_XML);
    }

    @Test
    public void shouldProvideMimeTypeForXml_xml_handwritten_xml() throws Exception {
        testMimeType("docs/xml-handwritten.xml", TEXT_XML);
    }

    @Test
    public void shouldProvideMimeTypeForXml_xml_nonexistent_dtd_xml() throws Exception {
        testMimeType("docs/xml-nonexistent-dtd.xml", TEXT_XML);
    }

    @Test
    public void shouldProvideMimeTypeForXml_xml_nonexistent_remote_dtd_xml() throws Exception {
        testMimeType("docs/xml-nonexistent-remote-dtd.xml", TEXT_XML);
    }

    @Test
    public void shouldProvideMimeTypeForXml_xml_nonexistent_remote_xsd_xml() throws Exception {
        testMimeType("docs/xml-nonexistent-remote-xsd.xml", TEXT_XML);
    }

    @Test
    public void shouldProvideMimeTypeForXml_xml_nonexistent_xsd_xml() throws Exception {
        testMimeType("docs/xml-nonexistent-xsd.xml", TEXT_XML);
    }

    @Test
    public void shouldProvideMimeTypeForXml_xml_utf8_bom() throws Exception {
        testMimeType("docs/xml-utf8-bom", TEXT_XML);
    }

    @Test
    public void shouldProvideMimeTypeForXsd() throws Exception {
        testMimeType("Descriptor.1.0.xsd", TEXT_XML);
    }

    @Test
    public void shouldProvideMimeTypeForDtd() throws Exception {
        testMimeType("test.dtd", TEXT_PLAIN);
    }

    @Test
    public void shouldProvideMimeTypeForHtml_master_xml() throws Exception {
        testMimeType("master.xml", HTML);
    }

    @Test
    public void shouldProvideMimeTypeForHtml_html_condenast_html() throws Exception {
        testMimeType("docs/html-condenast.html", HTML);
    }

    @Test
    public void shouldProvideMimeTypeForHtml_html_handwritten_html() throws Exception {
        testMimeType("docs/html-handwritten.html", HTML);
    }

    @Test
    public void shouldProvideMimeTypeForHtml_html_handwritten_with_wrong_file_extension_txt() throws Exception {
        testMimeType("docs/html-handwritten-with-wrong-file-extension.txt", HTML);
    }

    @Test
    public void shouldProvideMimeTypeForHtml_html_quelle_de_html() throws Exception {
        testMimeType("docs/html-quelle.de.html", HTML);
    }

    @Test
    public void shouldProvideMimeTypeForHtml_html_utf16_leading_whitespace_wrong_extension_doc() throws Exception {
        testMimeType("docs/html-utf16-leading-whitespace-wrong-extension.doc", HTML);
    }

    @Test
    public void shouldProvideMimeTypeForJava() throws Exception {
        testMimeType("test.java", JAVA);
    }

    @Test
    public void shouldProvideMimeTypeFor1_2Class() throws Exception {
        testMimeType("test_1.2.class", JAVA_CLASS);
    }

    @Test
    public void shouldProvideMimeTypeFor1_3Class() throws Exception {
        testMimeType("test_1.3.class", JAVA_CLASS);
    }

    @Test
    public void shouldProvideMimeTypeFor1_4Class() throws Exception {
        testMimeType("test_1.4.class", JAVA_CLASS);
    }

    @Test
    public void shouldProvideMimeTypeForPerl() throws Exception {
        testMimeType("test.pl", TEXT_PLAIN);
    }

    @Test
    public void shouldProvideMimeTypeForPython() throws Exception {
        testMimeType("test.py", TEXT_PLAIN);
    }

    @Test
    public void shouldProvideMimeTypeForPdf_test_pdf() throws Exception {
        testMimeType("test.pdf", PDF);
    }

    @Test
    public void shouldProvideMimeTypeForPdf_pdf_distiller_6_weirdchars_pdf() throws Exception {
        testMimeType("docs/pdf-distiller-6-weirdchars.pdf", PDF);
    }

    @Test
    public void shouldProvideMimeTypeForPdf_pdf_no_author_pdf() throws Exception {
        testMimeType("docs/pdf-no-author.pdf", PDF);
    }

    @Test
    public void shouldProvideMimeTypeForPdf_pdf_openoffice_1_1_5_writer_pdf() throws Exception {
        testMimeType("docs/pdf-openoffice-1.1.5-writer.pdf", PDF);
    }

    @Test
    public void shouldProvideMimeTypeForPdf_pdf_openoffice_2_0_writer_pdf() throws Exception {
        testMimeType("docs/pdf-openoffice-2.0-writer.pdf", PDF);
    }

    @Test
    public void shouldProvideMimeTypeForPdf_pdf_word_2000_pdfcreator_0_8_0_pdf() throws Exception {
        testMimeType("docs/pdf-word-2000-pdfcreator-0.8.0.pdf", PDF);
    }

    @Test
    public void shouldProvideMimeTypeForPdf_pdf_word_2000_pdfmaker_7_0_pdf() throws Exception {
        testMimeType("docs/pdf-word-2000-pdfmaker-7.0.pdf", PDF);
    }

    @Test
    public void shouldProvideMimeTypeForPdf_pdf_word_2000_pdfwriter_7_0_pdf() throws Exception {
        testMimeType("docs/pdf-word-2000-pdfwriter-7.0.pdf", PDF);
    }

    @Test
    public void shouldProvideMimeTypeForPostscript_test_ps() throws Exception {
        testMimeType("test.ps", POSTSCRIPT);
    }

    @Test
    public void shouldProvideMimeTypeForPostscript_test_eps() throws Exception {
        testMimeType("test.eps", POSTSCRIPT);
    }

    @Test
    public void shouldProvideMimeTypeForJar() throws Exception {
        testMimeType("dna-repository-0.2-SNAPSHOT.jar", JAR);
    }

    @Test
    public void shouldProvideMimeTypeForJavaManifest() throws Exception {
        testMimeType("aperture.example.manifest.mf", JAR_MANIFEST);
    }

    @Test
    public void shouldProvideMimeTypeForGZip_test_tar_gz() throws Exception {
        testMimeType("test.tar.gz", GZIP);
    }

    @Test
    public void shouldProvideMimeTypeForGZip_test_txt_gz() throws Exception {
        testMimeType("test.txt.gz", GZIP);
    }

    @Test
    public void shouldProvideMimeTypeForZip() throws Exception {
        testMimeType("docs/counting-input-stream-test-file.dat", ZIP);
    }

    @Test
    public void shouldProvideMimeTypeForBash() throws Exception {
        testMimeType("test.sh", BASH);
    }

    @Test
    public void shouldProvideMimeTypeForOgg() throws Exception {
        testMimeType("test.ogg", OGG);
    }

    @Test
    public void shouldProvideMimeTypeForOpenDocumentFormula() throws Exception {
        testMimeType("docs/openoffice-2.0-formula.odf", OPEN_DOC_FORMULA);
    }

    @Test
    public void shouldProvideMimeTypeForOpenDocumentGraphics() throws Exception {
        testMimeType("docs/openoffice-2.0-draw.odg", OPEN_DOC_GRAPHICS);
    }

    @Test
    public void shouldProvideMimeTypeForOpenDocumentGraphicsTemplate() throws Exception {
        testMimeType("docs/openoffice-2.0-draw-template.otg", OPEN_DOC_GRAPHICS_TEMPLATE);
    }

    @Test
    public void shouldProvideMimeTypeForOpenDocumentPresentation_component_architecture_odp() throws Exception {
        testMimeType("component-architecture.odp", OPEN_DOC_PRESENTATION);
    }

    @Test
    public void shouldProvideMimeTypeForOpenDocumentPresentation_openoffice_2_0_impress_odp() throws Exception {
        testMimeType("docs/openoffice-2.0-impress.odp", OPEN_DOC_PRESENTATION);
    }

    @Test
    public void shouldProvideMimeTypeForOpenDocumentPresentationTemplate() throws Exception {
        testMimeType("docs/openoffice-2.0-impress-template.otp", OPEN_DOC_PRESENTATION_TEMPLATE);
    }

    @Test
    public void shouldProvideMimeTypeForOpenDocumentSpreadsheet() throws Exception {
        testMimeType("docs/openoffice-2.0-calc.ods", OPEN_DOC_SPREADSHEET);
    }

    @Test
    public void shouldProvideMimeTypeForOpenDocumentSpreadsheetTemplate() throws Exception {
        testMimeType("docs/openoffice-2.0-calc-template.ots", OPEN_DOC_SPREADSHEET_TEMPLATE);
    }

    @Test
    public void shouldProvideMimeTypeForOpenDocumentText() throws Exception {
        testMimeType("docs/openoffice-2.0-writer.odt", OPEN_DOC_TEXT);
    }

    @Test
    public void shouldProvideMimeTypeForOpenDocumentTextTemplate() throws Exception {
        testMimeType("docs/openoffice-2.0-writer-template.ott", OPEN_DOC_TEXT_TEMPLATE);
    }

    @Test
    public void shouldProvideMimeTypeForOpenOfficeCalc() throws Exception {
        testMimeType("docs/openoffice-1.1.5-calc.sxc", OPEN_OFFICE_CALC);
    }

    @Test
    public void shouldProvideMimeTypeForOpenOfficeCalcTemplate() throws Exception {
        testMimeType("docs/openoffice-1.1.5-calc-template.stc", OPEN_OFFICE_CALC_TEMPLATE);
    }

    @Test
    public void shouldProvideMimeTypeForOpenOfficeDraw() throws Exception {
        testMimeType("docs/openoffice-1.1.5-draw.sxd", OPEN_OFFICE_DRAW);
    }

    @Test
    public void shouldProvideMimeTypeForOpenOfficeDrawTemplate() throws Exception {
        testMimeType("docs/openoffice-1.1.5-draw-template.std", OPEN_OFFICE_DRAW_TEMPLATE);
    }

    @Test
    public void shouldProvideMimeTypeForOpenOfficeImpress() throws Exception {
        testMimeType("docs/openoffice-1.1.5-impress.sxi", OPEN_OFFICE_IMPRESS);
    }

    @Test
    public void shouldProvideMimeTypeForOpenOfficeImpressTemplate() throws Exception {
        testMimeType("docs/openoffice-1.1.5-impress-template.sti", OPEN_OFFICE_IMPRESS_TEMPLATE);
    }

    @Test
    public void shouldProvideMimeTypeForOpenOfficeWriter() throws Exception {
        testMimeType("docs/openoffice-1.1.5-writer.sxw", OPEN_OFFICE_WRITER);
    }

    @Test
    public void shouldProvideMimeTypeForOpenOfficeWriterTemplate() throws Exception {
        testMimeType("docs/openoffice-1.1.5-writer-template.stw", OPEN_OFFICE_WRITER_TEMPLATE);
    }

    @Test
    public void shouldProvideMimeTypeForStarOfficeCalc() throws Exception {
        testMimeType("docs/staroffice-5.2-calc.sdc", STAR_OFFICE_CALC);
    }

    @Test
    public void shouldProvideMimeTypeForStarOfficeDraw() throws Exception {
        testMimeType("docs/staroffice-5.2-draw.sda", STAR_OFFICE_DRAW);
    }

    @Test
    public void shouldProvideMimeTypeForStarOfficeImpress() throws Exception {
        testMimeType("docs/staroffice-5.2-impress.sdd", STAR_OFFICE_IMPRESS);
    }

    @Test
    public void shouldProvideMimeTypeForStarOfficeWriter() throws Exception {
        testMimeType("docs/staroffice-5.2-writer.sdw", STAR_OFFICE_WRITER);
    }

    @Test
    public void shouldProvideMimeTypeForStarOfficeCalcTemplate() throws Exception {
        testMimeType("docs/staroffice-5.2-calc-template.vor", MICROSOFT_OFFICE);
    }

    @Test
    public void shouldProvideMimeTypeForStarOfficeDrawTemplate() throws Exception {
        testMimeType("docs/staroffice-5.2-draw-template.vor", MICROSOFT_OFFICE);
    }

    @Test
    public void shouldProvideMimeTypeForStarOfficeImpressTemplate() throws Exception {
        testMimeType("docs/staroffice-5.2-impress-template.vor", MICROSOFT_OFFICE);
    }

    @Test
    public void shouldProvideMimeTypeForStarOfficeWriterTemplate() throws Exception {
        testMimeType("docs/staroffice-5.2-writer-template.vor", MICROSOFT_OFFICE);
    }

    @Test
    public void shouldProvideMimeTypeForWord_test_word_2000_doc() throws Exception {
        testMimeType("test_word_2000.doc", MICROSOFT_WORD);
    }

    @Test
    public void shouldProvideMimeTypeForWord_test_word_6_0_95_doc() throws Exception {
        testMimeType("test_word_6.0_95.doc", MICROSOFT_WORD);
    }

    @Test
    public void shouldProvideMimeTypeForWord_microsoft_word_2000_doc() throws Exception {
        testMimeType("docs/microsoft-word-2000.doc", MICROSOFT_WORD);
    }

    @Test
    public void shouldProvideMimeTypeForWord_microsoft_word_2000_with_wrong_file_extension_pdf() throws Exception {
        testMimeType("docs/microsoft-word-2000-with-wrong-file-extension.pdf", MICROSOFT_OFFICE);
    }

    @Test
    public void shouldProvideMimeTypeForWord_microsoft_word_2007beta2_docm() throws Exception {
        testMimeType("docs/microsoft-word-2007beta2.docm", MICROSOFT_OFFICE_DOCUMENT_OPENXML);
    }

    @Test
    public void shouldProvideMimeTypeForWord_microsoft_word_2007beta2_docx() throws Exception {
        testMimeType("docs/microsoft-word-2007beta2.docx", MICROSOFT_OFFICE_DOCUMENT_OPENXML);
    }

    @Test
    public void shouldProvideMimeTypeForWord_microsoft_word_2007beta2_dotm() throws Exception {
        testMimeType("docs/microsoft-word-2007beta2.dotm", MICROSOFT_OFFICE_DOCUMENT_OPENXML);
    }

    @Test
    public void shouldProvideMimeTypeForWord_microsoft_word_2007beta2_dotx() throws Exception {
        testMimeType("docs/microsoft-word-2007beta2.dotx", MICROSOFT_OFFICE_DOCUMENT_OPENXML);
    }

    @Test
    public void shouldProvideMimeTypeForWorks_microsoft_works_spreadsheet_4_0_2000_wks() throws Exception {
        testMimeType("docs/microsoft-works-spreadsheet-4.0-2000.wks", MICROSOFT_WORKS);
    }

    @Test
    public void shouldProvideMimeTypeForWorks_microsoft_works_spreadsheet_7_0_xlr() throws Exception {
        testMimeType("docs/microsoft-works-spreadsheet-7.0.xlr", MICROSOFT_WORKS);
    }

    @Test
    public void shouldProvideMimeTypeForWorks_microsoft_works_word_processor_2000_wps() throws Exception {
        testMimeType("docs/microsoft-works-word-processor-2000.wps", MICROSOFT_WORKS);
    }

    @Test
    public void shouldProvideMimeTypeForWorks_microsoft_works_word_processor_3_0_wps() throws Exception {
        testMimeType("docs/microsoft-works-word-processor-3.0.wps", MICROSOFT_WORKS);
    }

    @Test
    public void shouldProvideMimeTypeForWorks_microsoft_works_word_processor_4_0_wps() throws Exception {
        testMimeType("docs/microsoft-works-word-processor-4.0.wps", MICROSOFT_WORKS);
    }

    @Test
    public void shouldProvideMimeTypeForWorks_microsoft_works_word_processor_7_0_wps() throws Exception {
        testMimeType("docs/microsoft-works-word-processor-7.0.wps", MICROSOFT_WORKS);
    }

    @Test
    public void shouldProvideMimeTypeForWorkbook_corel_quattro_pro_6_wb2() throws Exception {
        testMimeType("docs/corel-quattro-pro-6.wb2", COREL_QUATTRO_SPREADSHEET);
    }

    @Test
    public void shouldProvideMimeTypeForWorkbook_microsoft_works_spreadsheet_3_0_wks() throws Exception {
        testMimeType("docs/microsoft-works-spreadsheet-3.0.wks", COREL_QUATTRO_SPREADSHEET);
    }

    @Test
    public void shouldProvideMimeTypeForExcel_test_excel_2000_xls() throws Exception {
        testMimeType("test_excel_2000.xls", MICROSOFT_EXCEL);
    }

    @Test
    public void shouldProvideMimeTypeForExcel_microsoft_excel_2000_xls() throws Exception {
        testMimeType("docs/microsoft-excel-2000.xls", MICROSOFT_EXCEL);
    }

    @Test
    public void shouldProvideMimeTypeForExcel_microsoft_excel_2007beta2_xlam() throws Exception {
        testMimeType("docs/microsoft-excel-2007beta2.xlam", MICROSOFT_EXCEL_OPENXML);
    }

    @Test
    public void shouldProvideMimeTypeForExcel_microsoft_excel_2007beta2_xlsb() throws Exception {
        testMimeType("docs/microsoft-excel-2007beta2.xlsb", MICROSOFT_EXCEL_OPENXML);
    }

    @Test
    public void shouldProvideMimeTypeForExcel_microsoft_excel_2007beta2_xlsm() throws Exception {
        testMimeType("docs/microsoft-excel-2007beta2.xlsm", MICROSOFT_EXCEL_OPENXML);
    }

    @Test
    public void shouldProvideMimeTypeForExcel_microsoft_excel_2007beta2_xlsx() throws Exception {
        testMimeType("docs/microsoft-excel-2007beta2.xlsx", MICROSOFT_EXCEL_OPENXML);
    }

    @Test
    public void shouldProvideMimeTypeForExcel_microsoft_excel_2007beta2_xltm() throws Exception {
        testMimeType("docs/microsoft-excel-2007beta2.xltm", MICROSOFT_EXCEL_OPENXML);
    }

    @Test
    public void shouldProvideMimeTypeForExcel_microsoft_excel_2007beta2_xltx() throws Exception {
        testMimeType("docs/microsoft-excel-2007beta2.xltx", MICROSOFT_EXCEL_OPENXML);
    }

    @Test
    public void shouldProvideMimeTypeForPowerpoint_test_ppt() throws Exception {
        testMimeType("test.ppt", MICROSOFT_POWERPOINT);
    }

    @Test
    public void shouldProvideMimeTypeForPowerpoint_microsoft_powerpoint_2000_ppt() throws Exception {
        testMimeType("docs/microsoft-powerpoint-2000.ppt", MICROSOFT_POWERPOINT);
    }

    @Test
    public void shouldProvideMimeTypeForPowerpoint_microsoft_powerpoint_2007beta2_potm() throws Exception {
        testMimeType("docs/microsoft-powerpoint-2007beta2.potm", MICROSOFT_POWERPOINT_OPENXML);
    }

    @Test
    public void shouldProvideMimeTypeForPowerpoint_microsoft_powerpoint_2007beta2_potx() throws Exception {
        testMimeType("docs/microsoft-powerpoint-2007beta2.potx", MICROSOFT_POWERPOINT_OPENXML);
    }

    @Test
    public void shouldProvideMimeTypeForPowerpoint_microsoft_powerpoint_2007beta2_ppsm() throws Exception {
        testMimeType("docs/microsoft-powerpoint-2007beta2.ppsm", MICROSOFT_POWERPOINT_OPENXML);
    }

    @Test
    public void shouldProvideMimeTypeForPowerpoint_microsoft_powerpoint_2007beta2_ppsx() throws Exception {
        testMimeType("docs/microsoft-powerpoint-2007beta2.ppsx", MICROSOFT_POWERPOINT_OPENXML);
    }

    @Test
    public void shouldProvideMimeTypeForPowerpoint_microsoft_powerpoint_2007beta2_pptm() throws Exception {
        testMimeType("docs/microsoft-powerpoint-2007beta2.pptm", MICROSOFT_POWERPOINT_OPENXML);
    }

    @Test
    public void shouldProvideMimeTypeForPowerpoint_microsoft_powerpoint_2007beta2_pptx() throws Exception {
        testMimeType("docs/microsoft-powerpoint-2007beta2.pptx", MICROSOFT_POWERPOINT_OPENXML);
    }

    @Test
    public void shouldProvideMimeTypeForPublisher() throws Exception {
        testMimeType("docs/microsoft-publisher-2003.pub", MICROSOFT_PUBLISHER);
    }

    @Test
    public void shouldProvideMimeTypeForVisio() throws Exception {
        testMimeType("docs/microsoft-visio.vsd", MICROSOFT_VISIO);
    }

    @Test
    public void shouldProvideMimeTypeForOutlook() throws Exception {
        testMimeType("TestData.pst", MICROSOFT_OUTLOOK);
    }

    @Test
    public void shouldProvideMimeTypeForShw_corel_presentations_3_0_shw() throws Exception {
        testMimeType("docs/corel-presentations-3.0.shw", COREL_PRESENTATION);
    }

    @Test
    public void shouldProvideMimeTypeForShw_corel_presentations_x3_shw() throws Exception {
        testMimeType("docs/corel-presentations-x3.shw", COREL_PRESENTATION);
    }

    @Test
    public void shouldProvideMimeTypeForQuattroPro_corel_quattro_pro_7_wb3() throws Exception {
        testMimeType("docs/corel-quattro-pro-7.wb3", COREL_QUATTRO_PRO);
    }

    @Test
    public void shouldProvideMimeTypeForQuattroPro_corel_quattro_pro_x3_qpw() throws Exception {
        testMimeType("docs/corel-quattro-pro-x3.qpw", COREL_QUATTRO_PRO);
    }

    @Test
    public void shouldProvideMimeTypeForWordperfect_corel_wordperfect_4_2_wp() throws Exception {
        testMimeType("docs/corel-wordperfect-4.2.wp", COREL_WORD_PERFECT);
    }

    @Test
    public void shouldProvideMimeTypeForWordperfect_corel_wordperfect_5_0_wp() throws Exception {
        testMimeType("docs/corel-wordperfect-5.0.wp", COREL_WORD_PERFECT);
    }

    @Test
    public void shouldProvideMimeTypeForWordperfect_corel_wordperfect_5_1_wp() throws Exception {
        testMimeType("docs/corel-wordperfect-5.1.wp", COREL_WORD_PERFECT);
    }

    @Test
    public void shouldProvideMimeTypeForWordperfect_corel_wordperfect_5_1_far_east_wp() throws Exception {
        testMimeType("docs/corel-wordperfect-5.1-far-east.wp", COREL_WORD_PERFECT);
    }

    @Test
    public void shouldProvideMimeTypeForWordperfect_corel_wordperfect_x3_wpd() throws Exception {
        testMimeType("docs/corel-wordperfect-x3.wpd", COREL_WORD_PERFECT);
    }

    @Test
    public void shouldProvideMimeTypeForMail_test_excel_web_archive_mht() throws Exception {
        testMimeType("test_excel_web_archive.mht", MESSAGE_RFC);
    }

    @Test
    public void shouldProvideMimeTypeForMail_mail_thunderbird_1_5_eml() throws Exception {
        testMimeType("docs/mail-thunderbird-1.5.eml", MESSAGE_RFC);
    }

    @Test
    public void shouldProvideMimeTypeForMail_mhtml_firefox_mht() throws Exception {
        testMimeType("docs/mhtml-firefox.mht", MESSAGE_RFC);
    }

    @Test
    public void shouldProvideMimeTypeForMail_mhtml_internet_explorer_mht() throws Exception {
        testMimeType("docs/mhtml-internet-explorer.mht", MESSAGE_RFC);
    }

    @Test
    public void shouldProvideMimeTypeForAddressBook() throws Exception {
        testMimeType("docs/thunderbird-addressbook.mab", MOZILLA_ADDRESS_BOOK);
    }

    @Test
    public void shouldProvideMimeTypeForVCard_vcard_antoni_kontact_vcf() throws Exception {
        testMimeType("docs/vcard-antoni-kontact.vcf", VCARD);
    }

    @Test
    public void shouldProvideMimeTypeForVCard_vcard_antoni_outlook2003_vcf() throws Exception {
        testMimeType("docs/vcard-antoni-outlook2003.vcf", VCARD);
    }

    @Test
    public void shouldProvideMimeTypeForVCard_vcard_dirk_vcf() throws Exception {
        testMimeType("docs/vcard-dirk.vcf", VCARD);
    }

    @Test
    public void shouldProvideMimeTypeForVCard_vcard_rfc2426_vcf() throws Exception {
        testMimeType("docs/vcard-rfc2426.vcf", VCARD);
    }

    @Test
    public void shouldProvideMimeTypeForVCard_vcard_vCards_SAP_vcf() throws Exception {
        testMimeType("docs/vcard-vCards-SAP.vcf", VCARD);
    }

    @Test
    public void shouldProvideMimeTypeForCalendar_basicCalendar_ics() throws Exception {
        testMimeType("docs/icaltestdata/basicCalendar.ics", CALENDAR);
    }

    @Test
    public void shouldProvideMimeTypeForCalendar_cal01_ics() throws Exception {
        testMimeType("docs/icaltestdata/cal01.ics", CALENDAR);
    }

    @Test
    public void shouldProvideMimeTypeForCalendar_cal01_1_ics() throws Exception {
        testMimeType("docs/icaltestdata/cal01-1.ics", CALENDAR);
    }

    @Test
    public void shouldProvideMimeTypeForCalendar_cal01_2_ics() throws Exception {
        testMimeType("docs/icaltestdata/cal01-2.ics", CALENDAR);
    }

    @Test
    public void shouldProvideMimeTypeForCalendar_cal01_3_ics() throws Exception {
        testMimeType("docs/icaltestdata/cal01-3.ics", CALENDAR);
    }

    @Test
    public void shouldProvideMimeTypeForCalendar_cal01_4_ics() throws Exception {
        testMimeType("docs/icaltestdata/cal01-4.ics", CALENDAR);
    }

    @Test
    public void shouldProvideMimeTypeForCalendar_cal01_5_ics() throws Exception {
        testMimeType("docs/icaltestdata/cal01-5.ics", CALENDAR);
    }

    @Test
    public void shouldProvideMimeTypeForCalendar_cal01_6_ics() throws Exception {
        testMimeType("docs/icaltestdata/cal01-6.ics", CALENDAR);
    }

    @Test
    public void shouldProvideMimeTypeForCalendar_cal01_exrule_ics() throws Exception {
        testMimeType("docs/icaltestdata/cal01-exrule.ics", CALENDAR);
    }

    @Test
    public void shouldProvideMimeTypeForCalendar_calconnect7_ics() throws Exception {
        testMimeType("docs/icaltestdata/calconnect7.ics", CALENDAR);
    }

    @Test
    public void shouldProvideMimeTypeForCalendar_calconnect9_ics() throws Exception {
        testMimeType("docs/icaltestdata/calconnect9.ics", CALENDAR);
    }

    @Test
    public void shouldProvideMimeTypeForCalendar_combined_multiplevcalendar_ics() throws Exception {
        testMimeType("docs/icaltestdata/combined_multiplevcalendar.ics", CALENDAR);
    }

    @Test
    public void shouldProvideMimeTypeForCalendar_combined_onevcalendar_ics() throws Exception {
        testMimeType("docs/icaltestdata/combined_onevcalendar.ics", CALENDAR);
    }

    @Test
    public void shouldProvideMimeTypeForCalendar_extendedCalendar_ics() throws Exception {
        testMimeType("docs/icaltestdata/extendedCalendar.ics", CALENDAR);
    }

    @Test
    public void shouldProvideMimeTypeForCalendar_freebusy_ics() throws Exception {
        testMimeType("docs/icaltestdata/freebusy.ics", CALENDAR);
    }

    @Test
    public void shouldProvideMimeTypeForCalendar_geol_ics() throws Exception {
        testMimeType("docs/icaltestdata/geo1.ics", CALENDAR);
    }

    @Test
    public void shouldProvideMimeTypeForCalendar_gkexample_ics() throws Exception {
        testMimeType("docs/icaltestdata/gkexample.ics", CALENDAR);
    }

    @Test
    public void shouldProvideMimeTypeForCalendar_incoming_ics() throws Exception {
        testMimeType("docs/icaltestdata/incoming.ics", CALENDAR);
    }

    @Test
    public void shouldProvideMimeTypeForCalendar_korganizer_jicaltest_vjournal_ics() throws Exception {
        testMimeType("docs/icaltestdata/korganizer-jicaltest-vjournal.ics", CALENDAR);
    }

    @Test
    public void shouldProvideMimeTypeForCalendar_korganizer_jicaltest_ics() throws Exception {
        testMimeType("docs/icaltestdata/korganizer-jicaltest.ics", CALENDAR);
    }

    @Test
    public void shouldProvideMimeTypeForCalendar_php_flp_ics() throws Exception {
        testMimeType("docs/icaltestdata/php-flp.ics", CALENDAR);
    }

    @Test
    public void shouldProvideMimeTypeForCalendar_simplevevent_ics() throws Exception {
        testMimeType("docs/icaltestdata/simplevevent.ics", CALENDAR);
    }

    @Test
    public void shouldProvideMimeTypeForCalendar_sunbird_sample_ics() throws Exception {
        testMimeType("docs/icaltestdata/sunbird_sample.ics", CALENDAR);
    }

    @Test
    public void shouldProvideMimeTypeForCalendar_tag_bug_ics() throws Exception {
        testMimeType("docs/icaltestdata/tag-bug.ics", CALENDAR);
    }

    @Test
    public void shouldProvideMimeTypeForCalendar_test_created_ics() throws Exception {
        testMimeType("docs/icaltestdata/test-created.ics", CALENDAR);
    }

    @Test
    public void shouldProvideMimeTypeForCalendar_Todos1_ics() throws Exception {
        testMimeType("docs/icaltestdata/Todos1.ics", CALENDAR);
    }

    @Test
    public void shouldProvideMimeTypeForAu() throws Exception {
        testMimeType("test.au", null);
    }

    @Test
    public void shouldProvideMimeTypeForBin() throws Exception {
        testMimeType("test.bin", null);
    }

    @Test
    public void shouldProvideMimeTypeForEmf() throws Exception {
        testMimeType("test.emf", null);
    }

    @Test
    public void shouldProvideMimeTypeForFli() throws Exception {
        testMimeType("test.fli", null);
    }

    @Test
    public void shouldProvideMimeTypeForPcx() throws Exception {
        testMimeType("test.pcx", null);
    }

    @Test
    public void shouldProvideMimeTypeForPict() throws Exception {
        testMimeType("test.pict", null);
    }

    @Test
    public void shouldProvideMimeTypeForPsd() throws Exception {
        testMimeType("test.psd", null);
    }

    @Test
    public void shouldProvideMimeTypeForTar() throws Exception {
        testMimeType("test.tar", null);
    }
}
