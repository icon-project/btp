/*
 * Copyright 2021 ICON Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package foundation.icon.btp.bmc;

import foundation.icon.score.data.EnumerableDictDB;
import foundation.icon.score.util.Logger;
import score.Address;
import score.Context;
import score.VarDB;

import java.util.List;

public class Relayers extends EnumerableDictDB<Address, Relayer> {
    private static final Logger logger = Logger.getLogger(Relayers.class);
    private final VarDB<RelayersProperties> varDB;

    public Relayers(String id) {
        super(id, Address.class, Relayer.class);
        varDB = Context.newVarDB(super.concatId("properties"), RelayersProperties.class);
    }

    public RelayersProperties getProperties() {
        return varDB.getOrDefault(RelayersProperties.DEFAULT);
    }

    public void setProperties(RelayersProperties properties) {
        varDB.set(properties);
    }

    public Relayer[] getValuesBySinceAndSortAsc(long since) {
        List<Relayer> list = super.values();
        Relayer[] filteredRelayers = filterSince(list, since);
        sortAsc(filteredRelayers);
        return filteredRelayers;
    }

    public static Relayer[] filterSince(List<Relayer> list, long since) {
        int sinceLen=0;
        for (Relayer relayer : list) {
            if (relayer.getSince() < since) {
                sinceLen++;
            }
        }
        Relayer[] array = new Relayer[sinceLen];
        int i=0;
        for (Relayer relayer : list) {
            if (relayer.getSince() < since) {
                array[i++] = relayer;
            }
        }
        return array;
    }

    /**
     * Compare Relayer for sorting
     * bond desc, since asc, sinceExtra asc
     *
     * @apiNote Not allowed to use java.util.Comparator in javaee
     * @apiNote If Relayer implements java.lang.Comparable,
     *          it makes 'No implementation found for compareTo(Ljava/lang/Object;)I' in Enum classes.
     *
     * @param o1 the first Relayer to compare
     * @param o2 the second Relayer to compare
     * @return bond desc, since asc, sinceExtra asc
     */
    public static int compare(Relayer o1, Relayer o2) {
        int compBond = o2.getBond().compareTo(o1.getBond());
        if (compBond == 0) {
            int compSince = Long.compare(o1.getSince(), o2.getSince());
            if (compSince == 0) {
                return Integer.compare(o1.getSinceExtra(), o2.getSinceExtra());
            } else {
                return compSince;
            }
        } else {
            return compBond;
        }
    }

    /**
     * Sorts array of Relayer
     * instead of foundation.icon.score.util.ArrayUtil#sort(java.lang.Comparable[])
     * @see Relayers#compare(Relayer, Relayer)
     *
     * @param a Array of Relayer
     */
    public static void sortAsc(Relayer[] a) {
        int len = a.length;
        for (int i = 0; i < len; i++) {
            Relayer v = a[i];
            for (int j = i + 1; j < len; j++) {
                if (compare(v, a[j]) > 0) {
                    Relayer t = v;
                    v = a[j];
                    a[j] = t;
                }
            }
            a[i] = v;
        }
    }
}
