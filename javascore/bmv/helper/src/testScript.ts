import util from 'util';
import { exec } from 'child_process';

const execPromise = util.promisify(exec);

async function main () {
    await execPromise(`cd .. && gradle loadMetaData -PmetaDataFilePath=/Users/leclevietnam/mwork/btp/javascore/bmv/helper/metaData.json`);
    await execPromise(`cd .. && cd eventDecoder && gradle optimizedJar`);
}

main().catch(console.error);