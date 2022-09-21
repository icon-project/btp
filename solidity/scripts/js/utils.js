const { access, constants, readFile, writeFile }  = require('fs');

const fn = process.env.VARS

module.exports = {
    readConfig: () => {
        return new Promise((resolve, reject) => {
            access(fn, constants.F_OK, (err) => {
                if (err) {
                    reject(err);
                    return;
                }

                readFile(fn, (err, data) => {
                    if (err) {
                        reject(err);
                        return;
                    }

                    resolve(JSON.parse(data));
                });
            });
        });
    },

    writeConfig: (cfg) => {
        return new Promise((resolve, reject) => {
            access(fn, constants.F_OK, (err) => {
                if (err) {
                    reject(err);
                    return;
                }

                writeFile(fn, JSON.stringify(cfg, null, 2), (err) => {
                    if (err) {
                        reject(err);
                        return;
                    }
                    resolve();
                });
            });
        });
    }
}
