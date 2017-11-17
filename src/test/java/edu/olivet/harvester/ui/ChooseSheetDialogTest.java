package edu.olivet.harvester.ui;

import edu.olivet.foundations.ui.UITools;
import edu.olivet.harvester.common.BaseTest;
import edu.olivet.harvester.spreadsheet.Spreadsheet;
import edu.olivet.harvester.spreadsheet.Worksheet;
import edu.olivet.harvester.ui.dialog.ChooseSheetDialog;
import edu.olivet.harvester.utils.Settings;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

public class ChooseSheetDialogTest extends BaseTest {



    @Test
    public void testGetSelectedSheets() throws Exception {

        List<String> spreadsheetIds = Settings.load(testConfigFilePath).listAllSpreadsheets();
        List<edu.olivet.harvester.spreadsheet.Spreadsheet> spreadsheets = new ArrayList<>();

        for (String spreadsheetId : spreadsheetIds) {
            try {
                Spreadsheet spreadsheet = appScript.reloadSpreadsheet(spreadsheetId);
                spreadsheets.add(spreadsheet);
            } catch (Exception e) {
                //ignore
            }
        }


        ChooseSheetDialog dialog = UITools.setDialogAttr(new ChooseSheetDialog(spreadsheets, appScript));


        if (dialog.isOk()) {
            List<Worksheet> selectedWorksheets = dialog.getSelectedWorksheets();
            System.out.println(selectedWorksheets);
        }

    }

}