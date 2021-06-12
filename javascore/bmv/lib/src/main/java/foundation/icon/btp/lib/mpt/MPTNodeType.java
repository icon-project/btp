package foundation.icon.btp.lib.mpt;

public enum MPTNodeType {
    EMPTY((byte)0b00),
    LEAF((byte)0b01),
    BRANCH((byte)0b10),
    BRANCH_WITH_VALUE((byte)0b11);

    private final byte type;
    MPTNodeType(byte type) {
        this.type = type;
    }
}