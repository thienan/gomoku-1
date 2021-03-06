/*
 * Gomoku
 * Maciej Kawecki 2015/16
 */
package gomoku;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;


/**
*
* Tłumaczenia (singleton)
* 
* @author Maciej Kawecki
* 
*/
public class Lang {

   /** Instancja klasy */
   private final static Lang INSTANCE = new Lang();
   
   /** Lokalizacja - indeks */
   private int localeIndex = IConf.DEFAULT_LOCALE_INDEX;
   /** Lokalizacja */  
   private Locale locale = IConf.LOCALES[localeIndex];
   /** Lista tłumaczeń */
   private ResourceBundle bundle;   
   
   
   private Lang() {
	   
	 Locale.setDefault(locale);
	 try {
	   bundle = ResourceBundle.getBundle("ApplicationMessages");
	 }
	 catch (MissingResourceException e) {
	   bundle = ResourceBundle.getBundle("resources/ApplicationMessages");
	 }
	   
   }
   
   

   /**
    * Ustawia wskazaną lokalizację
    * @param index Indeks lokalizacji
    * @return True jeżeli zmieniono
    */
   public static boolean setLocale(int index) {
 	  
 	 if (index != INSTANCE.localeIndex)  
 	 try { 
 	   INSTANCE.localeIndex = index;
 	   INSTANCE.locale = IConf.LOCALES[index];
 	   Locale.setDefault(INSTANCE.locale);
 	   try {
 	     INSTANCE.bundle = ResourceBundle.getBundle("ApplicationMessages");
 	   }
 	   catch (MissingResourceException e) {
 		 INSTANCE.bundle = ResourceBundle.getBundle("resources/ApplicationMessages");
 	   }
 	   return true;
 	 }
 	 catch (IndexOutOfBoundsException e) {
 	   System.err.println(e);
 	 }
 	 
 	 return false;
 	 
   }
   
   
   public static int getLocaleIndex() {
 	 return INSTANCE.localeIndex;
   }
   

   
   /**
    * Zwraca tłumaczenie z zawartymi parametrami
    * @param key Klucz frazy
    * @param params Parametry
    * @return Tłumaczenie
    */
   public static String get(String key, Object... params) {
 	  
	 try {    
 	   return MessageFormat.format(INSTANCE.bundle.getString(key), params);
	 }
	 catch (MissingResourceException e) {
	   System.err.println(e);
	   return key;
	 }
 	  
   }
      
   
   /**
    * Zwraca symbol lokalizacji (język + kraj)
    * @return Symbol lokalizacji
    */
   public static String getLocaleSymbol() {
	 
	 return INSTANCE.locale.getLanguage() + "_" + INSTANCE.locale.getCountry();
	   
   }
   
   
   /**
    * Zwraca nazwę języka
    * @return Nazwa języka
    */
   public static String getName() {
	   
	 return INSTANCE.locale.getDisplayLanguage();  
	   
   }
	
	
}
