package foundation.icon.btp.lib;

public class BlockVerifyResult {
    public byte[] stateRoot;
    public long lastHeight;
  
    public BlockVerifyResult(byte[] stateRoot, long lastHeight) {
      this.stateRoot = stateRoot;
      this.lastHeight = lastHeight;
    }
  
    public BlockVerifyResult() {
      this.stateRoot = null;
      this.lastHeight = 0;
    }
  }