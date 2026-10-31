const scalajsConfig = require('./scalajs.webpack.config');
module.exports = { ...scalajsConfig, resolve: { ...scalajsConfig.resolve, fallback: { fs: false } } };
