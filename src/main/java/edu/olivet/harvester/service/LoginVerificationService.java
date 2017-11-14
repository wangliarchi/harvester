package edu.olivet.harvester.service;

import edu.olivet.foundations.utils.BusinessException;
import edu.olivet.foundations.utils.Constants;
import edu.olivet.foundations.utils.WaitTime;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * @author <a href="mailto:rnd@olivetuniversity.edu">OU RnD</a> 11/10/17 3:16 PM
 */
public class LoginVerificationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(LoginVerificationService.class);

    public static String readVerificationCodeFromGmail(String gmailAddress) {

        if (!StringUtils.contains(gmailAddress.toLowerCase(), "gmail.com")) {
            throw new BusinessException("Non gmail is not supported to fetch verification code.");
        }

        try {
            for(int i = 0; i < Constants.MAX_REPEAT_TIMES; i++ ) {
                String code = Jsoup.connect("https://script.google.com/macros/s/AKfycbzLHToSdGh4tSGM-D5ETYnmo_EH1VdpnWzvDBzZIgFe5X3MFfE/exec")
                        .ignoreContentType(true)
                        .data("func", "readCode").data("email", gmailAddress)
                        .method(Connection.Method.GET).timeout(WaitTime.Long.valInMS()).execute().body();
                if (StringUtils.isNumeric(code) && StringUtils.length(code) == 6) {
                    return code;
                }

                WaitTime.Short.execute();

            }
        } catch (IOException e) {
            LOGGER.error("Failed to read the verification code from Gmail for {}: ", gmailAddress, e);
        } catch (Exception e) {
            LOGGER.error("Unexpected exception occurred while reading verification code from Gmail {}: ", gmailAddress, e);
        }

        throw new BusinessException("Failed to read the verification code from " + gmailAddress);
    }

    public static void main(String[] args) {
        LoginVerificationService.readVerificationCodeFromGmail("alysounkegel4573@gmail.com");
    }
}
