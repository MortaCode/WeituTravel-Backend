package com.myy.weitutravel.login.sms;

public interface SmsProvider {

    void send(String mobile, String templateParam);

    boolean isConfigured();
}
