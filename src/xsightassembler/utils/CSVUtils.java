package xsightassembler.utils;

import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CSVUtils implements MsgBox{
	
	public CSVUtils(){
	}
	
	public List<String[]> getCsvData() throws NullPointerException{
		String csvFile = selectFile();
        String line = "";
        String cvsSplitBy = ",";
        List<String[]> importingData = new ArrayList<String[]>(); 
        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
            while ((line = br.readLine()) != null) {
            	importingData.add(line.split(cvsSplitBy));
            }
            return importingData;
        } catch (IOException e) {
            MsgBox.msgException(e);
        }
        return null;
	}
	
	private String selectFile(){
		Stage selectedStage = new Stage();
		selectedStage.setTitle("Import file");
		FileChooser fileChooser = new FileChooser();
//		fileChooser.getInitialDirectory().getAbsolutePath(new File(".").getAbsolutePath());
		fileChooser.getExtensionFilters().addAll(
			     new FileChooser.ExtensionFilter("CSV Files", "*.csv")
			);
		File selectedFile = fileChooser.showOpenDialog(selectedStage);
		if (selectedFile == null)
			return null;
		return selectedFile.toString();
	}
}
