package xsightassembler.utils;

import javafx.collections.ObservableList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import xsightassembler.models.*;

import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.*;

public class ExcelReportGenerator2 {
    private static final Logger LOGGER = LogManager.getLogger(ExcelReportGenerator.class.getName());

    private final ObservableList<Pallet> reportData;
    private final String start;
    private final String stop;
    private File reportFile;
    private XSSFWorkbook workbook;
    private XSSFSheet sheet;
    private Map<String, CellStyle> styles;
    private HashMap<String, String[]> columnsMap;
    private HashMap<String, String> manufMap;
    private Cell cell;


    public ExcelReportGenerator2(LocalDate start, LocalDate stop, ObservableList<Pallet> reportData) {
        this.reportData = reportData;
        this.start = Utils.localDateToString(start);
        this.stop = Utils.localDateToString(stop);
        this.workbook = new XSSFWorkbook();
        this.styles = createStyles(workbook);
    }

    public boolean assemblyReportToExcell() {
        String TITLE = "Xsight production report";
        String reportFolder = "./Reports/";
        String fileName = "Xsight_report.xlsx";

        manufMap = Utils.getStringMap("isduh_manuf");

        String[] sheets = new String[]{"ISDUH", "BowlModule", "AzimutModule", "UpperSensorModule", "NoseModule", "CameraModule"};
        String[] isduhColumns = new String[]{"#", "ISDUH", "Fan", "Bowl", "Azimut", "UpperSensor", "Nose",
                "Camera", "CameraHouse", "Radar", "Pallet"};
        String[] bowlColumns = new String[]{"#", manufMap.get("manufBowlModule"), manufMap.get("manufComEx"),
                manufMap.get("manufBreakable"), manufMap.get("manufCarrier")};
        String[] azimutColumns = new String[]{"#", manufMap.get("manufAzimutModule"), manufMap.get("manufTop"),
                manufMap.get("manufBoard")};
        String[] upperSensorColumns = new String[]{"#", manufMap.get("manufUpperSensorModule"), manufMap.get("manufCooler")};
        String[] noseColumns = new String[]{"#", manufMap.get("manufNoseModule")};
        String[] cameraColumns = new String[]{"#", manufMap.get("manufCameraModule"), manufMap.get("manufCameraHouse"),
                manufMap.get("manufCamera"), manufMap.get("manufMcu")};

        columnsMap = new HashMap<>();
        columnsMap.put("ISDUH", isduhColumns);
        columnsMap.put("BowlModule", bowlColumns);
        columnsMap.put("AzimutModule", azimutColumns);
        columnsMap.put("UpperSensorModule", upperSensorColumns);
        columnsMap.put("NoseModule", noseColumns);
        columnsMap.put("CameraModule", cameraColumns);



        String[] titleRows = {TITLE, String.format("from %s to %s", start, stop)};
        createNewSheet("ISDUH", null, isduhColumns);


        for (String s : sheets) {
            createNewSheet(s, null, columnsMap.get(s));
        }

        for (Pallet pallet : reportData) {
            for (Isduh assembly: pallet.getIsduhList()){
                fillData(pallet, assembly);
                fillData(pallet, assembly.getBowlModule() == null ? "": assembly.getBowlModule());
                fillData(pallet, assembly.getAzimutModule() == null ? "": assembly.getAzimutModule());
                fillData(pallet, assembly.getUpperSensorModule() == null ? "": assembly.getUpperSensorModule());
                fillData(pallet, assembly.getNoseModule() == null ? "": assembly.getNoseModule());
                fillData(pallet, assembly.getCameraModule() == null ? "": assembly.getCameraModule());
            }
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

    public void fillData(Pallet pallet, Object o) {
        try {
            sheet = workbook.getSheet(o.getClass().getSimpleName());
            int dataRowNum = sheet.getLastRowNum();
            if (o.getClass() == Isduh.class) {
                fillIsduhRow((Isduh) o, dataRowNum, pallet);
            }
        } catch (NullPointerException ignore){

        }
    }

    private void fillIsduhRow(Isduh assembly, int dataRowNum, Pallet pallet) {
        Row dataRow = sheet.createRow(dataRowNum + 1);
        int count = dataRowNum; //- 4;
        setDataCell(dataRow, Integer.toString(count));
        setDataCell(dataRow, assembly.getSn());

        setDataCell(dataRow, assembly.getFanModuleSn());
        setDataCell(dataRow, assembly.getBowlModuleSn());
        setDataCell(dataRow, assembly.getAzimutModuleSn());
        setDataCell(dataRow, assembly.getUpperSensorModuleSn());
        setDataCell(dataRow, assembly.getNoseModuleSn());
        setDataCell(dataRow, assembly.getCameraModuleSn());
        setDataCell(dataRow, assembly.getHouseSn());
        setDataCell(dataRow, assembly.getRadarModuleSn());
        setDataCell(dataRow, pallet.getPalletNumber());


        fillModuleRow(count, assembly.getBowlModule());
        fillModuleRow(count, assembly.getAzimutModule());
        fillModuleRow(count, assembly.getUpperSensorModule());
        fillModuleRow(count, assembly.getNoseModule());
        fillModuleRow(count, assembly.getCameraModule());
        fillModuleRow(count, assembly.getRadarModule());
    }

    private void fillModuleRow(int count, Object o) {
        if (o == null){
            return;
        }
        String name = o.getClass().getSimpleName();
        sheet = workbook.getSheet(name);
        if (sheet == null) {
            return;
        }
        int dataRowNum = sheet.getLastRowNum();
        Row dataRow = sheet.createRow(dataRowNum + 1);
        setDataCell(dataRow, Integer.toString(count));
        if (o instanceof BowlModule) {
            BowlModule bowl = (BowlModule) o;
            setDataCell(dataRow, bowl.getModule());
            setDataCell(dataRow, bowl.getComEx());
            setDataCell(dataRow, bowl.getBreakable());
            setDataCell(dataRow, bowl.getCarrier());
        }
        if (o instanceof AzimutModule) {
            AzimutModule azimut = (AzimutModule) o;
            setDataCell(dataRow, azimut.getModule());
            setDataCell(dataRow, azimut.getTop());
            setDataCell(dataRow, azimut.getBoard());
        }
        if (o instanceof UpperSensorModule) {
            UpperSensorModule upper = (UpperSensorModule) o;
            setDataCell(dataRow, upper.getModule());
            setDataCell(dataRow, upper.getCooler());
        }
        if (o instanceof NoseModule) {
            NoseModule nose = (NoseModule) o;
            setDataCell(dataRow, nose.getModule());
        }
        if (o instanceof CameraModule) {
            CameraModule camera = (CameraModule) o;
            setDataCell(dataRow, camera.getModule());
            setDataCell(dataRow, camera.getCameraHouse());
            setDataCell(dataRow, camera.getCamera());
            setDataCell(dataRow, camera.getMcu());
        }
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


    public File getReportFile(){
        return reportFile;
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

