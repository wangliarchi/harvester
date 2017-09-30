package edu.olivet.harvester.utils;

import edu.olivet.foundations.amazon.Account;
import edu.olivet.foundations.amazon.Country;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">RnD</a> 9/29/17 1:40 PM
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderManAccountSettings {
    /** 一级电脑编号  */
    private String id;
    /** 二级电脑编号  */
    private int sortNo = 1;
    private Country[] countries = Country.values();
    //	private ArrayList<Country> countries = new ArrayList<Country>();
    private Map<Country, Account> sellers;
    private Map<Country, Account> sellerEmails;
    //private Map<Country, Account> ptBuyers;
    private Map<Country, Account> primeBuyers;
    private Map<Country, Account> prodPtBuyers;
    private Map<Country, Account> prodPrimeBuyers;
    private Map<Country, Account> cutomerServicePrimeAccounts;
    private Map<Country, Account> bwBuyers;
    private Map<Country, Account> halfBuyers;
    private Map<Country, Account> ebatesBuyers;
    private Map<Country, Account> giftCardBuyers;
    private Map<Country, Account> ingramBuyers;
    private Map<Country, String> orderFinders;
    private Map<Country, String> signatures;
    private Map<Country, String> googledrivebooks;
    private Map<Country, String> googledriveproducts;
    private Map<Country, String> sellerids;
    //private Map<Country, CreditCard> creditCard;
}
