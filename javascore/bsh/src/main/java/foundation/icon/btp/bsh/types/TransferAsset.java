package foundation.icon.btp.bsh.types;

import score.ObjectReader;
import score.ObjectWriter;
import scorex.util.ArrayList;

import java.util.List;

public class TransferAsset {
    private String from;
    private String to;
    private List<Asset> assets;
    private List<byte[]> test;

    public TransferAsset(String from, String to, List<Asset> assets) {
        this.from = from;
        this.to = to;
        this.assets = assets;
    }


    public static void writeObject(ObjectWriter w, TransferAsset v) {
        w.beginList(3);
        w.write(v.getFrom());
        w.write(v.getTo());
        w.beginList(v.getAssets().size());
        for (int i = 0; i < v.getAssets().size(); i++) {
            Asset.writeObject(w, v.getAssets().get(i));
            //w.write( v.getAsset());
        }
        w.end();
        w.end();
    }

    public static TransferAsset readObject(ObjectReader r) {
        r.beginList();
        String _from = r.readString();
        String _to = r.readString();
        List<Asset> assets = new ArrayList<>();
        r.beginList();
        while (r.hasNext()) {
            Asset _asset = Asset.readObject(r);
            assets.add(_asset);
        }
        r.end();
        r.end();
        TransferAsset result = new TransferAsset(_from, _to, assets);
        return result;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public List<Asset> getAssets() {
        return assets;
    }

    public void setAssets(List<Asset> assets) {
        this.assets = assets;
    }
}
