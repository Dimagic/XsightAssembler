package xsightassembler.utils;

import javafx.collections.ObservableList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import xsightassembler.models.History;
import xsightassembler.models.Isduh;
import xsightassembler.models.User;

import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.*;

import static xsightassembler.utils.Strings.*;

public class ExcelReportGenerator {
    private static final Logger LOGGER = LogManager.getLogger(ExcelReportGenerator.class.getName());

    private final ObservableList<Isduh> reportData;
    private final String start;
    private final String stop;
    private File reportFile;
    private XSSFWorkbook workbook;
    private XSSFSheet sheet;
    private Map<String, CellStyle> styles;
    private HashMap<String, String[]> columnsMap;
    private Cell cell;


    public ExcelReportGenerator(LocalDate start, LocalDate stop, ObservableList<Isduh> reportData) {
        this.reportData = reportData;
        this.start = Utils.localDateToString(start);
        this.stop = Utils.localDateToString(stop);
        this.workbook = new XSSFWorkbook();
        this.styles = createStyles(workbook);
    }

    private XSSFSheet createNewSheet(String name, String[] titleRows, String[] titlesColumn) {
        Row row;
        int rowNum = 0;
        sheet = workbook.getSheet(name);
        if (sheet != null) {
            return sheet;
        }

        sheet = workbook.createSheet(name);
        if (titleRows != null) {
            for (String s : titleRows) {
                /*
                create the report head
                */
                sheet.addMergedRegion(new CellRangeAddress(rowNum, rowNum, 0, titlesColumn.length - 1));
                row = sheet.createRow(rowNum);
                cell = row.createCell(0);
                cell.setCellValue(s);
                cell.setCellStyle(styles.get("reportHeader"));
                rowNum++;
            }
        }
        if (rowNum > 0) {
            rowNum += 2;
        }
        /*
          create column header row
         */
        HashMap<String, Field> stringsValuesMap = Utils.getStringsValuesHashMap();
        Field field;
        String tmp;
        Row headerRow = sheet.createRow(rowNum);
        headerRow.setHeightInPoints(12.75f);
        for (int i = 0; i < titlesColumn.length; i++) {
            cell = headerRow.createCell(i);
            tmp = String.format("manuf%sModule", titlesColumn[i]);
            field = stringsValuesMap.get(tmp);
            if (field == null) {
                tmp = String.format("manuf%s", titlesColumn[i]);
                field = stringsValuesMap.get(tmp);
            }
            if (titlesColumn[i].equals("Module")) {
                tmp = String.format("manuf%s", sheet.getSheetName());
                field = stringsValuesMap.get(tmp);
            }
            if (field != null) {
                try {
                    cell.setCellValue(field.get(Utils.class).toString());
                } catch (IllegalAccessException e) {
                    LOGGER.error("Exception", e);
                    MsgBox.msgError(e.getLocalizedMessage());
                }
            }
            cell.setCellStyle(styles.get("tableTitle"));
        }

        rowNum += 1;
        headerRow = sheet.createRow(rowNum);
        headerRow.setHeightInPoints(12.75f);
        for (int i = 0; i < titlesColumn.length; i++) {
            cell = headerRow.createCell(i);
            cell.setCellValue(titlesColumn[i]);
            cell.setCellStyle(styles.get("tableTitle"));
        }
        return sheet;
    }

    public void fillData(Object o) {
        try {
            sheet = workbook.getSheet(o.getClass().getSimpleName());
            int dataRowNum = sheet.getLastRowNum();
            if (o.getClass() == Isduh.class) {
                fillIsduhRow((Isduh) o, dataRowNum);
            }
        } catch (NullPointerException ignore){

        }
    }

    private void fillIsduhRow(Isduh assembly, int dataRowNum) {
        Row dataRow = sheet.createRow(dataRowNum + 1);
        int count = dataRowNum - 4;
        setDataCell(dataRow, Integer.toString(count));
        setDataCell(dataRow, assembly.getFormattedDate());
        setDataCell(dataRow, assembly.getSn());

        setHyperLink(dataRowNum - 2, dataRow, "AzimutModule", assembly.getUpperSensorModule().getAzimutModuleSn());
        fillModuleRow(count, assembly.getUpperSensorModule().getAzimutModule());

        setHyperLink(dataRowNum - 2, dataRow, "BowlModule", assembly.getBowlModuleSn());
        fillModuleRow(count, assembly.getBowlModule());

        setHyperLink(dataRowNum - 2, dataRow, "CameraModule", assembly.getUpperSensorModule().getCameraModuleSn());
        fillModuleRow(count, assembly.getUpperSensorModule().getCameraModule());

        setHyperLink(dataRowNum - 2, dataRow, "FanModule", assembly.getFanModuleSn());
        fillModuleRow(count, assembly.getFanModule());

        setHyperLink(dataRowNum - 2, dataRow, "NoseModule", assembly.getUpperSensorModule().getNoseModuleSn());
        fillModuleRow(count, assembly.getUpperSensorModule().getNoseModule());

        setHyperLink(dataRowNum - 2, dataRow, "RadarModule", assembly.getUpperSensorModule().getRadarModuleSn());
        fillModuleRow(count, assembly.getUpperSensorModule().getRadarModule());

        setHyperLink(dataRowNum - 2, dataRow, "UpperSensorModule", assembly.getUpperSensorModuleSn());
        fillModuleRow(count, assembly.getUpperSensorModule());

        setDataCell(dataRow, assembly.getUserLogin());

        Set<History> historySet = assembly.getHistorySet();
        if (historySet.size() != 0) {
            addHistorySheet("ISDUH", assembly.getSn(), historySet);
            setHyperLink(1, dataRow, assembly.getSn(), "View");
        } else {
            setDataCell(dataRow, "");
        }

    }

    private boolean isModule(String s) {
        for (int i = 0; i < sheets.length; i++) {
            if (sheets[i].contains(s)) {
                return true;
            }
        }
        return false;
    }

    private void fillModuleRow(int count, Object o) {
        if (o == null){
            return;
        }
        String name = o.getClass().getSimpleName();
        sheet = workbook.getSheet(name);
        int dataRowNum = sheet.getLastRowNum();
        Row dataRow = sheet.createRow(dataRowNum + 1);

        for (String s : columnsMap.get(name)) {
            try {
                if (s.equalsIgnoreCase("No")) {
                    setDataCell(dataRow, Integer.toString(count));
                    continue;
                } else if (s.equalsIgnoreCase("History")) {
                    Set<History> historySet = (Set<History>) o.getClass().getMethod("getHistorySet").invoke(o);
                    if (historySet.size() != 0) {
                        String sn = (String) o.getClass().getMethod("getModule").invoke(o);
                        addHistorySheet(Utils.setFirstCharToUpper(o.getClass().getSimpleName()), sn, historySet);
                        setHyperLink(1, dataRow, sn, "View");
                    } else {
                        setDataCell(dataRow, "");
                    }
                } else {
                    Object objVal = o.getClass().getMethod("get" + s).invoke(o);
                    String val;
                    if (objVal.getClass() == Timestamp.class) {
                        val = Utils.getFormattedDate((Date) objVal);
                    } else if (objVal.getClass().getSimpleName().contains("User")) {
                        val = ((User) objVal).getLogin();
                    } else {
                        val = (String) objVal;
                    }
                    setDataCell(dataRow, val);
                }
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean assemblyReportToExcell() {
        String TITLE = "Xsight production report";
        String reportFolder = "./Reports/";
        String fileName = "Xsight_report.xlsx";

        String[][] columns = {azimutColumns, bowlColumns, cameraColumns, fanColumns, noseColumns, radarColumns, upperSensorColumns};
        columnsMap = new HashMap<>();
        columnsMap.put("Isduh", isduhColumns);
        columnsMap.put("AzimutModule", azimutColumns);
        columnsMap.put("BowlModule", bowlColumns);
        columnsMap.put("CameraModule", cameraColumns);
        columnsMap.put("FanModule", fanColumns);
        columnsMap.put("NoseModule", noseColumns);
        columnsMap.put("RadarModule", radarColumns);
        columnsMap.put("UpperSensorModule", upperSensorColumns);


        String[] titleRows = {TITLE, String.format("from %s to %s", start, stop)};
        createNewSheet("ISDUH", titleRows, isduhColumns);


        for (String s : sheets) {
            createNewSheet(s, null, columnsMap.get(s));
        }

        for (Isduh assembly : reportData) {
            fillData(assembly);
            fillData(assembly.getAzimutModule() == null ? "": assembly.getUpperSensorModule().getAzimutModule());
            fillData(assembly.getBowlModule() == null ? "": assembly.getBowlModule() );
            fillData(assembly.getCameraModule() == null ? "": assembly.getUpperSensorModule().getCameraModule());
            fillData(assembly.getFanModule() == null ? "": assembly.getFanModule());
            fillData(assembly.getNoseModule() == null ? "": assembly.getUpperSensorModule().getNoseModule());
            fillData(assembly.getRadarModule() == null ? "": assembly.getUpperSensorModule().getRadarModule());
            fillData(assembly.getUpperSensorModule() == null ? "": assembly.getUpperSensorModule());
        }

        for (String s : columnsMap.keySet()) {
            for (int j = 0; j < 50; j++) {
                workbook.getSheet(s).autoSizeColumn(j);
            }
        }

        File f = new File(reportFolder);
        if (!f.exists()) {
            if (!f.mkdirs()) {
                MsgBox.msgError(String.format("Cant't create folder: %s", reportFolder));
                return false;
            }
        }

        try (FileOutputStream outputStream = new FileOutputStream(String.format("%s%s", reportFolder, fileName))) {
            workbook.write(outputStream);
            reportFile = new File(String.format("%s%s", reportFolder, fileName));
            Desktop.getDesktop().open(reportFile);
        } catch (IOException e) {
            MsgBox.msgWarning(e.getLocalizedMessage());
            return false;
        }
        return true;
    }

    public File getReportFile(){
        return reportFile;
    }

    private void setDataCell(Row dataRow, String value) {
        int cellNum = dataRow.getFirstCellNum();
        if (cellNum == -1) {
            cellNum = 0;
        } else {
            cellNum = dataRow.getLastCellNum();
        }
        cell = dataRow.createCell(cellNum);
        cell.setCellStyle(styles.get("dataCell_center"));
        cell.setCellValue(value);
    }

    private void setHyperLink(int rowNum, Row dataRow, String page, String data) {
        int cellNum = dataRow.getFirstCellNum();
        if (cellNum == -1) {
            cellNum = 0;
        } else {
            cellNum = dataRow.getLastCellNum();
        }
        cell = dataRow.createCell(cellNum);
        cell.setCellValue(data);
        Hyperlink link = workbook.getCreationHelper().createHyperlink(Hyperlink.LINK_DOCUMENT);
        link.setAddress(String.format("'%s'!C%d", page, rowNum));
        cell.setHyperlink(link);
        cell.setCellStyle(styles.get("hyperlink"));
    }

    private void addHistorySheet(String name, String boardSn, Set<History> historySet) {
        XSSFSheet sheet = workbook.createSheet(boardSn);
        String TITLE = String.format("%s SN: %s history report", name, boardSn);
        String[] titles = {"Date", "Field", "Old value", "New value", "User"};

        /*
          create the history head
         */
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, titles.length - 1));
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, titles.length - 1));
        Row reportNameRow = sheet.createRow(0);
        cell = reportNameRow.createCell(0);
        cell.setCellValue(TITLE);
        cell.setCellStyle(styles.get("reportHeader"));

        /*
          create the header row
         */
        int headerRowNum = 2;
        Row headerRow = sheet.createRow(headerRowNum);
        headerRow.setHeightInPoints(12.75f);

        LinkedList<History> sortedHistory = new LinkedList<>(historySet);
        Collections.sort(sortedHistory);
        for (int i = 0; i < titles.length; i++) {
            cell = headerRow.createCell(i);
            cell.setCellValue(titles[i]);
            cell.setCellStyle(styles.get("tableTitle"));
        }

        /*
          filling history data
         */
        int dataRowNum = headerRowNum;
        Row dataRow;
        for (History history : sortedHistory) {
            dataRowNum += 1;
            dataRow = sheet.createRow(dataRowNum);
            setDataCell(dataRow, Utils.getFormattedDate(history.getDate()));
            setDataCell(dataRow, history.getFieldChange());
            setDataCell(dataRow, history.getOldValue());
            setDataCell(dataRow, history.getNewValue());
            setDataCell(dataRow, history.getUserLogin());
        }

        for (int j = 0; j < titles.length; j++) {
            sheet.autoSizeColumn(j);
        }
    }

    /*
     * create a library of cell styles
     */
    private Map<String, CellStyle> createStyles(Workbook wb) {
        Map<String, CellStyle> styles = new HashMap<>();
        CellStyle style;
        Font font;

        font = wb.createFont();
        font.setBoldweight(Font.BOLDWEIGHT_BOLD);
        style = wb.createCellStyle();
        style.setAlignment(CellStyle.ALIGN_CENTER);
        style.setFont(font);
        styles.put("reportHeader", style);

        font = wb.createFont();
        font.setBoldweight(Font.BOLDWEIGHT_BOLD);
        style = createBorderedStyle(wb);
        style.setAlignment(CellStyle.ALIGN_CENTER);
        style.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
        style.setFillPattern(CellStyle.SOLID_FOREGROUND);
        style.setFont(font);
        styles.put("tableTitle", style);

        font = wb.createFont();
        font.setBoldweight(Font.BOLDWEIGHT_NORMAL);
        style = createBorderedStyle(wb);
        style.setAlignment(CellStyle.ALIGN_CENTER);
        style.setFont(font);
        styles.put("dataCell_center", style);

        font = wb.createFont();
        font.setBoldweight(Font.BOLDWEIGHT_BOLD);
        style = createBorderedStyle(wb);
        style.setAlignment(CellStyle.ALIGN_RIGHT);
        style.setFont(font);
        styles.put("dataCell_right_bold", style);

        font = wb.createFont();
        font.setBoldweight(Font.BOLDWEIGHT_BOLD);
        font.setColor(IndexedColors.BLUE.getIndex());
        style = createBorderedStyle(wb);
        style.setAlignment(CellStyle.ALIGN_CENTER);
        style.setFont(font);
        styles.put("hyperlink", style);

        return styles;
    }

    private CellStyle createBorderedStyle(Workbook wb) {
        short thin = 1;
        short black = IndexedColors.BLACK.getIndex();

        CellStyle style = wb.createCellStyle();
        style.setBorderRight(thin);
        style.setRightBorderColor(black);
        style.setBorderBottom(thin);
        style.setBottomBorderColor(black);
        style.setBorderLeft(thin);
        style.setLeftBorderColor(black);
        style.setBorderTop(thin);
        style.setTopBorderColor(black);
        return style;
    }
}

