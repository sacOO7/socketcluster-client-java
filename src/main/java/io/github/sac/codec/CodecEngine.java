package io.github.sac.codec;

import org.json.JSONObject;

/**
 * <p>Created by AUT0CRAT on 2/2/18.
 */
public abstract class CodecEngine {
    abstract public byte[] encode(JSONObject data);

    abstract public byte[] encode(String data);

    abstract public String decode(byte[] data);
}
