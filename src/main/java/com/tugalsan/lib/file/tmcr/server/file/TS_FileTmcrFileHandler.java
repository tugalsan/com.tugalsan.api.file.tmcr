package com.tugalsan.lib.file.tmcr.server.file;

import com.tugalsan.api.cast.client.TGS_CastUtils;
import com.tugalsan.api.charset.client.TGS_CharSetCast;
import com.tugalsan.api.file.client.TGS_FileUtilsEng;
import com.tugalsan.api.file.client.TGS_FileUtilsTur;
import com.tugalsan.api.file.html.server.TS_FileHtml;
import com.tugalsan.api.file.common.server.TS_FileCommonInterface;
import com.tugalsan.api.file.common.server.TS_FileCommonBall;
import com.tugalsan.api.file.common.server.TS_FileCommonFontTags;
import com.tugalsan.api.file.docx.server.TS_FileDocx;
import com.tugalsan.api.file.xlsx.server.TS_FileXlsx;
import com.tugalsan.api.list.client.*;
import java.awt.image.*;
import java.nio.file.*;
import java.util.*;
import com.tugalsan.api.log.server.*;
import com.tugalsan.api.file.pdf.server.TS_FilePdf;
import com.tugalsan.api.file.server.TS_FileUtils;
import com.tugalsan.api.file.zip.server.TS_FileZipUtils;
import com.tugalsan.api.runnable.client.TGS_RunnableType1;
import com.tugalsan.api.runnable.client.TGS_RunnableType2;
import com.tugalsan.api.stream.client.TGS_StreamUtils;
import com.tugalsan.api.string.server.TS_StringUtils;
import com.tugalsan.api.tuple.client.TGS_Tuple1;
import com.tugalsan.api.unsafe.client.TGS_UnSafe;
import com.tugalsan.lib.boot.server.TS_LibBootUtils;
import com.tugalsan.lib.file.tmcr.client.TGS_FileTmcrTypes;
import com.tugalsan.lib.file.tmcr.server.code.parser.TS_FileTmcrParser;
import java.util.stream.IntStream;

public class TS_FileTmcrFileHandler extends TS_FileCommonInterface {

    final private static TS_Log d = TS_Log.of(TS_FileTmcrFileHandler.class);
    final private static boolean PARALLEL = false; //may cause unexpected exception: java.lang.OutOfMemoryError: Java heap space

    public TS_FileCommonBall fileCommonBall;
    final public List<TS_FileCommonInterface> files;

    public boolean isZipFileRequested() {
        return fileCommonBall.requestedFileTypes.stream()
                .filter(type -> Objects.equals(type, TGS_FileTmcrTypes.FILE_TYPE_ZIP()))
                .findAny().isPresent();
    }

    public Path pathZipFile() {
        if (files.isEmpty()) {
            return null;
        }
        var aFile = files.get(0).getLocalFileName().toAbsolutePath().toString();
        var aFileDotIdx = aFile.lastIndexOf(".");
        if (aFileDotIdx == -1) {
            d.ce("act_ifZipRequestedZipFiles_setRemoteFileZipName_shortenUrlIfPossible", "aFileDotIdx == -1");
            return null;
        }
        var zipSuffix = ".zip";
        return Path.of(aFile.substring(0, aFileDotIdx) + zipSuffix);
    }

    public List<Path> zipableFiles() {
        return TGS_StreamUtils.toLst(
                files.stream()
                        .filter(mif -> mif.isEnabled())
                        .filter(mif -> !mif.getLocalFileName().toString().endsWith(TGS_FileTmcrTypes.FILE_TYPE_TMCR())
                        && !mif.getLocalFileName().toString().endsWith(TGS_FileTmcrTypes.FILE_TYPE_HTML()))
                        .map(mif -> mif.getLocalFileName())
        );
    }

    public List<String> getRemoteFiles() {
        List<String> remoteFiles = TGS_ListUtils.of();
        files.stream().filter(mif -> mif.isEnabled()).forEachOrdered(f -> remoteFiles.add(f.getRemoteFileName().url.toString()));
        return remoteFiles;
    }

    private TS_FileTmcrFileHandler(TS_FileCommonBall fileCommonBall, TS_FileCommonInterface... files) {
        super(true, null, null);
        this.fileCommonBall = fileCommonBall;
        this.files = TGS_StreamUtils.toLst(Arrays.stream(files));
    }

    public static boolean use(TS_FileCommonBall fileCommonBall,TGS_RunnableType2<String, Integer> progressUpdate_with_userDotTable_and_percentage, TGS_RunnableType1<TS_FileTmcrFileHandler> exeBeforeZip, TGS_RunnableType1<TS_FileTmcrFileHandler> exeAfterZip) {
        d.ci("use", "running macro code...");
        TS_FileTmcrFileHandler.use_do(fileCommonBall, fileHandler -> {
            d.ci("use", "compileCode");
            TS_FileTmcrParser.compileCode(TS_LibBootUtils.pck.sqlAnc, fileCommonBall, fileHandler, (userDotTable, percentage) -> {
                progressUpdate_with_userDotTable_and_percentage.run(userDotTable, percentage);
            });

            d.ci("use", "RENAME LOCAL FILES", "prefferedFileNameLabel", fileCommonBall.prefferedFileNameLabel);
            if (!fileCommonBall.prefferedFileNameLabel.isEmpty()) {
                if (TS_FileCommonInterface.FILENAME_CHAR_SUPPORT_TURKISH) {
                    fileCommonBall.prefferedFileNameLabel = TGS_FileUtilsTur.toSafe(fileCommonBall.prefferedFileNameLabel);
                } else {
                    fileCommonBall.prefferedFileNameLabel = TGS_FileUtilsEng.toSafe(fileCommonBall.prefferedFileNameLabel);
                }
                if (!TS_FileCommonInterface.FILENAME_CHAR_SUPPORT_SPACE) {
                    fileCommonBall.prefferedFileNameLabel = fileCommonBall.prefferedFileNameLabel.replace(" ", "_");
                }
                fileHandler.files.forEach(file -> TS_FileTmcrFilePreffredFileName.renameLocalFileName2prefferedFileNameLabel_ifEnabled(file, fileCommonBall));
            }

            exeBeforeZip.run(fileHandler);
            if (fileHandler.isZipFileRequested()) {
                var zipableFiles = fileHandler.zipableFiles();
                if (zipableFiles.isEmpty()) {
                    d.ce("use", "zipableFiles.isEmpty()!");
                    return;
                }
                var pathZIP = fileHandler.pathZipFile();
                if (pathZIP == null) {
                    d.ce("use", "pathZIP == null");
                    return;
                }
                TS_FileZipUtils.zipList(zipableFiles, pathZIP);
                if (!TS_FileUtils.isExistFile(pathZIP)) {
                    d.ce("use", "!TS_FileUtils.isExistFile", pathZIP);
                    return;
                }
                exeAfterZip.run(fileHandler);
            }
        });
        return fileCommonBall.runReport;
    }
    
    private static void use_do(TS_FileCommonBall fileCommonBall, TGS_RunnableType1<TS_FileTmcrFileHandler> fileHandler) {
        var webWidthScalePercent = 68;
        var webFontHightPercent = 60;
        var webHTMLBase64 = false;
        var webHTMBase64 = true;
        var enableTMCR = fileCommonBall.requestedFileTypes.contains(TGS_FileTmcrTypes.FILE_TYPE_TMCR());
        var enableHTML = fileCommonBall.requestedFileTypes.contains(TGS_FileTmcrTypes.FILE_TYPE_HTML());
        var enableHTM = fileCommonBall.requestedFileTypes.contains(TGS_FileTmcrTypes.FILE_TYPE_HTM());
        var enablePDF = fileCommonBall.requestedFileTypes.contains(TGS_FileTmcrTypes.FILE_TYPE_PDF());
        var enableXLSX = fileCommonBall.requestedFileTypes.contains(TGS_FileTmcrTypes.FILE_TYPE_XLSX());
        var enableDOCX = fileCommonBall.requestedFileTypes.contains(TGS_FileTmcrTypes.FILE_TYPE_DOCX());
        var fileNameFullTMCR = fileCommonBall.fileNameLabel + TGS_FileTmcrTypes.FILE_TYPE_TMCR();
        var fileNameFullHTML = fileCommonBall.fileNameLabel + TGS_FileTmcrTypes.FILE_TYPE_HTML();
        var fileNameFullHTM = fileCommonBall.fileNameLabel + TGS_FileTmcrTypes.FILE_TYPE_HTM();
        var fileNameFullPDF = fileCommonBall.fileNameLabel + TGS_FileTmcrTypes.FILE_TYPE_PDF();
        var fileNameFullXLSX = fileCommonBall.fileNameLabel + TGS_FileTmcrTypes.FILE_TYPE_XLSX();
        var fileNameFullDOCX = fileCommonBall.fileNameLabel + TGS_FileTmcrTypes.FILE_TYPE_DOCX();
        var localfileTMCR = TS_FileTmcrFileSetName.path(fileCommonBall, fileNameFullTMCR);
        var localfileHTML = TS_FileTmcrFileSetName.path(fileCommonBall, fileNameFullHTML);
        var localfileHTM = TS_FileTmcrFileSetName.path(fileCommonBall, fileNameFullHTM);
        var localfilePDF = TS_FileTmcrFileSetName.path(fileCommonBall, fileNameFullPDF);
        var localfileXLSX = TS_FileTmcrFileSetName.path(fileCommonBall, fileNameFullXLSX);
        var localfileDOCX = TS_FileTmcrFileSetName.path(fileCommonBall, fileNameFullDOCX);
        var remotefileTMCR = TS_FileTmcrFileSetName.urlUser(fileCommonBall, fileNameFullTMCR, true);
        var remotefileHTML = TS_FileTmcrFileSetName.urlUser(fileCommonBall, fileNameFullHTML, false);
        var remotefileHTM = TS_FileTmcrFileSetName.urlUser(fileCommonBall, fileNameFullHTM, true);
        var remotefilePDF = TS_FileTmcrFileSetName.urlUser(fileCommonBall, fileNameFullPDF, true);
        var remotefileXLSX = TS_FileTmcrFileSetName.urlUser(fileCommonBall, fileNameFullXLSX, true);
        var remotefileDOCX = TS_FileTmcrFileSetName.urlUser(fileCommonBall, fileNameFullDOCX, true);
        TS_FileTmcrFileTMCR.use(enableTMCR, fileCommonBall, localfileTMCR, remotefileTMCR, tmcr -> {
            TS_FileHtml.use(enableHTML, fileCommonBall, localfileHTML, remotefileHTML, webHTMLBase64, webWidthScalePercent, webFontHightPercent, (webHTM, imageLoc) -> TS_FileTmcrFileSetName.urlFromPath(fileCommonBall, imageLoc), webHTML -> {
                TS_FileHtml.use(enableHTM, fileCommonBall, localfileHTM, remotefileHTM, webHTMBase64, webWidthScalePercent, webFontHightPercent, (webHTM, imageLoc) -> TS_FileTmcrFileSetName.urlFromPath(fileCommonBall, imageLoc), webHTM -> {
                    TS_FilePdf.use(enablePDF, fileCommonBall, localfilePDF, remotefilePDF, pdf -> {
                        TS_FileXlsx.use(enableXLSX, fileCommonBall, localfileXLSX, remotefileXLSX, xlsx -> {
                            TS_FileDocx.use(enableDOCX, fileCommonBall, localfileDOCX, remotefileDOCX, docx -> {
                                var instance = new TS_FileTmcrFileHandler(fileCommonBall,
                                        tmcr, webHTML, webHTM, pdf, xlsx, docx
                                );
                                fileHandler.run(instance);
                            });
                        });
                    });
                }
                );
            });
        }
        );
    }

    @Override
    public boolean saveFile(String errorSource) {
        TGS_UnSafe.run(() -> {
            if (errorSource != null) {
                fileCommonBall.fontColor = TS_FileCommonFontTags.CODE_TOKEN_FONT_COLOR_RED();
                beginText(0);
                addText(errorSource);
                endText();
                d.ce("saveFile", "WARNING: error message added to files", errorSource);
            }
        }, e -> TGS_StreamUtils.runNothing());
        var stream = PARALLEL ? files.parallelStream() : files.stream();
        stream.filter(mif -> mif.isEnabled()).forEach(mi -> {
            TGS_UnSafe.run(() -> {
                if (!mi.saveFile(errorSource)) {
                    d.ce("saveFile", "ERROR: cannot save mi.getLocalFileName:" + mi.getLocalFileName());
                }
            }, e -> d.ct("saveFile", e));
        });
        return fileCommonBall.runReport = true;
    }

    @Override
    public boolean createNewPage(int pageSizeAX, boolean landscape, Integer marginLeft, Integer marginRight, Integer marginTop, Integer marginBottom) {
        TGS_Tuple1<Boolean> result = new TGS_Tuple1(true);
        var stream = PARALLEL ? files.parallelStream() : files.stream();
        stream.filter(mif -> mif.isEnabled()).forEach(mi -> {
            var b = mi.createNewPage(pageSizeAX, landscape, marginLeft, marginRight, marginTop, marginBottom);
            if (!b) {
                result.value0 = false;
            }
        });
        if (!result.value0) {
            d.ce("createNewPage.CODE_INSERT_PAGE HAD ERRORS.");
            return false;
        }
        d.ci("  processed.");
        return true;//I know
    }

    @Override
    public boolean addImage(BufferedImage pstImage, Path pstImageLoc, boolean textWrap, int left0_center1_right2, long imageCounter) {
        TGS_Tuple1<Boolean> result = new TGS_Tuple1(true);
        var stream = PARALLEL ? files.parallelStream() : files.stream();
        stream.filter(mif -> mif.isEnabled()).forEach(mi -> {
            var b = mi.addImage(pstImage, pstImageLoc, textWrap, left0_center1_right2, imageCounter);
            if (!b) {
                result.value0 = false;
            }
        });
        if (!result.value0) {
            d.ce("addImage.CODE_INSERTIMAGE HAD ERRORS.");
            return false;
        }
        return result.value0;
    }

    @Override
    public boolean beginTableCell(int rowSpan, int colSpan, Integer cellHeight) {
        TGS_Tuple1<Boolean> result = new TGS_Tuple1(true);
        var stream = PARALLEL ? files.parallelStream() : files.stream();
        stream.filter(mif -> mif.isEnabled()).forEach(mi -> {
            var b = mi.beginTableCell(rowSpan, colSpan, cellHeight);
            if (!b) {
                result.value0 = false;
            }
        });
        if (!result.value0) {
            d.ce("beginTableCell.CODE_BEGIN_TABLECELL HAD ERRORS.");
            return false;
        }
        return result.value0;
    }

    @Override
    public boolean endTableCell(int rotationInDegrees_0_90_180_270) {
        TGS_Tuple1<Boolean> result = new TGS_Tuple1(true);
        var stream = PARALLEL ? files.parallelStream() : files.stream();
        stream.filter(mif -> mif.isEnabled()).forEach(mi -> {
            var b = mi.endTableCell(rotationInDegrees_0_90_180_270);
            if (!b) {
                result.value0 = false;
            }
        });
        if (!result.value0) {
            d.ce("endTableCell.END_TABLECELL HAD ERRORS.");
            return false;
        }
        d.ci("  processed.");
        return result.value0;
    }

    @Override
    public boolean beginTable(int[] relColSizes) {
        d.ci("beginTable", "#1");
        TGS_Tuple1<Boolean> result = new TGS_Tuple1(true);
        d.ci("beginTable", "#2");
        files.stream().filter(mif -> mif.isEnabled()).forEachOrdered(mi -> {
            d.ci("beginTable", "#3.nm", mi.getClass().getSimpleName());
            var b = mi.beginTable(relColSizes);
            d.ci("beginTable", "#3.b", mi.getClass().getSimpleName());
            if (!b) {
                d.ci("beginTable", "#3.s", mi.getClass().getSimpleName());
                result.value0 = false;
            }
        });
        d.ci("beginTable", "#4");
        d.ci("beginTable", "#5");
        if (!result.value0) {
            d.ce("beginTable", "END_TABLECELL HAD ERRORS.");
            return false;
        }
        d.ci("beginTable", "  processed.");
        return true;
    }

    @Override
    public boolean endTable() {
        TGS_Tuple1<Boolean> result = new TGS_Tuple1(true);
        var stream = PARALLEL ? files.parallelStream() : files.stream();
        stream.filter(mif -> mif.isEnabled()).forEach(mi -> {
            var b = mi.endTable();
            if (!b) {
                result.value0 = false;
            }
        });
        if (!result.value0) {
            d.ce("endTable.END_TABLE HAD ERRORS.");
            return false;
        }
        d.ci("  processed.");
        return true;
    }

    @Override
    public boolean beginText(int allign_Left0_center1_right2_just3) {
        TGS_Tuple1<Boolean> result = new TGS_Tuple1(true);
        var stream = PARALLEL ? files.parallelStream() : files.stream();
        stream.filter(mif -> mif.isEnabled()).forEach(mi -> {
            var b = mi.beginText(allign_Left0_center1_right2_just3);
            if (!b) {
                result.value0 = false;
            }
        });
        if (!result.value0) {
            d.ce("beginText.CODE_BEGIN_TEXT HAD ERRORS.");
            return false;
        }
        d.ci("  processed.");
        return true;
    }

    @Override
    public boolean endText() {
        TGS_Tuple1<Boolean> result = new TGS_Tuple1(true);
        var stream = PARALLEL ? files.parallelStream() : files.stream();
        stream.filter(mif -> mif.isEnabled()).forEach(mi -> {
            var b = mi.endText();
            if (!b) {
                result.value0 = false;
            }
        });
        if (!result.value0) {
            d.ce("endText.CODE_END_TEXT HAD ERRORS.");
            return false;
        }
        d.ci("  processed.");
        return true;
    }

    private final static List<String> colors = TGS_ListUtils.of();

    @Override
    public boolean addText(String mainText) {
        TGS_Tuple1<Boolean> result = new TGS_Tuple1(true);
        var tokens = TS_StringUtils.toList(mainText, "\n");
        IntStream.range(0, tokens.size()).forEachOrdered(i -> {
            var text = tokens.get(i);
            if (!text.isEmpty()) {
                var b = addText2(text);
                if (!b) {
                    result.value0 = false;
                }
            }
            if (i != tokens.size() - 1 || mainText.endsWith("\n")) {
                var b = addLineBreak();
                if (!b) {
                    result.value0 = false;
                }
            }
        });
        return result.value0;
    }

    private boolean addText2(String mainText) {
        d.ci("addText2", "mainText", mainText);
        if (colors.isEmpty()) {
            colors.add(TS_FileCommonFontTags.CODE_TOKEN_FONT_COLOR_BLACK());
            colors.add(TS_FileCommonFontTags.CODE_TOKEN_FONT_COLOR_RED());
            colors.add(TS_FileCommonFontTags.CODE_TOKEN_FONT_COLOR_YELLOW());
            colors.add(TS_FileCommonFontTags.CODE_TOKEN_FONT_COLOR_BLUE());
            colors.add(TS_FileCommonFontTags.CODE_TOKEN_FONT_COLOR_GREEN());
            colors.add(TS_FileCommonFontTags.CODE_TOKEN_FONT_COLOR_PINK());
            colors.add(TS_FileCommonFontTags.CODE_TOKEN_FONT_COLOR_ORANGE());
            colors.add(TS_FileCommonFontTags.CODE_TOKEN_FONT_COLOR_CYAN());
            colors.add(TS_FileCommonFontTags.CODE_TOKEN_FONT_COLOR_DARK_GRAY());
            colors.add(TS_FileCommonFontTags.CODE_TOKEN_FONT_COLOR_GRAY());
            colors.add(TS_FileCommonFontTags.CODE_TOKEN_FONT_COLOR_LIGHT_GRAY());
            colors.add(TS_FileCommonFontTags.CODE_TOKEN_FONT_COLOR_MAGENTA());
        }
        TGS_Tuple1<Boolean> result = new TGS_Tuple1(true);
        mainText = mainText.replace("{fc_", "{FC_");
        mainText = mainText.replace("{fh_", "{FH_");
        mainText = mainText.replace("{b}", "{B}");
        mainText = mainText.replace("{p}", "{P}");
        mainText = mainText.replace("{i}", "{I}");
        d.ci("addText2.mainText:[" + mainText + "]");
        var plainArr = TS_StringUtils.toList(mainText, "{P}");
        for (var plainArr_i = 0; plainArr_i < plainArr.size(); plainArr_i++) {
            var plainText = plainArr.get(plainArr_i);
            d.ci("addText2.mainText.plainText[" + plainArr_i + "]:[" + plainText + "]");
            if (plainArr_i != 0) {
                fileCommonBall.fontItalic = false;
                fileCommonBall.fontBold = false;
                var b = setFontStyle();
                if (!b) {
                    result.value0 = false;
                }
            }
            var boldArr = TS_StringUtils.toList(plainText, "{B}");
            for (var boldArr_i = 0; boldArr_i < boldArr.size(); boldArr_i++) {
                var boldText = boldArr.get(boldArr_i);
                d.ci("addText2.mainText.plainText[" + plainArr_i + "].boldText[" + boldArr_i + "]:[" + boldText + "]");
                if (boldArr_i != 0) {
                    fileCommonBall.fontBold = true;
                    var b = setFontStyle();
                    if (!b) {
                        result.value0 = false;
                    }
                }
                var italicArr = TS_StringUtils.toList(boldText, "{I}");
                for (var italicArr_i = 0; italicArr_i < italicArr.size(); italicArr_i++) {
                    var italicText = italicArr.get(italicArr_i);
                    d.ci("addText2.mainText.plainText[" + plainArr_i + "].boldText[" + boldArr_i + "].italicText[" + italicArr_i + "]:[" + italicText + "]");
                    if (italicArr_i != 0) {
                        fileCommonBall.fontItalic = true;
                        var b = setFontStyle();
                        if (!b) {
                            result.value0 = false;
                        }
                    }
                    var fontColorArr = TS_StringUtils.toList(italicText, "{FC_");
                    for (var fontColorArr_i = 0; fontColorArr_i < fontColorArr.size(); fontColorArr_i++) {
                        var fontColorText = fontColorArr.get(fontColorArr_i);
                        d.ci("addText2.mainText.plainText[" + plainArr_i + "].boldText[" + boldArr_i + "].italicText[" + italicArr_i + "].colorText[" + fontColorArr_i + "]:[" + fontColorText + "]");
                        if (fontColorArr_i != 0) {
                            var i = fontColorText.indexOf("}");
                            if (i != -1) {
                                var fontColor = TGS_CharSetCast.toLocaleUpperCase(fontColorText.substring(0, i));
                                d.ci("addText2.fontColor to be parsed: [" + fontColor + "]");
                                fontColorText = fontColorText.substring(i + 1);
                                var found = false;
                                for (var cti = 0; cti < colors.size(); cti++) {
                                    if (colors.get(cti).equals(fontColor)) {
                                        fileCommonBall.fontColor = colors.get(cti);
                                        var b = setFontColor();
                                        if (!b) {
                                            result.value0 = false;
                                        }
                                        found = true;
                                        break;
                                    }
                                }
                                if (!found) {
                                    d.ci("addText2.fontColorText[" + fontColor + "] cannot be processed. BLACK will be used isntead");
                                    fileCommonBall.fontColor = colors.get(0);
                                    var b = setFontColor();
                                    if (!b) {
                                        result.value0 = false;
                                    }
                                }
                            }
                        }
                        var fontHeightArr = TS_StringUtils.toList(fontColorText, "{FH_");
                        for (var fontHeightArr_i = 0; fontHeightArr_i < fontHeightArr.size(); fontHeightArr_i++) {
                            var fontHeightText = fontHeightArr.get(fontHeightArr_i);
                            d.ci("addText2.mainText.plainText[" + plainArr_i + "].boldText[" + boldArr_i + "].italicText[" + italicArr_i + "].colorText[" + fontColorArr_i + "].fontHeightText[" + fontHeightArr_i + "]:[" + fontHeightText + "]");
                            if (fontHeightArr_i != 0) {
                                var i = fontHeightText.indexOf("}");
                                if (i != -1) {
                                    var fontHeight = fontHeightText.substring(0, i);
                                    d.ci("addText2.fontHeight to be parsed: [" + fontHeight + "]");
                                    var fsInt = TGS_CastUtils.toInteger(fontHeight);
                                    if (fsInt == null) {
                                        d.ci("addText2.fontHeight[" + fontHeight + "] cannot be processed. 10 will be used instead.");
                                        fontHeightText = fontHeightText.substring(i + 1);
                                        fileCommonBall.fontHeight = 10;
                                        var b = setFontHeight();
                                        if (!b) {
                                            result.value0 = false;
                                        }
                                    } else {
                                        fontHeightText = fontHeightText.substring(i + 1);
                                        fileCommonBall.fontHeight = fsInt;
                                        var b = setFontHeight();
                                        if (!b) {
                                            result.value0 = false;
                                        }
                                    }
                                }
                            }
                            result.value0 = result.value0 && addText3(fontHeightText);
                        }
                    }
                }
            }
        }
        return result.value0;
    }

    private boolean addText3(String text) {
        TGS_Tuple1<Boolean> result = new TGS_Tuple1(true);
        var stream = PARALLEL ? files.parallelStream() : files.stream();
        stream.filter(mif -> mif.isEnabled()).forEach(mi -> {
            var b = mi.addText(text);
            if (!b) {
                result.value0 = false;
            }
        });
        if (!result.value0) {
            d.ce("addText3.CODE_ADD_TEXT HAD ERRORS.");
            return false;
        }
        d.ci("  processed.");
        return true;
    }

    @Override
    public boolean addLineBreak() {
        TGS_Tuple1<Boolean> result = new TGS_Tuple1(true);
        var stream = PARALLEL ? files.parallelStream() : files.stream();
        stream.filter(mif -> mif.isEnabled()).forEach(mi -> {
            var b = mi.addLineBreak();
            if (!b) {
                result.value0 = false;
            }
        });
        if (!result.value0) {
            d.ce("addLineBreak.CODE_ADD_TEXT_HR HAD ERRORS.");
            return false;
        }
        d.ci("  processed.");
        return true;
    }

    @Override
    public boolean setFontStyle() {
        TGS_Tuple1<Boolean> result = new TGS_Tuple1(true);
        var stream = PARALLEL ? files.parallelStream() : files.stream();
        stream.filter(mif -> mif.isEnabled()).forEach(mi -> {
            var b = mi.setFontStyle();
            if (!b) {
                result.value0 = false;
            }
        });
        if (!result.value0) {
            d.ce("setFontStyle.CODE_SET_FONT_STYLE HAD ERRORS.");
            return false;
        }
        d.ci("  processed.");
        return true;
    }

    @Override
    public boolean setFontHeight() {
        TGS_Tuple1<Boolean> result = new TGS_Tuple1(true);
        var stream = PARALLEL ? files.parallelStream() : files.stream();
        stream.filter(mif -> mif.isEnabled()).forEach(mi -> {
            var b = mi.setFontHeight();
            if (!b) {
                result.value0 = false;
            }
        });
        if (!result.value0) {
            d.ce("setFontHeight.CODE_SET_FONT_SIZE HAD ERRORS.");
            return false;
        }
        d.ci("  processed.");
        return true;
    }

    @Override
    public boolean setFontColor() {
        TGS_Tuple1<Boolean> result = new TGS_Tuple1(true);
        var stream = PARALLEL ? files.parallelStream() : files.stream();
        stream.filter(mif -> mif.isEnabled()).forEach(mi -> {
            var b = mi.setFontColor();
            if (!b) {
                result.value0 = false;
            }
        });
        if (!result.value0) {
            d.ce("setFontColor.CODE_SET_FONT_COLOR HAD ERRORS.");
            return false;
        }
        d.ci("  processed.");
        return true;
    }

}
