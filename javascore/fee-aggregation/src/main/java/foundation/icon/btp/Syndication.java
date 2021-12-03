package foundation.icon.btp;

import score.Address;
import score.ObjectReader;
import score.ObjectWriter;

import java.math.BigInteger;
import java.util.Map;
import score.*;

public class Syndication {
    private BigInteger id;

    /**
     * Amount of token to bid
     */
    private BigInteger tokenAmount;

    /**
     * Total amount has been deposited to syndication
     */
    private BigInteger totalDeposited;

    /**
     *  Start time of a syndication by token contract address
     */
    private long startTime;

    private long endTime;

    public Syndication(BigInteger _tokenAmount, Address _depositor, BigInteger _amount, long _startTime) {
        this.id = FeeAggregationSCORE.syndicationCount.getOrDefault(BigInteger.ZERO).add(BigInteger.ONE);
        FeeAggregationSCORE.syndicationCount.set(this.id);
        this.tokenAmount = _tokenAmount;
        this.startTime = _startTime;
        this.endTime = _startTime + FeeAggregationSCORE.durationTime.get();
        this.totalDeposited = BigInteger.ZERO;
        this.deposit(_depositor, _amount);
    }

    private Syndication(BigInteger _id, BigInteger _tokenAmount, BigInteger _totalDeposited, long _startTime, long _endTime) {
        this.id = _id;
        this.tokenAmount = _tokenAmount;
        this.startTime = _startTime;
        this.endTime = _endTime;
        this.totalDeposited = _totalDeposited;
    }

    public static void writeObject(ObjectWriter w, Syndication t) {
        w.beginList(6);
        w.write(t.id);
        w.write(t.tokenAmount);
        w.write(t.totalDeposited);
        w.write(t.startTime);
        w.write(t.endTime);
        w.end();
    }

    public static Syndication readObject(ObjectReader r) {
        r.beginList();
        Syndication t = new Syndication(
                r.readBigInteger(),
                r.readBigInteger(),
                r.readBigInteger(),
                r.readLong(),
                r.readLong());
        r.end();
        return t;
    }

    public BigInteger id() { return this.id; }

    public BigInteger tokenAmount() {
        return this.tokenAmount;
    }

    public BigInteger totalDeposited() {
        return this.totalDeposited;
    }

    public void deposit(Address depositor, BigInteger amount) {
        BigInteger totalUserDeposit = FeeAggregationSCORE.syndicationDepositorAmount.at(this.id).getOrDefault(depositor, BigInteger.ZERO);
        if (totalUserDeposit.compareTo(BigInteger.ZERO) == 0) {
            FeeAggregationSCORE.syndicationDepositorList.at(this.id).add(depositor);
        }
        totalUserDeposit = totalUserDeposit.add(amount);
        this.totalDeposited = this.totalDeposited.add(amount);
        FeeAggregationSCORE.syndicationDepositorAmount.at(this.id).set(depositor, totalUserDeposit);
    }

    public long startTime() { return this.startTime; }

    public long endTime() { return this.endTime; }

    public Map<String, String> toMap() {
        return Map.of(
                "_id", "0x" + this.id.toString(16),
                "_tokenAmount", "0x" + this.tokenAmount.toString(16),
                "_totalDeposited", "0x" + this.totalDeposited.toString(16),
                "_startTime", "0x" + BigInteger.valueOf(this.startTime).toString(16),
                "_endTime", "0x" + BigInteger.valueOf(this.endTime).toString(16)
        );
    }
}
