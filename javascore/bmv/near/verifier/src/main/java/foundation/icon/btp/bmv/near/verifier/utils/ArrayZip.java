package foundation.icon.btp.bmv.near.verifier.utils;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import foundation.icon.btp.bmv.near.verifier.types.MapItem;
import scorex.util.ArrayList;

public class ArrayZip {
    public static <A, B> List<MapItem<A, B>> zip(List<A> as, List<B> bs) {
        Iterator<A> it1 = as.iterator();
        Iterator<B> it2 = bs.iterator();
        List<MapItem<A, B>> result = new ArrayList<MapItem<A,B>>();
        while (it1.hasNext() && it2.hasNext()) {
            result.add((MapItem<A, B>) Map.entry(it1.next(), it2.next()));
        }
        return result;
    }
}
