package com.example.project.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Utility component for internationalization (i18n) and localization support.
 * Provides static methods to retrieve translated messages and manage
 * the current user locale using Spring's MessageSource and LocaleContextHolder.
 */

@Getter
@Setter
@Component
public class Translator {

    private static MessageSource messageSource;

    @Autowired
    public Translator(MessageSource messageSource) {
        Translator.messageSource = messageSource;
    }

    public static String toLocale(String msgCode) {
        Locale locale = LocaleContextHolder.getLocale();
        return messageSource.getMessage(msgCode, null, locale);
    }

    public static Locale getCurrentLocale() {
        return LocaleContextHolder.getLocale();
    }

    public static void setCurrentLocale(Locale locale) {
        LocaleContextHolder.setLocale(locale);
    }
}