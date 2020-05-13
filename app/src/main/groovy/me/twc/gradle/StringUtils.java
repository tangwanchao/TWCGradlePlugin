package me.twc.gradle;

/**
 * @author 唐万超
 */
public class StringUtils {

    private StringUtils(){}

    public static String firstToUpperCase(String input) {
        String outPut = "";
        if (input.length() == 1) {
            outPut = input.toUpperCase();
        } else if (input.length() > 1){
            String firstUpperCase = input.substring(0, 1).toUpperCase();
            outPut = firstUpperCase + input.substring(1);
        }
        return outPut;
    }
}
