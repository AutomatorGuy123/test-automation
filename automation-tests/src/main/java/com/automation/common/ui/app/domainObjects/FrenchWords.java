package com.automation.common.ui.app.domainObjects;

/**
 * Contains all words that translation was needed and can be added to the vocabulary during
 * instantiation & use later as translations are needed. (This prevents issues if the words are changed.)
 */
public class FrenchWords {
    private FrenchWords() {
        // Prevent initialization of class as all public methods should be static
    }

    public final static String ERROR_CREDENTIALS_INVALID = "La combinaison de votre adresse Courriel et/ou mot de passe est incorrect.";

}
