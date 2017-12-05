package edu.olivet.harvester.fulfill.utils;

import com.google.inject.Singleton;
import edu.olivet.foundations.ui.UIText;
import edu.olivet.harvester.fulfill.model.FulfillmentEnum;
import edu.olivet.harvester.fulfill.model.setting.RuntimeSettings;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/8/17 10:45 AM
 */
@Singleton
public class RuntimeSettingsValidator {
    private static final Logger logger = LoggerFactory.getLogger(RuntimeSettingsValidator.class);

    @Data
    public static class CheckResult {
        private List<String> errors;
    }

    interface RuntimeSettingsChecker {
        CheckResult check(RuntimeSettings settings);
    }


    List<String> checkSheet(RuntimeSettings settings) {
        List<String> errors = new ArrayList<>();

        if (StringUtils.isBlank(settings.getSheetName()) || StringUtils.isBlank(settings.getSpreadsheetId()) || StringUtils.isBlank(settings.getSpreadsheetName())) {
            errors.add(UIText.text("error.appcfg.sheet"));
        }

        return errors;
    }

    List<String> checkSheetAndRange(RuntimeSettings settings) {
        List<String> errors = this.checkSheet(settings);
        String error = settings.getAdvancedSubmitSetting().validate();
        if (StringUtils.isNotBlank(error)) {
            errors.add(error);
        }
        return errors;
    }

    class SheetRangeChecker implements RuntimeSettingsChecker {
        @Override
        public CheckResult check(RuntimeSettings settings) {
            CheckResult result = new CheckResult();
            List<String> errors = checkSheetAndRange(settings);
            result.setErrors(errors);
            return result;
        }
    }

    public CheckResult validate(RuntimeSettings settings, FulfillmentEnum.Action action) {
        switch (action) {
            default:
                return new SheetRangeChecker().check(settings);
        }
    }

}

