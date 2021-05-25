const path = require('path');

module.exports = {
  entry: {
    app: './ui/app.js'
  },
  output: {
    path: path.resolve(__dirname, 'public/js'),
    filename: '[name].js',
  },
  module: {
    rules: [
      {
        test: /\.js$/,
        loader: 'babel-loader',
        query: {
          presets: ['@babel/preset-env', '@babel/preset-react']
        }
      }
    ]
  }
};