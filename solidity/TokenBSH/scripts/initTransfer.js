const Web3 = require('web3');

const BMCManagement = require('./build/contracts/BMCManagement.json');
const BMCPeriphery = require('./build/contracts/BMCPeriphery.json');
const BSHImpl = require('./build/contracts/BSHImpl.json');
const MockBMV = require('./build/contracts/BMV.json');
const BEP20TKN = require('./build/contracts/BEP20TKN.json');

const BSHProxy = require('./build/contracts/BSHProxy.json');
const address = require('./addresses.json');

const ethers = require('ethers')

var Personal = require('web3-eth-personal')
const http_url = "http://localhost:8545"
const ws_url = "ws://localhost:8546"
const provider = new ethers.providers.JsonRpcProvider(http_url)

let owner = "0x70E789D2f5D469eA30e0525DbfDD5515d6EAd30D"

const personal = new Personal(http_url)
function errorJson(ex) {
  let lines = ex.toString().split('\n');
  lines.splice(0, 1);
  return JSON.parse(lines.join('\n'));
}

function hex_to_ascii(str1) {
  var hex = str1.toString();
  var str = '';
  for (var n = 0; n < hex.length; n += 2) {
    str += String.fromCharCode(parseInt(hex.substr(n, 2), 16));
  }
  return str;
}

async function reason(hash) {
  console.log('tx hash:', hash)
  let tx = await provider.getTransaction(hash)
  if (!tx) {
    console.log('tx not found')
  } else {
    let code = await provider.call(tx, tx.blockNumber)
    console.log(code)
    let reason = hex_to_ascii(code.substr(138))
    console.log('revert reason:', reason)
  }
}
const init = async () => {
  let web3 = new Web3(http_url);
  const networkId = await web3.eth.net.getId();
  console.log(networkId)
  var accounts = await web3.eth.getAccounts();
  console.log(accounts)
  var balance = await web3.eth.getBalance(accounts[2]);
  console.log(balance)
  var eth = await web3.utils.fromWei(balance);
  console.log(eth)
  //await web3.eth.personal.unlockAccount(accounts[2], "Perlia0", 1000); 
  const bmcManagement = await new web3.eth.Contract(
    BMCManagement.abi,
    address.solidity.BMCManagement,
    { from: owner, gas: "4712388" }
  );
  const bmcPeriphery = await new web3.eth.Contract(
    BMCPeriphery.abi,
    address.solidity.BMCPeriphery,
    { from: owner, gas: "4712388" }
  );

  const bshImpl = await new web3.eth.Contract(
    BSHImpl.abi,
    address.solidity.BSHImpl,
    { from: owner, gas: "4712388" }
  );

  const bshProxy = await new web3.eth.Contract(
    BSHProxy.abi,
    address.solidity.BSHProxy,
    { from: owner, gas: "4712388" }
  );

  const bmv = await new web3.eth.Contract(
    MockBMV.abi,
    address.solidity.BMV,
    { from: owner, gas: "4712388" }
  );

  const bep20tkn = await new web3.eth.Contract(
    BEP20TKN.abi,
    address.solidity.BEP20TKN,
    { from: owner, gas: "4712388" }
  );

  bmcManagement.address = address.solidity.BMCManagement
  bmcPeriphery.address = address.solidity.BMCPeriphery
  bmv.address = address.solidity.BMV
  bshImpl.address = address.solidity.BSHImpl
  bshProxy.address = address.solidity.BSHProxy
  bep20tkn.address = address.solidity.BEP20TKN

  console.log("BMC mgmt: " + bmcManagement.address)
  console.log("BMC Periphery: " + bmcPeriphery.address)
  console.log("MOCK BMV: " + bmv.address)
  /*
  console.log("BSHImpl: "+bshImpl.address)
  console.log("BSHProxy: "+bshProxy.address)
  console.log("BEP20 TKN:"+  bep20tkn.address) 
  */

  let link = "btp://0x03.icon/cxe284d1774ade175c273b52742c3763ec5fe43844"
  let link1 = 'btp://0x97.bsc/cxe284d1774ade175c273b52742c3763ec5fe43844'

  let bmc = 'btp://0x97.bsc/0xAD50f33C3346F8e3403c510ee75FEBA1D904fa3F'
  //let prev = "btp://0x03.icon/cxe284d1774ade175c273b52742c3763ec5fe43844"
  let prev = "btp://0x954aa3.icon/cxa71f743c2ea9a8dcb8a7488ae4de03ca47bfe778"

  //let base64Msg = "-Qjv-QWWuQK8-QK5uQGh-QGeAoMN_82HBciUF9L_t5UA1jxMc7Yj6X9nQH1oevTvz-SGpRWgAbDHCRSeZdrXrzjRtViv2Siez6SoZe4-ag8sNIgnMRKgUaJiUgJkAvvz1WvOv9bYhcqDvi1eXcHNHr6Lx_JfrligZ3LUBWCxhGBqqeyRzWgy1JVM3Cqk4bkFTVgzb8VkuP34AKDd89pbri3xcTYkao5LpHL7497EHvJpi2OIHi4hWG0amaIBACBwSCwaDwMEQQAwiGw6HxAAQqIwaJxSLxiMwcgRqBwEuND4zqDpWVmCjkSo_sGOr5BP9VObSozH7rbAYb7nhy043b09qPgAoK5eD6gCuCtfaCPAqo262NbArdJizA7UWV1lixQw7Lj2uIj4hqAKrtuwLI_LNEiboI2Qesus2K69ke-yQfwwV6gGcL5f76DOqWKZsOCc1dJ7M_l5LgRINyydgPW1PZth-cofTpsvD_gAoLDrX8AXFxwYh6W64pPIjmVnH0NzRs94pznds_PDanqqoPABSOCQaKpMEzQDXr6WutDittPI9yuZOhuPADFEF00TuQEQ-QENAOIBoFXgx480SosI1zbxKVYywb8THL7uEPkpTXv7e270pr12-Of4S4cFyJQX8ZN_uEHn8UHAopnHhMUi15usTBjy0whPds6umiKbumkyXDnHonyGBZ3p-ctAjcek_bdQ2ZzeUB5rhd_3R18raIYl62awAPhLhwXIlBfxjIK4QcFhjbhFi5ynR3lsYjfyubiKtHYU2MAuZfC0YDxYJRGEC2AItrKC0rzulH4s9GBPkGRaINtTiFM-nr8UrfDmYPwA-EuHBciUF_GGcrhBHAVjqDWDftM628sJlbISdBWK-b_YETQmrkbB3COYDt8FkB8iCtsrauZCldbpREDcXEFO31vH1ZEIjqHJox9T2QH4ALkC1PkC0bkBufkBtgKDDf_OhwXIlBfxkACVAHiC2s4l_36UfTolF4oqEWKHTP3coDTCfPGwAKYlbx5jYxxDZpoGIMNkbjl0fWD4umUgWALKoMaHh2xxH8xzoMchaYy8x1ISl3tLefDHrglk5cDy3F99oGdy1AVgsYRgaqnskc1oMtSVTNwqpOG5BU1YM2_FZLj9-ACg2nqomUClMqK4tC8ZFO7glJViUEpEIILQWwGX95dhGd-4OQCAIGQAFAwRA4SEITDIbA4MACBDocBInDYhDBBFotCIHAo3IJDE4xDI7IoSgJNJ5XLJRLYjL4HAQLjQ-M6g7FBB-vv9nRnqq06fGZrLskAJnP1yZazm0WVUwb3pnMD4AKCdcUqzpD3i5XowB2vJ3YXP8pj813h7UU5lO5cBl1Zf6biI-IagFNlGvFRYLPiTA6ZcWAIxCTMh5SjVVIF5VBhsjGnh45mgzqlimbDgnNXSezP5eS4ESDcsnYD1tT2bYfnKH06bLw_4AKCw61_AFxccGIeluuKTyI5lZx9Dc0bPeKc53bPzw2p6qqDwAUjgkGiqTBM0A16-lrrQ4rbTyPcrmTobjwAxRBdNE7kBEPkBDQDiAaB78JX8CmJtPdn2qwmiMntIRIxc-9Sw8jxHe3CvFtUyWPjn-EuHBciUGA_-7bhBqFOFTesHqrrOoL9EghrbmrMFCzXNS9ihbp_b381mvCsd70ebNS6NRLpEhlgItyVClMznVc2trlNG5FxNuf9dygH4S4cFyJQYD_ybuEG1HZQkew3PU6cyJZG0_mFa8NXBHQmmUN_oKTRFNRypii8_dksui_p5DADlGL7xxDGCR5rln9CQq5dyHXa-4nUcAfhLhwXIlBgQCe24QXim0OY2j9DLWJxPb6TOFy4puEBlhPNSB929ezNioq_Ofn8bcL3VpK6YfJ0jEdMQBdv34oqUPWDGqxnC_QhCdH4B-AD4APkDUbkDTvkDSwG5AcT5AcGj4hCgdepiau3myZOZ_Arw0zstTz8pGhhTm-4meqZMXKzuPES4U_hRoBm81oI6QeDBOqzunyV4eEY9JJsTgJlGnv_K6RfyUuVdoEEWpA8VTBglCrpKRFRTEuCuQhmrRJYABsuiyjo_LKDTgICAgICAgICAgICAgICAuQFF-QFCILkBPvkBOwCVAf-ag0Yip4BfznuS-ec5DyXaY-J4gwZ9XYMGfV2FAukO3QC47wEAAABAAgAACAAAAAAAEAAAAAAAAAAAAAAAAAAAAgAAQAAAAAAAAAAAAAAAAAQAAAAAAAAAAAAAAAAAAAAAAAAAIAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAgAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAACAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAIAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA-AD4AKC4PJeQoXeGerxzJV9cvPq_YO76UB3eJSNC0fbRa5m4ifkBgPkBfQC5AXn5AXa5AXP5AXCCIAC5AWr5AWeVAeKE0XdK3hdcJztSdCw3Y-xf5DhE-FOWTWVzc2FnZShzdHIsaW50LGJ5dGVzKbg5YnRwOi8vMHg5Ny5ic2MvMHhBYUZjOEVlYUVFOGQ5QzhiRDMyNjJDQ0UzRDczRTU2RGVFM0ZCNzc2A_j6uPj49rg6YnRwOi8vMHgwMy5pY29uL2N4ZTI4NGQxNzc0YWRlMTc1YzI3M2I1Mjc0MmMzNzYzZWM1ZmU0Mzg0NLg5YnRwOi8vMHg5Ny5ic2MvMHhBYUZjOEVlYUVFOGQ5QzhiRDMyNjJDQ0UzRDczRTU2RGVFM0ZCNzc2iFRva2VuQlNIB7hz-HEA-G6qaHgxNTA5NjhjZjczMTE0N2ZhNDAzOGY1OWJlNWYwZjI2M2YyNGJjYTgyuDlidHA6Ly8weDk3LmJzYy8weGI2ODc1MDBjZTc3OTkwNjA2Mzk2YzMzYTcyYjJFRTdiNUVEOTY1QkbHxoNFVEgFAA=="  
  let base64Msg = "-Qvo-QiHuQGJ-QGGuQEK-QEHAoIDL4cFyfqXATxKlQAnXBGGF2EOZbpXKsCmId3RMlUkK6DJqntR8owbs60OhSbgaWaDyX8kPcbTxlLuPJ_G24hGaKCiWOEtGqCA7Syf2gCCJIn2F_-3ZB54Jv4G7vVJNGIlDaCtxTN2kAt-pE-iTQUZ-J-9-UKz4pUMzkwRQ6dCLLPjrPgA-AC1EAAgcEgsGAACAIAAkHgkMgsCgYChsUisWi8YjMai8TggQgsfjcijcdg0Kg6AisnkYAiIAgK4RvhEoM26TRUuVUqYDGIg7LVb-URmukK42a4Y00GdwBO91lwq-ACgLbl7TBEVY7fKYSW-yCnEYb1wFjMqM71dNXkWqEUg79S4dfhzAOIBoEbaLufGESazqDxa1xPBmHk4DWwQsXp7WzHzwW5xpB5p-E34S4cFyfqXEFiiuEGvrHCkYabQ-Jxs7334SgGMhTibgKhkpDnOh7u26p5gLyb8ob-1Ed5hmU4U40QJGhCFlcYGDv4AZyeejWljAvlWAPgAuQEx-QEuuLP4sQKCAzCHBcn6lxBYopUAJ1wRhhdhDmW6VyrApiHd0TJVJCugwt7v7B427zU3IWxj5PJI_965lWNScrfiOcwTahmMgdCga5i3ulKmT9BWQvZDRlAlJG4sbYlGy4YwB7bzE7uZ2cWgrcUzdpALfqRPok0FGfifvflCs-KVDM5MEUOnQiyz46z4APgAgKbloM26TRUuVUqYDGIg7LVb-URmukK42a4Y00GdwBO91lwq-AD4ALh1-HMA4gGgak_67TS4P5aZkQhtb9UWrQwG4VNwBN3ySxTkzZagzCD4TfhLhwXJ-pcfw4S4QbdbH4ZxcfAvMHfQckssr0gRwU4Z7eIlxlQO08UuHN8BXsqTfSMd9N7dcbn7U3O0Wg2qtGNQ0JRxl4KjdJwf0QsB-AC5AVD5AU240vjQAoIDMYcFyfqXH8OElQAnXBGGF2EOZbpXKsCmId3RMlUkK6CFSX4oWYv_5aoqYK2QhZ-uipiVMbTSLX-SLIjwwNuiZqBfOjlFVJ0MeX7oy3mdbuIwXXCUN0Z3wLdpNheqxGyqoqCtxTN2kAt-pE-iTQUZ-J-9-UKz4pUMzkwRQ6dCLLPjrPgAoC3CcHLhUoOQgeu72wh-AoO0JCwuWXyDcAYpGi3tTVmMgKbloM26TRUuVUqYDGIg7LVb-URmukK42a4Y00GdwBO91lwq-AD4ALh1-HMA4gGgKLXNYPHf14I9qVbWwYmjFCvp_6n1a8ExGN0bHxzujXD4TfhLhwXJ-pcvDXm4QfMbEZlPjwHTn23Ix9r5Y9vmKzUVpvrMg9t4o70id3qlX4-S6vqxVEsPnft0A75PV0JDbFhcenC2ndSd_RTZPcMA-AC5AYf5AYS5AQj5AQUCggMyhwXJ-pcvDXmVACdcEYYXYQ5lulcqwKYh3dEyVSQroFTQXactJx-bCnwNGkkPyvNs7q80kSW2PmisQvGnItv5oFU30cKT68K6TgLvz8kGVFN4hQ6U9DSJ-D6m9SmEfIhGoK3FM3aQC36kT6JNBRn4n735QrPilQzOTBFDp0Iss-Os-AD4ALMSACBwMEQSDwcAgACQiGwcQQQBQ6JxSKxYABCLxqNxuMwSPReIRyRxOFSSOQyBhCRACAi4RvhEoC_SFZu74evwlwGc39t0zQcwUHVaOfh1-UtQXD6KTTH5-ACgmN5gY2npp-tOuztYSDdvQyLe3XXUiD5SbHWXOO7MKSm4dfhzAOIBoPrCwocCLO3geYTl4K9Wfw7p2NdLdRr6YS0g4MNrtgoi-E34S4cFyfqXPjRnuEGxUWleDePNzseUabf8-VB5KVy34bLvEu1EOESyDZP51UEQ6u1pKcHfc7xMpBISIsexoxv43_nmk1fcZfw-gXypAPgAuQFQ-QFNuNL40AKCAzOHBcn6lz40Z5UAJ1wRhhdhDmW6VyrApiHd0TJVJCugbAETQcUDeZX527QVUeMVKum1Y2uM13hxA6uJeBaOYEigpBR1eKS4lwr7GhlGCoL-2BiPCro0KUaPAwDVdy2-IbmgrcUzdpALfqRPok0FGfifvflCs-KVDM5MEUOnQiyz46z4AKBNHpEA5Cr6_6TEmN59eM25TsbEXP-_5b8l-jKavnQKPICm5aAv0hWbu-Hr8JcBnN_bdM0HMFB1Wjn4dflLUFw-ik0x-fgA-AC4dfhzAOIBoOv0_V3a2Vjqq57fzvqTDy5WkhXju9-6wYHGt_YragbP-E34S4cFyfqXTWmbuEFB0Ts0taKIeBOzK78xDAFBTqNPBPvLSxQZATRD6XNWGlz69lopoJIYu7cuRQjeeIvjvzK67SOyjY0j5PK9boclAPgAuQGU-QGRuQEV-QESAoIDNIcFyfqXTWmblQAnXBGGF2EOZbpXKsCmId3RMlUkK6AtCiQYEQE1mviQ29hK7I0VX8Fd_wn4nqbxKcfFW88306CUHrejq48DhRqnyiER_6KdXLMRHavXhb6MCMtKtu4DX6CtxTN2kAt-pE-iTQUZ-J-9-UKz4pUMzkwRQ6dCLLPjrPgA-AC4PwIAIHAwDBCAAoGCIJDIbDoHCQAQQAEIfFovDhBGIbFY3DIFAgBIY9BIXEI8gIpGJTJJbDINIo8QJdLZZKoJAbhG-ESgL47NafMr91iW5ehba4-tZ_yJ3aBqPUAsKfLt4JeDkv74AKAhWgfM3tGqXZ_cPlZGKaDUQvhnIFHaxoUM-iQ7nqvlIbh1-HMA4gGglbd56sJ_FD1b7VSD1Dx5dF5rX91VrjaCWk7Z3Fm7uR74TfhLhwXJ-pdcq1m4QfPf5dE-rNRoNIwWEsq09KM47fdAjM_g9l-uovNS2G6JSjb8RWhwd_2WpLg_N9u8wMsWEwy0UorUHOK57mZOXMIB-AD4APkDWbkDVvkDUwC5AU75AUu5AUj5AUWCIAC5AT_5ATwAlQGlJtzmpm2kMoC5ZWcKS9FkUQHOR4MC-xSDAvsUALj1BAAAAAAAAQAAAEACAAAIAAAAAAAAAAAAAAAAAAAAAAAAAAACAABBABAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAgAAAAAAAAAAAAAAAAAAAAEAAAAAAAAAAAAAAAAAAAAAAEAAQAAAQAAAAAAAAAAAAAAAAAAAAAAAgAAAACAAAAAAAAAAAAAAAAgAAQAAAAAAAAAAAAAIAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAEAAAQAAAAAAAAAAAAAAABAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAACAAAAAEAAAAAD4APgAoG7POEUMktcmNDixx0kUJuXcl6XLNqPZ7sIMzBNc9D7F-QH--QH7ALkB9_kB9KPiEKDIl0A_LKSjWmia0NMnttGAMV542ox3KpdlJPWb1OMirbhT-FGgLui9FOlj2aE4gQOk5ieJ0FZUZrL4791-ORApRN0XPzigbKNNGxUQslpGTDS25MmqE4EaRE2pKjAafFhfqheZ2eeAgICAgICAgICAgICAgIC5AXj5AXUguQFx-QFulQFDh5U-lsUcxY-7GgKyzIwyj-RLgPhTlk1lc3NhZ2Uoc3RyLGludCxieXRlcym4OWJ0cDovLzB4OTcuYnNjLzB4QWFGYzhFZWFFRThkOUM4YkQzMjYyQ0NFM0Q3M0U1NkRlRTNGQjc3NgP5AQC4_vj8uD5idHA6Ly8weDk1NGFhMy5pY29uL2N4NDM4Nzk1M2U5NmM1MWNjNThmYmIxYTAyYjJjYzhjMzI4ZmU0NGI4MLg5YnRwOi8vMHg5Ny5ic2MvMHhBYUZjOEVlYUVFOGQ5QzhiRDMyNjJDQ0UzRDczRTU2RGVFM0ZCNzc2iFRva2VuQlNIArh1-HMAuHD4bqpoeGQ4MjQyMzhkNDAyNjY1ZTdjZTUyMGY4M2Q0YTI4MjcxMTllYzY4ZDK4OWJ0cDovLzB4OTcuYnNjLzB4NzBlNzg5ZDJmNWQ0NjllYTMwZTA1MjVkYmZkZDU1MTVkNmVhZDMwZMfGg0VUSAoA"



  let height = 0;
  let offset = 0;
  let lastHeight = 0;
  let blockInterval = 3000;
  let maxAggregation = 5;
  let delayLimit = 3;
  let relays;
  relays = [accounts[2], accounts[3]];

  await personal.unlockAccount(owner, "Perlia0", 60 * 60 * 12)
  await personal.unlockAccount(accounts[3], "Perlia0", 60 * 60 * 12)
  //comment this try catch for second pass///
  /* 
    try {
      //await bmcManagement.methods.removeService("TokenBSH").send({from:owner,gas: "4712388"}) ;   
      //await bmcManagement.methods.addService("TokenBSH", bshImpl.address).send({from:owner,gas: "4712388"}) ; 
      //await bmcManagement.methods.addVerifier("0x03.icon", bmv.address).send({from:accounts[2],gas: "4712388"}) ;
      let verifiers = await bmcManagement.methods.getVerifiers().call();
      //console.log("verifiers list#####################");
      //console.log(verifiers);
      //await bmcManagement.methods.removeVerifier("0x03.bsc").send({from:owner,gas: "4712388"}) ;
  
      //await bmcManagement.methods.addLink(link).send({from:owner,gas: "4712388"}) ;
      let links = await bmcManagement.methods.getLinks().call();
      //console.log("Links list#####################");
      //console.log(links);
  
      //await bmcManagement.methods.addRelay(link, relays).send({from:owner,gas: "4712388"});
      //comment this line catch for second pass
      //await bmcManagement.methods.updateLinkRxSeq(link, 2).send({from:owner,gas: "4712388"}) 
  
    } catch (ex) {
      //console.log(ex)
      let error = errorJson(ex)
      console.log("Transaction failed, ", error.transactionHash);
      reason(error.transactionHash)
    }
   */
  var sendtx = await web3.eth.sendTransaction({ to: accounts[0], from: accounts[3], value: 30000 });
  console.log("ok");
  console.log(sendtx)
  try {
    //await bshProxy.methods.register("ETH", "ETH", 18, 1, bep20tkn.address ).send({from:owner,gas: "4712388"}) 
    let tokens = await bshProxy.methods.tokenNames().call();
    console.log(tokens);
    let isreg = await bshProxy.methods.isTokenRegisterd("ETH").call();
    console.log("Is token registerd:" + isreg);
    var balanceBeforeTransfer = await bep20tkn.methods.balanceOf(bshProxy.address).call();
    console.log("Balance Before transfer BEP20 to BSHProxy:", balanceBeforeTransfer)
    await bep20tkn.methods.transfer(bshProxy.address, 100).send({ from: owner, gas: "4712388" });
    var balanceAfterTransfer = await bep20tkn.methods.balanceOf(bshProxy.address).call();
    console.log("Balance After transfer BEP20 to BSHProxy:", balanceAfterTransfer)
    //await bmcManagement.methods.updateLinkRxSeq(prev, 1).send({ from: owner, gas: "4712388" })
    let seq = await bmcManagement.methods.getLinkRxSeq(prev).call();
    console.log(seq);

    let transferAmount = 10
    var _to = 'btp://0x954aa3.icon/hx275c118617610e65ba572ac0a621ddd13255242b';
    tokenName = "ETH"
    //approve & initiate transfer
    var balanceBefore = await bshProxy.methods.getBalanceOf(owner, tokenName).call()
    console.log("BEP20 token user balance before transfer", balanceBefore)
    await bep20tkn.methods.approve(bshProxy.address, transferAmount).send({ from: owner, gas: "4712388" });
    var txn = await bshProxy.methods.transfer(tokenName, transferAmount, _to).send({ from: owner, gas: "4712388" })
    console.log(txn);
    //check balance after transfer
    var balanceafter = await bshProxy.methods.getBalanceOf(owner, tokenName).call()
    console.log("BEP20 token user balance after transfer", balanceafter)
    let bshBal = await bep20tkn.methods.balanceOf(bshProxy.address).call();
    console.log("Balance of BSH Proxy" + bshProxy.address + " after the transfer:" + bshBal);
  } catch (ex) {
    console.log(ex)
    let error = errorJson(ex)
    console.log("Transaction failed2, ", error.transactionHash);
    reason(error.transactionHash)
  }
}

init().then(async () => {
  console.log("Init done")

});