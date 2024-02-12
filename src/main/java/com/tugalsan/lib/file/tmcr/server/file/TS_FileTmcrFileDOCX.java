package com.tugalsan.lib.file.tmcr.server.file;

import com.tugalsan.api.string.server.*;
import com.tugalsan.api.cast.client.*;
import java.awt.image.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;
import org.apache.poi.xwpf.usermodel.*;
import com.tugalsan.api.log.server.*;
import com.tugalsan.api.file.server.*;
import com.tugalsan.api.file.docx.server.*;
import com.tugalsan.api.list.client.*;
import com.tugalsan.api.runnable.client.TGS_RunnableType1;
import com.tugalsan.api.stream.client.*;
import com.tugalsan.api.string.client.*;
import com.tugalsan.api.url.client.*;
import com.tugalsan.lib.file.tmcr.server.code.parser.TS_FileTmcrParser_Globals;

public class TS_FileTmcrFileDOCX extends TS_FileTmcrFileInterface {

    final private static TS_Log d = TS_Log.of(TS_FileTmcrFileDOCX.class);

    private static int FONT_HEIGHT_OFFSET() {
        return -3;
    }

    private TS_FileDocx docx;
    private XWPFParagraph docxParag;

    private XWPFTable table = null;
    private TGS_ListTable tableAbstract;
    private int[] table_relColSizes = null;
    private XWPFTableRow tableRow = null;
    private XWPFTableCell tableRowCell = null;
    private int currentRowIndex = 0;
    final private String CELL_FULL = "CELL_FULL";
    final private String CELL_EMPTY = "CELL_EMPTY";
    final private String CELL_INIT = "";

    private TS_FileTmcrParser_Globals macroGlobals;

    private TS_FileTmcrFileDOCX(boolean enabled, Path localFile, TGS_Url remoteFile) {
        super(enabled, localFile, remoteFile);
    }

    public static void use(boolean enabled, TS_FileTmcrParser_Globals macroGlobals, Path localFile, TGS_Url remoteFile, TGS_RunnableType1<TS_FileTmcrFileDOCX> docx) {
        var instance = new TS_FileTmcrFileDOCX(enabled, localFile, remoteFile);
        try {
            instance.use_init(macroGlobals);
            docx.run(instance);
        } catch (Exception e) {
            instance.saveFile(e.getMessage());
            throw e;
        } finally {
            instance.saveFile(null);
        }
    }

    private void use_init(TS_FileTmcrParser_Globals macroGlobals) {
        this.macroGlobals = macroGlobals;
        if (isClosed()) {
            return;
        }
        docx = new TS_FileDocx(localFile);
    }

    @Override
    public boolean saveFile(String errorSource) {
        if (isClosed()) {
            return true;
        }
        setClosed();
        d.ci("saveFile.DOCX->");
        if (docx == null) {
            d.ci("DOCX File is null");
        } else {
            docx.close();
            if (TS_FileUtils.isExistFile(docx.getFile())) {
                d.ci("saveFile.FIX: DOCX File save", docx.getFile(), "successfull");
            } else {
                d.ce("saveFile.FIX: DOCX File save", docx.getFile(), "failed");
            }
        }
        return errorSource == null;
    }

    @Override
    public boolean createNewPage(int pageSizeAX, boolean landscape, Integer marginLeft, Integer marginRight, Integer marginTop, Integer marginBottom) {
        if (isClosed()) {
            return true;
        }
        d.ci("createNewPage");
        docx.insertPage();
        docx.setCurrentPage(landscape, pageSizeAX);
        return true;
    }

    @Override
    public boolean endText() {
        if (isClosed()) {
            return true;
        }
        d.ci("endText");
        docxParag = null;
        return true;
    }

    @Override
    public boolean addText(String text) {
        if (isClosed()) {
            return true;
        }
        d.ci("addText");
        var lines = TS_StringUtils.toList(text, "\n");
        var fh = macroGlobals.fontHeight + FONT_HEIGHT_OFFSET() < 1 ? 1 : macroGlobals.fontHeight + FONT_HEIGHT_OFFSET();
        for (var i = 0; i < lines.size(); i++) {
            var line = lines.get(i);
            if (!line.isEmpty()) {
                if (!TGS_StringDouble.may(text)) {
                    docx.addText(docxParag, line, macroGlobals.fontBold, macroGlobals.fontItalic,
                            macroGlobals.fontUnderlined, fh, macroGlobals.getHexColor());
                } else {
                    var k = 0.8f;
                    var fh_half = (int) (fh * k) < 1 ? 1 : (int) (fh * k);
                    var tags = TS_StringUtils.toList_spc(line);
                    IntStream.range(0, tags.size()).forEachOrdered(j -> {
                        var tag = tags.get(j);
                        var dbl = TGS_StringDouble.of(text);
                        if (dbl.isEmpty()) {
                            docx.addText(docxParag, tag, macroGlobals.fontBold, macroGlobals.fontItalic,
                                    macroGlobals.fontUnderlined, fh, macroGlobals.getHexColor());
                        } else {
                            docx.addText(docxParag, String.valueOf(dbl.get().left), macroGlobals.fontBold, macroGlobals.fontItalic,
                                    macroGlobals.fontUnderlined, fh, macroGlobals.getHexColor());
                            docx.addText(docxParag, String.valueOf(dbl.get().dim()) + String.valueOf(dbl.get().right), macroGlobals.fontBold, macroGlobals.fontItalic,
                                    macroGlobals.fontUnderlined, fh_half, macroGlobals.getHexColor());
                        }
                        if (tags.size() - 1 != j) {
                            docx.addText(docxParag, " ", macroGlobals.fontBold, macroGlobals.fontItalic,
                                    macroGlobals.fontUnderlined, fh, macroGlobals.getHexColor());
                        }
                    });
                }
            }
            if (i != lines.size() - 1 || text.endsWith("\n")) {
                addLineBreak();
            }
        }
        return true;
    }

    @Override
    public boolean addLineBreak() {
        if (isClosed()) {
            return true;
        }
        d.ci("addLineBreak");
        docx.addNewLine(docxParag);
        return true;
    }

    @Override
    public boolean setFontStyle() {
        if (isClosed()) {
            return true;
        }
        d.ci("setFontStyle");
        return true;
    }

    @Override
    public boolean setFontHeight() {
        if (isClosed()) {
            return true;
        }
        d.ci("setFontHeight");
        return true;
    }

    @Override
    public boolean setFontColor() {
        if (isClosed()) {
            return true;
        }
        d.ci("setFontColor");
        return true;
    }

    @Override
    public boolean addImage(BufferedImage pstImage, Path pstImageLoc, boolean textWrap, int left0_center1_right2, long imageCounter) {
        if (isClosed()) {
            return true;
        }
        d.ci("addImage", "init", pstImageLoc);
        beginText(0);
        boolean result;
        if (docx.addImage(docxParag, pstImageLoc.toAbsolutePath().toString(), pstImage.getWidth(), pstImage.getHeight())) {
            result = true;
        } else {
            d.ce("addImage", "ERROR: TS_MIFDOCX.addImage_returns false");
            result = false;
        }
        endText();
        d.ci("addImage", "fin");
        return result;
    }

    @Override
    public boolean beginTableCell(int rowSpan, int colSpan, Integer cellHeight) {
        if (isClosed()) {
            return true;
        }
        d.ci("beginTableCell");
        if (tableRowCell != null) {
            d.ce("beginTableCell.ERROR: TS_MIFDOCX.beginTableCell -> why tableRowCell exists!");
            return false;
        }
        if (tableRow == null) {
            d.ce("beginTableCell.ERROR: TS_MIFDOCX.beginTableCell -> why tableRow not exists!");
            return false;
        }
        if (table == null) {
            d.ce("beginTableCell.ERROR: TS_MIFDOCX.beginTableCell -> why table not exists!");
            return false;
        }
        //CALCULATE rowCellColSpanOffset
        var rowCellColSpanOffset = calculateRowCellColSpanOffset();
        if (rowCellColSpanOffset == -1) {
            return false;
        }
        while (checkMaxColumnSize(rowCellColSpanOffset)) {
            rowCellColSpanOffset = calculateRowCellColSpanOffset();
            if (rowCellColSpanOffset == -1) {
                return false;
            }
        }

        //SET CELL
        {
            if (table_relColSizes.length <= rowCellColSpanOffset) {
                d.ce("beginTableCell.ERROR: TS_MIFDOCX.beginTableCell -> FAILED(table_relColSizes.length <= cellColCounter)");
                return false;
            }
            tableRowCell = tableRow.getCell(rowCellColSpanOffset);
            d.ci("beginTableCell.INFO: TS_MIFDOCX.currentRow/Col: " + currentRowIndex + "/" + rowCellColSpanOffset);
            tableAbstract.setValue(currentRowIndex, rowCellColSpanOffset, CELL_INIT);
            spanList.add(currentRowIndex + " " + rowCellColSpanOffset + " " + rowSpan + " " + colSpan);
        }

        {//SET STYLE
            //            int sumWidth = 0;
            //            for (int c = 0; c < colSpan; c++) {
            //                if (rowCellColSpanOffset + c <= table_relColSizes.length - 1) {
            //                    sumWidth += table_relColSizes[rowCellColSpanOffset + c];
            //                } else {
            //                    d("ERROR: TS_MIFDOCX.beginTableCell -> sumWidth WHY CANOT ADD COLSPANWIDTH: rowCellColSpanOffset + c <= table_relColSizes.length - 1");
            //                    d("ERROR: TS_MIFDOCX.beginTableCell -> rowCellColSpanOffset: " + rowCellColSpanOffset);
            //                    d("ERROR: TS_MIFDOCX.beginTableCell -> table_relColSizes.length: " + table_relColSizes.length);
            //                }
            //            }
            //            String styleWidth = "width:" + sumWidth + "%;";
            //            String styleHeight = cellHeight == null ? "" : "height:" + cellHeight + "px;";
            //            tableRowCell.setStyle_Properties2("vertical-align:top;border:1px solid black;" + styleWidth + styleHeight);
            if (cellHeight != null) {
                tableRow.setHeight(cellHeight * 1440 / 96);
            }
        }

        var frowCellColSpanOffset = rowCellColSpanOffset;
        IntStream.range(1, colSpan).forEachOrdered(ci -> {//ADD COLSPAN FILL TODO
            if (frowCellColSpanOffset + ci <= table_relColSizes.length - 1) {
                tableAbstract.setValue(currentRowIndex, frowCellColSpanOffset + ci, CELL_INIT);
            } else {
                d.ci("beginTableCell.ERROR: TS_MIFDOCX.beginTableCell -> eColSpan WHY CANOT ADD COLSPANFULL: rowCellColSpanOffset + c <= table_relColSizes.length - 1", "rowCellColSpanOffset: ", frowCellColSpanOffset, "table_relColSizes.length: " + table_relColSizes.length);
            }
        });

        IntStream.range(1, rowSpan).forEachOrdered(ri -> {
            var rowCreated = false;
            if (table.getRows().size() <= currentRowIndex + ri) {
                TS_FileDocx.addTableRow(table, table_relColSizes.length);
                d.ci("beginTableCell.table.createRow();  *** //ADD ROWSPAN FILL");
                rowCreated = true;
            }
            if (rowCreated) {
                for (var ci = 0; ci < table_relColSizes.length; ci++) {
                    tableAbstract.setValue(currentRowIndex + ri, ci, CELL_EMPTY);
                }
            }
            for (var ci = 0; ci < colSpan; ci++) {
                tableAbstract.setValue(currentRowIndex + ri, frowCellColSpanOffset + ci, CELL_FULL);
            }
        });
        return true;
    }
    public List<String> spanList;

    private int calculateRowCellColSpanOffset() {
        d.ci("calcultaeRowCellColSpanOffset");
        var rowCellColSpanOffset = 0;
        if (isClosed()) {
            return rowCellColSpanOffset;
        }
        OUTER:
        for (int ci = 0; ci < table_relColSizes.length; ci++) {
            var eRowCellText = tableAbstract.getValueAsString(currentRowIndex, ci);
            if (null == eRowCellText) {
                d.ci("calcultaeRowCellColSpanOffset.O(", currentRowIndex + "," + ci + ")[", eRowCellText, "]");
                rowCellColSpanOffset += 1; //full already adds 1
            } else {
                switch (eRowCellText) {
                    case CELL_EMPTY -> {
                        d.ci("calcultaeRowCellColSpanOffset.E(", currentRowIndex, "," + ci, ")[", eRowCellText, "]");
                        break OUTER;
                    }
                    case CELL_FULL -> {
                        d.ci("calcultaeRowCellColSpanOffset.F(", currentRowIndex + "," + ci + ")[", eRowCellText, "]");
                        rowCellColSpanOffset += 1; //from rowspan
                    }
                    default -> {
                        d.ci("calcultaeRowCellColSpanOffset.O(", currentRowIndex + "," + ci + ")[", eRowCellText, "]");
                        rowCellColSpanOffset += 1; //full already adds 1
                    }
                }
            }
        }
        d.ci("calcultaeRowCellColSpanOffset", "rowCellColSpanMax: " + table_relColSizes.length, "rowCellColSpanOffset: ", rowCellColSpanOffset);
        return rowCellColSpanOffset;
    }

    private boolean checkMaxColumnSize(int rowCellColSpanOffset) {
        if (isClosed()) {
            return true;
        }
        d.ci("checkMaxColumnSize.tablesize:" + table.getRows().size());
        var rowAdded = false;
        if (table_relColSizes.length <= rowCellColSpanOffset) {
            rowAdded = true;
            if (table.getRows().size() - 1 == currentRowIndex) {
                currentRowIndex++;
                tableRow = TS_FileDocx.addTableRow(table, table_relColSizes.length);
                d.ci("checkMaxColumnSize. *** table.createRow();  *** //checkMaxColumnSize");
                for (var c = 0; c < table_relColSizes.length; c++) {
                    tableAbstract.setValue(currentRowIndex, c, CELL_EMPTY);
                }
                d.ci("checkMaxColumnSize.DECISION: NEWROW_ADDED");
            } else {
                currentRowIndex++;
                tableRow = table.getRow(currentRowIndex);
                d.ci("checkMaxColumnSize.DECISION: ROW_ALREADY_EXISTS");
            }
        } else {
            d.ci("checkMaxColumnSize.DECISION: PASS");
        }
        return rowAdded;
    }

    @Override
    public boolean endTableCell(int rotationInDegrees_0_90_180_270) {
        if (isClosed()) {
            return true;
        }
        d.ci("endTableCell");
        if (tableRow == null) {
            d.ce("endTableCell.ERROR: TS_MIFDOCX.endTableCell -> why tableRow not exists!");
            return false;
        }
        if (tableRowCell == null) {
            d.ce("endTableCell.ERROR: TS_MIFDOCX.endTableCell -> why tableRowCell not exists!");
            return false;
        }
        tableRowCell = null;
        return true;
    }

    @Override
    public boolean beginTable(int[] table_relColSizes) {
        if (isClosed()) {
            return true;
        }
        d.ci("beginTable");
        this.table_relColSizes = table_relColSizes;
        if (table != null) {
            d.ce("beginTable.ERROR: TS_MIFDOCX.beginTable -> table already exists");
            return false;
        }
        if (tableRow != null) {
            d.ce("beginTable.ERROR: TS_MIFDOCX.beginTable -> tableRow already exists");
            return false;
        }

        spanList = TGS_ListUtils.of();
        table = docx.createTable(1, table_relColSizes.length);
        tableRow = table.getRow(0);
        tableAbstract = TGS_ListTable.of(false);
        for (var c = 0; c < table_relColSizes.length; c++) {
            tableAbstract.setValue(0, c, CELL_EMPTY);
        }
        currentRowIndex = 0;
        return true;
    }

    @Override
    public boolean endTable() {
        if (isClosed()) {
            return true;
        }
        d.ci("endTable");
        if (table == null) {
            d.ce("endTable.ERROR: TS_MIFDOCX.endTable -> table not exists");
            return false;
        }
        if (tableRow == null) {
            d.ce("endTable.ERROR: TS_MIFDOCX.endTable -> tableRow not exists");
            return false;
        }

        docx.setTableColWidths(table, table_relColSizes);

        TGS_StreamUtils.reverse(0, spanList.size()).forEach(i -> {//FRW NOT WORKING
            var tokens = TS_StringUtils.toList_spc(spanList.get(i));
            var rIdx = TGS_CastUtils.toInteger(tokens.get(0));
            var cIdx = TGS_CastUtils.toInteger(tokens.get(1));
            var rSpan = TGS_CastUtils.toInteger(tokens.get(2));
            var cSpan = TGS_CastUtils.toInteger(tokens.get(3));
            d.ci("endTable.INFO: TS_MIFDOCX.endTable -> cell ri/ci/rc/cs:", rIdx, cIdx, rSpan, cSpan);
            docx.mergeCell_bySpan(table, rIdx, cIdx, rSpan, cSpan, table_relColSizes);
        });
        currentRowIndex = 0;
        table = null;
        tableRow = null;
        spanList.clear();
        spanList = null;
        tableAbstract.clear();
        tableAbstract = null;
        return true;
    }

    @Override
    public boolean beginText(int allign_Left0_center1_right2_just3) {
        if (isClosed()) {
            return true;
        }
        d.ci("beginText", "allign_Left0_center1_right2_just3", allign_Left0_center1_right2_just3);
        if (tableRowCell == null) {
            docxParag = docx.createParagraph(allign_Left0_center1_right2_just3, false);
        } else {
            docxParag = docx.getParagraph(tableRowCell, allign_Left0_center1_right2_just3);
        }
        return true;
    }
}
