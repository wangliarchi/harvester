package edu.olivet.harvester.fulfill.utils.validation;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import edu.olivet.foundations.ui.UIText;
import edu.olivet.foundations.utils.Configs;
import edu.olivet.foundations.utils.RegexUtils;
import edu.olivet.harvester.utils.Config;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/16/17 3:28 PM
 */
@Singleton
public class ItemValidator {
    private static final Logger logger = LoggerFactory.getLogger(ItemValidator.class);
    private static final int PASS_DIFF_RATE = 20;
    private static final int PASS_DIFF_COUNT = 2;
    private static final int PASS_SAME_RATE = 80;
    private static final int PASS_SAME_COUNT = 5;
    private static final int MAX_MINUS_PROFIT = 20;
    private Map<String, String> stopWords;
    private Map<String, String> conditionLvls;


    @Inject
    public void init() throws IOException {
        stopWords = Configs.load(Config.StopWords.fileName());
        conditionLvls = Configs.load(Config.ConditionLevel.fileName());
    }


    /**
     * 书名校验报告
     *
     * @author <a href="mailto:nathanael4ever@gmail.com>Nathanael Yang</a> Oct 24, 2014 4:06:02 PM
     */
    public static class ValidateReport {
        public boolean pass;
        public boolean exactlySame;
        public boolean contain;
        public int wordsOfISBN;
        public int wordsOfForm;
        public int sameWords;
        public int diffWords;
        public int sameRate;
        public int diffRate;

        @Override
        public String toString() {
            if (exactlySame) {
                return UIText.text("text.compare.pass.same");
            } else if (contain) {
                return UIText.text("text.compare.pass.contain");
            } else if (sameWords > 0) {
                String prefix = UIText.label(pass ? "label.pass" : "label.notpass");
                return UIText.text("text.compare.report",
                        prefix, wordsOfISBN, wordsOfForm, sameWords, diffWords, sameRate, diffRate);
            } else {
                return UIText.text("text.compare.fail");
            }
        }
    }

    /**
     * <pre>
     * 校验书名是否相同：
     * 1.标点符号替换为空白字符，校验真名是否包含于原名之中
     * 2.如果不包含(比如顺序不一致等等)，切分为单词，校验两个集合的交集是否完全等同于待审查的书名单词集合
     * 3.考虑后续检查相似度高于某个接受值（比如一些无意义的stop word应当去掉:the a an等等）
     * </pre>
     *
     * @param name2Review 待检查书名，位于Order Review Page上（真实isbn对应书名）
     * @param nameInForm  表单数据中填入的书名
     */
    @SuppressWarnings("unchecked")
    public ValidateReport validateItemName(String name2Review, String nameInForm) {
        ValidateReport report = new ValidateReport();
        boolean exactlySame = name2Review.endsWith(nameInForm);
        if (exactlySame) {
            report.exactlySame = report.pass = true;
            return report;
        }

        String s1 = name2Review.replaceAll(RegexUtils.Regex.PUNCTUATION.val(), StringUtils.EMPTY).toLowerCase();
        String s2 = nameInForm.replaceAll(RegexUtils.Regex.PUNCTUATION.val(), StringUtils.EMPTY).toLowerCase();
        boolean contain = s2.contains(s1);

        if (!contain) {
            List<String> words1 = convert2Words(StringUtils.split(s1, StringUtils.SPACE));
            List<String> words2 = convert2Words(StringUtils.split(s2, StringUtils.SPACE));
            int isbnCnt = words1.size(), formCnt = words2.size();
            report.wordsOfISBN = isbnCnt;
            report.wordsOfForm = formCnt;

            Collection<String> intersection = CollectionUtils.intersection(words1, words2);
            int sameCnt = intersection.size();
            if (CollectionUtils.isNotEmpty(intersection)) {
                Collection<String> disjunction = CollectionUtils.disjunction(words1, intersection);

                int diffCnt = disjunction.size();
                float sameRate = (float) (100 * sameCnt) / (float) isbnCnt;
                float diffRate = (float) (100 * diffCnt) / (float) isbnCnt;

                report.sameWords = sameCnt;
                report.diffWords = diffCnt;
                report.sameRate = (int) sameRate;
                report.diffRate = (int) diffRate;

                report.pass = CollectionUtils.isEmpty(disjunction) || sameCnt >= PASS_SAME_COUNT || sameRate >= PASS_SAME_RATE || diffCnt <= PASS_DIFF_COUNT || diffRate <= PASS_DIFF_RATE;
            } else {
                report.pass = false;
            }
        } else {
            report.contain = report.pass = true;
        }
        logger.debug("{} -> {}比较结果: {}", name2Review, nameInForm, report.toString());
        return report;
    }

    public static String nameCompareRules() {
        return UIText.text("text.compare.rules", PASS_SAME_COUNT, PASS_SAME_RATE, PASS_DIFF_COUNT, PASS_DIFF_RATE);
    }

    public List<String> convert2Words(String[] arr) {
        List<String> result = new ArrayList<>(arr.length);
        for (String t : arr) {
            // 去掉停词
            if (stopWords.get(t) == null) {
                result.add(t);
            }
        }
        return result;
    }



    private void appendCheckResults(List<String> skuCheckResults, StringBuilder sb) {
        boolean shorten = skuCheckResults.size() > 10;
        if (shorten) {
            skuCheckResults = skuCheckResults.subList(0, 10);
        }
        sb.append(StringUtils.join(skuCheckResults, StringUtils.LF));
        sb.append(shorten ? "\n............" : StringUtils.EMPTY);
    }

    private static final String NAME_PATTERN_ORDER = "Order";
    private static final String NAME_PATTERN_UPDATE = "Update";


    private static CheckResult INSTANCE = new CheckResult();


    /**
     *
     *
     * @author <a href="mailto:nathanael4ever@gmail.com>Nathanael Yang</a> Feb 10, 2015 4:27:08 PM
     */
    public static class CheckResult {
        private List<String> errors;
        private boolean popupSheetRange;
        private boolean popupSystemConfig;
        private boolean popupAccountsConfig;

        public List<String> getErrors() {
            return errors;
        }

        public void setErrors(List<String> errors) {
            this.errors = errors;
        }

        public boolean isPopupSheetRange() {
            return popupSheetRange;
        }

        public void setPopupSheetRange(boolean popupSheetRange) {
            this.popupSheetRange = popupSheetRange;
        }

        public boolean isPopupSystemConfig() {
            return popupSystemConfig;
        }

        public void setPopupSystemConfig(boolean popupSystemConfig) {
            this.popupSystemConfig = popupSystemConfig;
        }

        public boolean isPopupAccountsConfig() {
            return popupAccountsConfig;
        }

        public void setPopupAccountsConfig(boolean popupAccountsConfig) {
            this.popupAccountsConfig = popupAccountsConfig;
        }
    }


}
