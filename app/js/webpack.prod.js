const scalajsConfig = require('./scalajs.webpack.config');
module.exports = { ...scalajsConfig, mode: 'production', resolve: { ...scalajsConfig.resolve, fallback: { fs: false } } };
