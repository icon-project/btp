package foundation.icon.btp.bmv.near.verifier.types;

import org.near.borshj.Borsh;

public abstract class Item implements Borsh {
    public abstract Item fromString(String string);
}
