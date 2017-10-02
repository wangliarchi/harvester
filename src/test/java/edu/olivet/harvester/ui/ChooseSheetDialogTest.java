package edu.olivet.harvester.ui;

import com.alibaba.fastjson.JSON;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.ui.InformationLevel;
import edu.olivet.foundations.ui.ProgressDetail;
import edu.olivet.foundations.ui.UITools;
import edu.olivet.foundations.utils.Strings;
import edu.olivet.foundations.utils.Tools;
import edu.olivet.harvester.common.BaseTest;
import edu.olivet.harvester.spreadsheet.Spreadsheet;
import edu.olivet.harvester.spreadsheet.Worksheet;
import edu.olivet.harvester.spreadsheet.service.AppScript;
import edu.olivet.harvester.utils.Settings;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.*;

public class ChooseSheetDialogTest extends BaseTest {

    private AppScript appScript;

    @BeforeClass
    public void init() {
        appScript = new AppScript() {
            @Override
            public Spreadsheet reloadSpreadsheet(String spreadId) {
                File localJsonFile = new File(BaseTest.TEST_DATA_ROOT + File.separator + "spreadsheet-" + spreadId + ".json");

                return JSON.parseObject(Tools.readFileToString(localJsonFile), Spreadsheet.class);

            }
        };
    }

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
            List<Worksheet> selectedWorksheets = dialog.getSelectedSheets();
            System.out.println(selectedWorksheets);
        }

    }

}