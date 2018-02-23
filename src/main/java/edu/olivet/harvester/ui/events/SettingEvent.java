package edu.olivet.harvester.ui.events;

import com.google.inject.Inject;
import edu.olivet.foundations.ui.UITools;
import edu.olivet.harvester.spreadsheet.service.AppScript;
import edu.olivet.harvester.spreadsheet.service.SheetAPI;
import edu.olivet.harvester.ui.dialog.SettingsDialog;
import edu.olivet.harvester.utils.SettingValidator;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/4/17 11:07 AM
 */
public class SettingEvent implements HarvesterUIEvent {
    @Inject
    private SheetAPI sheetAPI;
    @Inject AppScript appScript;
    public void execute() {
        UITools.setDialogAttr(new SettingsDialog(new SettingValidator(appScript, sheetAPI)));
    }
}
