package edu.olivet.harvester.fulfill.service.addressvalidator;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import edu.olivet.foundations.amazon.Country;
import edu.olivet.foundations.aop.Profile;
import edu.olivet.foundations.ui.UIText;
import edu.olivet.foundations.utils.ApplicationContext;
import edu.olivet.foundations.utils.Configs;
import edu.olivet.foundations.utils.Constants;
import edu.olivet.foundations.utils.RegexUtils;
import edu.olivet.foundations.utils.RegexUtils.Regex;
import edu.olivet.harvester.fulfill.model.Address;
import edu.olivet.harvester.fulfill.service.AddressValidatorService;
import edu.olivet.harvester.fulfill.utils.CountryStateUtils;
import edu.olivet.harvester.message.ErrorAlertService;
import edu.olivet.harvester.model.State;
import edu.olivet.harvester.utils.Config;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/25/2017 8:15 AM
 */
public class OrderManAddressValidator implements AddressValidator {
    private static final Logger LOGGER = LoggerFactory.getLogger(OrderManAddressValidator.class);
    /**
     * 对数字类型文本做容错处理时，需补上缺少的字符内容:{@value}
     */
    private static final char DIGIT_ZERO = '0';
    /**
     * 一屏式模式下，地址信息的连接字符
     */
    private static final String SINGLE_PAGE_ADDR_SEPERATOR = ", ";
    /**
     * 比较地址属性时，在二者不相等的前提下，能接受的相似度最低值
     */
    private static final float MIN_SIMILARITY = 0.9f;
    /**
     * Rating value indicate two addresses are exactly same
     */
    private static final float SAME = 1.0f;

    private Map<String, String> addressRules;


    @Inject
    ErrorAlertService errorAlertService;

    /**
     * 合并普通缩写规则和UPS官方缩写规则，然后将亚马逊的规则写入覆盖
     */
    @Inject
    public void init() {
        addressRules = new HashMap<>();
        Map<String, String> upsAbbrev = Configs.load(Config.AddressAbbrev.fileName());
        addressRules.putAll(upsAbbrev);

        Map<String, String> addressRule = Configs.load(Config.AmazonAddressRule.fileName());
        addressRules.putAll(addressRule);
    }

    @Override
    @Profile
    public boolean verify(Address old, Address entered) {
        try {
            if (_verify(old, entered)) {
                return true;
            }
            String finalOrAddr = applyRule2Addr(old.toString());
            String finalAddr = applyRule2Addr(entered.toString());

            if (!finalOrAddr.equals(finalAddr)) {
                //noinspection deprecation
                double similarity = StringUtils.getJaroWinklerDistance(finalOrAddr, finalAddr);
                if (similarity < MIN_SIMILARITY) {
                    AddressValidatorService.logFailed(old.toString(), entered.toString(), finalOrAddr + ", " + finalAddr);
                    String msg = "OrderMan Address failed verification. Entered " + entered + ", original " + old +
                            ", Addresses after rules applied " + finalOrAddr + ", " + finalAddr;
                    LOGGER.error(msg);
                    return false;
                }
            }

            return true;

        } catch (Exception e) {
            LOGGER.error("${message}", e);
            return false;
        }
    }


    private boolean _verify(Address old, Address entered) {
        List<String> results = new ArrayList<>();
        if (!old.getRecipient().replace(StringUtils.SPACE, StringUtils.EMPTY)
                .equalsIgnoreCase(entered.getRecipient().replace(StringUtils.SPACE, StringUtils.EMPTY))) {
            results.add(UIText.text("error.addr.name", old.getRecipient(), entered.getRecipient()));
        }

        String orAddr = old.getAddress1() + StringUtils.SPACE + old.getAddress2();
        String addr = entered.getAddress1() + StringUtils.SPACE + entered.getAddress2();
        String result = this.compareAddress(orAddr, addr);
        if (StringUtils.isNotBlank(result)) {
            results.add(result);
        }

        result = this.compareCity(old.getCity(), entered.getCity());
        if (StringUtils.isNotBlank(result)) {
            results.add(result);
        }

        List<String> list = this.compareStateZip(old, entered);
        results.addAll(list);

        if (StringUtils.isNotBlank(entered.getCountry())) {
            result = this.compareCountry(old.getCountry(), entered.getCountry());
            if (StringUtils.isNotBlank(result)) {
                results.add(result);
            }
        }

        if (CollectionUtils.isNotEmpty(results)) {
            String msg = "OrderMan Address failed verification. Entered " + entered +
                    ", original " + old + ". " + StringUtils.join(results, "; ");
            LOGGER.error(msg);
            AddressValidatorService.logFailed(old.toString(), entered.toString(), "");
            return false;
        }
        return true;


    }


    /**
     * 将地址去掉标点符号、全部转换为小写之后，按照亚马逊地址替换规则进行转换
     *
     * @param addr 输入的地址信息
     * @return 转换后的地址信息
     */
    private String applyRule2Addr(String addr) {
        if (StringUtils.isBlank(addr)) {
            return StringUtils.EMPTY;
        }
        String[] array = StringUtils.split(addr.replaceAll(RegexUtils.Regex.PUNCTUATION.val(), StringUtils.SPACE), StringUtils.SPACE);
        List<String> list = new ArrayList<>(array.length);
        for (String s : array) {
            if (addressRules == null) {
                this.init();
            }
            while (addressRules.get(s.toLowerCase()) != null && !addressRules.get(s.toLowerCase()).equalsIgnoreCase(s)) {
                s = addressRules.get(s.toLowerCase());
            }

            // 亚马逊可能会自动添加TH等后缀，形如4350 N 84TH PL => 4350 N. 84 Place，4350n84thpl => 4350n84pl，将TH等后缀替换为空
            if (s.toUpperCase().matches(Regex.ADDRESS_NO.val())) {
                s = s.toUpperCase().replaceAll(Regex.NON_DIGITS.val(), StringUtils.EMPTY);
            }
            list.add(s.toLowerCase());
        }

        return StringUtils.join(list, StringUtils.SPACE).replaceAll(Regex.PUNCTUATION.val(), StringUtils.SPACE);
    }

    /**
     * 检查地址
     *
     * @param address01 OrderReview页面的address1+address2
     * @param address02 Order本身的ship_address1+ship_address2
     * @return 校验结果
     */
    private String compareAddress(String address01, String address02) {
        if (this.sameInWords(address01, address02)) {
            return null;
        }

        String s1 = StringUtils.trim(applyRule2Addr(address01).replace(StringUtils.SPACE, StringUtils.EMPTY));
        String s2 = StringUtils.trim(applyRule2Addr(address02).replace(StringUtils.SPACE, StringUtils.EMPTY));
        if (!s1.equalsIgnoreCase(s2)) {
            char[] arr1 = s1.toCharArray();
            char[] arr2 = s2.toCharArray();
            Arrays.sort(arr1);
            Arrays.sort(arr2);
            // 顺序打乱，但所有字符排序后拼接起来完全一致，也视为相同
            if (new String(arr1).equalsIgnoreCase(new String(arr2))) {
                return null;
            }

            return UIText.message("error.addr.1and2", address01, address02, s1, s2);
        }
        return null;
    }

    /**
     * 亚马逊有时会将地址中单词顺序按照其规则重排，为此需先比较两个长句中所有单词是否相同: 先去掉所有空白字符看是否相同，然后拆分为集合，差集为空
     */
    private boolean sameInWords(String sentence1, String sentence2) {
        if (sentence1.replaceAll(Regex.BLANK.val(), StringUtils.EMPTY).trim()
                .equalsIgnoreCase(sentence2.replaceAll(Regex.BLANK.val(), StringUtils.EMPTY).trim())) {
            return true;
        }

        List<String> list1 = Arrays.asList(
                StringUtils.split(sentence1.replaceAll(Regex.PUNCTUATION.val(), StringUtils.EMPTY).toLowerCase(), StringUtils.SPACE));
        List<String> list2 = Arrays.asList(
                StringUtils.split(sentence2.replaceAll(Regex.PUNCTUATION.val(), StringUtils.EMPTY).toLowerCase(), StringUtils.SPACE));
        return CollectionUtils.disjunction(list1, list2).size() == 0;
    }

    /**
     * 比较地址中的国家，一般只需根据名称比较。但也存在国家名称不同，却指向相同国家代码的情况
     * eg: South Korea -> Korea, Republic of
     *
     * @param country1 OrderReview页面的country名称
     * @param country2 Order本身的ship_country列
     * @return 校验结果
     */
    private String compareCountry(String country1, String country2) {
        if (!country1.equalsIgnoreCase(country2)) {
            String code1 = CountryStateUtils.getInstance().getCountryCode(country1);
            String code2 = CountryStateUtils.getInstance().getCountryCode(country2);
            if (!code1.equalsIgnoreCase(code2)) {
                return UIText.message("error.addr.country", country1, code1, country2, code2);
            }
        }
        return null;
    }

    /**
     * 比较邮政编码，存在比如两个美国邮政编码应当一致，但经过Amazon自动转换之后后四位不同的情况
     * eg: 44224-2297 -> 44224-2209
     *
     * @param zip1 OrderReview页面的zipcode，可能为空白，但不能为null
     * @param zip2 Order本身ship_zipcode列，可能为空白，但不能为null
     * @return 校验结果
     */
    private String compareZipCode(String zip1, String zip2) {
        if (!zip1.replaceAll(Regex.PUNCTUATION.val(), StringUtils.EMPTY)
                .equals(zip2.replaceAll(Regex.PUNCTUATION.val(), StringUtils.EMPTY)) &&
                !zip1.contains(zip2) && !zip2.contains(zip1)) {
            String[] arr1 = StringUtils.splitPreserveAllTokens(zip1, Constants.HYPHEN);
            String[] arr2 = StringUtils.splitPreserveAllTokens(zip2, Constants.HYPHEN);
            String correctedZip1 = arr1.length > 0 ? arr1[0] : StringUtils.EMPTY;
            String correctedZip2 = arr2.length > 0 ? arr2[0] : StringUtils.EMPTY;
            if (!correctedZip1.equalsIgnoreCase(correctedZip2)) {
                return UIText.message("error.addr.zip", zip1, zip2);
            }
        }
        return null;
    }


    /**
     * 比较两个城市名称是否一致: 去掉标点符号、转换为小写，结果完全一致或相似度大于可接受值，予以通过
     */
    private String compareCity(String city1, String city2) {
        if (city1.equalsIgnoreCase(city2)) {
            return null;
        }

        String correctedCity1 = city1.replaceAll(Regex.PUNCTUATION.val(), StringUtils.EMPTY).toLowerCase();
        String correctedCity2 = city2.replaceAll(Regex.PUNCTUATION.val(), StringUtils.EMPTY).toLowerCase();
        if (!correctedCity1.equals(correctedCity2)) {
            @SuppressWarnings("deprecation") double similarity = StringUtils.getJaroWinklerDistance(correctedCity1, correctedCity2);
            if (similarity < MIN_SIMILARITY) {
                return UIText.text("error.addr.city", city1, city2);
            }
        }
        return null;
    }

    private List<String> compareStateZip(Address old, Address entered) {
        List<String> results = new ArrayList<>();

        String orStateZip = (old.getFullStateName() + old.getZip()).replace(StringUtils.SPACE, StringUtils.EMPTY).toLowerCase();
        String stateZip = (entered.getFullStateName() + entered.getZip()).replace(StringUtils.SPACE, StringUtils.EMPTY).toLowerCase();
        // 如果州名和邮政编码连接一起的结果不等同，或互不包含(存在一些情况州名会亚马逊自动去掉了)，再分解检查
        if (!orStateZip.equalsIgnoreCase(stateZip) && !orStateZip.contains(stateZip) && !stateZip.contains(orStateZip)) {
            String result = this.compareZipCode(old.getZip(), entered.getZip());
            if (StringUtils.isNotBlank(result)) {
                results.add(result);
            }

            result = this.compareState(old.getState(), entered.getState(), true);
            if (StringUtils.isNotBlank(result)) {
                results.add(result);
            }
        }

        return results;
    }

    private String compareState(String state1, String state2, boolean inUS) {
        if (inUS) {
            try {
                State stateObj1 = State.parse(state1);
                State stateObj2 = State.parse(state2);
                if (stateObj1 != stateObj2) {
                    return UIText.text("error.addr.state", state1, state2);
                }
                return StringUtils.EMPTY;
            } catch (IllegalArgumentException e) {
                // -> Ignore
            }
        }

        // 目前州名和邮政编码用空格分割，因为邮政编码的规则比较复杂，有时州名可能会分割多或少，此时只需互相包含即可
        if (!state1.equalsIgnoreCase(state2) && !state1.contains(state2) && !state2.contains(state1)) {
            return UIText.text("error.addr.state", state1, state2);
        }
        return StringUtils.EMPTY;
    }

    /**
     * 目前订单中会出现部分邮政编码文字变为数字，导致首位数字0被自动取消的情况，尝试自动补齐
     *
     * @param zipCode 当前邮政编码
     */
    public String correctUSZipCode(String zipCode) {
        if (StringUtils.isBlank(zipCode)) {
            throw new IllegalArgumentException(UIText.message("error.addr.uszip.empty"));
        }

        String first5 = StringUtils.split(zipCode, Constants.HYPHEN)[0];
        int count = 5 - first5.length() + zipCode.length();
        return StringUtils.leftPad(zipCode, count, DIGIT_ZERO);
    }

    /**
     * 目前订单中会出现部分邮政编码文字变为数字，导致首位数字0被自动取消的情况，尝试自动补齐
     *
     * @param zipCode 当前邮政编码
     */
    public String correctDEZipCode(String zipCode) {
        return StringUtils.leftPad(zipCode, 5, DIGIT_ZERO);
    }


    /**
     * 对美国州名进行容错处理，比如Ks.，Pa.等等
     *
     * @param shipState 原始订单数据中的州名
     */
    public static String correctUSState(String shipState) {
        if (!shipState.contains(".")) {
            return shipState;
        }

        String state = shipState.toUpperCase().replace(".", StringUtils.EMPTY).trim();
        for (State st : State.values()) {
            if (st.name().equals(state)) {
                return st.name();
            }
        }

        return shipState;
    }

    /**
     * Get the first word of a sentence
     */
    private static String getFirstWord(String sentence) {
        if (StringUtils.isBlank(sentence)) {
            return StringUtils.EMPTY;
        }

        Scanner scanner = new Scanner(sentence);
        Scanner s = scanner.useDelimiter(Regex.BLANK.val());
        String result = null;
        while (s.hasNext()) {
            if (StringUtils.isNotBlank(result = s.next())) {
                break;
            }
        }
        s.close();
        scanner.close();
        assert result != null;
        return result.replaceAll(Regex.PUNCTUATION.val(), StringUtils.EMPTY);
    }

    /**
     * <pre>
     * Calculate similarity between two addresses
     * Used to determine whether the latest shipment location is similar as that of customer or seller
     * <strong>NOT</strong> for common usage!
     * </pre>
     */
    private double calcSimilarity(String addr1, String addr2) {
        String[] arr1 = StringUtils.splitPreserveAllTokens(
                addr1.replaceAll(Regex.NONE_DIGITS_MINUS.val(), StringUtils.EMPTY).trim(), Constants.HYPHEN);
        String[] arr2 = StringUtils.splitPreserveAllTokens(
                addr2.replaceAll(Regex.NONE_DIGITS_MINUS.val(), StringUtils.EMPTY).trim(), Constants.HYPHEN);
        String zipCode1 = arr1.length > 0 ? arr1[0] : null;
        String zipCode2 = arr2.length > 0 ? arr2[0] : null;
        // if zip code are exactly same, we treat the addresses as same
        if (StringUtils.isNotBlank(zipCode1) && StringUtils.isNotBlank(zipCode2) && zipCode1.equals(zipCode2)) {
            return SAME;
        }

        // If the first word which typically indicator the city is same, we treat the addresses as same
        String firstWord1 = getFirstWord(addr1);
        String firstWord2 = getFirstWord(addr2);
        if (firstWord1.equalsIgnoreCase(firstWord2)) {
            return SAME;
        }

        String s1 = this.removeNonKeyPart(addr1).toLowerCase();
        String s2 = this.removeNonKeyPart(addr2).toLowerCase();
        if (s1.equalsIgnoreCase(s2)) {
            return SAME;
        }

        //noinspection deprecation
        return StringUtils.getJaroWinklerDistance(s1, s2);
    }

    /**
     * Determine whether two addresses are same or not according similarity calculation and comparison
     */
    public boolean same(String addr1, String addr2) {
        double rate = this.calcSimilarity(addr1, addr2);
        return rate >= MIN_SIMILARITY;
    }


    private String removeNonKeyPart(String addr) {
        String correctedAddress = StringUtils.defaultString(addr);
        for (State state : State.values()) {
            correctedAddress = correctedAddress.replace(state.desc(), state.name()).replace(state.desc().toUpperCase(), state.name());
        }
        return correctedAddress.replace("USA", StringUtils.EMPTY).replace("US", StringUtils.EMPTY)
                .replaceAll(Regex.DIGITS_MINUS.val(), StringUtils.EMPTY)
                .replaceAll(Regex.PUNCTUATION.val(), StringUtils.EMPTY);
    }

    /**
     * <pre>
     * Usually the last line would be short address, however sometimes it might be country name
     * If the last line points to a valid country name, we will return the last 2nd line
     * Or we will simply return the last line
     * </pre>
     */
    public String getShortAddr(String src, Country country) {
        String[] array = StringUtils.split(src, StringUtils.LF);
        for (String s : array) {
            if (RegexUtils.containsRegex(s, "[A-Z|a-z]+, [A-Z|a-z]{2,20} [0-9]{5}(-[0-9]{4})?")) {
                return s;
            }
        }

        List<String> list = Lists.newArrayList(array);
        Collections.reverse(list);
        for (Iterator<String> iterator = list.iterator(); iterator.hasNext(); ) {
            String string = iterator.next();
            if (StringUtils.startsWith(string, "Phone:")) {
                iterator.remove();
                continue;
            }

            try {
                CountryStateUtils.getInstance().getCountryCode(string);
                iterator.remove();
            } catch (IllegalArgumentException e) {
                // -> Ignore
            }
        }

        if (CollectionUtils.isNotEmpty(list)) {
            // 欧洲区域地址通常为city, state, zipCode三行
            if (country.europe() && list.size() >= 3) {
                return list.get(2) + Constants.COMMA_WHITESPACE + list.get(1) + Constants.COMMA_WHITESPACE + list.get(0);
            }
            return list.get(0);
        }
        throw new IllegalArgumentException("Failed to retrieve key information from address raw text: " + src);
    }


    public static void main(String[] args) {
        Address address = new Address();
        address.setAddress1("131 East 69th Street");
        address.setAddress2("3A");
        address.setCity("New York");
        address.setState("NY");
        address.setZip("10021");
        address.setCountry("United States");

        Address enteredAddress = new Address();
        enteredAddress.setAddress1("131 E 69TH ST 3A");
        enteredAddress.setAddress2("");
        enteredAddress.setCity("NEW YORK");
        enteredAddress.setState("NY");
        enteredAddress.setZip("10021-5158");
        enteredAddress.setCountry("United States");

        OrderManAddressValidator validator = ApplicationContext.getBean(OrderManAddressValidator.class);
        System.out.println(validator.verify(address, enteredAddress));

    }


}
