package foundation.icon.btp.bmv.near.verifier.types;

import java.util.List;
import java.util.Random;

import foundation.icon.score.util.StringUtil;
import scorex.util.ArrayList;

public class ItemList<T extends Item> extends ArrayList<T> {
    public <T extends Item> T getRandom() {
        Random rand = new Random();
        return (T) this.get(rand.nextInt(this.size()));
    }
    
    public static <T extends Item> ItemList<T> fromString(Class<T> klass, String str) throws InstantiationException, IllegalAccessException {
        ItemList<T> items = new ItemList<T>();
        List<String> tokenized = StringUtil.tokenize(str, ',');
        try {
            for (int i = 0; i < tokenized.size(); i++) {
                T item = klass.newInstance();
                items.add((T) item.fromString(tokenized.get(i)));
            }
        } catch (IllegalArgumentException e) {
            throw e;
        }
        return items;
    }
}