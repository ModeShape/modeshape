/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * distribution For a full listing of individual contributors. 
 *
 * ModeShape is free software. Unless otherwise indicated, all code in ModeShape
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * ModeShape is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS For A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License For more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.modeshape.mimetype.aperture;

import java.io.File;
import java.io.FileNotFoundException;
import org.modeshape.graph.mimetype.AbstractMimeTypeTest;
import org.junit.Test;

/**
 * @author jverhaeg
 */
public class MimeTypeTest extends AbstractMimeTypeTest {

    public MimeTypeTest() {
        super(ApertureMimeTypeDetector.class);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.mimetype.AbstractMimeTypeTest#getFile(java.lang.String)
     */
    @Override
    protected File getFile( String name ) {
        return new File("src/test/resources/" + name);
    }

    @Override
    protected String expectedMimeTypeForText_test_txt() {
        return "text/plain";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForText_plain_text_ansi_txt()
     */
    @Override
    protected String expectedMimeTypeForText_plain_text_ansi_txt() {
        return "text/plain";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForText_plain_text_empty_txt()
     */
    @Override
    protected String expectedMimeTypeForText_plain_text_empty_txt() {
        return "text/plain";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForText_plain_text_txt()
     */
    @Override
    protected String expectedMimeTypeForText_plain_text_txt() {
        return "text/plain";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForText_plain_text_with_null_character_txt()
     */
    @Override
    protected String expectedMimeTypeForText_plain_text_with_null_character_txt() {
        return "text/plain";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForText_plain_text_utf16be_txt()
     */
    @Override
    protected String expectedMimeTypeForText_plain_text_utf16be_txt() {
        return "text/plain";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForText_plain_text_utf16le_txt()
     */
    @Override
    protected String expectedMimeTypeForText_plain_text_utf16le_txt() {
        return "text/plain";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForText_plain_text_utf8_txt()
     */
    @Override
    protected String expectedMimeTypeForText_plain_text_utf8_txt() {
        return "text/plain";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForText_plain_text_without_extension()
     */
    @Override
    protected String expectedMimeTypeForText_plain_text_without_extension() {
        return "text/plain";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForRtf_rtf_staroffice_5_2_rtf()
     */
    @Override
    protected String expectedMimeTypeForRtf_rtf_staroffice_5_2_rtf() {
        return "text/rtf";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForRtf_rtf_openoffice_2_0_rtf()
     */
    @Override
    protected String expectedMimeTypeForRtf_rtf_openoffice_2_0_rtf() {
        return "text/rtf";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForRtf_rtf_word_2000_rtf()
     */
    @Override
    protected String expectedMimeTypeForRtf_rtf_word_2000_rtf() {
        return "text/rtf";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForRtf_rtf_openoffice_1_1_5_rtf()
     */
    @Override
    protected String expectedMimeTypeForRtf_rtf_openoffice_1_1_5_rtf() {
        return "text/rtf";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForRtf_test_rtf()
     */
    @Override
    protected String expectedMimeTypeForRtf_test_rtf() {
        return "text/rtf";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForMp3_jingle3_mp3()
     */
    @Override
    protected String expectedMimeTypeForMp3_jingle3_mp3() {
        return "audio/mpeg";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForMp3_jingle1_mp3()
     */
    @Override
    protected String expectedMimeTypeForMp3_jingle1_mp3() {
        return "audio/mpeg";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForMp3_jingle2_mp3()
     */
    @Override
    protected String expectedMimeTypeForMp3_jingle2_mp3() {
        return "audio/mpeg";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForMp3_test_128_44_jstereo_mp3()
     */
    @Override
    protected String expectedMimeTypeForMp3_test_128_44_jstereo_mp3() {
        return "audio/mpeg";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForMp3_test_mp3()
     */
    @Override
    protected String expectedMimeTypeForMp3_test_mp3() {
        return "audio/mpeg";
    }

    @Override
    public String expectedMimeTypeForWav() {
        return "audio/x-wav";
    }

    @Override
    public String expectedMimeTypeForBmp() {
        return "image/bmp";
    }

    @Override
    public String expectedMimeTypeForGif() {
        return "image/gif";
    }

    @Override
    public String expectedMimeTypeForIcon() {
        return "image/x-icon";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForJpeg_jpg_geotagged_jpg()
     */
    @Override
    protected String expectedMimeTypeForJpeg_jpg_geotagged_jpg() {
        return "image/jpeg";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForJpeg_jpg_exif_zerolength_jpg()
     */
    @Override
    protected String expectedMimeTypeForJpeg_jpg_exif_zerolength_jpg() {
        return "image/jpeg";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForJpeg_jpg_geotagged_ipanema_jpg()
     */
    @Override
    protected String expectedMimeTypeForJpeg_jpg_geotagged_ipanema_jpg() {
        return "image/jpeg";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForJpeg_jpg_exif_img_9367_JPG()
     */
    @Override
    protected String expectedMimeTypeForJpeg_jpg_exif_img_9367_JPG() {
        return "image/jpeg";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForJpeg_test_jpg()
     */
    @Override
    protected String expectedMimeTypeForJpeg_test_jpg() {
        return "image/jpeg";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForPortablePixelMap_test_ppm()
     */
    @Override
    protected String expectedMimeTypeForPortablePixelMap_test_ppm() {
        return "image/x-portable-pixmap";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForPortablePixelMap_test_pnm()
     */
    @Override
    protected String expectedMimeTypeForPortablePixelMap_test_pnm() {
        return "image/x-portable-pixmap";
    }

    @Override
    public String expectedMimeTypeForPng() {
        return "image/png";
    }

    @Override
    public String expectedMimeTypeForTiff() {
        return "image/tiff";
    }

    @Override
    public String expectedMimeTypeForTga() {
        return "image/x-tga";
    }

    @Override
    public String expectedMimeTypeForWmf() {
        return "image/wmf";
    }

    @Override
    public String expectedMimeTypeForXcf() {
        return "image/xcf";
    }

    @Override
    public String expectedMimeTypeForXpm() {
        return "image/xpm";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForXml_xml_utf8_bom()
     */
    @Override
    protected String expectedMimeTypeForXml_xml_utf8_bom() {
        return "text/xml";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForXml_CurrencyFormatterExample_mxml()
     */
    @Override
    protected String expectedMimeTypeForXml_CurrencyFormatterExample_mxml() {
        return "text/xml";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForXml_test_excel_spreadsheet_xml()
     */
    @Override
    protected String expectedMimeTypeForXml_test_excel_spreadsheet_xml() {
        return "text/xml";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForXml_test_xml()
     */
    @Override
    protected String expectedMimeTypeForXml_test_xml() {
        return "text/xml";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForXml_xml_handwritten_xml()
     */
    @Override
    protected String expectedMimeTypeForXml_xml_handwritten_xml() {
        return "text/xml";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForXml_xml_nonexistent_dtd_xml()
     */
    @Override
    protected String expectedMimeTypeForXml_xml_nonexistent_dtd_xml() {
        return "text/xml";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForXml_xml_nonexistent_remote_dtd_xml()
     */
    @Override
    protected String expectedMimeTypeForXml_xml_nonexistent_remote_dtd_xml() {
        return "text/xml";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForXml_xml_nonexistent_remote_xsd_xml()
     */
    @Override
    protected String expectedMimeTypeForXml_xml_nonexistent_remote_xsd_xml() {
        return "text/xml";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForXml_xml_nonexistent_xsd_xml()
     */
    @Override
    protected String expectedMimeTypeForXml_xml_nonexistent_xsd_xml() {
        return "text/xml";
    }

    @Override
    public String expectedMimeTypeForXsd() {
        return "text/xml";
    }

    @Override
    public String expectedMimeTypeForDtd() {
        return "text/plain";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForHtml_html_condenast_html()
     */
    @Override
    protected String expectedMimeTypeForHtml_html_condenast_html() {
        return "text/html";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForHtml_html_handwritten_html()
     */
    @Override
    protected String expectedMimeTypeForHtml_html_handwritten_html() {
        return "text/html";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForHtml_html_handwritten_with_wrong_file_extension_txt()
     */
    @Override
    protected String expectedMimeTypeForHtml_html_handwritten_with_wrong_file_extension_txt() {
        return "text/html";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForHtml_html_quelle_de_html()
     */
    @Override
    protected String expectedMimeTypeForHtml_html_quelle_de_html() {
        return "text/html";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForHtml_html_utf16_leading_whitespace_wrong_extension_doc()
     */
    @Override
    protected String expectedMimeTypeForHtml_html_utf16_leading_whitespace_wrong_extension_doc() {
        return "text/html";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForHtml_master_xml()
     */
    @Override
    protected String expectedMimeTypeForHtml_master_xml() {
        return "text/html";
    }

    @Override
    public String expectedMimeTypeForJava() {
        return "text/java";
    }

    @Override
    public String expectedMimeTypeFor1_2Class() {
        return "application/x-java-class";
    }

    @Override
    public String expectedMimeTypeFor1_3Class() {
        return "application/x-java-class";
    }

    @Override
    public String expectedMimeTypeFor1_4Class() {
        return "application/x-java-class";
    }

    @Override
    public String expectedMimeTypeForPerl() {
        return "text/plain";
    }

    @Override
    public String expectedMimeTypeForPython() {
        return "text/plain";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForPdf_test_pdf()
     */
    @Override
    protected String expectedMimeTypeForPdf_test_pdf() {
        return "application/pdf";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForPdf_pdf_no_author_pdf()
     */
    @Override
    protected String expectedMimeTypeForPdf_pdf_no_author_pdf() {
        return "application/pdf";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForPdf_pdf_openoffice_1_1_5_writer_pdf()
     */
    @Override
    protected String expectedMimeTypeForPdf_pdf_openoffice_1_1_5_writer_pdf() {
        return "application/pdf";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForPdf_pdf_openoffice_2_0_writer_pdf()
     */
    @Override
    protected String expectedMimeTypeForPdf_pdf_openoffice_2_0_writer_pdf() {
        return "application/pdf";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForPdf_pdf_word_2000_pdfcreator_0_8_0_pdf()
     */
    @Override
    protected String expectedMimeTypeForPdf_pdf_word_2000_pdfcreator_0_8_0_pdf() {
        return "application/pdf";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForPdf_pdf_word_2000_pdfmaker_7_0_pdf()
     */
    @Override
    protected String expectedMimeTypeForPdf_pdf_word_2000_pdfmaker_7_0_pdf() {
        return "application/pdf";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForPdf_pdf_word_2000_pdfwriter_7_0_pdf()
     */
    @Override
    protected String expectedMimeTypeForPdf_pdf_word_2000_pdfwriter_7_0_pdf() {
        return "application/pdf";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForPdf_pdf_distiller_6_weirdchars_pdf()
     */
    @Override
    protected String expectedMimeTypeForPdf_pdf_distiller_6_weirdchars_pdf() {
        return "application/pdf";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForPostscript_test_eps()
     */
    @Override
    protected String expectedMimeTypeForPostscript_test_eps() {
        return "application/postscript";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForPostscript_test_ps()
     */
    @Override
    protected String expectedMimeTypeForPostscript_test_ps() {
        return "application/postscript";
    }

    @Override
    public String expectedMimeTypeForJar() {
        return "application/java-archive";
    }

    @Override
    public String expectedMimeTypeForJavaManifest() {
        return "application/x-java-manifest";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForGZip_test_tar_gz()
     */
    @Override
    protected String expectedMimeTypeForGZip_test_tar_gz() {
        return "application/gzip";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForGZip_test_txt_gz()
     */
    @Override
    protected String expectedMimeTypeForGZip_test_txt_gz() {
        return "application/gzip";
    }

    @Override
    public String expectedMimeTypeForZip() {
        return "application/zip";
    }

    @Override
    public String expectedMimeTypeForBash() {
        return "application/x-bash";
    }

    @Override
    public String expectedMimeTypeForOgg() {
        return "application/x-ogg";
    }

    @Override
    public String expectedMimeTypeForOpenDocumentFormula() {
        return "application/vnd.oasis.opendocument.formula";
    }

    @Override
    public String expectedMimeTypeForOpenDocumentGraphics() {
        return "application/vnd.oasis.opendocument.graphics";
    }

    @Override
    public String expectedMimeTypeForOpenDocumentGraphicsTemplate() {
        return "application/vnd.oasis.opendocument.graphics-template";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForOpenDocumentPresentation_openoffice_2_0_impress_odp()
     */
    @Override
    protected String expectedMimeTypeForOpenDocumentPresentation_openoffice_2_0_impress_odp() {
        return "application/vnd.oasis.opendocument.presentation";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForOpenDocumentPresentation_component_architecture_odp()
     */
    @Override
    protected String expectedMimeTypeForOpenDocumentPresentation_component_architecture_odp() {
        return "application/vnd.oasis.opendocument.presentation";
    }

    @Override
    public String expectedMimeTypeForOpenDocumentPresentationTemplate() {
        return "application/vnd.oasis.opendocument.presentation-template";
    }

    @Override
    public String expectedMimeTypeForOpenDocumentSpreadsheet() {
        return "application/vnd.oasis.opendocument.spreadsheet";
    }

    @Override
    public String expectedMimeTypeForOpenDocumentSpreadsheetTemplate() {
        return "application/vnd.oasis.opendocument.spreadsheet-template";
    }

    @Override
    public String expectedMimeTypeForOpenDocumentText() {
        return "application/vnd.oasis.opendocument.text";
    }

    @Override
    public String expectedMimeTypeForOpenDocumentTextTemplate() {
        return "application/vnd.oasis.opendocument.text-template";
    }

    @Override
    public String expectedMimeTypeForOpenOfficeCalc() {
        return "application/vnd.sun.xml.calc";
    }

    @Override
    public String expectedMimeTypeForOpenOfficeCalcTemplate() {
        return "application/vnd.sun.xml.calc.template";
    }

    @Override
    public String expectedMimeTypeForOpenOfficeDraw() {
        return "application/vnd.sun.xml.draw";
    }

    @Override
    public String expectedMimeTypeForOpenOfficeDrawTemplate() {
        return "application/vnd.sun.xml.draw.template";
    }

    @Override
    public String expectedMimeTypeForOpenOfficeImpress() {
        return "application/vnd.sun.xml.impress";
    }

    @Override
    public String expectedMimeTypeForOpenOfficeImpressTemplate() {
        return "application/vnd.sun.xml.impress.template";
    }

    @Override
    public String expectedMimeTypeForOpenOfficeWriter() {
        return "application/vnd.sun.xml.writer";
    }

    @Override
    public String expectedMimeTypeForOpenOfficeWriterTemplate() {
        return "application/vnd.sun.xml.writer.template";
    }

    @Override
    public String expectedMimeTypeForStarOfficeCalc() {
        return "application/vnd.stardivision.calc";
    }

    @Override
    public String expectedMimeTypeForStarOfficeDraw() {
        return "application/vnd.stardivision.draw";
    }

    @Override
    public String expectedMimeTypeForStarOfficeImpress() {
        return "application/vnd.stardivision.impress";
    }

    @Override
    public String expectedMimeTypeForStarOfficeWriter() {
        return "application/vnd.stardivision.writer";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForStarOfficeWriterTemplate()
     */
    @Override
    protected String expectedMimeTypeForStarOfficeWriterTemplate() {
        return "application/vnd.ms-office";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForStarOfficeCalcTemplate()
     */
    @Override
    protected String expectedMimeTypeForStarOfficeCalcTemplate() {
        return "application/vnd.ms-office";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForStarOfficeDrawTemplate()
     */
    @Override
    protected String expectedMimeTypeForStarOfficeDrawTemplate() {
        return "application/vnd.ms-office";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForStarOfficeImpressTemplate()
     */
    @Override
    protected String expectedMimeTypeForStarOfficeImpressTemplate() {
        return "application/vnd.ms-office";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForWord_test_word_2000_doc()
     */
    @Override
    protected String expectedMimeTypeForWord_test_word_2000_doc() {
        return "application/vnd.ms-word";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForWord_test_word_6_0_95_doc()
     */
    @Override
    protected String expectedMimeTypeForWord_test_word_6_0_95_doc() {
        return "application/vnd.ms-word";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForWord_microsoft_word_2000_doc()
     */
    @Override
    protected String expectedMimeTypeForWord_microsoft_word_2000_doc() {
        return "application/vnd.ms-word";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForWord_microsoft_word_2000_with_wrong_file_extension_pdf()
     */
    @Override
    protected String expectedMimeTypeForWord_microsoft_word_2000_with_wrong_file_extension_pdf() {
        return "application/vnd.ms-office";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForWord_microsoft_word_2007beta2_dotx()
     */
    @Override
    protected String expectedMimeTypeForWord_microsoft_word_2007beta2_dotx() {
        return "application/vnd.openxmlformats-officedocument.wordprocessingml";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForWord_microsoft_word_2007beta2_docm()
     */
    @Override
    protected String expectedMimeTypeForWord_microsoft_word_2007beta2_docm() {
        return "application/vnd.openxmlformats-officedocument.wordprocessingml";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForWord_microsoft_word_2007beta2_docx()
     */
    @Override
    protected String expectedMimeTypeForWord_microsoft_word_2007beta2_docx() {
        return "application/vnd.openxmlformats-officedocument.wordprocessingml";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForWord_microsoft_word_2007beta2_dotm()
     */
    @Override
    protected String expectedMimeTypeForWord_microsoft_word_2007beta2_dotm() {
        return "application/vnd.openxmlformats-officedocument.wordprocessingml";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForWorks_microsoft_works_spreadsheet_4_0_2000_wks()
     */
    @Override
    protected String expectedMimeTypeForWorks_microsoft_works_spreadsheet_4_0_2000_wks() {
        return "application/vnd.ms-works";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForWorks_microsoft_works_word_processor_7_0_wps()
     */
    @Override
    protected String expectedMimeTypeForWorks_microsoft_works_word_processor_7_0_wps() {
        return "application/vnd.ms-works";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForWorks_microsoft_works_spreadsheet_7_0_xlr()
     */
    @Override
    protected String expectedMimeTypeForWorks_microsoft_works_spreadsheet_7_0_xlr() {
        return "application/vnd.ms-works";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForWorks_microsoft_works_word_processor_2000_wps()
     */
    @Override
    protected String expectedMimeTypeForWorks_microsoft_works_word_processor_2000_wps() {
        return "application/vnd.ms-works";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForWorks_microsoft_works_word_processor_4_0_wps()
     */
    @Override
    protected String expectedMimeTypeForWorks_microsoft_works_word_processor_4_0_wps() {
        return "application/vnd.ms-works";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForWorks_microsoft_works_word_processor_3_0_wps()
     */
    @Override
    protected String expectedMimeTypeForWorks_microsoft_works_word_processor_3_0_wps() {
        return "application/vnd.ms-works";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForWorkbook_corel_quattro_pro_6_wb2()
     */
    @Override
    protected String expectedMimeTypeForWorkbook_corel_quattro_pro_6_wb2() {
        return "application/wb2";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForWorkbook_microsoft_works_spreadsheet_3_0_wks()
     */
    @Override
    protected String expectedMimeTypeForWorkbook_microsoft_works_spreadsheet_3_0_wks() {
        return "application/wb2";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForExcel_test_excel_2000_xls()
     */
    @Override
    protected String expectedMimeTypeForExcel_test_excel_2000_xls() {
        return "application/vnd.ms-excel";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForExcel_microsoft_excel_2007beta2_xltx()
     */
    @Override
    protected String expectedMimeTypeForExcel_microsoft_excel_2007beta2_xltx() {
        return "application/vnd.openxmlformats-officedocument.spreadsheetml";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForExcel_microsoft_excel_2000_xls()
     */
    @Override
    protected String expectedMimeTypeForExcel_microsoft_excel_2000_xls() {
        return "application/vnd.ms-excel";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForExcel_microsoft_excel_2007beta2_xlam()
     */
    @Override
    protected String expectedMimeTypeForExcel_microsoft_excel_2007beta2_xlam() {
        return "application/vnd.openxmlformats-officedocument.spreadsheetml";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForExcel_microsoft_excel_2007beta2_xltm()
     */
    @Override
    protected String expectedMimeTypeForExcel_microsoft_excel_2007beta2_xltm() {
        return "application/vnd.openxmlformats-officedocument.spreadsheetml";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForExcel_microsoft_excel_2007beta2_xlsx()
     */
    @Override
    protected String expectedMimeTypeForExcel_microsoft_excel_2007beta2_xlsx() {
        return "application/vnd.openxmlformats-officedocument.spreadsheetml";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForExcel_microsoft_excel_2007beta2_xlsb()
     */
    @Override
    protected String expectedMimeTypeForExcel_microsoft_excel_2007beta2_xlsb() {
        return "application/vnd.openxmlformats-officedocument.spreadsheetml";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForExcel_microsoft_excel_2007beta2_xlsm()
     */
    @Override
    protected String expectedMimeTypeForExcel_microsoft_excel_2007beta2_xlsm() {
        return "application/vnd.openxmlformats-officedocument.spreadsheetml";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForPowerpoint_microsoft_powerpoint_2007beta2_potm()
     */
    @Override
    protected String expectedMimeTypeForPowerpoint_microsoft_powerpoint_2007beta2_potm() {
        return "application/vnd.openxmlformats-officedocument.presentationml";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForPowerpoint_microsoft_powerpoint_2007beta2_ppsm()
     */
    @Override
    protected String expectedMimeTypeForPowerpoint_microsoft_powerpoint_2007beta2_ppsm() {
        return "application/vnd.openxmlformats-officedocument.presentationml";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForPowerpoint_microsoft_powerpoint_2007beta2_pptm()
     */
    @Override
    protected String expectedMimeTypeForPowerpoint_microsoft_powerpoint_2007beta2_pptm() {
        return "application/vnd.openxmlformats-officedocument.presentationml";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForPowerpoint_microsoft_powerpoint_2007beta2_pptx()
     */
    @Override
    protected String expectedMimeTypeForPowerpoint_microsoft_powerpoint_2007beta2_pptx() {
        return "application/vnd.openxmlformats-officedocument.presentationml";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForPowerpoint_microsoft_powerpoint_2007beta2_potx()
     */
    @Override
    protected String expectedMimeTypeForPowerpoint_microsoft_powerpoint_2007beta2_potx() {
        return "application/vnd.openxmlformats-officedocument.presentationml";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForPowerpoint_microsoft_powerpoint_2000_ppt()
     */
    @Override
    protected String expectedMimeTypeForPowerpoint_microsoft_powerpoint_2000_ppt() {
        return "application/vnd.ms-powerpoint";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForPowerpoint_test_ppt()
     */
    @Override
    protected String expectedMimeTypeForPowerpoint_test_ppt() {
        return "application/vnd.ms-powerpoint";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForPowerpoint_microsoft_powerpoint_2007beta2_ppsx()
     */
    @Override
    protected String expectedMimeTypeForPowerpoint_microsoft_powerpoint_2007beta2_ppsx() {
        return "application/vnd.openxmlformats-officedocument.presentationml";
    }

    @Override
    public String expectedMimeTypeForPublisher() {
        return "application/x-mspublisher";
    }

    @Override
    public String expectedMimeTypeForVisio() {
        return "application/vnd.visio";
    }

    @Override
    public String expectedMimeTypeForOutlook() {
        return "application/vnd.ms-outlook";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForShw_corel_presentations_3_0_shw()
     */
    @Override
    protected String expectedMimeTypeForShw_corel_presentations_3_0_shw() {
        return "application/presentations";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForShw_corel_presentations_x3_shw()
     */
    @Override
    protected String expectedMimeTypeForShw_corel_presentations_x3_shw() {
        return "application/presentations";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForPro_corel_quattro_pro_7_wb3()
     */
    @Override
    protected String expectedMimeTypeForPro_corel_quattro_pro_7_wb3() {
        return "application/x-quattropro";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForPro_corel_quattro_pro_x3_qpw()
     */
    @Override
    protected String expectedMimeTypeForPro_corel_quattro_pro_x3_qpw() {
        return "application/x-quattropro";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForWordperfect_corel_wordperfect_x3_wpd()
     */
    @Override
    protected String expectedMimeTypeForWordperfect_corel_wordperfect_x3_wpd() {
        return "application/vnd.wordperfect";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForWordperfect_corel_wordperfect_4_2_wp()
     */
    @Override
    protected String expectedMimeTypeForWordperfect_corel_wordperfect_4_2_wp() {
        return "application/vnd.wordperfect";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForWordperfect_corel_wordperfect_5_0_wp()
     */
    @Override
    protected String expectedMimeTypeForWordperfect_corel_wordperfect_5_0_wp() {
        return "application/vnd.wordperfect";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForWordperfect_corel_wordperfect_5_1_far_east_wp()
     */
    @Override
    protected String expectedMimeTypeForWordperfect_corel_wordperfect_5_1_far_east_wp() {
        return "application/vnd.wordperfect";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForWordperfect_corel_wordperfect_5_1_wp()
     */
    @Override
    protected String expectedMimeTypeForWordperfect_corel_wordperfect_5_1_wp() {
        return "application/vnd.wordperfect";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForMail_test_excel_web_archive_mht()
     */
    @Override
    protected String expectedMimeTypeForMail_test_excel_web_archive_mht() {
        return "message/rfc822";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForMail_mail_thunderbird_1_5_eml()
     */
    @Override
    protected String expectedMimeTypeForMail_mail_thunderbird_1_5_eml() {
        return "message/rfc822";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForMail_mhtml_firefox_mht()
     */
    @Override
    protected String expectedMimeTypeForMail_mhtml_firefox_mht() {
        return "message/rfc822";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForMail_mhtml_internet_explorer_mht()
     */
    @Override
    protected String expectedMimeTypeForMail_mhtml_internet_explorer_mht() {
        return "message/rfc822";
    }

    @Override
    public String expectedMimeTypeForAddressBook() {
        return "application/x-mozilla-addressbook";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForVCard_vcard_vCards_SAP_vcf()
     */
    @Override
    protected String expectedMimeTypeForVCard_vcard_vCards_SAP_vcf() {
        return "text/x-vcard";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForVCard_vcard_antoni_kontact_vcf()
     */
    @Override
    protected String expectedMimeTypeForVCard_vcard_antoni_kontact_vcf() {
        return "text/x-vcard";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForVCard_vcard_antoni_outlook2003_vcf()
     */
    @Override
    protected String expectedMimeTypeForVCard_vcard_antoni_outlook2003_vcf() {
        return "text/x-vcard";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForVCard_vcard_dirk_vcf()
     */
    @Override
    protected String expectedMimeTypeForVCard_vcard_dirk_vcf() {
        return "text/x-vcard";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForVCard_vcard_rfc2426_vcf()
     */
    @Override
    protected String expectedMimeTypeForVCard_vcard_rfc2426_vcf() {
        return "text/x-vcard";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForCalendar_Todos1_ics()
     */
    @Override
    protected String expectedMimeTypeForCalendar_Todos1_ics() {
        return "text/calendar";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForCalendar_basicCalendar_ics()
     */
    @Override
    protected String expectedMimeTypeForCalendar_basicCalendar_ics() {
        return "text/calendar";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForCalendar_cal01_1_ics()
     */
    @Override
    protected String expectedMimeTypeForCalendar_cal01_1_ics() {
        return "text/calendar";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForCalendar_cal01_2_ics()
     */
    @Override
    protected String expectedMimeTypeForCalendar_cal01_2_ics() {
        return "text/calendar";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForCalendar_cal01_3_ics()
     */
    @Override
    protected String expectedMimeTypeForCalendar_cal01_3_ics() {
        return "text/calendar";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForCalendar_cal01_4_ics()
     */
    @Override
    protected String expectedMimeTypeForCalendar_cal01_4_ics() {
        return "text/calendar";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForCalendar_cal01_5_ics()
     */
    @Override
    protected String expectedMimeTypeForCalendar_cal01_5_ics() {
        return "text/calendar";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForCalendar_cal01_6_ics()
     */
    @Override
    protected String expectedMimeTypeForCalendar_cal01_6_ics() {
        return "text/calendar";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForCalendar_cal01_exrule_ics()
     */
    @Override
    protected String expectedMimeTypeForCalendar_cal01_exrule_ics() {
        return "text/calendar";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForCalendar_cal01_ics()
     */
    @Override
    protected String expectedMimeTypeForCalendar_cal01_ics() {
        return "text/calendar";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForCalendar_calconnect7_ics()
     */
    @Override
    protected String expectedMimeTypeForCalendar_calconnect7_ics() {
        return "text/calendar";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForCalendar_calconnect9_ics()
     */
    @Override
    protected String expectedMimeTypeForCalendar_calconnect9_ics() {
        return "text/calendar";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForCalendar_combined_multiplevcalendar_ics()
     */
    @Override
    protected String expectedMimeTypeForCalendar_combined_multiplevcalendar_ics() {
        return "text/calendar";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForCalendar_combined_onevcalendar_ics()
     */
    @Override
    protected String expectedMimeTypeForCalendar_combined_onevcalendar_ics() {
        return "text/calendar";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForCalendar_extendedCalendar_ics()
     */
    @Override
    protected String expectedMimeTypeForCalendar_extendedCalendar_ics() {
        return "text/calendar";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForCalendar_freebusy_ics()
     */
    @Override
    protected String expectedMimeTypeForCalendar_freebusy_ics() {
        return "text/calendar";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForCalendar_geol_ics()
     */
    @Override
    protected String expectedMimeTypeForCalendar_geol_ics() {
        return "text/calendar";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForCalendar_gkexample_ics()
     */
    @Override
    protected String expectedMimeTypeForCalendar_gkexample_ics() {
        return "text/calendar";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForCalendar_incoming_ics()
     */
    @Override
    protected String expectedMimeTypeForCalendar_incoming_ics() {
        return "text/calendar";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForCalendar_korganizer_jicaltest_ics()
     */
    @Override
    protected String expectedMimeTypeForCalendar_korganizer_jicaltest_ics() {
        return "text/calendar";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForCalendar_korganizer_jicaltest_vjournal_ics()
     */
    @Override
    protected String expectedMimeTypeForCalendar_korganizer_jicaltest_vjournal_ics() {
        return "text/calendar";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForCalendar_php_flp_ics()
     */
    @Override
    protected String expectedMimeTypeForCalendar_php_flp_ics() {
        return "text/calendar";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForCalendar_simplevevent_ics()
     */
    @Override
    protected String expectedMimeTypeForCalendar_simplevevent_ics() {
        return "text/calendar";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForCalendar_sunbird_sample_ics()
     */
    @Override
    protected String expectedMimeTypeForCalendar_sunbird_sample_ics() {
        return "text/calendar";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForCalendar_tag_bug_ics()
     */
    @Override
    protected String expectedMimeTypeForCalendar_tag_bug_ics() {
        return "text/calendar";
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractMimeTypeTest#expectedMimeTypeForCalendar_test_created_ics()
     */
    @Override
    protected String expectedMimeTypeForCalendar_test_created_ics() {
        return "text/calendar";
    }

    @Override
    public String expectedMimeTypeForAu() {
        return "application/octet-stream";
    }

    @Override
    public String expectedMimeTypeForBin() {
        return "application/octet-stream";
    }

    @Override
    public String expectedMimeTypeForEmf() {
        return "application/octet-stream";
    }

    @Override
    public String expectedMimeTypeForFli() {
        return "application/octet-stream";
    }

    @Override
    public String expectedMimeTypeForPcx() {
        return "application/octet-stream";
    }

    @Override
    public String expectedMimeTypeForPict() {
        return "application/octet-stream";
    }

    @Override
    public String expectedMimeTypeForPsd() {
        return "application/octet-stream";
    }

    @Override
    public String expectedMimeTypeForTar() {
        return "application/octet-stream";
    }

    @Test( expected = FileNotFoundException.class )
    public void shouldFailIfFileNotFound() throws Exception {
        testMimeType("missing.file", "");
    }
}
