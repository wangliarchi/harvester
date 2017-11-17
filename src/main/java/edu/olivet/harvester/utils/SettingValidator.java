package edu.olivet.harvester.utils;

import com.google.api.services.drive.model.File;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.utils.Constants;
import edu.olivet.harvester.spreadsheet.Spreadsheet;
import edu.olivet.harvester.spreadsheet.service.AppScript;
import edu.olivet.harvester.spreadsheet.service.SheetAPI;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 10/6/17 9:53 PM
 */
public class SettingValidator {


    private AppScript appScript;
    private SheetAPI sheetAPI;

    public SettingValidator(AppScript appScript, SheetAPI sheetAPI) {
        this.appScript = appScript;
        this.sheetAPI = sheetAPI;
    }

    public @Nullable List<String> validate(String sid, List<Settings.Configuration> configs) {
        List<String> errors = new ArrayList<>();

        errors.addAll(spreadsheetIdNeedToBeUnique(configs));

        configs.forEach(config -> {
            errors.addAll(validateSpreadsheetIds(config));
            errors.addAll(spreadsheetTypeAndTitleShouldMatch(config));
            errors.addAll(spreadsheetAccountCountryAndTitleShouldMatch(sid,config));
            errors.addAll(possibleSpreadsheetExistedButNotEntered(sid, config));
        });

        return errors;
    }


    public List<String> validateSpreadsheetIds(Settings.Configuration config) {

        List<String> errors = new ArrayList<>();
        Country country = config.getCountry();

        if (StringUtils.isNotEmpty(config.getBookDataSourceUrl())) {
            try {
                appScript.getSpreadsheet(config.getBookDataSourceUrl());
            } catch (Exception e) {
                errors.add(String.format("%s BOOK spreadsheet %s is not a valid spreadsheet id, or not shared with %s yet.",
                        country.name(), config.getBookDataSourceUrl(), Constants.RND_EMAIL));
            }

        }


        if (StringUtils.isNotEmpty(config.getProductDataSourceUrl())) {
            try {
                appScript.getSpreadsheet(config.getProductDataSourceUrl());
            } catch (Exception e) {
                errors.add(String.format("%s PRODUCT spreadsheet %s is not a valid spreadsheet id, or not shared with %s yet.",
                        country.name(), config.getProductDataSourceUrl(), Constants.RND_EMAIL));
            }

        }

        return errors;
    }

    public List<String> spreadsheetIdNeedToBeUnique(List<Settings.Configuration> configs) {
        List<String> errors = new ArrayList<>();


        Map<String, ArrayList<String>> spreadsheetIds = new HashMap<>();
        for (Settings.Configuration config : configs) {

            Map<String, String> spreadsheetsMap = new HashMap<>(2);
            spreadsheetsMap.put("BOOK", config.getBookDataSourceUrl());
            spreadsheetsMap.put("PRODUCT", config.getProductDataSourceUrl());

            spreadsheetsMap.forEach((String sheetType, String spreadsheetId) -> {
                if (StringUtils.isNotEmpty(spreadsheetId)) {
                    if (spreadsheetIds.containsKey(spreadsheetId)) {
                        List<String> origins = spreadsheetIds.get(spreadsheetId);
                        origins.add(config.getCountry().name() + " " + sheetType);
                    } else {
                        spreadsheetIds.put(spreadsheetId, new ArrayList<>(Collections.singletonList(config.getCountry().name() + " " + sheetType)));
                    }
                }
            });

        }



        spreadsheetIds.forEach((spreadsheetId, origins) -> {
            if (origins.size() > 1) {
                errors.add(String.format("%s have the same spreadsheet id %s.", StringUtils.join(origins, ", "), spreadsheetId));
            }

        });


        return errors;
    }

    public List<String> spreadsheetAccountCountryAndTitleShouldMatch(String sid,Settings.Configuration config) {

        List<String> errors = new ArrayList<>();
        Country country = config.getCountry();

        List<String> spreadsheetIds =
                Stream.of(config.getBookDataSourceUrl(), config.getProductDataSourceUrl()).collect(Collectors.toList());

        spreadsheetIds.forEach(spreadsheetId -> {
            if (StringUtils.isNotEmpty(spreadsheetId)) {
                Spreadsheet spreadsheet = appScript.getSpreadsheet(spreadsheetId);
                if (spreadsheet != null) {

                    if(!StringUtils.containsIgnoreCase(spreadsheet.getTitle(), sid)) {
                        errors.add(String.format("%s spreadsheet %s(%s) seems not for account %s.",
                                country.name(), spreadsheet.getTitle(), spreadsheetId, sid));
                        return;
                    }

                    if (country.europe()) {
                        if (!StringUtils.containsIgnoreCase(spreadsheet.getTitle(), country.name()) && !StringUtils.containsIgnoreCase(spreadsheet.getTitle(), "EU")) {
                            errors.add(String.format("%s spreadsheet %s(%s) seems not for %s.",
                                    country.name(), spreadsheet.getTitle(), spreadsheetId, country.name()));
                        }
                    } else {
                        if (!StringUtils.containsIgnoreCase(spreadsheet.getTitle(), country.name())) {
                            errors.add(String.format("%s spreadsheet %s(%s) seems not for %s.",
                                    country.name(), spreadsheet.getTitle(), spreadsheetId, country.name()));
                        }
                    }
                }

            }

        });

        return errors;
    }

    public List<String> spreadsheetTypeAndTitleShouldMatch(Settings.Configuration config) {

        List<String> errors = new ArrayList<>();
        Country country = config.getCountry();

        if (StringUtils.isNotEmpty(config.getBookDataSourceUrl())) {
            Spreadsheet spreadsheet = appScript.getSpreadsheetFromCache(config.getBookDataSourceUrl());

            if (spreadsheet != null && StringUtils.containsIgnoreCase(spreadsheet.getTitle(), "product")) {
                errors.add(String.format("%s BOOK spreadsheet %s(%s) seems a product order sheet.",
                        country.name(), config.getBookDataSourceUrl(), spreadsheet.getTitle()));
            }

            if (spreadsheet != null && StringUtils.containsIgnoreCase(spreadsheet.getTitle(), "product")) {
                errors.add(String.format("%s PRODUCT spreadsheet %s(%s) seems a book order sheet.",
                        country.name(), config.getBookDataSourceUrl(), spreadsheet.getTitle()));
            }

        }


        if (StringUtils.isNotEmpty(config.getProductDataSourceUrl())) {


            Spreadsheet spreadsheet = appScript.getSpreadsheetFromCache(config.getProductDataSourceUrl());

            if (spreadsheet != null && StringUtils.containsIgnoreCase(spreadsheet.getTitle(), "book")) {
                errors.add(String.format("%s PRODUCT spreadsheet %s(%s) seems a book order sheet.",
                        country.name(), config.getProductDataSourceUrl(), spreadsheet.getTitle()));
            }


        }

        return errors;
    }



    public List<String> possibleSpreadsheetExistedButNotEntered(String sid, Settings.Configuration config){
        List<String> errors = new ArrayList<>();

        if(StringUtils.isBlank(config.getBookDataSourceUrl())) {
            try {
                List<File> availableSheets = sheetAPI.getAvailableSheets(sid, config.getCountry(), "BOOK");
                if (CollectionUtils.isNotEmpty(availableSheets)) {
                    errors.add(String.format("%s does'nt fill BOOK spreadsheet, but possible sheets found. %s",
                            config.getCountry().name(), availableSheets.stream().map(File::getName).collect(Collectors.toSet())));
                }
            }catch (Exception e) {
                //ignore
            }
        }

        if(StringUtils.isBlank(config.getProductDataSourceUrl())) {
            try {
                List<File> availableSheets = sheetAPI.getAvailableSheets(sid, config.getCountry(), "ExportedOrder");
                if (CollectionUtils.isNotEmpty(availableSheets)) {
                    errors.add(String.format("%s does'nt fill PRODUCT spreadsheet, but possible sheets found. %s",
                            config.getCountry().name(), availableSheets.stream().map(File::getName).collect(Collectors.toSet())));
                }
            }catch (Exception e) {
                //ignore
            }
        }

        return errors;
    }


}