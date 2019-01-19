package com.taf.automation.ui.support;

import com.codoid.products.fillo.Connection;
import com.codoid.products.fillo.Recordset;
import com.taf.automation.ui.support.csv.CsvOutputRecord;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.FilenameUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Utilities for Fillo library
 */
public class FilloUtils {
    private FilloUtils() {
        // Prevent initialization of class as all public methods should be static
    }

    /**
     * Get Connection to Excel file
     *
     * @param resourceFilePath - Resource File Path (or actual file system path) to the Excel file to read
     * @return Connection
     */
    public static Connection getConnection(String resourceFilePath) {
        Connection connection;

        try {
            FileInputStream file = openFileInputStream(resourceFilePath);
            assertThat("Could not open stream - " + resourceFilePath, file, notNullValue());

            Workbook workbook = getWorkbook(file, resourceFilePath);
            assertThat("Unable to connect workbook - " + resourceFilePath, workbook, notNullValue());

            connection = new Connection(workbook, file, resourceFilePath, true);
        } catch (Exception ignore) {
            connection = null;
        }

        assertThat("Workbook is not found - " + resourceFilePath, connection, notNullValue());
        return connection;
    }

    /**
     * Open FileInputStream from resource file and if that fails try from the actual file system
     *
     * @param resourceFilePath - Resource File Path (or actual file system path) to the excel file to read
     * @return null if cannot open else FileInputStream
     */
    private static FileInputStream openFileInputStream(String resourceFilePath) {
        FileInputStream fileInputStream = null;

        try {
            File file = Helper.getFile(resourceFilePath);
            fileInputStream = new FileInputStream(file);
        } catch (Exception ex) {
            //
        }

        return fileInputStream;
    }

    private static Workbook getWorkbook(InputStream in, String filePath) {
        Workbook workbook;

        try {
            if (FilenameUtils.getExtension(filePath).equalsIgnoreCase("XLS")) {
                workbook = new HSSFWorkbook(in);
            } else if (FilenameUtils.getExtension(filePath).equalsIgnoreCase("XLSX")) {
                workbook = new XSSFWorkbook(in);
            } else if (FilenameUtils.getExtension(filePath).equalsIgnoreCase("XLSM")) {
                workbook = new XSSFWorkbook(OPCPackage.open(in));
            } else {
                workbook = null;
            }
        } catch (Exception ignore) {
            workbook = null;
        }

        return workbook;
    }

    /**
     * Gets all the data from a specific Excel Worksheet
     *
     * @param resourceFilePath - Resource File Path (or actual file system path) to the Excel file to read
     * @param workSheet        - Excel Worksheet to read from
     * @return String[][]
     */
    @SuppressWarnings("squid:S2259")
    public static String[][] getAllData(String resourceFilePath, String workSheet) {
        String[][] data;
        String error = "";

        try {
            Connection connection = getConnection(resourceFilePath);
            Recordset records = connection.executeQuery("select * from \"" + workSheet + "\"");
            int size = records.getCount();
            List<String> headers = records.getFieldNames();
            data = new String[size][headers.size()];
            int i = 0;
            while (records.next()) {
                for (int j = 0; j < headers.size(); j++) {
                    data[i][j] = records.getField(headers.get(j));
                }

                i++;
            }
        } catch (Exception ex) {
            error = ex.getMessage();
            data = null;
        }

        assertThat("Could not load excel file due to error:  " + error, data, notNullValue());
        return data;
    }

    /**
     * Get Excel records from the specified resource and update the records parameter and headers parameter<BR>
     * <B>Note: </B> The CsvTestData class is being re-used to store the data
     *
     * @param resourceFilePath - Resource File Path (or actual file system path) to the Excel file to read
     * @param workSheet        - Excel Worksheet to read from
     * @param records          - Updated with the Excel records
     * @param headers          - Updated with the Excel header record
     */
    @SuppressWarnings("squid:S2259")
    public static void read(
            String resourceFilePath,
            String workSheet,
            List<CSVRecord> records,
            Map<String, Integer> headers
    ) {
        try {
            Connection connection = getConnection(resourceFilePath);
            Recordset excelRecords = connection.executeQuery("select * from \"" + workSheet + "\"");

            List<String> excelHeaders = excelRecords.getFieldNames();
            for (int i = 0; i < excelHeaders.size(); i++) {
                headers.put(excelHeaders.get(i), i);
            }

            Constructor<CSVRecord> constructor = CSVRecord.class
                    .getDeclaredConstructor(String[].class, Map.class, String.class, long.class, long.class);
            constructor.setAccessible(true);
            int index = 0;
            while (excelRecords.next()) {
                String comment = "";
                long recordNumber = index;
                long characterPosition = index;
                String[] values = new String[excelHeaders.size()];
                for (int i = 0; i < excelHeaders.size(); i++) {
                    values[i] = excelRecords.getField(excelHeaders.get(i));
                }

                CSVRecord record = constructor.newInstance(values, headers, comment, recordNumber, characterPosition);
                records.add(record);
                index++;
            }
        } catch (Exception ex) {
            assertThat("Could not read records from Excel file due to error:  " + ex.getMessage(), false);
        }
    }

    /**
     * Write to an Excel (xlsx) file<BR>
     * <B>Note: </B> When appending, if there are files with lists, then it is necessary to ensure the totals are
     * the max between the existing file and records to be appended.
     *
     * @param filename      - Location &amp; File to create or append
     * @param workSheet     - Excel Worksheet to create or append the data
     * @param append        - true to append to an existing file, false to create a new file
     * @param headers       - Headers to be written if creating a new file
     * @param outputRecords - Records to be appended or created
     * @param totals        - For each list in the object, the total number of items there needs to be
     */
    public static void writeToExcel(
            String filename,
            String workSheet,
            boolean append,
            String[] headers,
            List<CsvOutputRecord> outputRecords,
            int... totals
    ) {
        if (append) {
            appendToExcel(filename, workSheet, outputRecords, totals);
        } else {
            createNewExcel(filename, workSheet, headers, outputRecords, totals);
        }
    }

    /**
     * Append to existing Excel (xlsx) file<BR>
     * <B>Note: </B> For files with lists, it is necessary to ensure the totals are
     * the max between the existing file and records to be appended.
     *
     * @param filename      - Location &amp; File to append to
     * @param workSheet     - Excel Worksheet to append the data
     * @param outputRecords - Records to be appended
     * @param totals        - For each list in the object, the total number of items there needs to be
     */
    private static void appendToExcel(
            String filename,
            String workSheet,
            List<CsvOutputRecord> outputRecords,
            int... totals
    ) {
        try (
                FileInputStream in = new FileInputStream(new File(filename));
                Workbook wb = new XSSFWorkbook(in)
        ) {
            Sheet sheet = wb.getSheet(workSheet);
            int rows = sheet.getLastRowNum() + 1;
            for (CsvOutputRecord item : outputRecords) {
                item.padListsWithEmptyItems(totals);

                Row dataRow = sheet.createRow(rows);
                rows++;

                int cells = 0;
                for (String cellData : item.asList()) {
                    Cell cell = dataRow.createCell(cells);
                    cell.setCellValue(cellData);
                    cells++;
                }
            }

            in.close();
            OutputStream fileOut = new FileOutputStream(filename);
            wb.write(fileOut);
        } catch (Exception ex) {
            assertThat("Could not append to Excel file due to error:  " + ex.getMessage(), false);
        }
    }

    /**
     * Create new Excel (xlsx) file
     *
     * @param filename      - Location &amp; File to write to
     * @param workSheet     - Excel Worksheet to create
     * @param headers       - Headers to be written
     * @param outputRecords - Records to be output to the file
     * @param totals        - For each list in the object, the total number of items there needs to be
     */
    private static void createNewExcel(
            String filename,
            String workSheet,
            String[] headers,
            List<CsvOutputRecord> outputRecords,
            int... totals
    ) {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet(WorkbookUtil.createSafeSheetName(workSheet));

            // Create header row & populate
            int rows = 0;
            Row row = sheet.createRow(rows);
            rows++;

            int cells = 0;
            for (String header : headers) {
                Cell cell = row.createCell(cells);
                cell.setCellValue(header);
                cells++;
            }

            // Create data rows & populate
            for (CsvOutputRecord item : outputRecords) {
                item.padListsWithEmptyItems(totals);

                Row dataRow = sheet.createRow(rows);
                rows++;

                cells = 0;
                for (String cellData : item.asList()) {
                    Cell cell = dataRow.createCell(cells);
                    cell.setCellValue(cellData);
                    cells++;
                }
            }

            // Write file
            OutputStream fileOut = new FileOutputStream(filename);
            wb.write(fileOut);
        } catch (Exception ex) {
            assertThat("Could not create Excel file due to error:  " + ex.getMessage(), false);
        }
    }

}
