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
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.modeshape.graph.mimetype;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import java.io.File;
import java.io.InputStream;
import java.util.Collections;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * All test classes that test {@link MimeTypeDetector MIME-type detector implementations} should extend this class to help ensure
 * all implementations test the same types of data sources.
 * 
 * @author John Verhaeg
 */
public abstract class AbstractMimeTypeTest {

    private final MimeTypeDetectorConfig config;
    private MimeTypeDetectors detectors;

    protected AbstractMimeTypeTest( Class<? extends MimeTypeDetector> detector ) {
        assertThat(detector, notNullValue());
        this.config = new MimeTypeDetectorConfig("MIME-Type Detector", "MIME-Type Detector",
                                                 Collections.<String, Object>emptyMap(), detector.getName(), null);
    }

    @Before
    public void before() throws Exception {
        detectors = new MimeTypeDetectors();
        detectors.addDetector(config);
    }

    @After
    public void after() {
        detectors.removeDetector(config);
    }

    protected void testMimeType( String name,
                                 String mimeType ) throws Exception {
        File file = getFile(name);
        InputStream content = file.toURI().toURL().openStream();
        assertThat(detectors.mimeTypeOf(name, content), is(mimeType));
    }

    protected abstract File getFile( String name );

    protected abstract String expectedMimeTypeForText_test_txt();

    protected abstract String expectedMimeTypeForText_plain_text_txt();

    protected abstract String expectedMimeTypeForText_plain_text_ansi_txt();

    protected abstract String expectedMimeTypeForText_plain_text_empty_txt();

    protected abstract String expectedMimeTypeForText_plain_text_utf16be_txt();

    protected abstract String expectedMimeTypeForText_plain_text_utf16le_txt();

    protected abstract String expectedMimeTypeForText_plain_text_utf8_txt();

    protected abstract String expectedMimeTypeForText_plain_text_with_null_character_txt();

    protected abstract String expectedMimeTypeForText_plain_text_without_extension();

    protected abstract String expectedMimeTypeForRtf_test_rtf();

    protected abstract String expectedMimeTypeForRtf_rtf_openoffice_1_1_5_rtf();

    protected abstract String expectedMimeTypeForRtf_rtf_openoffice_2_0_rtf();

    protected abstract String expectedMimeTypeForRtf_rtf_staroffice_5_2_rtf();

    protected abstract String expectedMimeTypeForRtf_rtf_word_2000_rtf();

    protected abstract String expectedMimeTypeForMp3_test_mp3();

    protected abstract String expectedMimeTypeForMp3_test_128_44_jstereo_mp3();

    protected abstract String expectedMimeTypeForMp3_jingle1_mp3();

    protected abstract String expectedMimeTypeForMp3_jingle2_mp3();

    protected abstract String expectedMimeTypeForMp3_jingle3_mp3();

    protected abstract String expectedMimeTypeForWav();

    protected abstract String expectedMimeTypeForBmp();

    protected abstract String expectedMimeTypeForGif();

    protected abstract String expectedMimeTypeForIcon();

    protected abstract String expectedMimeTypeForJpeg_test_jpg();

    protected abstract String expectedMimeTypeForJpeg_jpg_exif_img_9367_JPG();

    protected abstract String expectedMimeTypeForJpeg_jpg_exif_zerolength_jpg();

    protected abstract String expectedMimeTypeForJpeg_jpg_geotagged_jpg();

    protected abstract String expectedMimeTypeForJpeg_jpg_geotagged_ipanema_jpg();

    protected abstract String expectedMimeTypeForPortablePixelMap_test_ppm();

    protected abstract String expectedMimeTypeForPortablePixelMap_test_pnm();

    protected abstract String expectedMimeTypeForPng();

    protected abstract String expectedMimeTypeForTiff();

    protected abstract String expectedMimeTypeForTga();

    protected abstract String expectedMimeTypeForWmf();

    protected abstract String expectedMimeTypeForXcf();

    protected abstract String expectedMimeTypeForXpm();

    protected abstract String expectedMimeTypeForXml_test_xml();

    protected abstract String expectedMimeTypeForXml_test_excel_spreadsheet_xml();

    protected abstract String expectedMimeTypeForXml_CurrencyFormatterExample_mxml();

    protected abstract String expectedMimeTypeForXml_xml_handwritten_xml();

    protected abstract String expectedMimeTypeForXml_xml_nonexistent_dtd_xml();

    protected abstract String expectedMimeTypeForXml_xml_nonexistent_remote_dtd_xml();

    protected abstract String expectedMimeTypeForXml_xml_nonexistent_remote_xsd_xml();

    protected abstract String expectedMimeTypeForXml_xml_nonexistent_xsd_xml();

    protected abstract String expectedMimeTypeForXml_xml_utf8_bom();

    protected abstract String expectedMimeTypeForXsd();

    protected abstract String expectedMimeTypeForDtd();

    protected abstract String expectedMimeTypeForHtml_master_xml();

    protected abstract String expectedMimeTypeForHtml_html_condenast_html();

    protected abstract String expectedMimeTypeForHtml_html_handwritten_html();

    protected abstract String expectedMimeTypeForHtml_html_handwritten_with_wrong_file_extension_txt();

    protected abstract String expectedMimeTypeForHtml_html_quelle_de_html();

    protected abstract String expectedMimeTypeForHtml_html_utf16_leading_whitespace_wrong_extension_doc();

    protected abstract String expectedMimeTypeForJava();

    protected abstract String expectedMimeTypeFor1_2Class();

    protected abstract String expectedMimeTypeFor1_3Class();

    protected abstract String expectedMimeTypeFor1_4Class();

    protected abstract String expectedMimeTypeForPerl();

    protected abstract String expectedMimeTypeForPython();

    protected abstract String expectedMimeTypeForPdf_test_pdf();

    protected abstract String expectedMimeTypeForPdf_pdf_distiller_6_weirdchars_pdf();

    protected abstract String expectedMimeTypeForPdf_pdf_no_author_pdf();

    protected abstract String expectedMimeTypeForPdf_pdf_openoffice_1_1_5_writer_pdf();

    protected abstract String expectedMimeTypeForPdf_pdf_openoffice_2_0_writer_pdf();

    protected abstract String expectedMimeTypeForPdf_pdf_word_2000_pdfcreator_0_8_0_pdf();

    protected abstract String expectedMimeTypeForPdf_pdf_word_2000_pdfmaker_7_0_pdf();

    protected abstract String expectedMimeTypeForPdf_pdf_word_2000_pdfwriter_7_0_pdf();

    protected abstract String expectedMimeTypeForPostscript_test_ps();

    protected abstract String expectedMimeTypeForPostscript_test_eps();

    protected abstract String expectedMimeTypeForJar();

    protected abstract String expectedMimeTypeForJavaManifest();

    protected abstract String expectedMimeTypeForGZip_test_tar_gz();

    protected abstract String expectedMimeTypeForGZip_test_txt_gz();

    protected abstract String expectedMimeTypeForZip();

    protected abstract String expectedMimeTypeForBash();

    protected abstract String expectedMimeTypeForOgg();

    protected abstract String expectedMimeTypeForOpenDocumentFormula();

    protected abstract String expectedMimeTypeForOpenDocumentGraphics();

    protected abstract String expectedMimeTypeForOpenDocumentGraphicsTemplate();

    protected abstract String expectedMimeTypeForOpenDocumentPresentation_component_architecture_odp();

    protected abstract String expectedMimeTypeForOpenDocumentPresentation_openoffice_2_0_impress_odp();

    protected abstract String expectedMimeTypeForOpenDocumentPresentationTemplate();

    protected abstract String expectedMimeTypeForOpenDocumentSpreadsheet();

    protected abstract String expectedMimeTypeForOpenDocumentSpreadsheetTemplate();

    protected abstract String expectedMimeTypeForOpenDocumentText();

    protected abstract String expectedMimeTypeForOpenDocumentTextTemplate();

    protected abstract String expectedMimeTypeForOpenOfficeCalc();

    protected abstract String expectedMimeTypeForOpenOfficeCalcTemplate();

    protected abstract String expectedMimeTypeForOpenOfficeDraw();

    protected abstract String expectedMimeTypeForOpenOfficeDrawTemplate();

    protected abstract String expectedMimeTypeForOpenOfficeImpress();

    protected abstract String expectedMimeTypeForOpenOfficeImpressTemplate();

    protected abstract String expectedMimeTypeForOpenOfficeWriter();

    protected abstract String expectedMimeTypeForOpenOfficeWriterTemplate();

    protected abstract String expectedMimeTypeForStarOfficeCalc();

    protected abstract String expectedMimeTypeForStarOfficeDraw();

    protected abstract String expectedMimeTypeForStarOfficeImpress();

    protected abstract String expectedMimeTypeForStarOfficeWriter();

    protected abstract String expectedMimeTypeForStarOfficeCalcTemplate();

    protected abstract String expectedMimeTypeForStarOfficeDrawTemplate();

    protected abstract String expectedMimeTypeForStarOfficeImpressTemplate();

    protected abstract String expectedMimeTypeForStarOfficeWriterTemplate();

    protected abstract String expectedMimeTypeForWord_test_word_2000_doc();

    protected abstract String expectedMimeTypeForWord_test_word_6_0_95_doc();

    protected abstract String expectedMimeTypeForWord_microsoft_word_2000_doc();

    protected abstract String expectedMimeTypeForWord_microsoft_word_2000_with_wrong_file_extension_pdf();

    protected abstract String expectedMimeTypeForWord_microsoft_word_2007beta2_docm();

    protected abstract String expectedMimeTypeForWord_microsoft_word_2007beta2_docx();

    protected abstract String expectedMimeTypeForWord_microsoft_word_2007beta2_dotm();

    protected abstract String expectedMimeTypeForWord_microsoft_word_2007beta2_dotx();

    protected abstract String expectedMimeTypeForWorks_microsoft_works_spreadsheet_4_0_2000_wks();

    protected abstract String expectedMimeTypeForWorks_microsoft_works_spreadsheet_7_0_xlr();

    protected abstract String expectedMimeTypeForWorks_microsoft_works_word_processor_2000_wps();

    protected abstract String expectedMimeTypeForWorks_microsoft_works_word_processor_3_0_wps();

    protected abstract String expectedMimeTypeForWorks_microsoft_works_word_processor_4_0_wps();

    protected abstract String expectedMimeTypeForWorks_microsoft_works_word_processor_7_0_wps();

    protected abstract String expectedMimeTypeForWorkbook_corel_quattro_pro_6_wb2();

    protected abstract String expectedMimeTypeForWorkbook_microsoft_works_spreadsheet_3_0_wks();

    protected abstract String expectedMimeTypeForExcel_test_excel_2000_xls();

    protected abstract String expectedMimeTypeForExcel_microsoft_excel_2000_xls();

    protected abstract String expectedMimeTypeForExcel_microsoft_excel_2007beta2_xlam();

    protected abstract String expectedMimeTypeForExcel_microsoft_excel_2007beta2_xlsb();

    protected abstract String expectedMimeTypeForExcel_microsoft_excel_2007beta2_xlsm();

    protected abstract String expectedMimeTypeForExcel_microsoft_excel_2007beta2_xlsx();

    protected abstract String expectedMimeTypeForExcel_microsoft_excel_2007beta2_xltm();

    protected abstract String expectedMimeTypeForExcel_microsoft_excel_2007beta2_xltx();

    protected abstract String expectedMimeTypeForPowerpoint_test_ppt();

    protected abstract String expectedMimeTypeForPowerpoint_microsoft_powerpoint_2000_ppt();

    protected abstract String expectedMimeTypeForPowerpoint_microsoft_powerpoint_2007beta2_potm();

    protected abstract String expectedMimeTypeForPowerpoint_microsoft_powerpoint_2007beta2_potx();

    protected abstract String expectedMimeTypeForPowerpoint_microsoft_powerpoint_2007beta2_ppsm();

    protected abstract String expectedMimeTypeForPowerpoint_microsoft_powerpoint_2007beta2_ppsx();

    protected abstract String expectedMimeTypeForPowerpoint_microsoft_powerpoint_2007beta2_pptm();

    protected abstract String expectedMimeTypeForPowerpoint_microsoft_powerpoint_2007beta2_pptx();

    protected abstract String expectedMimeTypeForPublisher();

    protected abstract String expectedMimeTypeForVisio();

    protected abstract String expectedMimeTypeForOutlook();

    protected abstract String expectedMimeTypeForShw_corel_presentations_3_0_shw();

    protected abstract String expectedMimeTypeForShw_corel_presentations_x3_shw();

    protected abstract String expectedMimeTypeForPro_corel_quattro_pro_7_wb3();

    protected abstract String expectedMimeTypeForPro_corel_quattro_pro_x3_qpw();

    protected abstract String expectedMimeTypeForWordperfect_corel_wordperfect_4_2_wp();

    protected abstract String expectedMimeTypeForWordperfect_corel_wordperfect_5_0_wp();

    protected abstract String expectedMimeTypeForWordperfect_corel_wordperfect_5_1_wp();

    protected abstract String expectedMimeTypeForWordperfect_corel_wordperfect_5_1_far_east_wp();

    protected abstract String expectedMimeTypeForWordperfect_corel_wordperfect_x3_wpd();

    protected abstract String expectedMimeTypeForMail_test_excel_web_archive_mht();

    protected abstract String expectedMimeTypeForMail_mail_thunderbird_1_5_eml();

    protected abstract String expectedMimeTypeForMail_mhtml_firefox_mht();

    protected abstract String expectedMimeTypeForMail_mhtml_internet_explorer_mht();

    protected abstract String expectedMimeTypeForAddressBook();

    protected abstract String expectedMimeTypeForVCard_vcard_antoni_kontact_vcf();

    protected abstract String expectedMimeTypeForVCard_vcard_antoni_outlook2003_vcf();

    protected abstract String expectedMimeTypeForVCard_vcard_dirk_vcf();

    protected abstract String expectedMimeTypeForVCard_vcard_rfc2426_vcf();

    protected abstract String expectedMimeTypeForVCard_vcard_vCards_SAP_vcf();

    protected abstract String expectedMimeTypeForCalendar_basicCalendar_ics();

    protected abstract String expectedMimeTypeForCalendar_cal01_ics();

    protected abstract String expectedMimeTypeForCalendar_cal01_1_ics();

    protected abstract String expectedMimeTypeForCalendar_cal01_2_ics();

    protected abstract String expectedMimeTypeForCalendar_cal01_3_ics();

    protected abstract String expectedMimeTypeForCalendar_cal01_4_ics();

    protected abstract String expectedMimeTypeForCalendar_cal01_5_ics();

    protected abstract String expectedMimeTypeForCalendar_cal01_6_ics();

    protected abstract String expectedMimeTypeForCalendar_cal01_exrule_ics();

    protected abstract String expectedMimeTypeForCalendar_calconnect7_ics();

    protected abstract String expectedMimeTypeForCalendar_calconnect9_ics();

    protected abstract String expectedMimeTypeForCalendar_combined_multiplevcalendar_ics();

    protected abstract String expectedMimeTypeForCalendar_combined_onevcalendar_ics();

    protected abstract String expectedMimeTypeForCalendar_extendedCalendar_ics();

    protected abstract String expectedMimeTypeForCalendar_freebusy_ics();

    protected abstract String expectedMimeTypeForCalendar_geol_ics();

    protected abstract String expectedMimeTypeForCalendar_gkexample_ics();

    protected abstract String expectedMimeTypeForCalendar_incoming_ics();

    protected abstract String expectedMimeTypeForCalendar_korganizer_jicaltest_vjournal_ics();

    protected abstract String expectedMimeTypeForCalendar_korganizer_jicaltest_ics();

    protected abstract String expectedMimeTypeForCalendar_php_flp_ics();

    protected abstract String expectedMimeTypeForCalendar_simplevevent_ics();

    protected abstract String expectedMimeTypeForCalendar_sunbird_sample_ics();

    protected abstract String expectedMimeTypeForCalendar_tag_bug_ics();

    protected abstract String expectedMimeTypeForCalendar_test_created_ics();

    protected abstract String expectedMimeTypeForCalendar_Todos1_ics();

    protected abstract String expectedMimeTypeForAu();

    protected abstract String expectedMimeTypeForBin();

    protected abstract String expectedMimeTypeForEmf();

    protected abstract String expectedMimeTypeForFli();

    protected abstract String expectedMimeTypeForPcx();

    protected abstract String expectedMimeTypeForPict();

    protected abstract String expectedMimeTypeForPsd();

    protected abstract String expectedMimeTypeForTar();

    @Test
    public void shouldProvideMimeTypeForText_test_txt() throws Exception {
        testMimeType("test.txt", expectedMimeTypeForText_test_txt());
    }

    @Test
    public void shouldProvideMimeTypeForText_plain_text_txt() throws Exception {
        testMimeType("docs/plain-text.txt", expectedMimeTypeForText_plain_text_txt());
    }

    @Test
    public void shouldProvideMimeTypeForText_plain_text_ansi_txt() throws Exception {
        testMimeType("docs/plain-text-ansi.txt", expectedMimeTypeForText_plain_text_ansi_txt());
    }

    @Test
    public void shouldProvideMimeTypeForText_plain_text_empty_txt() throws Exception {
        testMimeType("docs/plain-text-empty.txt", expectedMimeTypeForText_plain_text_empty_txt());
    }

    @Test
    public void shouldProvideMimeTypeForText_plain_text_utf16be_txt() throws Exception {
        testMimeType("docs/plain-text-utf16be.txt", expectedMimeTypeForText_plain_text_utf16be_txt());
    }

    @Test
    public void shouldProvideMimeTypeForText_plain_text_utf16le_txt() throws Exception {
        testMimeType("docs/plain-text-utf16le.txt", expectedMimeTypeForText_plain_text_utf16le_txt());
    }

    @Test
    public void shouldProvideMimeTypeForText_plain_text_utf8_txt() throws Exception {
        testMimeType("docs/plain-text-utf8.txt", expectedMimeTypeForText_plain_text_utf8_txt());
    }

    @Test
    public void shouldProvideMimeTypeForText_plain_text_with_null_character_txt() throws Exception {
        testMimeType("docs/plain-text-with-null-character.txt", expectedMimeTypeForText_plain_text_with_null_character_txt());
    }

    @Test
    public void shouldProvideMimeTypeForText_plain_text_without_extension() throws Exception {
        testMimeType("docs/plain-text-without-extension", expectedMimeTypeForText_plain_text_without_extension());
    }

    @Test
    public void shouldProvideMimeTypeForRtf_test_rtf() throws Exception {
        testMimeType("test.rtf", expectedMimeTypeForRtf_test_rtf());
    }

    @Test
    public void shouldProvideMimeTypeForRtf_rtf_openoffice_1_1_5_rtf() throws Exception {
        testMimeType("docs/rtf-openoffice-1.1.5.rtf", expectedMimeTypeForRtf_rtf_openoffice_1_1_5_rtf());
    }

    @Test
    public void shouldProvideMimeTypeForRtf_rtf_openoffice_2_0_rtf() throws Exception {
        testMimeType("docs/rtf-openoffice-2.0.rtf", expectedMimeTypeForRtf_rtf_openoffice_2_0_rtf());
    }

    @Test
    public void shouldProvideMimeTypeForRtf_rtf_staroffice_5_2_rtf() throws Exception {
        testMimeType("docs/rtf-staroffice-5.2.rtf", expectedMimeTypeForRtf_rtf_staroffice_5_2_rtf());
    }

    @Test
    public void shouldProvideMimeTypeForRtf_rtf_word_2000_rtf() throws Exception {
        testMimeType("docs/rtf-word-2000.rtf", expectedMimeTypeForRtf_rtf_word_2000_rtf());
    }

    @Test
    public void shouldProvideMimeTypeForMp3_test_mp3() throws Exception {
        testMimeType("test.mp3", expectedMimeTypeForMp3_test_mp3());
    }

    @Test
    public void shouldProvideMimeTypeForMp3_test_128_44_jstereo_mp3() throws Exception {
        testMimeType("test_128_44_jstereo.mp3", expectedMimeTypeForMp3_test_128_44_jstereo_mp3());
    }

    @Test
    public void shouldProvideMimeTypeForMp3_jingle1_mp3() throws Exception {
        testMimeType("docs/jingle1.mp3", expectedMimeTypeForMp3_jingle1_mp3());
    }

    @Test
    public void shouldProvideMimeTypeForMp3_jingle2_mp3() throws Exception {
        testMimeType("docs/jingle2.mp3", expectedMimeTypeForMp3_jingle2_mp3());
    }

    @Test
    public void shouldProvideMimeTypeForMp3_jingle3_mp3() throws Exception {
        testMimeType("docs/jingle3.mp3", expectedMimeTypeForMp3_jingle3_mp3());
    }

    @Test
    public void shouldProvideMimeTypeForWav() throws Exception {
        testMimeType("test.wav", expectedMimeTypeForWav());
    }

    @Test
    public void shouldProvideMimeTypeForBmp() throws Exception {
        testMimeType("test.bmp", expectedMimeTypeForBmp());
    }

    @Test
    public void shouldProvideMimeTypeForGif() throws Exception {
        testMimeType("test.gif", expectedMimeTypeForGif());
    }

    @Test
    public void shouldProvideMimeTypeForIcon() throws Exception {
        testMimeType("test.ico", expectedMimeTypeForIcon());
    }

    @Test
    public void shouldProvideMimeTypeForJpeg_test_jpg() throws Exception {
        testMimeType("test.jpg", expectedMimeTypeForJpeg_test_jpg());
    }

    @Test
    public void shouldProvideMimeTypeForJpeg_jpg_exif_img_9367_JPG() throws Exception {
        testMimeType("docs/jpg-exif-img_9367.JPG", expectedMimeTypeForJpeg_jpg_exif_img_9367_JPG());
    }

    @Test
    public void shouldProvideMimeTypeForJpeg_jpg_exif_zerolength_jpg() throws Exception {
        testMimeType("docs/jpg-exif-zerolength.jpg", expectedMimeTypeForJpeg_jpg_exif_zerolength_jpg());
    }

    @Test
    public void shouldProvideMimeTypeForJpeg_jpg_geotagged_jpg() throws Exception {
        testMimeType("docs/jpg-geotagged.jpg", expectedMimeTypeForJpeg_jpg_geotagged_jpg());
    }

    @Test
    public void shouldProvideMimeTypeForJpeg_jpg_geotagged_ipanema_jpg() throws Exception {
        testMimeType("docs/jpg-geotagged-ipanema.jpg", expectedMimeTypeForJpeg_jpg_geotagged_ipanema_jpg());
    }

    @Test
    public void shouldProvideMimeTypeForPortablePixelMap_test_ppm() throws Exception {
        testMimeType("test.ppm", expectedMimeTypeForPortablePixelMap_test_ppm());
    }

    @Test
    public void shouldProvideMimeTypeForPortablePixelMap_test_pnm() throws Exception {
        testMimeType("test.pnm", expectedMimeTypeForPortablePixelMap_test_pnm());
    }

    @Test
    public void shouldProvideMimeTypeForPng() throws Exception {
        testMimeType("test.png", expectedMimeTypeForPng());
    }

    @Test
    public void shouldProvideMimeTypeForTiff() throws Exception {
        testMimeType("test_nocompress.tif", expectedMimeTypeForTiff());
    }

    @Test
    public void shouldProvideMimeTypeForTga() throws Exception {
        testMimeType("test.tga", expectedMimeTypeForTga());
    }

    @Test
    public void shouldProvideMimeTypeForWmf() throws Exception {
        testMimeType("test.wmf", expectedMimeTypeForWmf());
    }

    @Test
    public void shouldProvideMimeTypeForXcf() throws Exception {
        testMimeType("test.xcf", expectedMimeTypeForXcf());
    }

    @Test
    public void shouldProvideMimeTypeForXpm() throws Exception {
        testMimeType("test.xpm", expectedMimeTypeForXpm());
    }

    @Test
    public void shouldProvideMimeTypeForXml_test_xml() throws Exception {
        testMimeType("test.xml", expectedMimeTypeForXml_test_xml());
    }

    @Test
    public void shouldProvideMimeTypeForXml_test_excel_spreadsheet_xml() throws Exception {
        testMimeType("test_excel_spreadsheet.xml", expectedMimeTypeForXml_test_excel_spreadsheet_xml());
    }

    @Test
    public void shouldProvideMimeTypeForXml_CurrencyFormatterExample_mxml() throws Exception {
        testMimeType("CurrencyFormatterExample.mxml", expectedMimeTypeForXml_CurrencyFormatterExample_mxml());
    }

    @Test
    public void shouldProvideMimeTypeForXml_xml_handwritten_xml() throws Exception {
        testMimeType("docs/xml-handwritten.xml", expectedMimeTypeForXml_xml_handwritten_xml());
    }

    @Test
    public void shouldProvideMimeTypeForXml_xml_nonexistent_dtd_xml() throws Exception {
        testMimeType("docs/xml-nonexistent-dtd.xml", expectedMimeTypeForXml_xml_nonexistent_dtd_xml());
    }

    @Test
    public void shouldProvideMimeTypeForXml_xml_nonexistent_remote_dtd_xml() throws Exception {
        testMimeType("docs/xml-nonexistent-remote-dtd.xml", expectedMimeTypeForXml_xml_nonexistent_remote_dtd_xml());
    }

    @Test
    public void shouldProvideMimeTypeForXml_xml_nonexistent_remote_xsd_xml() throws Exception {
        testMimeType("docs/xml-nonexistent-remote-xsd.xml", expectedMimeTypeForXml_xml_nonexistent_remote_xsd_xml());
    }

    @Test
    public void shouldProvideMimeTypeForXml_xml_nonexistent_xsd_xml() throws Exception {
        testMimeType("docs/xml-nonexistent-xsd.xml", expectedMimeTypeForXml_xml_nonexistent_xsd_xml());
    }

    @Test
    public void shouldProvideMimeTypeForXml_xml_utf8_bom() throws Exception {
        testMimeType("docs/xml-utf8-bom", expectedMimeTypeForXml_xml_utf8_bom());
    }

    @Test
    public void shouldProvideMimeTypeForXsd() throws Exception {
        testMimeType("Descriptor.1.0.xsd", expectedMimeTypeForXsd());
    }

    @Test
    public void shouldProvideMimeTypeForDtd() throws Exception {
        testMimeType("test.dtd", expectedMimeTypeForDtd());
    }

    @Test
    public void shouldProvideMimeTypeForHtml_master_xml() throws Exception {
        testMimeType("master.xml", expectedMimeTypeForHtml_master_xml());
    }

    @Test
    public void shouldProvideMimeTypeForHtml_html_condenast_html() throws Exception {
        testMimeType("docs/html-condenast.html", expectedMimeTypeForHtml_html_condenast_html());
    }

    @Test
    public void shouldProvideMimeTypeForHtml_html_handwritten_html() throws Exception {
        testMimeType("docs/html-handwritten.html", expectedMimeTypeForHtml_html_handwritten_html());
    }

    @Test
    public void shouldProvideMimeTypeForHtml_html_handwritten_with_wrong_file_extension_txt() throws Exception {
        testMimeType("docs/html-handwritten-with-wrong-file-extension.txt",
                     expectedMimeTypeForHtml_html_handwritten_with_wrong_file_extension_txt());
    }

    @Test
    public void shouldProvideMimeTypeForHtml_html_quelle_de_html() throws Exception {
        testMimeType("docs/html-quelle.de.html", expectedMimeTypeForHtml_html_quelle_de_html());
    }

    @Test
    public void shouldProvideMimeTypeForHtml_html_utf16_leading_whitespace_wrong_extension_doc() throws Exception {
        testMimeType("docs/html-utf16-leading-whitespace-wrong-extension.doc",
                     expectedMimeTypeForHtml_html_utf16_leading_whitespace_wrong_extension_doc());
    }

    @Test
    public void shouldProvideMimeTypeForJava() throws Exception {
        testMimeType("test.java", expectedMimeTypeForJava());
    }

    @Test
    public void shouldProvideMimeTypeFor1_2Class() throws Exception {
        testMimeType("test_1.2.class", expectedMimeTypeFor1_2Class());
    }

    @Test
    public void shouldProvideMimeTypeFor1_3Class() throws Exception {
        testMimeType("test_1.3.class", expectedMimeTypeFor1_3Class());
    }

    @Test
    public void shouldProvideMimeTypeFor1_4Class() throws Exception {
        testMimeType("test_1.4.class", expectedMimeTypeFor1_4Class());
    }

    @Test
    public void shouldProvideMimeTypeForPerl() throws Exception {
        testMimeType("test.pl", expectedMimeTypeForPerl());
    }

    @Test
    public void shouldProvideMimeTypeForPython() throws Exception {
        testMimeType("test.py", expectedMimeTypeForPython());
    }

    @Test
    public void shouldProvideMimeTypeForPdf_test_pdf() throws Exception {
        testMimeType("test.pdf", expectedMimeTypeForPdf_test_pdf());
    }

    @Test
    public void shouldProvideMimeTypeForPdf_pdf_distiller_6_weirdchars_pdf() throws Exception {
        testMimeType("docs/pdf-distiller-6-weirdchars.pdf", expectedMimeTypeForPdf_pdf_distiller_6_weirdchars_pdf());
    }

    @Test
    public void shouldProvideMimeTypeForPdf_pdf_no_author_pdf() throws Exception {
        testMimeType("docs/pdf-no-author.pdf", expectedMimeTypeForPdf_pdf_no_author_pdf());
    }

    @Test
    public void shouldProvideMimeTypeForPdf_pdf_openoffice_1_1_5_writer_pdf() throws Exception {
        testMimeType("docs/pdf-openoffice-1.1.5-writer.pdf", expectedMimeTypeForPdf_pdf_openoffice_1_1_5_writer_pdf());
    }

    @Test
    public void shouldProvideMimeTypeForPdf_pdf_openoffice_2_0_writer_pdf() throws Exception {
        testMimeType("docs/pdf-openoffice-2.0-writer.pdf", expectedMimeTypeForPdf_pdf_openoffice_2_0_writer_pdf());
    }

    @Test
    public void shouldProvideMimeTypeForPdf_pdf_word_2000_pdfcreator_0_8_0_pdf() throws Exception {
        testMimeType("docs/pdf-word-2000-pdfcreator-0.8.0.pdf", expectedMimeTypeForPdf_pdf_word_2000_pdfcreator_0_8_0_pdf());
    }

    @Test
    public void shouldProvideMimeTypeForPdf_pdf_word_2000_pdfmaker_7_0_pdf() throws Exception {
        testMimeType("docs/pdf-word-2000-pdfmaker-7.0.pdf", expectedMimeTypeForPdf_pdf_word_2000_pdfmaker_7_0_pdf());
    }

    @Test
    public void shouldProvideMimeTypeForPdf_pdf_word_2000_pdfwriter_7_0_pdf() throws Exception {
        testMimeType("docs/pdf-word-2000-pdfwriter-7.0.pdf", expectedMimeTypeForPdf_pdf_word_2000_pdfwriter_7_0_pdf());
    }

    @Test
    public void shouldProvideMimeTypeForPostscript_test_ps() throws Exception {
        testMimeType("test.ps", expectedMimeTypeForPostscript_test_ps());
    }

    @Test
    public void shouldProvideMimeTypeForPostscript_test_eps() throws Exception {
        testMimeType("test.eps", expectedMimeTypeForPostscript_test_eps());
    }

    @Test
    public void shouldProvideMimeTypeForJar() throws Exception {
        testMimeType("dna-repository-0.2-SNAPSHOT.jar", expectedMimeTypeForJar());
    }

    @Test
    public void shouldProvideMimeTypeForJavaManifest() throws Exception {
        testMimeType("aperture.example.manifest.mf", expectedMimeTypeForJavaManifest());
    }

    @Test
    public void shouldProvideMimeTypeForGZip_test_tar_gz() throws Exception {
        testMimeType("test.tar.gz", expectedMimeTypeForGZip_test_tar_gz());
    }

    @Test
    public void shouldProvideMimeTypeForGZip_test_txt_gz() throws Exception {
        testMimeType("test.txt.gz", expectedMimeTypeForGZip_test_txt_gz());
    }

    @Test
    public void shouldProvideMimeTypeForZip() throws Exception {
        testMimeType("docs/counting-input-stream-test-file.dat", expectedMimeTypeForZip());
    }

    @Test
    public void shouldProvideMimeTypeForBash() throws Exception {
        testMimeType("test.sh", expectedMimeTypeForBash());
    }

    @Test
    public void shouldProvideMimeTypeForOgg() throws Exception {
        testMimeType("test.ogg", expectedMimeTypeForOgg());
    }

    @Test
    public void shouldProvideMimeTypeForOpenDocumentFormula() throws Exception {
        testMimeType("docs/openoffice-2.0-formula.odf", expectedMimeTypeForOpenDocumentFormula());
    }

    @Test
    public void shouldProvideMimeTypeForOpenDocumentGraphics() throws Exception {
        testMimeType("docs/openoffice-2.0-draw.odg", expectedMimeTypeForOpenDocumentGraphics());
    }

    @Test
    public void shouldProvideMimeTypeForOpenDocumentGraphicsTemplate() throws Exception {
        testMimeType("docs/openoffice-2.0-draw-template.otg", expectedMimeTypeForOpenDocumentGraphicsTemplate());
    }

    @Test
    public void shouldProvideMimeTypeForOpenDocumentPresentation_component_architecture_odp() throws Exception {
        testMimeType("component-architecture.odp", expectedMimeTypeForOpenDocumentPresentation_component_architecture_odp());
    }

    @Test
    public void shouldProvideMimeTypeForOpenDocumentPresentation_openoffice_2_0_impress_odp() throws Exception {
        testMimeType("docs/openoffice-2.0-impress.odp", expectedMimeTypeForOpenDocumentPresentation_openoffice_2_0_impress_odp());
    }

    @Test
    public void shouldProvideMimeTypeForOpenDocumentPresentationTemplate() throws Exception {
        testMimeType("docs/openoffice-2.0-impress-template.otp", expectedMimeTypeForOpenDocumentPresentationTemplate());
    }

    @Test
    public void shouldProvideMimeTypeForOpenDocumentSpreadsheet() throws Exception {
        testMimeType("docs/openoffice-2.0-calc.ods", expectedMimeTypeForOpenDocumentSpreadsheet());
    }

    @Test
    public void shouldProvideMimeTypeForOpenDocumentSpreadsheetTemplate() throws Exception {
        testMimeType("docs/openoffice-2.0-calc-template.ots", expectedMimeTypeForOpenDocumentSpreadsheetTemplate());
    }

    @Test
    public void shouldProvideMimeTypeForOpenDocumentText() throws Exception {
        testMimeType("docs/openoffice-2.0-writer.odt", expectedMimeTypeForOpenDocumentText());
    }

    @Test
    public void shouldProvideMimeTypeForOpenDocumentTextTemplate() throws Exception {
        testMimeType("docs/openoffice-2.0-writer-template.ott", expectedMimeTypeForOpenDocumentTextTemplate());
    }

    @Test
    public void shouldProvideMimeTypeForOpenOfficeCalc() throws Exception {
        testMimeType("docs/openoffice-1.1.5-calc.sxc", expectedMimeTypeForOpenOfficeCalc());
    }

    @Test
    public void shouldProvideMimeTypeForOpenOfficeCalcTemplate() throws Exception {
        testMimeType("docs/openoffice-1.1.5-calc-template.stc", expectedMimeTypeForOpenOfficeCalcTemplate());
    }

    @Test
    public void shouldProvideMimeTypeForOpenOfficeDraw() throws Exception {
        testMimeType("docs/openoffice-1.1.5-draw.sxd", expectedMimeTypeForOpenOfficeDraw());
    }

    @Test
    public void shouldProvideMimeTypeForOpenOfficeDrawTemplate() throws Exception {
        testMimeType("docs/openoffice-1.1.5-draw-template.std", expectedMimeTypeForOpenOfficeDrawTemplate());
    }

    @Test
    public void shouldProvideMimeTypeForOpenOfficeImpress() throws Exception {
        testMimeType("docs/openoffice-1.1.5-impress.sxi", expectedMimeTypeForOpenOfficeImpress());
    }

    @Test
    public void shouldProvideMimeTypeForOpenOfficeImpressTemplate() throws Exception {
        testMimeType("docs/openoffice-1.1.5-impress-template.sti", expectedMimeTypeForOpenOfficeImpressTemplate());
    }

    @Test
    public void shouldProvideMimeTypeForOpenOfficeWriter() throws Exception {
        testMimeType("docs/openoffice-1.1.5-writer.sxw", expectedMimeTypeForOpenOfficeWriter());
    }

    @Test
    public void shouldProvideMimeTypeForOpenOfficeWriterTemplate() throws Exception {
        testMimeType("docs/openoffice-1.1.5-writer-template.stw", expectedMimeTypeForOpenOfficeWriterTemplate());
    }

    @Test
    public void shouldProvideMimeTypeForStarOfficeCalc() throws Exception {
        testMimeType("docs/staroffice-5.2-calc.sdc", expectedMimeTypeForStarOfficeCalc());
    }

    @Test
    public void shouldProvideMimeTypeForStarOfficeDraw() throws Exception {
        testMimeType("docs/staroffice-5.2-draw.sda", expectedMimeTypeForStarOfficeDraw());
    }

    @Test
    public void shouldProvideMimeTypeForStarOfficeImpress() throws Exception {
        testMimeType("docs/staroffice-5.2-impress.sdd", expectedMimeTypeForStarOfficeImpress());
    }

    @Test
    public void shouldProvideMimeTypeForStarOfficeWriter() throws Exception {
        testMimeType("docs/staroffice-5.2-writer.sdw", expectedMimeTypeForStarOfficeWriter());
    }

    @Test
    public void shouldProvideMimeTypeForStarOfficeCalcTemplate() throws Exception {
        testMimeType("docs/staroffice-5.2-calc-template.vor", expectedMimeTypeForStarOfficeCalcTemplate());
    }

    @Test
    public void shouldProvideMimeTypeForStarOfficeDrawTemplate() throws Exception {
        testMimeType("docs/staroffice-5.2-draw-template.vor", expectedMimeTypeForStarOfficeDrawTemplate());
    }

    @Test
    public void shouldProvideMimeTypeForStarOfficeImpressTemplate() throws Exception {
        testMimeType("docs/staroffice-5.2-impress-template.vor", expectedMimeTypeForStarOfficeImpressTemplate());
    }

    @Test
    public void shouldProvideMimeTypeForStarOfficeWriterTemplate() throws Exception {
        testMimeType("docs/staroffice-5.2-writer-template.vor", expectedMimeTypeForStarOfficeWriterTemplate());
    }

    @Test
    public void shouldProvideMimeTypeForWord_test_word_2000_doc() throws Exception {
        testMimeType("test_word_2000.doc", expectedMimeTypeForWord_test_word_2000_doc());
    }

    @Test
    public void shouldProvideMimeTypeForWord_test_word_6_0_95_doc() throws Exception {
        testMimeType("test_word_6.0_95.doc", expectedMimeTypeForWord_test_word_6_0_95_doc());
    }

    @Test
    public void shouldProvideMimeTypeForWord_microsoft_word_2000_doc() throws Exception {
        testMimeType("docs/microsoft-word-2000.doc", expectedMimeTypeForWord_microsoft_word_2000_doc());
    }

    @Test
    public void shouldProvideMimeTypeForWord_microsoft_word_2000_with_wrong_file_extension_pdf() throws Exception {
        testMimeType("docs/microsoft-word-2000-with-wrong-file-extension.pdf",
                     expectedMimeTypeForWord_microsoft_word_2000_with_wrong_file_extension_pdf());
    }

    @Test
    public void shouldProvideMimeTypeForWord_microsoft_word_2007beta2_docm() throws Exception {
        testMimeType("docs/microsoft-word-2007beta2.docm", expectedMimeTypeForWord_microsoft_word_2007beta2_docm());
    }

    @Test
    public void shouldProvideMimeTypeForWord_microsoft_word_2007beta2_docx() throws Exception {
        testMimeType("docs/microsoft-word-2007beta2.docx", expectedMimeTypeForWord_microsoft_word_2007beta2_docx());
    }

    @Test
    public void shouldProvideMimeTypeForWord_microsoft_word_2007beta2_dotm() throws Exception {
        testMimeType("docs/microsoft-word-2007beta2.dotm", expectedMimeTypeForWord_microsoft_word_2007beta2_dotm());
    }

    @Test
    public void shouldProvideMimeTypeForWord_microsoft_word_2007beta2_dotx() throws Exception {
        testMimeType("docs/microsoft-word-2007beta2.dotx", expectedMimeTypeForWord_microsoft_word_2007beta2_dotx());
    }

    @Test
    public void shouldProvideMimeTypeForWorks_microsoft_works_spreadsheet_4_0_2000_wks() throws Exception {
        testMimeType("docs/microsoft-works-spreadsheet-4.0-2000.wks",
                     expectedMimeTypeForWorks_microsoft_works_spreadsheet_4_0_2000_wks());
    }

    @Test
    public void shouldProvideMimeTypeForWorks_microsoft_works_spreadsheet_7_0_xlr() throws Exception {
        testMimeType("docs/microsoft-works-spreadsheet-7.0.xlr", expectedMimeTypeForWorks_microsoft_works_spreadsheet_7_0_xlr());
    }

    @Test
    public void shouldProvideMimeTypeForWorks_microsoft_works_word_processor_2000_wps() throws Exception {
        testMimeType("docs/microsoft-works-word-processor-2000.wps",
                     expectedMimeTypeForWorks_microsoft_works_word_processor_2000_wps());
    }

    @Test
    public void shouldProvideMimeTypeForWorks_microsoft_works_word_processor_3_0_wps() throws Exception {
        testMimeType("docs/microsoft-works-word-processor-3.0.wps",
                     expectedMimeTypeForWorks_microsoft_works_word_processor_3_0_wps());
    }

    @Test
    public void shouldProvideMimeTypeForWorks_microsoft_works_word_processor_4_0_wps() throws Exception {
        testMimeType("docs/microsoft-works-word-processor-4.0.wps",
                     expectedMimeTypeForWorks_microsoft_works_word_processor_4_0_wps());
    }

    @Test
    public void shouldProvideMimeTypeForWorks_microsoft_works_word_processor_7_0_wps() throws Exception {
        testMimeType("docs/microsoft-works-word-processor-7.0.wps",
                     expectedMimeTypeForWorks_microsoft_works_word_processor_7_0_wps());
    }

    @Test
    public void shouldProvideMimeTypeForWorkbook_corel_quattro_pro_6_wb2() throws Exception {
        testMimeType("docs/corel-quattro-pro-6.wb2", expectedMimeTypeForWorkbook_corel_quattro_pro_6_wb2());
    }

    @Test
    public void shouldProvideMimeTypeForWorkbook_microsoft_works_spreadsheet_3_0_wks() throws Exception {
        testMimeType("docs/microsoft-works-spreadsheet-3.0.wks",
                     expectedMimeTypeForWorkbook_microsoft_works_spreadsheet_3_0_wks());
    }

    @Test
    public void shouldProvideMimeTypeForExcel_test_excel_2000_xls() throws Exception {
        testMimeType("test_excel_2000.xls", expectedMimeTypeForExcel_test_excel_2000_xls());
    }

    @Test
    public void shouldProvideMimeTypeForExcel_microsoft_excel_2000_xls() throws Exception {
        testMimeType("docs/microsoft-excel-2000.xls", expectedMimeTypeForExcel_microsoft_excel_2000_xls());
    }

    @Test
    public void shouldProvideMimeTypeForExcel_microsoft_excel_2007beta2_xlam() throws Exception {
        testMimeType("docs/microsoft-excel-2007beta2.xlam", expectedMimeTypeForExcel_microsoft_excel_2007beta2_xlam());
    }

    @Test
    public void shouldProvideMimeTypeForExcel_microsoft_excel_2007beta2_xlsb() throws Exception {
        testMimeType("docs/microsoft-excel-2007beta2.xlsb", expectedMimeTypeForExcel_microsoft_excel_2007beta2_xlsb());
    }

    @Test
    public void shouldProvideMimeTypeForExcel_microsoft_excel_2007beta2_xlsm() throws Exception {
        testMimeType("docs/microsoft-excel-2007beta2.xlsm", expectedMimeTypeForExcel_microsoft_excel_2007beta2_xlsm());
    }

    @Test
    public void shouldProvideMimeTypeForExcel_microsoft_excel_2007beta2_xlsx() throws Exception {
        testMimeType("docs/microsoft-excel-2007beta2.xlsx", expectedMimeTypeForExcel_microsoft_excel_2007beta2_xlsx());
    }

    @Test
    public void shouldProvideMimeTypeForExcel_microsoft_excel_2007beta2_xltm() throws Exception {
        testMimeType("docs/microsoft-excel-2007beta2.xltm", expectedMimeTypeForExcel_microsoft_excel_2007beta2_xltm());
    }

    @Test
    public void shouldProvideMimeTypeForExcel_microsoft_excel_2007beta2_xltx() throws Exception {
        testMimeType("docs/microsoft-excel-2007beta2.xltx", expectedMimeTypeForExcel_microsoft_excel_2007beta2_xltx());
    }

    @Test
    public void shouldProvideMimeTypeForPowerpoint_test_ppt() throws Exception {
        testMimeType("test.ppt", expectedMimeTypeForPowerpoint_test_ppt());
    }

    @Test
    public void shouldProvideMimeTypeForPowerpoint_microsoft_powerpoint_2000_ppt() throws Exception {
        testMimeType("docs/microsoft-powerpoint-2000.ppt", expectedMimeTypeForPowerpoint_microsoft_powerpoint_2000_ppt());
    }

    @Test
    public void shouldProvideMimeTypeForPowerpoint_microsoft_powerpoint_2007beta2_potm() throws Exception {
        testMimeType("docs/microsoft-powerpoint-2007beta2.potm",
                     expectedMimeTypeForPowerpoint_microsoft_powerpoint_2007beta2_potm());
    }

    @Test
    public void shouldProvideMimeTypeForPowerpoint_microsoft_powerpoint_2007beta2_potx() throws Exception {
        testMimeType("docs/microsoft-powerpoint-2007beta2.potx",
                     expectedMimeTypeForPowerpoint_microsoft_powerpoint_2007beta2_potx());
    }

    @Test
    public void shouldProvideMimeTypeForPowerpoint_microsoft_powerpoint_2007beta2_ppsm() throws Exception {
        testMimeType("docs/microsoft-powerpoint-2007beta2.ppsm",
                     expectedMimeTypeForPowerpoint_microsoft_powerpoint_2007beta2_ppsm());
    }

    @Test
    public void shouldProvideMimeTypeForPowerpoint_microsoft_powerpoint_2007beta2_ppsx() throws Exception {
        testMimeType("docs/microsoft-powerpoint-2007beta2.ppsx",
                     expectedMimeTypeForPowerpoint_microsoft_powerpoint_2007beta2_ppsx());
    }

    @Test
    public void shouldProvideMimeTypeForPowerpoint_microsoft_powerpoint_2007beta2_pptm() throws Exception {
        testMimeType("docs/microsoft-powerpoint-2007beta2.pptm",
                     expectedMimeTypeForPowerpoint_microsoft_powerpoint_2007beta2_pptm());
    }

    @Test
    public void shouldProvideMimeTypeForPowerpoint_microsoft_powerpoint_2007beta2_pptx() throws Exception {
        testMimeType("docs/microsoft-powerpoint-2007beta2.pptx",
                     expectedMimeTypeForPowerpoint_microsoft_powerpoint_2007beta2_pptx());
    }

    @Test
    public void shouldProvideMimeTypeForPublisher() throws Exception {
        testMimeType("docs/microsoft-publisher-2003.pub", expectedMimeTypeForPublisher());
    }

    @Test
    public void shouldProvideMimeTypeForVisio() throws Exception {
        testMimeType("docs/microsoft-visio.vsd", expectedMimeTypeForVisio());
    }

    @Test
    public void shouldProvideMimeTypeForOutlook() throws Exception {
        testMimeType("TestData.pst", expectedMimeTypeForOutlook());
    }

    @Test
    public void shouldProvideMimeTypeForShw_corel_presentations_3_0_shw() throws Exception {
        testMimeType("docs/corel-presentations-3.0.shw", expectedMimeTypeForShw_corel_presentations_3_0_shw());
    }

    @Test
    public void shouldProvideMimeTypeForShw_corel_presentations_x3_shw() throws Exception {
        testMimeType("docs/corel-presentations-x3.shw", expectedMimeTypeForShw_corel_presentations_x3_shw());
    }

    @Test
    public void shouldProvideMimeTypeForQuattroPro_corel_quattro_pro_7_wb3() throws Exception {
        testMimeType("docs/corel-quattro-pro-7.wb3", expectedMimeTypeForPro_corel_quattro_pro_7_wb3());
    }

    @Test
    public void shouldProvideMimeTypeForQuattroPro_corel_quattro_pro_x3_qpw() throws Exception {
        testMimeType("docs/corel-quattro-pro-x3.qpw", expectedMimeTypeForPro_corel_quattro_pro_x3_qpw());
    }

    @Test
    public void shouldProvideMimeTypeForWordperfect_corel_wordperfect_4_2_wp() throws Exception {
        testMimeType("docs/corel-wordperfect-4.2.wp", expectedMimeTypeForWordperfect_corel_wordperfect_4_2_wp());
    }

    @Test
    public void shouldProvideMimeTypeForWordperfect_corel_wordperfect_5_0_wp() throws Exception {
        testMimeType("docs/corel-wordperfect-5.0.wp", expectedMimeTypeForWordperfect_corel_wordperfect_5_0_wp());
    }

    @Test
    public void shouldProvideMimeTypeForWordperfect_corel_wordperfect_5_1_wp() throws Exception {
        testMimeType("docs/corel-wordperfect-5.1.wp", expectedMimeTypeForWordperfect_corel_wordperfect_5_1_wp());
    }

    @Test
    public void shouldProvideMimeTypeForWordperfect_corel_wordperfect_5_1_far_east_wp() throws Exception {
        testMimeType("docs/corel-wordperfect-5.1-far-east.wp", expectedMimeTypeForWordperfect_corel_wordperfect_5_1_far_east_wp());
    }

    @Test
    public void shouldProvideMimeTypeForWordperfect_corel_wordperfect_x3_wpd() throws Exception {
        testMimeType("docs/corel-wordperfect-x3.wpd", expectedMimeTypeForWordperfect_corel_wordperfect_x3_wpd());
    }

    @Test
    public void shouldProvideMimeTypeForMail_test_excel_web_archive_mht() throws Exception {
        testMimeType("test_excel_web_archive.mht", expectedMimeTypeForMail_test_excel_web_archive_mht());
    }

    @Test
    public void shouldProvideMimeTypeForMail_mail_thunderbird_1_5_eml() throws Exception {
        testMimeType("docs/mail-thunderbird-1.5.eml", expectedMimeTypeForMail_mail_thunderbird_1_5_eml());
    }

    @Test
    public void shouldProvideMimeTypeForMail_mhtml_firefox_mht() throws Exception {
        testMimeType("docs/mhtml-firefox.mht", expectedMimeTypeForMail_mhtml_firefox_mht());
    }

    @Test
    public void shouldProvideMimeTypeForMail_mhtml_internet_explorer_mht() throws Exception {
        testMimeType("docs/mhtml-internet-explorer.mht", expectedMimeTypeForMail_mhtml_internet_explorer_mht());
    }

    @Test
    public void shouldProvideMimeTypeForAddressBook() throws Exception {
        testMimeType("docs/thunderbird-addressbook.mab", expectedMimeTypeForAddressBook());
    }

    @Test
    public void shouldProvideMimeTypeForVCard_vcard_antoni_kontact_vcf() throws Exception {
        testMimeType("docs/vcard-antoni-kontact.vcf", expectedMimeTypeForVCard_vcard_antoni_kontact_vcf());
    }

    @Test
    public void shouldProvideMimeTypeForVCard_vcard_antoni_outlook2003_vcf() throws Exception {
        testMimeType("docs/vcard-antoni-outlook2003.vcf", expectedMimeTypeForVCard_vcard_antoni_outlook2003_vcf());
    }

    @Test
    public void shouldProvideMimeTypeForVCard_vcard_dirk_vcf() throws Exception {
        testMimeType("docs/vcard-dirk.vcf", expectedMimeTypeForVCard_vcard_dirk_vcf());
    }

    @Test
    public void shouldProvideMimeTypeForVCard_vcard_rfc2426_vcf() throws Exception {
        testMimeType("docs/vcard-rfc2426.vcf", expectedMimeTypeForVCard_vcard_rfc2426_vcf());
    }

    @Test
    public void shouldProvideMimeTypeForVCard_vcard_vCards_SAP_vcf() throws Exception {
        testMimeType("docs/vcard-vCards-SAP.vcf", expectedMimeTypeForVCard_vcard_vCards_SAP_vcf());
    }

    @Test
    public void shouldProvideMimeTypeForCalendar_basicCalendar_ics() throws Exception {
        testMimeType("docs/icaltestdata/basicCalendar.ics", expectedMimeTypeForCalendar_basicCalendar_ics());
    }

    @Test
    public void shouldProvideMimeTypeForCalendar_cal01_ics() throws Exception {
        testMimeType("docs/icaltestdata/cal01.ics", expectedMimeTypeForCalendar_cal01_ics());
    }

    @Test
    public void shouldProvideMimeTypeForCalendar_cal01_1_ics() throws Exception {
        testMimeType("docs/icaltestdata/cal01-1.ics", expectedMimeTypeForCalendar_cal01_1_ics());
    }

    @Test
    public void shouldProvideMimeTypeForCalendar_cal01_2_ics() throws Exception {
        testMimeType("docs/icaltestdata/cal01-2.ics", expectedMimeTypeForCalendar_cal01_2_ics());
    }

    @Test
    public void shouldProvideMimeTypeForCalendar_cal01_3_ics() throws Exception {
        testMimeType("docs/icaltestdata/cal01-3.ics", expectedMimeTypeForCalendar_cal01_3_ics());
    }

    @Test
    public void shouldProvideMimeTypeForCalendar_cal01_4_ics() throws Exception {
        testMimeType("docs/icaltestdata/cal01-4.ics", expectedMimeTypeForCalendar_cal01_4_ics());
    }

    @Test
    public void shouldProvideMimeTypeForCalendar_cal01_5_ics() throws Exception {
        testMimeType("docs/icaltestdata/cal01-5.ics", expectedMimeTypeForCalendar_cal01_5_ics());
    }

    @Test
    public void shouldProvideMimeTypeForCalendar_cal01_6_ics() throws Exception {
        testMimeType("docs/icaltestdata/cal01-6.ics", expectedMimeTypeForCalendar_cal01_6_ics());
    }

    @Test
    public void shouldProvideMimeTypeForCalendar_cal01_exrule_ics() throws Exception {
        testMimeType("docs/icaltestdata/cal01-exrule.ics", expectedMimeTypeForCalendar_cal01_exrule_ics());
    }

    @Test
    public void shouldProvideMimeTypeForCalendar_calconnect7_ics() throws Exception {
        testMimeType("docs/icaltestdata/calconnect7.ics", expectedMimeTypeForCalendar_calconnect7_ics());
    }

    @Test
    public void shouldProvideMimeTypeForCalendar_calconnect9_ics() throws Exception {
        testMimeType("docs/icaltestdata/calconnect9.ics", expectedMimeTypeForCalendar_calconnect9_ics());
    }

    @Test
    public void shouldProvideMimeTypeForCalendar_combined_multiplevcalendar_ics() throws Exception {
        testMimeType("docs/icaltestdata/combined_multiplevcalendar.ics",
                     expectedMimeTypeForCalendar_combined_multiplevcalendar_ics());
    }

    @Test
    public void shouldProvideMimeTypeForCalendar_combined_onevcalendar_ics() throws Exception {
        testMimeType("docs/icaltestdata/combined_onevcalendar.ics", expectedMimeTypeForCalendar_combined_onevcalendar_ics());
    }

    @Test
    public void shouldProvideMimeTypeForCalendar_extendedCalendar_ics() throws Exception {
        testMimeType("docs/icaltestdata/extendedCalendar.ics", expectedMimeTypeForCalendar_extendedCalendar_ics());
    }

    @Test
    public void shouldProvideMimeTypeForCalendar_freebusy_ics() throws Exception {
        testMimeType("docs/icaltestdata/freebusy.ics", expectedMimeTypeForCalendar_freebusy_ics());
    }

    @Test
    public void shouldProvideMimeTypeForCalendar_geol_ics() throws Exception {
        testMimeType("docs/icaltestdata/geo1.ics", expectedMimeTypeForCalendar_geol_ics());
    }

    @Test
    public void shouldProvideMimeTypeForCalendar_gkexample_ics() throws Exception {
        testMimeType("docs/icaltestdata/gkexample.ics", expectedMimeTypeForCalendar_gkexample_ics());
    }

    @Test
    public void shouldProvideMimeTypeForCalendar_incoming_ics() throws Exception {
        testMimeType("docs/icaltestdata/incoming.ics", expectedMimeTypeForCalendar_incoming_ics());
    }

    @Test
    public void shouldProvideMimeTypeForCalendar_korganizer_jicaltest_vjournal_ics() throws Exception {
        testMimeType("docs/icaltestdata/korganizer-jicaltest-vjournal.ics",
                     expectedMimeTypeForCalendar_korganizer_jicaltest_vjournal_ics());
    }

    @Test
    public void shouldProvideMimeTypeForCalendar_korganizer_jicaltest_ics() throws Exception {
        testMimeType("docs/icaltestdata/korganizer-jicaltest.ics", expectedMimeTypeForCalendar_korganizer_jicaltest_ics());
    }

    @Test
    public void shouldProvideMimeTypeForCalendar_php_flp_ics() throws Exception {
        testMimeType("docs/icaltestdata/php-flp.ics", expectedMimeTypeForCalendar_php_flp_ics());
    }

    @Test
    public void shouldProvideMimeTypeForCalendar_simplevevent_ics() throws Exception {
        testMimeType("docs/icaltestdata/simplevevent.ics", expectedMimeTypeForCalendar_simplevevent_ics());
    }

    @Test
    public void shouldProvideMimeTypeForCalendar_sunbird_sample_ics() throws Exception {
        testMimeType("docs/icaltestdata/sunbird_sample.ics", expectedMimeTypeForCalendar_sunbird_sample_ics());
    }

    @Test
    public void shouldProvideMimeTypeForCalendar_tag_bug_ics() throws Exception {
        testMimeType("docs/icaltestdata/tag-bug.ics", expectedMimeTypeForCalendar_tag_bug_ics());
    }

    @Test
    public void shouldProvideMimeTypeForCalendar_test_created_ics() throws Exception {
        testMimeType("docs/icaltestdata/test-created.ics", expectedMimeTypeForCalendar_test_created_ics());
    }

    @Test
    public void shouldProvideMimeTypeForCalendar_Todos1_ics() throws Exception {
        testMimeType("docs/icaltestdata/Todos1.ics", expectedMimeTypeForCalendar_Todos1_ics());
    }

    @Test
    public void shouldProvideMimeTypeForAu() throws Exception {
        testMimeType("test.au", expectedMimeTypeForAu());
    }

    @Test
    public void shouldProvideMimeTypeForBin() throws Exception {
        testMimeType("test.bin", expectedMimeTypeForBin());
    }

    @Test
    public void shouldProvideMimeTypeForEmf() throws Exception {
        testMimeType("test.emf", expectedMimeTypeForEmf());
    }

    @Test
    public void shouldProvideMimeTypeForFli() throws Exception {
        testMimeType("test.fli", expectedMimeTypeForFli());
    }

    @Test
    public void shouldProvideMimeTypeForPcx() throws Exception {
        testMimeType("test.pcx", expectedMimeTypeForPcx());
    }

    @Test
    public void shouldProvideMimeTypeForPict() throws Exception {
        testMimeType("test.pict", expectedMimeTypeForPict());
    }

    @Test
    public void shouldProvideMimeTypeForPsd() throws Exception {
        testMimeType("test.psd", expectedMimeTypeForPsd());
    }

    @Test
    public void shouldProvideMimeTypeForTar() throws Exception {
        testMimeType("test.tar", expectedMimeTypeForTar());
    }
}
