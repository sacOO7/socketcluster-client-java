package io.github.sac;

import org.json.simple.JSONObject;

/**
 * Created by sachin on 15/11/16.
 */
public class Parser {

    public enum ParseResult{
        ISAUTHENTICATED,
        PUBLISH,
        REMOVETOKEN,
        SETTOKEN,
        EVENT,
        ACKRECEIVE
    }


    public static ParseResult parse(Object dataobject, Long rid, Long cid, String event) {

//        System.out.println("rid is"+rid);
        if (dataobject instanceof JSONObject && ((JSONObject) dataobject).get("isAuthenticated") != null) {
                return ParseResult.ISAUTHENTICATED;
        } else if (event != null){
                if (event.equals("#publish")) {
                    return ParseResult.PUBLISH;
                } else if (event.equals("#removeAuthToken")) {
                    return ParseResult.REMOVETOKEN;
                } else if (event.equals("#setAuthToken")) {
                    return ParseResult.SETTOKEN;
                } else {
                    System.out.println("Event got called with cid "+cid);
                    return ParseResult.EVENT;
                }
        }else {
            return ParseResult.ACKRECEIVE;
        }
    }

}
