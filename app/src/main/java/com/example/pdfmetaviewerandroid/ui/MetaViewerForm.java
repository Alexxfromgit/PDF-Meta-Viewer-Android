package com.example.pdfmetaviewerandroid.ui;

import static com.example.pdfmetaviewerandroid.utilities.LookAndFeelConstants.MAC_OSS_X;
import static com.example.pdfmetaviewerandroid.utilities.PdfMetaViewerConstants.APP_MAIN_TITLE;
import static com.example.pdfmetaviewerandroid.utilities.PdfMetaViewerConstants.DESCRIPTION_FILE_EXPORTED;
import static com.example.pdfmetaviewerandroid.utilities.PdfMetaViewerConstants.DESCRIPTION_FILE_NOT_SELECTED;
import static com.example.pdfmetaviewerandroid.utilities.PdfMetaViewerConstants.DESCRIPTION_FILE_SELECTED;
import static com.example.pdfmetaviewerandroid.utilities.PdfMetaViewerConstants.DESCRIPTION_JSON;
import static com.example.pdfmetaviewerandroid.utilities.PdfMetaViewerConstants.DESCRIPTION_META_NOT_RETRIEVED;
import static com.example.pdfmetaviewerandroid.utilities.PdfMetaViewerConstants.DESCRIPTION_META_RETRIEVED;
import static com.example.pdfmetaviewerandroid.utilities.PdfMetaViewerConstants.DESCRIPTION_PDF;
import static com.example.pdfmetaviewerandroid.utilities.PdfMetaViewerConstants.EXTENSION_JSON;
import static com.example.pdfmetaviewerandroid.utilities.PdfMetaViewerConstants.EXTENSION_PDF;
import static com.example.pdfmetaviewerandroid.utilities.PdfMetaViewerConstants.HEIGHT_APP;
import static com.example.pdfmetaviewerandroid.utilities.PdfMetaViewerConstants.WIDTH_APP;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.example.pdfmetaviewerandroid.converters.ConvertJavaMapToJson;
import com.example.pdfmetaviewerandroid.extractor.PDFExtractor;
import com.example.pdfmetaviewerandroid.repository.DocumentRecord;

import java.io.File;
import java.io.FileFilter;

public class MetaViewerForm {

    File selectedFile;
    DocumentRecord documentRecord;
    private ConstraintLayout panelMain;
    private Button choosePDFButton;
    private Button readMetaButton;
    private EditText textArea1;
    private Button saveAsJsonButton;
    private TextView statusLabel;

    public MetaViewerForm() {
        /*
        choosePDFButton.setOnClickListener(e -> {
            JFileChooser jFileChooser = new JFileChooser();
            jFileChooser.setMultiSelectionEnabled(false);
            jFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            jFileChooser.setFileFilter(new FileFilter() {
                @Override
                public boolean accept(File f) {
                    return f.getName().endsWith(EXTENSION_PDF);
                }

                @Override
                public String getDescription() {
                    return DESCRIPTION_PDF;
                }
            });
            jFileChooser.showOpenDialog(panelMain);
            selectedFile = jFileChooser.getSelectedFile();

            if (selectedFile != null && selectedFile.isFile()) {
                statusLabel.setText(DESCRIPTION_FILE_SELECTED);
                statusLabel.setForeground(Color.GREEN);
            } else {
                statusLabel.setText(DESCRIPTION_FILE_NOT_SELECTED);
                statusLabel.setForeground(Color.RED);
            }
        });
        */

        readMetaButton.setOnClickListener(e -> {
            if (selectedFile != null) {
                documentRecord = new PDFExtractor().getDocumentRecordFromImportedPDF(selectedFile.getAbsolutePath());

                textArea1.setText(new ConvertJavaMapToJson()
                        .convertMapToJson(documentRecord.getMetadata())
                        .replace(",", "," + System.lineSeparator()));
                statusLabel.setText(DESCRIPTION_META_RETRIEVED);
                statusLabel.setTextColor(Color.GREEN);
                //statusLabel.setForeground(Color.GREEN);
            } else {
                statusLabel.setText(DESCRIPTION_FILE_NOT_SELECTED);
                statusLabel.setTextColor(Color.RED);
                //statusLabel.setForeground(Color.RED);
            }
        });

        /* TODO
        saveAsJsonButton.setOnClickListener(e -> {
            if (statusLabel.getText().equals(DESCRIPTION_META_RETRIEVED)) {
                JFileChooser jFileChooser = new JFileChooser();
                jFileChooser.setMultiSelectionEnabled(false);
                jFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                jFileChooser.setFileFilter(new FileFilter() {
                    @Override
                    public boolean accept(File f) {
                        return f.getName().endsWith(EXTENSION_JSON);
                    }

                    @Override
                    public String getDescription() {
                        return DESCRIPTION_JSON;
                    }
                });
                int userSelection = jFileChooser.showSaveDialog(panelMain);

                if (userSelection == JFileChooser.APPROVE_OPTION) {
                    File fileToSave = jFileChooser.getSelectedFile();
                    new PDFExtractor().storeDocumentRecordAs(documentRecord, fileToSave.getAbsolutePath(), false);
                    statusLabel.setText(DESCRIPTION_FILE_EXPORTED);
                    statusLabel.setTextColor(Color.GREEN);
                    //statusLabel.setForeground(Color.GREEN);
                }
            } else {
                statusLabel.setText(DESCRIPTION_META_NOT_RETRIEVED);
                statusLabel.setTextColor(Color.RED);
                //statusLabel.setForeground(Color.RED);
            }
        });
        */
    }


    /*
    public static void main(String[] args) {
        setUpFrame(new MetaViewerForm());
    }
    */

    /*
    private void setLookAndFeel() {
        try {
            UIManager.setLookAndFeel(MAC_OSS_X);
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException |
                 UnsupportedLookAndFeelException e) {
            throw new RuntimeException(e);
        }
    }
    */

    /*
    private static MetaViewerForm setUpFrame(MetaViewerForm jFrame) {
        jFrame.setContentPane(jFrame.panelMain);
        jFrame.setVisible(true);
        jFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        jFrame.setTitle(APP_MAIN_TITLE);

        Dimension dimension = Toolkit.getDefaultToolkit().getScreenSize();

        jFrame.setBounds(dimension.width / 2 - (WIDTH_APP / 2),
                dimension.height / 2 - (HEIGHT_APP / 2),
                WIDTH_APP, HEIGHT_APP);
        return jFrame;
    }
    */

    // TODO:: jFrame.setIconImage();
    // TODO:: panelMain.setBackground(Color.BLUE);
    // TODO:: Font font = new Font("Bitstream Charter", Font.BOLD, 20);

}
