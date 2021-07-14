package foundation.icon.btp;

import score.Address;
import score.ObjectReader;
import score.ObjectWriter;

import java.math.BigInteger;
import java.util.Map;

public class Auction {
    private BigInteger id;

    /**
     * Amount of token to bid
     */
    private BigInteger tokenAmount;

    /**
     *  Start time of a auction by token contract address
     */
    private long startTime;

    private long endTime;

    private Address bidder;

    /**
     * Bid value (ICX)
     */
    private BigInteger bidAmount;

    public Auction(BigInteger _tokenAmount, Address _bidder, BigInteger _bidAmount, long _startTime) {
        this.id = FeeAggregationSCORE.auctionCount.getOrDefault(BigInteger.ZERO).add(BigInteger.ONE);
        FeeAggregationSCORE.auctionCount.set(this.id);
        this.tokenAmount = _tokenAmount;
        this.bidAmount = _bidAmount;
        this.bidder = _bidder;
        this.startTime = _startTime;
        this.endTime = _startTime + FeeAggregationSCORE.durationTime.get();
    }

    private Auction(BigInteger _id, Address _bidder, BigInteger _tokenAmount, BigInteger _bidAmount, long _startTime, long _endTime) {
        this.id = _id;
        this.tokenAmount = _tokenAmount;
        this.bidAmount = _bidAmount;
        this.bidder = _bidder;
        this.startTime = _startTime;
        this.endTime = _endTime;
    }

    public static void writeObject(ObjectWriter w, Auction t) {
        w.beginList(6);
        w.write(t.id);
        w.write(t.bidder);
        w.write(t.tokenAmount);
        w.write(t.bidAmount);
        w.write(t.startTime);
        w.write(t.endTime);
        w.end();
    }

    public static Auction readObject(ObjectReader r) {
        r.beginList();
        Auction t = new Auction(
                r.readBigInteger(),
                r.readAddress(),
                r.readBigInteger(),
                r.readBigInteger(),
                r.readLong(),
                r.readLong());
        r.end();
        return t;
    }

    public BigInteger id() { return this.id; }

    public BigInteger bidAmount() {
        return this.bidAmount;
    }

    public BigInteger tokenAmount() {
        return this.tokenAmount;
    }

    public Address bidder() {
        return this.bidder;
    }

    public long startTime() { return this.startTime; }

    public long endTime() { return this.endTime; }

    public Auction setNewBidder(Address bidder, BigInteger _bidAmount) {
        this.bidder = bidder;
        this.bidAmount = _bidAmount;
        return this;
    }

    public Map<String, String> toMap() {
        return Map.of(
                "_id", "0x" + this.id.toString(16),
                "_bidder", this.bidder.toString(),
                "_tokenAmount", "0x" + this.tokenAmount.toString(16),
                "_bidAmount", "0x" + this.bidAmount.toString(16),
                "_startTime", "0x" + BigInteger.valueOf(this.startTime).toString(16),
                "_endTime", "0x" + BigInteger.valueOf(this.endTime).toString(16)
        );
    }
}
