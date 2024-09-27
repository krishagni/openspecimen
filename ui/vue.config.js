module.exports = {
  publicPath: process.env.NODE_ENV === 'production'
    ? './'
    : '/',

  devServer: {
    proxy: 'http://localhost:8080/openspecimen/'
  },

  chainWebpack: config => {
    config.plugin("copy")
      .use(require.resolve("copy-webpack-plugin"), [{patterns: [{
        from: "src/i18n",
        to: "i18n"
      }]}]);
  }
}

