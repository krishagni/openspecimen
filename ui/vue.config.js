module.exports = {
  publicPath: process.env.NODE_ENV === 'production'
    ? './'
    : '/',

  devServer: {
    proxy: 'http://localhost:8080/openspecimen/'
  }
}

